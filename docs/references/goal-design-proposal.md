# Proposal — Goal creation as want-to building (NOT a reminder input)

**Status:** Proposal / discussion only. Nothing here is built. This is options + tradeoffs
for Tim to react to. Implementation intentions, cues, and reminders are **out of scope**
and stay exactly as they are — the wanting lives in the *goal*, never in the reminder.

**Grounding:** lean-prd "## Thesis" (job #1 = help build goals worth pursuing);
DESIGN_DECISIONS 2026-06-14 "Intrinsic motivation is goal-DESIGN, not a reminder input".

---

## The job

When a user creates a goal, help them leave with a goal that *pulls* them — not just a
title they "should" do. Concretely, four moves (from the thesis):

1. **Find / build the want-to** in the goal.
2. **Reframe a have-to** toward a way they'd actually enjoy.
3. **Connect** the goal to their own passions/interests (the About You palette).
4. **Choose the path that pulls** them (pick among ways to pursue it).

The bar for v1 is low and honest: a couple of light, skippable prompts that nudge toward
want-to, reusing data we already have. Not a wizard, not a quiz (the beta died partly on
"reflection burden" — lean-prd Problem). Everything optional, no guilt, no gate.

---

## Current state (what we're changing)

Three goal-touch points exist today:

- **`NewGoalFlowScreen`** — name-only quick add → "Save & add a reminder". (Primary path.)
- **`ReminderBuilderScreen` step 0 (`StepGoal`)** — inline "+ New goal": title + "Why it
  matters" + templates, created on the way to a reminder.
- **`GoalDetailScreen` EditGoalDialog** — edit title + "Why it matters".

The goal model (`data/Goal.kt`) currently carries: `title`, `whyItMatters`, `priority:
Int?`, follow-through state. We just removed `enjoyableWay` — do **not** re-add a single
free-text "enjoyable way" field; that was the thing that wrongly fed the intention.

The palette already exists and is the lever: `PassionInterest(label, emoji)` and
`Learning(text)` are person-level, reusable, surfaced on About You. The reminder builder
already pulls passions into a cue tray — but the *goal* never touches them. That's the gap.

---

## Dimension 1 — What changes in the goal-creation flow

### Option A — One optional "want-to" prompt, inline (lightest)
After the title, show a single optional prompt that reframes toward enjoyment, e.g.
"What would make this one you'd *want* to do?" plus tappable chips drawn from the user's
passions ("with ☕ specialty coffee", "🎧 + an audiobook"). Tapping a chip is the whole
interaction; typing is allowed but never required.
- **+** Cheapest; fits the existing name-only screen; chips do the reframing work so the
  user isn't staring at a blank box. Reuses palette directly (thesis move #3).
- **−** One prompt can't really separate "reframe a have-to" from "pick a path." Risks
  reading as the old enjoyableWay field with a nicer label.

### Option B — Two-beat micro-flow: "why" then "want-to" (recommended for v1)
Beat 1: title (+ optional "why it matters", already in builder). Beat 2: an optional
"make it want-to" beat — passion chips to connect + a short "the version of this I'd
enjoy" line. Two beats, both skippable, still well under the 3-minute activation target.
- **+** Cleanly separates *importance* (why) from *want-to* (how you'd enjoy it) — the
  exact distinction the old design muddled. Natural home for palette connection.
- **−** A second beat on the quick-add path; must stay skippable so power users blow past.
- **Open Q:** does this live in `NewGoalFlow`, in builder `StepGoal`, or both? They'd need
  to share a composable to avoid drift (today they're separate, slightly divergent forms).

### Option C — Guided "path that pulls" picker (richest, later)
Goal type first (want-to vs have-to). For a have-to, generate 2–3 candidate *paths*
("walk-and-call" vs "Sunday-lunch call") seeded from passions, user picks the one that
pulls. For a want-to, just confirm + connect a passion.
- **+** Delivers all four thesis moves, including "choose the path" (move #4) explicitly.
- **−** Most build; closest to a wizard; needs careful copy to not feel like a quiz. The
  candidate-path generation is hand-authored templates (no AI — hard exclusion).

**Recommendation:** B for v1, with the passion-chip mechanic from A. C is the north star,
staged later once B proves users engage.

---

## Dimension 2 — What data the goal needs

Principle: prefer **structured links to the palette** over free text, and keep any new
field **goal-level** (never touched by the reminder/cue/intention path).

- **`motivationType`** (enum: `WANT_TO` / `HAVE_TO` / unset) — enables the "want-to
  slightly prioritized, have-to harnessed" thesis rule and could feed ordering (we already
  have `priority: Int?`). Cheap column; powers reframing copy. *Candidate for v1.*
- **Passion links** — which palette items this goal connects to. A join table
  (`goal_id` ↔ `passion_interest_id`) is the clean model; a denormalized id list is the
  cheap one. This is the concrete "connect to your passions" data (move #3). *v1 if we do
  any connection at all.*
- **`enjoyableFraming`** (optional short text) — the user's own words for the want-to
  version, shown on goal detail as encouragement. NOT seeded anywhere downstream. Only add
  if B's free line proves wanted; otherwise the passion links carry it. *Later / optional.*
- **Candidate paths** — only needed for Option C; structured `{label, passionId}` list with
  one marked chosen. *Later.*

Each needs a Room migration (next is **v37** — the slot we just freed by reverting). Adding
`motivationType` + a passion-link table is one additive migration; keep it additive and
default-empty so existing goals are untouched.

**Explicitly not added:** any single "enjoyable way to do it" free-text field that flows
into the intention. That's the reverted design.

---

## Dimension 3 — v1 vs later

**v1 (small, honest):**
- Option B two-beat flow, shared by `NewGoalFlow` + builder `StepGoal` (one composable).
- `motivationType` enum on the goal (drives reframing copy + light prioritization).
- Passion connection via chips → passion-link table. Reuse the builder's existing
  passion-tray pattern so it looks/feels familiar.
- All prompts optional; activation-time budget unchanged; copy reviewed against
  no-guilt / no-quiz rules.
- Goal detail shows the connected passions + (if entered) the want-to framing as warm
  encouragement — reinforces that the goal pulls, independent of any reminder.

**Later:**
- Option C guided path-picker (template-generated candidate paths, no AI).
- `enjoyableFraming` free text if the structured links prove insufficient.
- Prioritization surfacing (how want-to vs have-to actually orders Today/Goals).
- Revisit/refresh-the-want-to over a goal's life (the thesis's "drifts into a have-to"
  problem is longitudinal, not just at creation).

---

## Risks / things to decide

- **Don't rebuild enjoyableWay.** A single free-text "enjoyable way" box, even goal-level,
  invites re-wiring it into the intention later. Structured palette links resist that.
- **Two creation paths must converge** or the want-to beat will exist in one and not the
  other. Recommend extracting a shared goal-creation composable as a precondition for v1.
- **Quiz risk.** The beta flagged reflection burden (43%). Every prompt must be one-tap
  satisfiable (chips), skippable, and silent if skipped.
- **No AI** for path generation — Option C's candidate paths are hand-authored templates
  keyed off passions, or omitted.
- **Scope creep into reminders.** Hard line: nothing in this proposal changes the cue, the
  implementation intention, or the reminder's text.
