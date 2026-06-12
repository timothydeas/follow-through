# AI Co-Pilot Operating Playbook — Executive Attack Mode

*What the FollowThru process should have been from message one. Written as a reusable kickoff protocol, with the retro on this project at the end. Keep beside the PRD template; this governs HOW the template gets run.*

---

## Step 0 — Declare input tiers BEFORE any work begins

Every input handed to the co-pilot gets a tier. The co-pilot's behavior toward each tier is non-negotiable.

**Tier 1 — Fixed (never challenge):**
- Business directional outcomes (written as a one-pager BEFORE this prompt — see Step 1)
- User feedback data (raw, uninterpreted)
- Cited research findings (the studies themselves, not anyone's summary of them)

**Tier 2 — Challenge by default (adopt only what survives attack):**
- My briefs, my prior product decisions, my proposed solutions, my feature ideas
- Any prior team's design choices
- Rule of evidence: for every Tier 2 item the co-pilot adopts, it must state in one sentence WHY it survived — traced to a Tier 1 input. "The PM wrote it" is not a reason. An untraceable Tier 2 item gets labeled ASSUMPTION and added to the falsification guide (Step 2).

**Tier 3 — Context only (informs, never decides):**
- Current app state, old assets, legacy copy, screenshots

**The kickoff sentence that enforces this** (paste verbatim at the top of the first prompt):

> "Operate in attack mode. Tier 1 inputs are fixed: [list]. Tier 2 inputs must be challenged before adoption — for each one you keep, state why it survived, traced to Tier 1; label survivors-without-evidence as ASSUMPTIONS. Tier 3 is context only. Maintain a running decision log: every product decision, its tier-1 justification or assumption label, and what launch data would falsify it."

---

## Step 1 — Outcomes one-pager (one hour, before any template)

A single page, written by the PM, containing exactly four things:

1. **Business directional outcomes** — what must be true in 7 days, 30 days, 6 months.
2. **The hypothesis** — one falsifiable sentence.
3. **The release gate** — pass/fail conditions that replace any calendar date. Derived from prior failure data (every known bug and complaint restated as a check).
4. **The metric tiers** — lagging (North Star), leading, proxy, counter — with the tiebreaker rule stated: when a leading number improves while a counter degrades, the counter wins.

This page is Tier 1. Everything downstream serves it. (In the FollowThru run, items 3 and 4 were retrofitted mid-stream under deadline pressure; both should have existed before the PRD.)

---## Step 2 — Red-team the founding documents (before building on them)

Before the PRD, the co-pilot attacks every Tier 2 document and produces a **falsification guide**:

- The 3–5 assumptions the thesis rests on that are NOT directly evidenced by Tier 1
- For each: what launch-data pattern would break it, and what the correct response would be
- This guide gets pasted INTO the founding document as a "Risks to the hypothesis" section, so the brief carries its own kill-conditions into the repo

*FollowThru's guide (produced late; should have been turn one):*
1. **Users can self-author distinctive cues.** Rogers & Milkman's cues were researcher-designed. If follow-through is flat → suspect cue quality before the mechanism. Response: cue-crafting guidance or curated cue suggestions, not abandonment.
2. **Clock-time scheduling approximates "the moment."** The science fires cues at the actionable moment; the MVP fires at a predicted time. Mediocre results are partially confounded by timing error — this is precisely the gap the AI iteration closes. Don't kill the mechanism for the scheduler's sins.
3. **Privacy-first measurement can detect the effect.** Small n + aggregate-only data can hide a real, modest effect. Ambiguous results may mean "more users," not "wrong mechanism."
4. **One cue beats many** is an extrapolation from concurrent-distinctiveness logic, not a tested finding (R&M never tested stacking). Defensible; labeled ASSUMPTION.

---

## Step 3 — Run the PRD template against Tier 1 + survivors only

The PRD's every line must trace to: a Tier 1 input, or a Tier 2 survivor with its stated reason, or a labeled assumption. Scope discipline per the template: 3 features max, explicit cuts, constraints, ≤3 open questions — and open questions are framed as experiments the MVP will answer, not debates to settle by opinion.

## Step 4 — Route each decision to its cheapest reversible medium

- **Experience decisions** (flows, copy, vocabulary, progress mechanics, iconography, journey) → prototype (Loveable). Every one of these is a prompt-edit there and a refactor in Kotlin.
- **Engineering decisions** (delivery reliability, permissions, data schema, feature flags) → spec directly (PRD constraints + CLAUDE.md). The prototype cannot model them; routing them there wastes the medium.
- **Market decisions** (banner copy, store positioning) → cheapest empirical test available (Play store listing experiments), not debate.

## Step 5 — Canonize, then code

Handoff doc generated only AFTER the prototype is final (names, copy, icons settled — the doc canonizes whatever exists). Sanity greps on identity and banned vocabulary. Reference docs + CLAUDE.md committed BEFORE code work, in one commit. CLAUDE.md states the tier rules so attack-mode survives into the coding phase: "reference docs are TARGET not current state; diff before proposing; refine, don't rewrite."

## Step 6 — Schema check against the future state

Before launch, one pass against the next iteration's needs (here: the AI phase): does today's data model capture what tomorrow's consumer requires? Schema gaps are free this week and expensive after production data exists. (FollowThru: verify against Deas_assignment4 once recovered — event timestamp granularity, cue-derivation lineage, context fields.)

---

## The measurement doctrine (resolving privacy-first vs. instrument-from-day-one)

Two principles collided; the resolution is consent-tiered measurement, all free:

| Tier | What | Cost | Consent |
|---|---|---|---|
| Automatic, anonymous | Play Console aggregates: D1/D7/D30 retention, uninstalls, ratings, Android vitals crash rate | Free, no SDK | Platform-level, no app involvement |
| On-device only | `local_metrics` counters (activation time, follow-through rate, palette-cue share) — never transmitted | Free | N/A — never leaves device |
| Explicit, user-initiated | Settings row "Share my stats with the developer" → opens a prefilled message (email or Google Form) displaying the exact local counters being sent; the user sees the full payload and chooses to send | Free, no backend, no SDK | Visible, voluntary, per-send |
| Structured qualitative | Identical beta survey re-run at staged rollout; before→after scorecard | Free | Voluntary |

Hard line preserved: **nothing leaves the device automatically, ever.** The brand promise "no tracking" stays literally true. Revisit trigger: if launch results are ambiguous AND voluntary share rate is too low to interpret, the next decision is recruiting more closed testers — not adding an analytics SDK.

---

## Retro — what attack mode would have changed on FollowThru

**Converged anyway (the brief restated Tier 1):** the mechanism, the inputs/palette, no-AI sequencing, accessibility architecture, the spine. The business outcomes already contained these; the brief survived because it was largely a restatement.

**Attack mode would have changed:**
1. The absolutist no-transmission stance → consent-tiered measurement above (now adopted: the voluntary stat-share row).
2. One-cue labeled as ASSUMPTION in the PRD, with its falsification condition.
3. Gate and metric tiers defined at Step 1 instead of retrofitted.
4. The falsification guide written turn one instead of post-hoc.

**What the run got right regardless:** decisions made in cheapest-reversible media (streaks, vocabulary, copy, nav all changed at prompt cost); engineering routed to spec; the founding hypothesis kept falsifiable; identity and scope discipline held.
