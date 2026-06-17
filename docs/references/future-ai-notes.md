# Future-AI Notes — what the AI phase will need from the MVP

**Status:** forward-looking notes, NOT current scope. The MVP is free, local-only, no-AI
(see `Product_Strategy.md`). This file records what the *future paid AI tier* will need so
today's decisions don't paint it into a corner — especially the cheap data/schema additions
that are free now and expensive once production data exists.

---

## The AI vision (from Product_Strategy.md)
- **Right tool per job:** an on-device/open-source **LLM for cue *language*** (help craft
  distinctive, memorable cues; conversational setup) and a **prediction approach for cue
  *timing*** (learn the user's patterns to fire near the actionable moment). The LLM never
  decides *when*.
- **Privacy/architecture is non-negotiable:** runs **on-device**, learns from the user's
  **own local data** (never a pooled cross-user model), reasoning kept **inspectable (XAI)**;
  every suggestion editable, declinable, explained (Calboli autonomy). No third-party cloud LLM.
- **Phasing:** the free local MVP proves people create + keep using cue reminders and doubles
  as the **control condition**; the AI tier is a later paid subscription.

## What the local corpus already captures
- `Reminder` — the intention (`whenMoment` + `iWill`), the one cue (`cueType`/`cueValue`),
  schedule (incl. one-off `scheduleDate`), and cue lineage fields (`cueSourcePaletteId`,
  `cueIsPaletteDrawn`).
- `ReminderEvent` — **DONE** responses (`deliveredAt`, `actedAt`, `undone`).

## Gaps to close BEFORE the AI phase (cheap now, costly later)
1. **Event richness for timing.** Today we log only "Did it." To learn *timing* and *misses*,
   start logging (locally) when a cue actually **fires/delivered** and when the user **defers**
   ("Remind me later"), plus a **`scheduledForAt`** snapshot on each event. Without "fired but
   not acted," the AI can't learn timing accuracy or miss patterns. (Additive, low-risk.)
2. **Behavioral-history durability.** `ReminderEvent` CASCADE-deletes with its `Reminder`,
   which CASCADE-deletes with its `Goal` — so deleting an intention erases its follow-through
   history. For a long-lived "what's worked for me" corpus, decide a retention approach
   (denormalize key fields onto the event, or soft-delete) before the AI phase.
3. **Cue provenance for XAI.** `cueSourcePaletteId`/`cueIsPaletteDrawn` exist but the interests
   palette was removed in the MVP. If suggested/assisted cues return, keep provenance so XAI
   can explain "this cue came from X."
4. **Context signals.** None captured today (intentionally). Any future context (time-of-day
   actuals, routine, opt-in location/activity) must be **on-device + consented** (Calboli + XAI),
   never automatic.

## Guardrails the AI phase must keep
- On-device / open-source model; no automatic transmission; "no tracking" stays literally true.
- Measurement stays consent-tiered (Play aggregates + on-device counters). A *voluntary*
  share path was considered and **declined for the MVP** — it risks undercutting the
  local-only promise; revisit only with an explicit privacy-policy decision.
- Every AI suggestion editable, declinable, explained; engagement is a counter-metric, not a goal.
