# FollowThru MVP — Usability Test Plan

**Prepared by:** UX Research
**Scope:** Free, local-only, no-AI MVP (no accounts, no cloud, no telemetry)
**Method:** Moderated, think-aloud, task-based usability test + post-test SUS
**Companion use:** Part B is written so Claude Code can run a structured build evaluation against the latest build before live sessions.

---

## 1. Background

FollowThru helps people follow through on their own intentions by pairing each one with a **distinctive cue** that surfaces near the moment they can act. The product thesis rests on three things: reminding at the action moment (Rogers & Milkman's "reminders-through-association"), a *distinctive* cue beating a generic one, and an autonomy-first, privacy-first posture — local-only data, no nagging, no guilt mechanics.

This test validates whether new users can understand the app, build a cue, respond in the moment, and discover value — without hand-holding. It directly probes the loudest signals from beta: unclear purpose / "where do I start," and friction in the cue-creation and text-input steps.

---

## 2. Research objectives & questions

1. **Purpose clarity** — Within the first minute, does a new user understand what the app is for and what to do first? *(The No. 1 beta complaint.)*
2. **The hero flow** — Can users create an intention and craft a *distinctive* cue without stalling at the cue step?
3. **In the moment** — When a cue fires, do users understand the single "Did it" response, and do they discover that it's undoable?
4. **One-off intentions** — Can users find and create a "Just once" intention with a date?
5. **Self-discovery** — Do users find "What worked," and do they understand what it's telling them?
6. **Autonomy perception** — Does the experience feel respectful and non-coercive, rather than nagging or guilt-inducing?
7. **Trust & privacy** — Do users understand and trust that their data stays on the device?

---

## 3. Method

- **Format:** Moderated, one-on-one, think-aloud. ~45 minutes per session.
- **Setting:** Remote (screen-shared) or in-person, on an Android phone running the latest build. Participants use their own device where possible; otherwise a provided Pixel.
- **Why this method:** The MVP is pre-launch and formative. Think-aloud surfaces *why* users hesitate (especially at the cue step), which task metrics alone won't explain.
- **Moderator stance:** Neutral. No leading. Let participants struggle for a reasonable beat before offering a nudge; record whether a task needed assistance.

---

## 4. Participants

- **Profile:** Adults 18–50 who use a smartphone daily and have tried — and often abandoned — a habit, reminder, or to-do app. Lean toward people who describe themselves as privacy-minded or wary of "nagging" apps, matching the target audience.
- **Sample size:** 5–7 participants for this formative round (small n surfaces the majority of severe issues; recruit a second wave if findings don't saturate).
- **Screen out:** People who built or tested this app, close colleagues, and UX professionals (they don't behave like target users).
- **Incentive:** Standard for ~45 minutes of time.

---

## 5. Setup & materials

- Latest build installed; **app reset to first-run state before each participant** (clear app storage — note the build is on a recent Room DB version, so a clean reinstall avoids migration artifacts during testing).
- Screen + audio recording, with consent.
- Note-taking template (per-task: success, time, missteps, quotes).
- SEQ (Single Ease Question, 1–7) shown after each task.
- SUS (System Usability Scale, 10 items) at the end.
- A way to fire a cue on demand for Task 4 (set a cue ~1–2 minutes out, or a moderator-triggered test notification).

---

## 6. Session flow

| Segment | Time | Notes |
|---|---|---|
| Welcome & consent | 5 min | Purpose, recording consent, "there are no wrong answers; we're testing the app, not you." |
| Pre-test interview | 5 min | Warm-up + context (see §7). |
| Tasks (think-aloud) | 25 min | §8. Capture metrics per task. |
| Post-test: SUS + debrief | 8 min | §9. |
| Wrap | 2 min | Thanks, incentive. |

---

## 7. Pre-test interview (warm-up)

1. Walk me through how you currently remember the things you mean to do but don't always get to.
2. Have you used reminder, habit, or to-do apps? What made you stop using any of them?
3. When you say you want to "follow through" on something, what does that look like for you?

*(Purpose: context + a baseline read on language, before exposure to the product's framing.)*

---

## 8. Tasks

For each task: read the scenario in the participant's terms, stay silent, and observe. Capture **task success** (unassisted / assisted / failed), **time on task**, **misstep count**, and **SEQ**.

### Task 1 — First run (purpose clarity)
**Scenario:** "You just installed this app and opened it for the first time. Go ahead — do whatever feels natural."
**Success:** Participant reaches the point of creating their first cue without the moderator explaining what the app is for.
**Watch for:** Do they understand the purpose from onboarding? Does the worked example land? Where do they hesitate or look lost? Do they say out loud what they think the app does?

### Task 2 — Create a recurring intention with a distinctive cue (the hero flow)
**Scenario:** "Think of something you keep meaning to do regularly but often forget. Set the app up to help you remember it."
**Success:** A recurring intention is created, with a cue the participant authored, scheduled.
**Watch for:** The cue step specifically — do they stall at "design a distinctive cue"? Does the seeded example help or confuse? Do they grasp that a vivid cue is the point, or do they type a flat "reminder"? Any trouble seeing/editing text as they type.

### Task 3 — Create a "Just once" intention (one-off)
**Scenario:** "Now think of a one-time thing coming up on a specific day. Set that up too."
**Success:** A one-off intention with a date is created.
**Watch for:** Do they discover the "Just once" option among the scheduling choices? Is the date picker clear? Do they understand it fires once and then leaves the list?

### Task 4 — Respond in the moment
**Scenario:** (Moderator fires a cue.) "Your phone just buzzed — go ahead and deal with it as you would in real life."
**Success:** Participant opens the in-the-moment screen and responds with "Did it."
**Watch for:** Do they understand the single response? Do they expect more options (and is its absence a relief or a confusion)? Do they discover that "Did it" can be undone? Does the screen feel respectful vs. pushy?

### Task 5 — Find and interpret "What worked"
**Scenario:** "You want to see which of your cues are actually getting you to follow through. Where would you go?"
**Success:** Participant navigates to "What worked" and can describe what it's showing them.
**Watch for:** Findability of the tab; whether the self-discovery value is understood ("these are the cues that work *for me*") or read as a generic history.

### Task 6 — Understand what happens to their data
**Scenario:** "You're curious where your information lives and whether you can get rid of it. Find out."
**Success:** Participant reaches Settings → My data and correctly concludes the data is on-device and deletable.
**Watch for:** Whether the local-only model is discoverable and believable; reactions to "no cloud, no export"; any trust hesitations.

*(Optional Task 7, if time: "Change the cue on one of your intentions" — to test edit/manage from the intention detail.)*

---

## 9. Post-test

**SUS** — Administer the standard 10-item System Usability Scale (alternating positive/negative items, 1–5). Report the 0–100 score; ~68 is average, target ≥ 75 for an MVP this focused.

**Debrief questions:**
1. In your own words, what is this app for?
2. What was the clearest part? The most confusing?
3. The step where you wrote the cue — how did that feel? Did the example help?
4. Did the app ever feel pushy or like it was guilt-tripping you? Where?
5. How do you feel about your data living only on your phone — reassuring, limiting, or something else?
6. Realistically, would you still have this on your phone a month from now? Why or why not? *(Maps to the 30-day-retention directional outcome.)*

---

## 10. Metrics & success criteria

| Metric | How captured | Target |
|---|---|---|
| Task success (unassisted) | Per task | ≥ 80% on Tasks 1, 2, 4 (core loop) |
| Time on task | Per task | No hard target; flag outliers and stalls |
| Misstep count | Per task | Cluster by screen to locate friction |
| SEQ (1–7) | After each task | Mean ≥ 5.5 on core tasks |
| SUS (0–100) | End of session | ≥ 75 |
| Purpose comprehension | Task 1 + debrief Q1 | Majority articulate the purpose unaided |
| Autonomy perception | Debrief Q4 | No participant reports feeling nagged/guilted |

---

## 11. Analysis & reporting

- Rate each issue on a **0–4 severity scale** (0 = not a problem, 4 = usability catastrophe), weighting by frequency × impact.
- Affinity-map qualitative notes into themes.
- Prioritize fixes against the directional outcome (keeping new users through their first 30 days): anything blocking purpose comprehension or first-cue creation outranks polish.
- Deliver: top issues with severity, supporting clips/quotes, and recommended changes; SUS score; a go / iterate read on the core loop.

---

# Part B — Build evaluation for Claude Code

Run this against the **latest build** before scheduling live sessions. Goal: catch the issues a pilot tester would, and confirm every task in Part A is actually completable, so we don't waste participant time on broken flows. **Evaluate and report only — do not modify code, and do not touch anything affecting Play Console (signingConfig, keystore, applicationId, versionCode, versionName, package).**

### B1. Task walkthrough (completability)
For each task in §8, trace the flow through the current build (screens, navigation, state handling) and confirm it can be completed end to end. Flag any dead end, missing state, or step that can't be reached. Note specifically:
- First-run onboarding reaches first-cue creation.
- The create flow runs: "I will…" → How often (Daily / Weekly / **Just once** + date picker) → Design a distinctive cue (seeded example present) → Review.
- A fired cue opens the dedicated full-screen in-the-moment screen with the cue, the intention, and a **single "Did it"** (undoable); no-response logs neutrally.
- "What worked" and Settings → My data (view / delete, no cloud, no export) are reachable.

### B2. Heuristic evaluation (Nielsen's 10)
Walk the main screens and report violations with screen + severity (0–4): visibility of system status, match to real-world language, user control & freedom (incl. the undo), consistency, error prevention, recognition over recall, flexibility, minimalist design, error recovery, help.

### B3. Accessibility audit (WCAG 2.1 AA)
- **TalkBack / content labels** on all actionable controls (buttons, nav, FAB, the Did it action).
- **Touch targets** ≥ 48dp.
- **Contrast** ≥ 4.5:1 text / 3:1 large text & UI, in **both light and dark**.
- **Dynamic type** scales without clipping or overlap.
- **Visible focus** order is logical for keyboard/switch access.
- **Keyboard occlusion:** text fields scroll clear of the keyboard; the field's prompt copy is never covered by the keyboard or a focus/error outline. *(Direct beta regression — verify explicitly.)*

### B4. Beta-fix verification
Confirm each prior beta issue is resolved in the build:
- Onboarding **shows** purpose via a worked example (doesn't just state it).
- The distinctive-cue step is **seeded with a concrete example**.
- Text input is keyboard-aware, visibly editable, no overlap.
- **Undo** is available on "Did it."
- Light/dark treatment is consistent and matches the store screenshots.
- Orientation: landscape is handled gracefully or portrait is locked cleanly — rotation doesn't break layout or strand the keyboard.

### B5. Consistency & tone checks
- **Vocabulary:** intentions and cues throughout; the in-the-moment response is a single **Did it**; **no "Snooze" or "Not yet"** anywhere.
- **Autonomy-first tone:** a missed cue is logged neutrally — no guilt language, no streak/score/"you failed" framing anywhere.

### B6. Output
Produce a findings report: a table of issues with **severity (0–4)**, the screen/flow, a one-line description, and a file/line reference where applicable. Group by task and by check (B1–B5). Recommend nothing that requires touching signing or Play config. Leave the build unchanged.
