
# CLAUDE.md — FollowThru (com.ideasinc.followthrough)

FollowThru is an existing, live Android app (Kotlin / Jetpack Compose / Material 3) being refined toward a reminders-through-association release. The app name, package, and Play listing are fixed — never propose renaming.

## Standing rule — NEVER touch the Play release identity or signing
Do not modify, regenerate, refactor, or "clean up" any of the following:
- The signing config in `build.gradle(.kts)` — `signingConfigs`, the keystore file/path, key alias, and store/key passwords. The release AAB must stay signed with the existing key; if the signing key changes, Play Console rejects the upload as a signature mismatch and the app can't be updated.
- The keystore file(s) themselves, and any signing-related entries in `gradle.properties` / `local.properties` / CI config.
- `applicationId` (`com.ideasinc.followthrough`). Changing it creates a brand-new listing instead of updating the existing one.
- `versionCode` and `versionName` — Tim sets these himself. Do not bump or edit them.
- Leave the internal "Grounded" package/naming as-is (kept for Play Console continuity).

If you believe any of these genuinely needs to change to complete a task, STOP and flag it to Tim with the reason — do not change it yourself. No task should ever require regenerating the signing key or changing the app ID.

## Reference docs
- `docs/references/Product_Strategy.md` — the product strategy / vision: purpose, RTA + autonomy thesis (Rogers & Milkman 2016; Calboli 2025), target audience, value proposition, the phased rollout, and the **free, local-only, no-AI MVP** with its Play-Console-only success metrics (30-day retention North Star). The future paid AI tier is vision/north-star, NOT current scope.
- `docs/references/Customer Feedback.csv` — raw beta feedback (uninterpreted). **This grounds the MVP information architecture.** Recurring signals: unclear purpose / "where to start"; reflection questions feel like a burden; inputs you can't see while typing + keyboard occlusion; light/dark mismatch with the store listing; broken landscape; auto-dismissing splash text; wanting to undo an accidental completion; requests for sub-goal steps and a completed-goals showcase.
- `BRAND_STYLE_GUIDE.md` — color / type / **iconography** source of truth (brand wins on brand/color). Raw hex lives ONLY in `ui/theme/Color.kt`; screens consume `MaterialTheme.colorScheme` / `AppColors`.

Current-state truth is the **code itself** (screens, Room schema, navigation) plus `BRAND_STYLE_GUIDE.md` for brand. The reference docs above describe intent/strategy — diff against the existing screens in `com.ideasinc.followthrough` before proposing changes; refine, don't rewrite. **Exception:** `docs/references/MVP_User_Flow_IA.md` is an INTENTIONAL rewrite of the app's navigation, screens, and vocabulary — implement it as specified there, not as a refine-in-place. "Diff against code, refine don't rewrite" still governs everything OUTSIDE that IA replacement.

## Working rules
1. Strategy/feedback docs describe intent and user signal, not this codebase. Diff against existing screens before proposing changes; refine, don't rewrite — EXCEPT the `docs/references/MVP_User_Flow_IA.md` replacement, which is an intentional rewrite of navigation, screens, and vocabulary: implement it as specified.
2. Where a change would alter BEHAVIOR, preserve the known constraints: photo/sound cue types are feature-flagged OFF at launch (current implementations crash); the existing notification-permission round-trip (toggle → system settings → return to the same flow position) must be preserved.
3. Enforce throughout: one-cue principle (a single cue object, never a list); WCAG 2.1 AA (4.5:1 contrast, ≥48dp targets, TalkBack names, 200% text reflow, inputs never hidden by the keyboard); light/dark parity; undoable actions with the 8s snackbar; no guilt/loss language; no timed auto-dismiss.
4. Vocabulary: 'intention' = the thing the user wants to follow through on, and the primary object/UI surface (the Intentions tab). 'cue' = the distinctive trigger attached to an intention. 'reminder' = the fired notification only. 'What worked' = the self-discovery history surface. 'anchor' must not appear in copy, identifiers, routes, or comments — treat any 'anchor' wording in MVP_User_Flow_IA.md as 'trigger'.
5. Progress: a **flexible, forgiving streak is allowed and wanted** (grounded in Katy Milkman's work — flexibility beats rigidity), but **never rigid**. Permitted: a positive follow-through count, a forgiving weekly streak that survives missing a few times, and a never-resetting lifetime total, alongside the 'What worked' self-discovery history. Forbidden: break-the-chain mechanics, reset-to-zero, guilt/loss/anticipatory-failure framing, and flame/badge "don't lose it" pressure. A miss is a non-event — the streak bends, it does not break; gold accent only on the celebratory state.
6. Exclusions are hard for the MVP: no AI/ML features, no accounts/cloud/social, no analytics SDKs. The MVP is free and local-only with **no telemetry** — success is measured only through Play Console aggregates (retention, uninstalls, rating, crash-free). "No tracking" stays literally true. The paid AI tier is future vision, not launch scope.
7. Delivery reliability is the top engineering priority: `AlarmManager.setExactAndAllowWhileIdle` (best-effort fallback to inexact when exact-alarm permission is unavailable), reschedule after fire, re-register on `BOOT_COMPLETED`, survive Doze and OEM battery optimization. A reminders app whose notifications don't fire is untestable.
8. Emoji are user content (cue values, interest chips) — never interface decoration. UI icons: Material Symbols **Rounded** per the icon table in `BRAND_STYLE_GUIDE.md` §5.

## Pre-commit red-team (do this before proposing ANY commit)
Building to spec is not enough — code can be internally consistent yet still have journey, behavior, or viability gaps. Before declaring a feature or the MVP "done," and before proposing a commit, proactively review the change through these lenses and surface gaps (be adversarial — don't cheerlead, don't just confirm it compiles/works):
- **Product UX Designer / UX Researcher** — journey clarity, intuitiveness, blank-slate and dead-end traps, discoverability, accessibility (WCAG AA), real consumer behavior.
- **Sales / Strategy** — does it actually serve the directional outcome (30-day retention) in `docs/references/Product_Strategy.md`?
- **Market Researcher** — fit for the 18–50 privacy-conscious audience, competitive perception, notification fatigue, behavior-change-genre churn.
- **VC / Founder** — market gap, defensibility/moat, monetization path, measurement blindness (no telemetry), solo-dev viability.

Tag each finding **[fix in app] / [your call] / [strategic risk]**, prioritize by impact, recommend the top few, and ask which to implement — don't silently build them all. Do not commit until Tim has decided on the findings and explicitly approved.

## Release gate
Ship when all pass: (1) reminders fire across Doze/battery-optimization/reboot on real devices; (2) cold user states the purpose in 30s; (3) first reminder < 3 min unaided; (4) beta bugs dead (keyboard occlusion, invisible typing, overlap — see Customer Feedback.csv); (5) TalkBack pass on the full spine; (6) light/dark parity incl. store assets; (7) landscape + 200% text; (8) crash-free cue picker; (9) undo + all weekly-progress states correct; (10) tablet pass — adaptive layout (rail, two-column, list-detail), not a stretched phone UI, both orientations and themes.