# Spec: Intention "Direction" + periodic progress check-in

**Status:** approved-in-principle spec, NOT yet built. Forward-looking feature for the MVP's
Progress surface. Implement only after the open decisions (§9) are confirmed. Must honor every
MVP guardrail (local-only, no telemetry, no guilt, autonomy-first, accessibility, no 'anchor').

---

## 1. Concept
Each intention can optionally name the **direction** it serves — the bigger thing it's in
service of ("be a calmer parent," "get strong again," "finish the thesis"). Occasionally the app
asks whether that direction feels like it's moving, turning blind follow-through into *"is this
actually working, or do I need to change something?"* The difference between "I tapped Did it 12
times" and "I'm becoming who I wanted to be."

**Naming:** "direction" in UI copy. Never "goal" (the goal row stays hidden; goal-tracker framing
is off-thesis). 'anchor' is banned (rule #4).

## 2. How it serves the directional outcome (metrics) — the frank version
The MVP's North Star is **30-day retention**, measured **only** through Play Console aggregates
(retention, uninstalls, rating, crash-free). There is **no telemetry**, so we will **never see**
check-in responses — they are not a metric we collect. The check-in is therefore a *retention &
meaning* feature whose only measurable effect is indirect, via retention. It aligns three ways:

1. **Meaning beats counters.** Behavior-change apps churn when they feel like a hollow tally.
   Tying follow-through to a direction the user chose is what makes it feel worth returning to.
2. **The tweak loop keeps intentions effective.** "Not yet" → a concrete "adjust the cue/moment"
   path means intentions that aren't working get fixed instead of abandoned → habit forms → retention.
3. **The insight → new-intention loop is the most retention-aligned piece** (see §5, option 4):
   capturing a learning and optionally spinning it into a *new* intention is the core return loop
   — the app becomes generative and grows with the user, rather than static.

**Implication for design:** rarer is safer (over-prompting is the #1 churn risk — beta feedback
flagged reflection as a burden), tone must be gentle, and the **insight capture should be treated
as the priority element**, not an afterthought. The on-device check-in/insight history exists for
the **user** (and the future AI), never for our metrics.

## 3. Data — mostly already there
- **Direction text** → reuse `Goal.whyItMatters` (exists in v40, currently unused by MVP UI).
  **No migration to store the direction itself.**
- **Check-in + insight history** → a small new table is now warranted (we're storing free-text
  insights, §5 option 4, which must be a list, not one overwritten value):
  `DirectionCheckIn(id, goalId, askedAt, answeredAt, feeling, noteText?)`. Migration v40→v41
  (Room schema version — NOT the app `versionCode`, so in-bounds). This history is the longitudinal
  signal the future AI wants (how the felt-progress changed, and the user's own-words learnings).
- Track `directionLastReviewedAt` (derivable from the table's latest row per goal — no extra column needed).

## 4. Capture (create flow)
- One **optional** field on **Step 0** (the intention-name step) — not Step 2 (cue), the known stall point.
- Label: *"What's this in service of? (optional)"* + a hint example + a clear **Skip**. Keyboard-aware
  scrolling so the field is never hidden (the recurring beta bug).
- **Blank = no direction = never triggers a check-in.** The whole feature is opt-in and self-selecting.
- Editable later from the intention's edit screen.

## 5. The periodic check-in (the heart)
- **Where:** a card on the **Progress tab** — never a notification (cues own notifications; no reflection nags).
- **When:** the **first** check-in lands **early — around day 7–10** after a direction is set (gated on a
  little real activity to reflect on), deliberately *before* the ~2-week uninstall cliff so it can re-engage
  a wavering user with meaning. **After that it's rare — ~every 3–4 weeks** per direction. One at a time.
  Dismissible. **No timed auto-dismiss.** (Early first touch, then infrequent — catch them before the cliff,
  then don't become a burden.)
- **Prompt:** *"You set this up to **be a calmer parent**. Feel like you're getting there?"*
- **Three responses** — kept few and warm, not a survey:
  1. **"Yeah, getting there"** → warm acknowledgement; log; next check-in ~3–4 weeks out.
  2. **"Not really"** → honest + actionable (Fishbach): *"Good to know — that's the useful signal.
     Sometimes the cue or the moment needs a tweak more than more willpower does. Want to adjust
     this intention?"* → deep-link to edit. Log.
  3. **"Learned something"** → expands a clean inline text field for the user's own words (a moment they
     had no cue for, or something realized afterward). On save, a **quiet, always-available
     "Make this an intention" link** sits beneath the note — never a popup, never forced; tapping it opens
     the create-cue flow **seeded** with the note. Log it in `DirectionCheckIn.noteText`. This is the
     generative loop (and the most retention-aligned element — see §2).
- The three are a clean, equal, warm set; #3 expands its field inline rather than navigating away.
- **Tone:** no guilt, no "failing." "Not really" and learnings are useful information that leads to a
  concrete next step, never shame.

## 6. Display of direction & insights
- Subtle "Toward: …" line on the **intention card** and **edit screen**. **Not** on the in-the-moment
  screen (stays cue + intention + Did it).
- Past **insights** surface lightly under the relevant direction on the Progress tab (a short, reusable
  "things you've learned" list, each optionally convertible to a new intention). Keep minimal; a full
  insights browser is out of scope for v1.

## 7. Guardrails (all honored)
Optional/skippable · autonomy-first · no guilt/loss framing · no timed auto-dismiss · WCAG AA +
keyboard-occlusion fix · local-only, no telemetry · in-the-moment screen untouched · no 'anchor' ·
Room migration only (not app version) · rare cadence (burden is the churn risk).

## 8. Deliberately NOT in scope (stays FollowThru, not a goal app)
No areas/folders, no multiple goals, no %-to-goal progress bars, no dashboards, no required fields,
no per-direction streaks, no notification-based prompts. One optional line, one occasional kind
question, and an optional own-words insight that can spark the next intention.

## 9. Decisions (resolved 2026-06-17)
1. **Cadence** — first check-in early (~day 7–10, before the ~2-week uninstall cliff), then ~3–4 weeks. ✓
2. **Scale** — three warm options: "Yeah, getting there / Not really / Learned something." ✓
3. **Insight → intention** — a quiet, always-available "Make this an intention" link under a saved note;
   never a popup, no settings toggle. ✓
Remaining nit: exact first-check-in day (7 vs 10) and the activity gate — settle at build time.

## 10. Future-AI value (to fold into future-ai-notes.md on build)
Direction + a time-series of felt-progress + own-words insights gives the AI: what each intention is
*for*, the user's outcome signal beyond raw counts, which cues/timings correlate with *felt* progress,
and a seed bank of user-authored learnings to suggest future cues from — all on-device.
