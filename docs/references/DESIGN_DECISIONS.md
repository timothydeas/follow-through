# Design Decisions — FollowThru

Append-only log of **locked** visual / design / copy decisions, so they compound
instead of being re-litigated. Each entry: `YYYY-MM-DD — what was decided (why).`
Newest at the bottom. One or two lines each — this is a reference, not prose.

When a new decision replaces an older one, **edit the superseded entry** to mark it
`~~superseded~~ → see <date>` so the log never contradicts itself (see the CLAUDE.md
working rule). Tiebreaker on copy is `ANDROID_HANDOFF.md`; on brand/color the
`BRAND_STYLE_GUIDE.md`; on scope the `lean-prd.md`.

---

## Copy
- 2026-06-12 — Welcome Pane 1 headline = **"Progress, not perfection."** (handoff §8 is the copy tiebreaker; retires "Following through is the hard part" as too daunting/loss-framed).
- 2026-06-12 — **"in every moment"** tagline retired — no longer shown under the FollowThru wordmark anywhere.
- 2026-06-12 — Onboarding step-2 cue body keeps the verbatim §8 copy ("☕ Starting the morning coffee — the pill sits right by the orange Chemex."); match screenshots for *layout*, never transcribe wording off images.

## Screens & layout
- 2026-06-12 — **Goals screen / home cards** match the prototype (`goals.png`): "Goals" header + "What you're working toward — and why it matters." purpose line; clean cards = title → why-it-matters subtitle → coral reminder-count → chevron (no priority badges, accent strips, drag grips, or bell); 2-column grid on tablet, single column on phone.
- 2026-06-12 — **Onboarding step 2** matches `onboardingExample.png` (each idea in a pale-coral icon circle; scaled-up headline). Other onboarding steps are intentionally **unchanged**.
- 2026-06-12 — **Onboarding nav buttons**: full-width primary with "Back" below it (matches prototype); the primary label wraps to 2 lines rather than overflowing, so long labels fit including at 200% text scale.
- 2026-06-12 — **Stats + FollowThrus** have no standalone screens — folded into Today (weekly progress line + lifetime count) and Goal detail (barriers/progress). Encouraging progress content preserved, not siloed.

## Removed (too academic / retired)
- 2026-06-12 — **LaunchInsight splash removed entirely** — no launch-insight screen at all (too academic; also the auto-dismiss splash was a beta complaint). Supersedes the earlier "keep it but user-dismissed once/day" decision.
- 2026-06-12 — **User-facing science content removed** — the Science screen and the About "science behind FollowThru" row are cut (too academic).

## Mechanics & components
- 2026-06-12 — **Weekly progress** is the flexible, forgiving §4a indicator (Monday boundary, ≤2 auto-applied passes, never-resetting lifetime count) — never a badge, score, flame, or break-the-chain streak counter.
- 2026-06-12 — **Cue types at launch = emoji + phrase only**; photo and sound are shown as the target state but feature-flagged OFF (current pickers crash).
- 2026-06-12 — **Biometric lock** stays as an optional Settings toggle (a FollowThru extra beyond the prototype).
- 2026-06-12 — **Settings** carries: theme (light/dark/system), text size 100–200% (live reflow), reduce motion, notification sound, "Replay the intro", privacy line, biometric lock.
