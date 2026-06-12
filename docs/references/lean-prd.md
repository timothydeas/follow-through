# Lean PRD — FollowThru: Reminders-Through-Association Release (existing app, package `com.ideasinc.followthrough`)

**Version:** 1.0 · **Owner:** Senior PM · **Ship target:** readiness-gated (see Constraints) — closed test → staged production · **Platform:** Native Android (phone + tablet)

---

## Problem
People fail to follow through on intentions primarily because they forget at the actionable moment — not because they lack willpower — and FollowThru's beta proved that an unclear purpose, heavy reflection, and inaccessible UI kill engagement before the science can work (43% "where to start," 43% reflection burden, 43% accessibility, 29% layout). The MVP boundary is the single spine — goal → relevant self-knowledge → implementation intention → one user-chosen distinctive cue → reminder surfaced at the moment — with zero AI, zero accounts, zero cloud.

## North Star Metric
**Day-7 device retention** (Play Console aggregates — privacy-first, no *automatic* transmission). Target: **≥ 12%** vs. ~5.15% Android average (Pushwoosh 2025) and ~10.7% global average. Measurement is consent-tiered, not absolutist: Play aggregates (anonymous) + on-device counters (never auto-sent) + a voluntary user-initiated "share my stats" path (fast-follow) + survey re-run. Launch on the free local-only option; see measurement-options.md for the free/paid × local-only/shared matrix and revisit triggers. "No tracking" stays literally true.

## Leading Indicators
1. **First-session activation:** % of new users who create their first cue-attached reminder in session one, in under 3 minutes (target ≥ 60%) — measured by local, on-device counter.
2. **Follow-through rate:** % of delivered reminders marked "Done" within 24h (local counter; this is the live test of the core hypothesis — Rogers & Milkman saw 74% vs 42% with distinctive cues).
3. **Palette-drawn cues:** % of reminders whose cue was selected from the user's own self-knowledge palette rather than a generic default (proxy for "users find value entering passions/interests/learnings").

## Counter Metrics (Guardrails)
1. **24h uninstall rate ↓** vs. FollowThru baseline, and **Play Store rating ≥ 4.0** (crash-free ≥ 99% as a floor).
2. **Notification opt-out / snooze-forever rate ≤ 15%** — the autonomy guardrail. If users are muting us, the cue is a nag, not a reminder (Calboli: interventions must remain "easy and cheap to avoid").

## Persona — "Maya, 32, intends-but-forgets"
- **Who:** Busy professional with real intentions (gym after work, call mom, submit the form, take the vitamins) and a phone full of ignored generic reminders.
- **What she feels:** Quiet guilt and self-blame — she reads her forgetting as a willpower failure, and apps that interrogate her with reflection questions make it worse.
- **What she needs:** To be reminded *at the moment she can act*, by something that feels like hers — one vivid, personal trigger plus plain text that tells her exactly what to do — with the freedom to say "not today" without punishment.

## Top 3 Jobs to Be Done (user's voice)
1. "When I set an intention, help me **tie it to something I'll actually notice in the moment**, so I stop dropping the things I genuinely meant to do."
2. "Help me **use what I already know about myself** — what I love, what's worked before, what trips me up — so the reminder feels like mine, not an app nagging me."
3. "When the moment comes, **surface it fast and let me act or decline in one tap**, so it helps me follow through without taking over my day."

## Redesign Principle (every MVP element must satisfy)
**"Purpose visible, one cue, full meaning in text, always declinable":** every screen must move the user one step along the spine, state why it matters, carry complete meaning in plain accessible text (never in a single sense or channel), and leave the user one effortless tap from saying no.

## MVP Features (3 max)

### 1. Guided Reminder Builder (the spine)
A 4-step flow: name the goal → surface relevant self-knowledge (palette items + this goal's barriers) → write the implementation intention as "When **[moment]**, I will **[action]**" → choose exactly **one** distinctive cue (emoji, short vivid phrase, photo, or sound) and set when it surfaces. Includes a 30-second worked example on first run (fixes "unclear where to start," 43%) and goal templates to beat the blank-slate problem.
**Metric impacted:** First-session activation (leading #1); Day-1 retention.

### 2. Self-Knowledge Palette ("About You" + per-goal context)
Lightweight capture, never interrogation: passions/interests and learnings ("what's worked before") live on the person and are reusable across goals; barriers and progress live on each goal. Each entry exists only to make cues more distinctive and intentions better-fitted — entries are tappable chips inside the Reminder Builder. This is also the local data corpus the future AI iteration will draw from.
**Metric impacted:** Palette-drawn cues (leading #3); Day-7 retention.

### 3. Cue-Fire Delivery + Lead-Light Check-In
The notification shows the one cue **plus** the full intention text (text always carries complete meaning — accessibility and the brief's multi-sense principle). One tap: **Done / Snooze / Not today** — all undoable (fixes the accidental-completion complaint), all judgment-free (Milkman's what-the-hell balance: celebrate the win, forgive the miss, no broken-streak shaming). "Reflect more" is optional and collapsed by default (fixes 43% reflection burden).
**Metric impacted:** Follow-through rate (leading #2); 24h uninstall; rating.

## Out of Scope (explicit cuts)
1. **All AI/ML** — no cue suggestion, no generated copy, no real-time inference. The MVP tests whether the *human-authored* mechanism works; automation is the deliberate next iteration (per brief and future-state plan).
2. **Sub-goal cascades / milestone trees and the completed-goals showcase** (beta request #3) — valuable, but a goal gets a simple progress note in MVP; hierarchies expand scope past the hypothesis.
3. **Accounts, cloud sync, social, and rigid streak mechanics** — local-only by principle; rigid break-the-chain streaks are *replaced* (not deferred) by a flexible weekly streak that survives ≤2 misses via auto-applied emergency passes, plus a never-resetting lifetime follow-through count and fresh-start copy on a failed week (Milkman; Sharif & Shu; Dai, Milkman & Riis).

## Constraints
- **Release trigger — readiness gate, not a date.** Sequence: Loveable prototype → reference build in Android Studio → internal testing → small *fresh* closed test (testers who never saw the prior beta) → identical survey re-run (fill the before→after scorecard column) → staged production rollout. Launch when the gate passes: (1) reminders reliably fire across Doze, battery optimization, and reboots on real devices — the non-negotiable; (2) cold user states the app's purpose within 30s; (3) first reminder created <3 min unaided; (4) beta bugs verified dead (keyboard occlusion, invisible typing, overlap); (5) TalkBack pass on the full spine; (6) light/dark parity incl. store assets matching the shipping app; (7) landscape + 200% text intact; (8) crash-free cue picker; (9) undo + all progress/streak states correct (all met / passes used / streak ended / no data); (10) tablet pass — a beta tester reported an awful tablet experience in the current app, so verify on a tablet emulator/device that Medium/Expanded WindowSizeClass renders the adaptive layout (left nav rail, two-column Today/Goals grid, list-detail Goal screen) rather than a stretched phone UI, in both orientations and themes. Note: Play's closed-test requirement is already satisfied by the prior 14-day beta; production submission can proceed as soon as the gate passes, with the fresh-tester survey re-run folded into staged rollout.
- **Team:** Solo PM building with Claude Code, Loveable, GitHub Copilot; no backend engineers — therefore **no backend** (Room/DataStore local persistence, WorkManager/AlarmManager for delivery).
- **Technical:** Native Kotlin/Jetpack Compose, phone + tablet layouts, portrait + landscape (beta found landscape broken), Light/Dark/System parity, WCAG 2.1 AA (TalkBack, 4.5:1 contrast, dynamic text size, keyboard never occludes inputs), notifications must fire offline.
- **Cue types at launch — emoji and phrase only.** Photo and sound exist in the current FollowThru app but crash or fail; feature-flag them OFF for this release and stabilize as a fast-follow (suspects: Photo Picker / content-URI permission persistence; notification-channel sound config). A crashing picker inside the first-reminder flow directly threatens the crash-free ≥99% and rating ≥4.0 guardrails. The Loveable prototype shows all four types as the *target* state — not license to ship them broken.
- **Preserve existing notification-permission flow.** Current FollowThru behavior: reminder toggle → routes user to system settings to enable notifications → returns them to the same in-app flow location. This contextual, no-dead-end permission round-trip is correct (Android 13+ POST_NOTIFICATIONS) and must survive any refactor; the web prototype cannot model it, so its absence there is not a signal to remove it.

## Risks to the Hypothesis (falsification guide)
Attack-mode labeling: assumptions the design rests on that are NOT directly evidenced by the fixed inputs (business outcomes, beta feedback, cited research). Each carries the launch-data pattern that would break it and the correct response — so a weak result triggers diagnosis, not abandonment.
1. **Users can self-author distinctive cues.** ASSUMPTION. Rogers & Milkman's cues were researcher-designed (the alien, the elephant); we assume users prompted by their own palette pick cues distinctive *enough*. If follow-through is flat → suspect cue quality before the mechanism; response is cue-crafting guidance or curated suggestions, not cutting the feature.
2. **Clock-time scheduling approximates "the moment."** ASSUMPTION. The science fires cues at the actionable moment; the MVP fires at a user-predicted clock time. Mediocre results are partially confounded by timing error — the exact gap the future AI closes. Don't kill the mechanism for the scheduler's limits.
3. **Privacy-first measurement can detect the effect.** ASSUMPTION. Small n + aggregate-only data can hide a real but modest effect. Ambiguous results may mean "more users / more closed testers," not "wrong mechanism." (See measurement-options.md.)
4. **One cue beats many.** ASSUMPTION (extrapolation, not a tested finding). R&M never tested stacking; concurrent-distinctiveness logic supports one cue but does not prove it. Defensible; hold unless data suggests otherwise.

## Open Questions (≤3)
1. Is **time-based scheduling plus a user-described moment** enough for MVP, or do "moment" reminders need location/context triggers to validate the hypothesis? (Recommendation: time + moment description only; context triggers ride with the AI iteration.)
2. What is the **minimum viable palette** — do we require ≥1 passion entry before the cue step, or allow generic cues and measure the delta? (Measuring the delta gives us a natural baseline for the hypothesis.)
3. How do we re-run the **identical beta survey** at staged rollout without an in-app prompt feeling like the interrogation we just removed? (Candidate: single, dismissible card on day 6.)
