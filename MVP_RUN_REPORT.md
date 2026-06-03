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

(Per-phase entries appended below as work proceeds.)
