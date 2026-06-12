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
- **#6 Goal framing (Fishbach).** Goal-creation placeholder reframed to an aspirational/approach example ("What you want to move toward — e.g., 'be heard at work'"), a soft hint only — no blocker/lint, Q1 wording unchanged.
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

### Phase 4 — Accessibility deep pass
The app already carries strong TalkBack semantics throughout; this pass confirmed the existing screens and brought the new/changed screens up to the same bar. **Code-level remediation (per screen):**
- **Onboarding** — headings on titles; progress dots are a polite live region announcing "Step n of 2"; primary button + swipe carry contentDescriptions; app-lock `Switch` has `Role.Switch` + on/off state. New "See an example" toggle has `Role.Button` + click label; revealed block is one merged node. Lead framing is a polite live region.
- **Launch Insight** — whole screen is a single node reading only the insight; `clearAndSetSemantics`; auto-dismiss timer skipped when an a11y service is active (read at own pace, tap to continue).
- **Home / List** — wordmark+tagline merged into one heading ("FollowThru — in every moment"); FAB, settings, search all labeled; drag handle is decorative (`clearAndSetSemantics{}`) with explicit ≥48dp reorder arrow buttons + "move to position" dialog for non-gesture users; stat chips merged.
- **New Goal Flow** — step label is a polite live region; question headings; `imePadding()`.
- **Goal Detail** — back/edit/delete labeled; the follow-through button announces its state and "double tap to undo"; reassurance overlay is one node. (Steps UI removed.)
- **Check-In (rebuilt)** — "Check in" heading; read-only context is one merged node ("Your goal… Your plan…"); each field carries its question as `contentDescription`; "Reflect more" toggle has `Role.Button` + label; Save is full-width ≥48dp; `imePadding()` on the scroller + Save bar.
- **Settings** — switches have role+state; theme choices are a `selectableGroup` of radio rows; day chips are ≥48dp with selected state; "Send feedback" row has `Role.Button` + click label.
- **Stats** — heading; stat rows merged ("label value"); "Your FollowThrus" row has `Role.Button` + click label.
- **Your FollowThrus (new)** — heading; back labeled; each record card is one merged node (title + plan + what-you-did + date) with `Role.Button` "Open goal".
- **Customize Questions / Check-In Read** — unchanged; retain existing semantics.

**Deferred to the testing week (require a real device / can't be auto-verified here):** dynamic type at 200% with no clipping on every screen; measured AA contrast sweep in light + dark; visible focus-indicator sweep; explicit reduced-motion handling for the onboarding swipe-hint/fade and card-drag scale animations. Targets are ≥48dp and tokens route through `AppColors` (see Phase-2/Design notes).

---

## Automated verification — results

| Check | Result |
|---|---|
| `:app:compileDebugKotlin` | ✅ PASS |
| `:app:assembleDebug` (debug APK) | ✅ PASS — `app/build/outputs/apk/debug/app-debug.apk` produced |
| `:app:testDebugUnitTest` (unit + Robolectric) | ✅ PASS — `tests=2, failures=0, errors=0` |
| **🔒 BLOCKING migration test (v26→v27)** | ✅ **PASS** — populated v26 DB upgrades with `goals`/`check_ins`/`question_labels` intact; `steps`/`notes`/`follow_throughs` dropped (see Phase 5) |
| `:app:lintDebug` (incl. a11y rules) | ✅ PASS — **0 errors, 68 warnings**; no accessibility errors. Warnings are routine: `GradleDependency`/`UseTomlInstead` (newer dep versions available), `ObsoleteSdkInt`, `ConstantLocale`, `MonochromeLauncherIcon`, `DataExtractionRules`, etc. |
| **Privacy check** | ✅ PASS — no network/analytics/location/Firebase/OkHttp/Retrofit anywhere. Source matches for `http(s)` are only XML namespace URIs + the user-tapped privacy-policy link. |
| Manifest permissions (merged) | `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`, `USE_BIOMETRIC` + `USE_FINGERPRINT` (pre-existing biometric lib), and the internal `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (AndroidX, signature-level). **No INTERNET, no location** — the Play In-App Review library added none. |
| "Your FollowThrus" present & honest | ✅ reachable only via an explicit Stats row, never auto-surfaced, no "proof you can" framing, not a done-tasks bucket; no done-tasks bucket on Home. |
| Reminder scheduling test | ⚠️ Not added — global reminder unchanged this run; intention-anchored reminders were deferred (D3.1), so no new scheduling logic to test. |

**Reminder/instrumented note:** no emulator or `adb` exists in this environment, so the device-based upgrade test and any instrumented (`androidTest`) tests can't be executed here. The blocking migration is proven via the Robolectric JVM test instead; an `androidTest` dependency set is wired for the testing-week device run.

---

## Human verification checklist — auto cannot complete these (testing week)
- [ ] **Real-device matrix:** Pixel phone + 7" and 10"+ tablets, portrait + landscape — first-run path, no stretching on tablet, keyboard never covers fields, check-in ≤ 4 taps.
- [ ] **Manual TalkBack sweep** on all 8 screens (+ Your FollowThrus, Customize Questions, Check-In Read).
- [ ] **Dynamic type @ 200%** with no clipping on every screen.
- [ ] **Dark-mode visual sweep** (Light/Dark/System) on every screen, incl. Launch Insight + Your FollowThrus; measured **AA contrast**.
- [ ] **Reduced-motion**: verify onboarding swipe-hint/fade + card-drag scale respect the system setting.
- [ ] **Real upgrade test:** install prior **v26** build, upgrade in place; confirm `Goal`/`CheckIn`/`QuestionLabel` survive and `Step`+legacy tables are gone — no crash/data loss — **and that an already-onboarded user sees the redesigned onboarding** after upgrade (onboarding version 93→94).
- [ ] **Visible focus indicators** sweep (keyboard/switch navigation).
- [ ] **Play In-App Review** actually surfaces after ≥3 check-ins on a Play-installed build (Google rate-limits; verify wiring, not guaranteed appearance); **Send feedback** opens the mail app to `ideas@insightsemerge.com`.
- [ ] **Signed release AAB**, version bump (already `versionCode=2`, `versionName=1.1`), then Play steps: Data Safety = zero data collection, content rating Everyone, category Productivity, no ads, phone + tablet screenshots, "not a substitute for professional advice" disclaimer present.
- [ ] **(Deferred features to finish or descope before GA):** intention-anchored per-goal reminders (D3.1); optional adjust-the-plan-after-miss nudge (D3.2); "in every moment" tagline on remaining secondary screens (D2.2).

### Store-listing positioning (hand to listing author)
Lead the title/short description and first screenshot with the **follow-through identity** — "we all set goals; following through is the hard part" — so arrivals understand this is the *follow-through* tool, not a planner/to-do/tracker. Own "follow-through" positively; **do not** add a defensive "not a task manager" line. **No pricing or "free/free-forever" copy.** Use Play tags (habits, motivation, self-improvement).

---

## Ready-for-testing-week status
**This is "ready for your testing week," NOT "ready to ship."** The build compiles, lints clean (0 errors), assembles a debug APK, and the **blocking v26→v27 migration test passes**. Privacy-first architecture is intact (no network/analytics/location added). Phases 1, 2, 3 (less the deferred anchored reminders), 4 (code-level), and 5 are complete and committed one-per-phase. Remaining before GA: the device/visual/upgrade checks above and the two deferred features (D3.1, D3.2). No release AAB is signed and no human/device verification has been performed — those are explicitly handed back.

---

# Follow-up run — Per-goal intention reminders + onboarding example (2026-06-03)

Branch: `followthru-pergoal-reminders` (off `mvp-launch-fixes`). One commit per section. This run implements the previously-deferred **D3.1 intention-anchored per-goal reminders** plus the onboarding worked-example refresh.

## What shipped

1. **Per-goal implementation-intention reminders (additive; reuses existing plumbing).**
   - New `GoalReminderScheduler` persists **one reminder per goal in SharedPreferences** (`grounded_goal_reminders`, keyed by `goalId`), scheduling one exact alarm per selected weekday. It reuses the global reminder's `setExactAndAllowWhileIdle`, the shared `canScheduleExactAlarmsCompat` permission check, and the shared `computeNextTriggerMs` — **no new alarm infrastructure, no new permission.**
   - `ReminderReceiver` now branches on a new `ACTION_GOAL_REMINDER`: it posts a notification whose body **surfaces that goal's implementation intention** (BigText), under a per-goal notification id, then reschedules that goal+day for next week. `BootReceiver` reschedules per-goal reminders alongside the global one.
   - **"Remind me for this"** UI (time + day-of-week, local notification only) on **Goal Detail** and as an **optional step in the New Goal Flow** once a plan is written. The global reminder is untouched and still works.
   - The exact-alarm permission gate is **reused, not duplicated**: the permission flow, time-picker button, day chips and the two permission dialogs were extracted from `SettingsScreen` into a shared `ReminderControls.kt` that both the global reminder and per-goal reminders use. The "go to settings, come back" exact-alarm flow and POST_NOTIFICATIONS request are the same code path.
   - **No orphans:** deleting a goal removes its reminder (cancels alarms + clears its prefs entry); abandoning the New Goal Flow removes any reminder set up in-flow.

2. **Onboarding "See an example" → health example.** Goal "Take care of my health"; Plan "When I pour my morning coffee → I will make a breakfast I actually look forward to"; goal-vs-plan caption. Still optional, behind the "See an example" toggle, off the required path; a11y description updated.

3. **Onboarding increment.** `CURRENT_ONBOARDING_VERSION` bumped **94 → 95** so already-onboarded installs re-see onboarding once on upgrade. **No user data is reset** — only the onboarding-seen gate advances.

## ⚠️ Premise correction (surfaced before implementing)
The task brief assumed the Room schema "already defines a reminder with `goalId`, `anchoredToIntention`, `time`, `daysOfWeek`, `notification.body`" and that the goal stores a structured **cue/action**. Neither exists: the app's only reminder was a *single global* reminder in SharedPreferences, and a goal's implementation intention is a *single free-text string* (`CheckIn.implementationIntention`), not split cue/action. With the user's go-ahead, per-goal reminders were persisted in **SharedPreferences keyed by `goalId`** (no schema change, leaving the v26→v27 migration and its test untouched), and the notification body surfaces the **single free-text intention** (no cue/action was fabricated).

## Automated verification — results

| Check | Result |
|---|---|
| `:app:compileDebugKotlin` | ✅ PASS |
| `:app:assembleDebug` | ✅ PASS |
| `:app:testDebugUnitTest` | ✅ PASS |
| **BLOCKING v26→v27 migration test** | ✅ **PASS** — `Migration26To27Test`: `tests=2, failures=0, errors=0` (undisturbed) |
| `:app:lintDebug` (incl. a11y rules) | ✅ PASS — **0 errors**; no new accessibility errors. New UI routes colour through `AppColors` and keeps ≥48dp touch targets (shared day chips + switches + time-picker button). |
| Privacy grep | ✅ PASS — no INTERNET / location / analytics / Firebase / GMS references in source. |
| Manifest permissions (merged) | Unchanged set: `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED` (+ pre-existing biometric-lib & AndroidX signature permissions). **No new permission.** Only manifest change: added the internal `GOAL_REMINDER` action to the existing (non-exported) `ReminderReceiver` filter. |

## Needs a real-device check (no emulator/adb here)
- **Per-goal reminder set → fires at the chosen time:** the scheduling/permission/boot/notification paths are wired and unit-compiled, but the end-to-end "alarm actually fires the right notification at the chosen day/time, survives reboot, and respects per-goal enable/disable" path **must be verified on a real device** — it cannot be exercised in this environment.
- **Onboarding bump 94→95:** existing closed-testing users **will see onboarding once more** after upgrading in place (no data reset). Confirm the redesigned slide + refreshed health example appear on a real in-place upgrade.
