# CLAUDE.md — FollowThru (com.ideasinc.followthrough)

FollowThru is an existing, live Android app (Kotlin / Jetpack Compose / Material 3) being refined toward a reminders-through-association release. The app name, package, and Play listing are fixed — never propose renaming.

## Reference docs (target-state inputs — NOT current state)
- `docs/references/lean-prd.md` — release scope, metrics, redesign principle, readiness gate, explicit cuts
- `docs/references/executive-summary.md` — business outcomes, research grounding, four-tier metrics framework
- `docs/references/ANDROID_HANDOFF.md` — Loveable prototype structure: screens, components, data model, streak state machine, design tokens, icon mapping, AA checklist
- `docs/references/data-model.json` — target schema with realistic seed data
- `docs/references/loveable-prompt-combined.md` — the prompt that produced the prototype (context only)
- `docs/references/measurement-options.md` — free/paid × local-only/shared measurement matrix; launch on option A (free, local-only), B (voluntary share) as fast-follow
- `docs/references/attack-mode-playbook.md` — the input-tier / red-team / cheapest-medium operating protocol governing how these docs were produced

## Current-state truth
- `BRAND_STYLE_GUIDE.md` — color/type source of truth; raw hex lives ONLY in `ui/theme/Color.kt`; screens consume `MaterialTheme.colorScheme` / `AppColors`
- If reference docs conflict: the style guide wins on brand/color, the PRD wins on scope, and `ANDROID_HANDOFF.md` is the tiebreaker on flows, copy, IA, and components — it is the latest canonized target. `executive-summary.md` and `loveable-prompt-combined.md` are upstream/context and may lag; see ANDROID_HANDOFF.md §10 for the recorded reconciliation decisions.

## Working rules
1. The reference docs describe the TARGET, not this codebase. The Loveable prototype has never seen this code — diff against existing screens in `com.ideasinc.followthrough` before proposing changes; refine, don't rewrite.
2. Where prototype and current app differ on BEHAVIOR (not just visuals), check the PRD constraints before assuming the prototype wins. Known cases: photo/sound cue types are feature-flagged OFF at launch (current implementations crash); the existing notification-permission round-trip (toggle → system settings → return to same flow position) must be preserved.
3. Enforce throughout: one-cue principle (single cue object, never a list); WCAG 2.1 AA per the handoff checklist; light/dark parity; undoable actions with the 8s snackbar; no guilt/loss language; no timed auto-dismiss.
4. Vocabulary: "reminder" = the object noun in all UI; "cue" = the trigger component only; "intention" = the When/I-will pairing; "anchor" must not appear in copy, identifiers, routes, or comments.
5. Streaks: ONLY the flexible weekly streak per ANDROID_HANDOFF.md §4a (Monday 00:00 local boundary, 2 auto-applied passes, lifetime count never resets, exact copy strings). No rigid break-the-chain mechanics.
6. Exclusions are hard: no AI features, no accounts/cloud/social, no analytics SDKs at launch. `local_metrics` stays on-device, never transmitted automatically. Measurement is consent-tiered (Play aggregates + on-device counters + a voluntary, payload-visible "share my stats" path as fast-follow); see measurement-options.md. "No automatic tracking" is the promise — voluntary user-initiated share is allowed, an analytics SDK is not without an explicit privacy-policy decision.
7. Delivery reliability is the top engineering priority: `AlarmManager.setExactAndAllowWhileIdle`, reschedule after fire, re-register on `BOOT_COMPLETED`, survive Doze and OEM battery optimization. A reminders app whose notifications don't fire is untestable.
8. Emoji are user content (cue values, palette items) — never interface decoration. UI icons: Material Symbols Rounded per the handoff icon mapping table.

## Release gate (from lean-prd.md)
Ship when all pass: (1) reminders fire across Doze/battery-optimization/reboot on real devices; (2) cold user states the purpose in 30s; (3) first reminder < 3 min unaided; (4) beta bugs dead (keyboard occlusion, invisible typing, overlap); (5) TalkBack pass on the full spine; (6) light/dark parity incl. store assets; (7) landscape + 200% text; (8) crash-free cue picker; (9) undo + all streak states correct; (10) tablet pass — adaptive layout (rail, two-column, list-detail), not a stretched phone UI, both orientations and themes.
