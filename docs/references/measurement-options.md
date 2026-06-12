# FollowThru — Measurement Options (attack-mode decision matrix)

Two principles collided and must both be honored: **privacy-first** ("no tracking," local by default) and **instrument from day one** (you can't improve what you can't see). This matrix lays out every option across two axes — cost (free vs. paid) and data location (stays on device vs. reaches you) — so the choice is deliberate, not inherited.

**Hard line that never moves:** nothing leaves the device automatically. Any data that reaches you is either platform-anonymous (Play Console) or explicitly sent by the user, payload visible, per send. The "no tracking" promise stays literally true under every option below.

---

## The 2×2

| | **Local-only (data never reaches you per-user)** | **Shared with you (you see real numbers)** |
|---|---|---|
| **Free** | **A — Pure local + Play aggregates** | **B — Consent-tiered voluntary share** |
| **Paid** | **C — Local + privacy analytics SDK** | **D — Self-hosted / managed backend** |

---

## A — Pure local + Play aggregates  ·  FREE · local-only  ·  **recommended for launch**

What you get: on-device `local_metrics` counters power the user's own streak/insight UI but never transmit. You see only what Play Console gives every developer for free — D1/D7/D30 retention, uninstalls, ratings, crash-free rate, Android vitals — all anonymous and aggregate. Plus the voluntary survey re-run.

- **Cost:** $0. No SDK, no backend, no new permissions.
- **Privacy:** maximal. "No tracking" is unqualified.
- **You can answer:** Is retention moving? Are uninstalls down vs. the FollowThru baseline? Is the rating ≥4.0? Did the survey deltas improve?
- **You cannot answer:** per-user funnel questions — activation rate, follow-through rate, palette-cue share — because those counters stay on the device. You infer them from the survey and aggregates.
- **Why it's the launch pick:** zero cost, zero risk, ships now, and the privacy stance is becoming a genuine brand differentiator. The gap (no funnel visibility) is covered well enough at launch scale by the survey re-run.

## B — Consent-tiered voluntary share  ·  FREE · shared  ·  **recommended fast-follow**

Everything in A, plus one Settings row: **"Share my stats with the developer."** Tapping it opens a prefilled email or Google Form showing the user the *exact* local counters about to be sent (activation time, follow-through rate, palette-cue share, reminder count) and nothing else. They read the full payload and choose to send or not.

- **Cost:** $0. A `mailto:`/Form intent and a free Google Form. No SDK, no backend, no server to run.
- **Privacy:** very high. Transmission is user-initiated, payload-visible, per-send, revocable by simply not sending. Still honest to "no automatic tracking."
- **You can answer:** the real funnel numbers — for the subset who choose to share. Self-selected and small, so directional, not representative.
- **Watch-out:** share rate may be low and skewed toward happy users. Treat as qualitative signal, not a clean metric. If share volume is too low to interpret, the answer is more closed testers, not an SDK.
- **Why fast-follow not launch:** trivial to add, but it's net-new surface and copy; don't let it delay submission. Add in the first post-launch update.

## C — Local + privacy-respecting analytics SDK  ·  PAID (low) · local-only-ish

A privacy-first analytics tool (e.g. self-hostable or anonymized-by-design products) that reports **aggregate, anonymized** funnel metrics without per-user identifiers — closer to "local-only in spirit" than D, but data does leave the device.

- **Cost:** low monthly at small scale; many have free tiers that you'll outgrow. Adds an SDK dependency and app size.
- **Privacy:** good if configured strictly (no device IDs, no PII, IP truncation), but you can no longer say "nothing leaves the device" — only "nothing identifying." That's a real change to your store promise and privacy policy.
- **You can answer:** real aggregate funnels continuously, no user action required.
- **Why not now:** contradicts the current "no tracking" positioning you've built the banner and onboarding around; introduces cost and a dependency; requires a privacy-policy rewrite. Reconsider only if A+B leave you genuinely blind at scale.

## D — Self-hosted or managed backend  ·  PAID · shared

Your own endpoint (or a BaaS) receiving opt-in events. Full fidelity, full control, full responsibility.

- **Cost:** hosting + maintenance + security + a privacy policy that now describes data you custody. Ongoing time, not just money.
- **Privacy:** entirely on you to get right — you become a data controller. Highest burden, highest liability.
- **You can answer:** anything, precisely. Real-time funnels, cohorts, experiments.
- **Why not now:** it's the AI iteration's infrastructure question, not the MVP's. Building it now is multiplying effort on an unvalidated mechanism — exactly the sequencing error attack mode exists to prevent. Defer to the AI phase, designed with consent from the start.

---

## Decision

**Launch on A. Add B as the first fast-follow.** Revisit C/D only if A+B leave you unable to interpret results at a scale where it matters — and if so, prefer C (privacy-respecting, aggregate) over D, and only with a privacy-policy update and a fresh consent surface. The revisit trigger is explicit: *ambiguous launch results AND voluntary-share volume too low to interpret → recruit more closed testers before adding any SDK.*

This keeps the MVP free to you, free to users, privacy-intact, and shippable now — while giving you a paid-upgrade path that you choose later with evidence, rather than inherit now by default.
