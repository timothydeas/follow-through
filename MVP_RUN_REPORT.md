# FollowThru MVP — Autonomous Build Run Report

Branch: `mvp-launch-fixes` (off `main` @ bb41831). One commit per phase.

---

## Step 0 — Exploration findings

**Stack.** Kotlin + Jetpack Compose (Material 3), Compose Navigation, Room v26, manual DI via `AppContainer`. `minSdk=31`, `targetSdk=35`, `compileSdk=35`. `versionCode=1`, `versionName=1.0`. Baseline `:app:compileDebugKotlin` passes (exit 0).

**Permissions (manifest).** `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`. No internet/network, analytics, or location. ✔ privacy-first intact.

**Navigation / screens.** `AppNavigation(container)` with routes: onboarding, launch_insight, list (Home), new_goal, goal_detail, checkin_flow, checkin_read, settings, customize_questions, stats. Onboarding gate: `CURRENT_ONBOARDING_VERSION = 93` in `AppNavigation.kt` (a versioned flag — re-shows onboarding when bumped).

**Room entities.** Live: `Goal` (goals), `CheckIn` (check_ins), `QuestionLabel` (question_labels), `Step` (steps). **Legacy/dead:** `GroundedNote` (notes) + `FollowThroughEntry` (follow_throughs) — these are the *2 legacy tables*. The legacy entities + `NoteDao`/`FollowThroughDao` + `ui/{stepflow,editor,readview,followthrough}` ViewModels/screens form a closed dead-code set referenced only by each other, `AppContainer`, and `GroundedDatabase` (never by `AppNavigation`).

**Follow-through model.** Tracked at **Goal** level: `Goal.followedThrough` + `followedThroughAt`. `CheckIn` has no follow-through column. Undo already implemented (`GoalDetailViewModel.undoFollowThrough`).

**7 reflection questions** (`QuestionKeys`): goalOrChange, madeProgress, avoiding, confidence, competingPriority, implementationIntention, accountability — with default labels + placeholders, fully customizable (enable/disable/edit/placeholder) via `question_labels`.

**Streaks.** `computeStreakWithFlex` already implements Milkman flex-protection (up to 2 missed days bridged). ✔

**Reminders.** Global reminder (time + day-of-week) in Settings via `ReminderScheduler` / `ReminderReceiver` / `BootReceiver`. Local only, no new permissions. Intention-anchored per-goal reminders: NOT yet present.

**Migrations.** Full chain MIGRATION_1_2 … MIGRATION_25_26 present; current version 26.

### Decision log — Step 0
- **D0.1** Phase 1 items "cloud sync", "AI accountability partner", "social sharing" — **already absent** from the codebase (grep for cloud/sync/backup/drive/AI/llm/share/ACTION_SEND/createChooser finds nothing but an unrelated code comment). No removal needed; logged here. The remaining Phase 1 work is Steps and the Active/Completed tabs.
- **D0.2** Step removal split across phases to keep each phase compiling: Phase 1 removes all Step **usage** (Goal Detail UI + VM + DAO wiring) and `StepDao`; Phase 5 removes the `Step` entity from `@Database` and **drops** the `steps` table (plus the 2 legacy tables) in MIGRATION_26_27.
- **D0.3** Home unified list: followed-through goals stay **off** Home (no done-tasks bucket, per spec); they remain reachable via "Your FollowThrus" (Phase 3) from Stats. `ListViewModel` already filters `!followedThrough` for the active list.

---

## CHANGELOG

### Phase 1 — Removals
- **Steps / sub-goals removed.** Deleted the Steps summary/list UI and add/edit/delete dialog from `GoalDetailScreen`; removed all step state + methods from `GoalDetailViewModel` (and `stepDao` from its constructor/Factory); removed `stepDao` wiring from `AppNavigation`, `AppContainer`, and the `stepDao()` accessor from `GroundedDatabase`; deleted `StepDao.kt`. The `Step` entity + `steps` table remain dormant until Phase 5 drops them (keeps each phase compiling).
- **Active / Completed tabs removed.** `ListScreen` is now a single unified goal list (search, ranks 1–3, drag-to-reorder, center FAB, stats chip preserved). Deleted `ListTabBar`, `ListTab`, `CompletedGoalCard`, `CompletedEmptyState`, and the `selectedTab` state. Followed-through goals stay off Home (no done-tasks bucket) — `ListViewModel` already excludes them; they surface in Your FollowThrus (Phase 3).
- **Cloud sync / AI accountability partner / social sharing** — none present in the codebase; nothing to remove (see D0.1).
- Verified: `:app:compileDebugKotlin` passes.

### Phase 5 — Data layer / migration  ✅ (done early to lock the blocking gate)
- **MIGRATION_26_27** added (explicit, **never** `fallbackToDestructiveMigration`): drops `follow_throughs` (child) → `notes` (parent) → `steps`. `Goal` / `CheckIn` / `QuestionLabel` are **not touched**.
- `@Database` bumped **26 → 27**; entities reduced to `Goal`, `CheckIn`, `QuestionLabel`; `exportSchema = true` with `room.schemaLocation = app/schemas` (v27 schema committed at `app/schemas/.../27.json`).
- Removed the `Converters`/`NoteType` type-converter (only the legacy `notes` table used it). Deleted dead legacy code: `GroundedNote`, `FollowThroughEntry`, `Step`, `NoteDao`, `FollowThroughDao`, and `ui/{stepflow,editor,readview,followthrough}` (all were a closed dead-code set, never reachable from navigation). `AppContainer` no longer exposes `noteDao`/`followThroughDao`.
- `versionCode 1 → 2`, `versionName "1.0" → "1.1"` for the in-place update.

> ### 🔒 BLOCKING migration test — **PASS**
> `app/src/test/.../Migration26To27Test.kt` (Robolectric JVM test — runs without an emulator). Builds a **populated** v26 DB by hand (real v26 table shapes + `steps`/`notes`/`follow_throughs` with rows), opens it through Room with `MIGRATION_26_27`, and asserts: `goals`/`check_ins`/`question_labels` rows + fields (incl. a followed-through goal and an implementation-intention string) survive, and `steps`/`notes`/`follow_throughs` are dropped. Plus an empty-DB case.
> **Result: `tests=2, failures=0, errors=0`** via `./gradlew :app:testDebugUnitTest`.

### Decision log — Phase 5
- **D5.1** Did Phase 5 **before** Phases 2–4 to de-risk and prove the blocking gate as early as possible. Phases 2–4 do not touch `Goal`/`CheckIn`/`QuestionLabel` columns, so this reordering is safe. Commits remain one-per-phase.
- **D5.2** Intention-anchored per-goal reminders (Phase 2 #5 / Phase 3 #1) will be stored in **SharedPreferences** (mirroring the existing global reminder), **not** a new Room table — so no further schema change is needed and the v26→v27 migration stays a pure, low-risk table drop. ("add any new reminder fields/tables" → none needed.)
- **D5.3** No emulator/`adb` and no instrumented-test infra exist in this environment, so the blocking test is implemented with **Robolectric** as a local unit test (executable here). An `androidTest` config is also wired for the device-based upgrade test during the testing week.

### Phase 2 — First-session fixes
- **#1 Onboarding re-shows on upgrade (critical).** `CURRENT_ONBOARDING_VERSION 93 → 94` so already-onboarded testers see the redesigned flow once after the in-place upgrade (no data reset). Header tagline changed `Goals & Changes → in every moment`; added the lead framing line ("We all set goals. Following through is the hard part — that's what FollowThru is for…") on slide 1.
- **#2 Worked example.** Optional "See an example" toggle on slide 2 revealing a sample implementation intention ("When standup reaches my turn → I will share the one blocker I wrote down") + caption. Off the required path.
- Slide-2 three-beat reframed to embody the in-the-moment essence: **Name the moment / Decide your move / Follow through**.
- **Home persistent header**: wordmark now carries the "in every moment" tagline (merged into one heading semantics node).
- **#3 Launch Insight curated.** Dropped the overlong "firefighter" message (a tightened version is already in the set). Auto-dismiss + tap-to-continue unchanged.
- **#7 Lead-light check-in (the most-cited friction fix).** `CheckInFlowScreen` rebuilt from a 6-step wall into a single calm page: read-only **goal + your plan (implementation intention)** context at top, a one-line purpose cue, **one** light lead prompt (progress), and the rest (avoiding, confidence, what's-in-the-way, edit-intention, accountability) behind an inviting **"Reflect more"**. Save is always available, so a progress-only (or single-line) check-in works. Form starts blank; only answered questions are stored (`ifBlank{null}`). `CheckInFlowViewModel` now loads the goal title + latest intention for the read-only context.
- **#4 Frictionless, visible-input.** Reflection fields use a **visible, persistent outline border**; `imePadding()` on the scroll container + bottom Save bar keeps the keyboard from covering the active field; check-in is reachable in ≤ 4 taps (Home → goal → + → field).
- **#6 Goal framing (Fischbach).** Goal-creation placeholder reframed to an aspirational/approach example ("What you want to move toward — e.g., 'be heard at work'"), a soft hint only — no blocker/lint, Q1 wording unchanged.
- **#7 q5 refined** to one clean clause: "What's getting in your way — the situation itself, or how you're seeing it?"
- Verified: `:app:compileDebugKotlin` passes.

### Decision log — Phase 2
- **D2.1** q5 reword + goal-framing placeholder were changed in code defaults only (no new schema/migration). Users who never customized these have no `question_labels` row and see the new text immediately; the rare user who toggled-without-editing keeps their stored text. Avoids a v28 churn on the just-proven migration chain.
- **D2.2** Persistent "in every moment" tagline added to onboarding + Home (the cold-start orientation moments). Adding it to every secondary screen's top bar is deferred (high churn, low marginal value) and listed for the testing week.

### Phase 3 — Verify & harden aligned features
- **#5 "Your FollowThrus" (new).** `FollowThrusScreen` + `FollowThrusViewModel` (`ui/followthrough`), reachable via an explicit **"Your FollowThrus ›"** row in Stats — **never auto-surfaced**, not tied to Q4/Q5, makes **no "proof you can"** claim. Each entry shows the moment (goal), the user's own intention, and what they did (their progress note), built only from existing follow-through data. Warm empty state. Tapping opens the goal. Not a done-tasks list.
- **#4 Feedback + Play review (priority) — done.** Added `AppReview` (`feedback/`): Google's **official In-App Review API** is requested after genuine use (≥ 3 check-ins, once ever), **never sentiment-gated** (we never branch on happy/unhappy and never condition on the outcome). Independent **"Send feedback"** mailto row added to Settings — always available, opt-in, no tracking. Deps: `com.google.android.play:review(-ktx):2.0.2` (no data collection of ours).
- **#2 Streaks & setbacks — verified.** Flex-protected streak (`computeStreakWithFlex`, up to 2 missed days bridged) already present and surfaced calmly in Stats with a non-judgmental note. Follow-through mark is already **reversible** (`undoFollowThrough` + undo dialog) and recomputes streaks/totals via the reactive flow. Gold accent reserved for celebration.
- **#3 Customizable questions — verified** present and unchanged (`CustomizeQuestionsScreen` enable/disable/edit label + placeholder; Q6 implementation-intention structurally intact).
- Verified: `:app:compileDebugKotlin` passes (incl. resolving the new Play review dependency).

### Decision log — Phase 3
- **D3.1 (escalation → documented default).** **Intention-anchored per-goal reminders (#1 / Phase 2 #5) are NOT implemented in this run** and are handed to the testing week. Rationale: the global local-notification reminder (Settings) is in place and correct; extending to per-goal scheduling (N alarms, intention-text notification body, deep-link-to-goal, boot reschedule) is a sizable, separately-verifiable feature that I could not implement *and* device-verify within this run without risking half-finished notification code. Per the operating rule I chose the safe, documented default (ship the proven global reminder, defer the anchored variant) rather than land unverified alarm code. No schema was added for it (see D5.2), so adding it later needs no migration.
- **D3.2** The optional "adjust the plan after a missed follow-through" nudge (#2 cognitive-barrier path) is also deferred — there is currently no explicit "missed" event (follow-through is a single goal flag), so a faithful, non-judgmental implementation is a small feature in its own right. Listed for the testing week. The non-shaming, reversible, flex-protected behavior that exists already honors the core of the guardrail.

(Per-phase entries appended below as work proceeds.)
