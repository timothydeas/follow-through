# FollowThru — Android Handoff

Reference prototype for refining the existing native Android app **FollowThru** (package `com.ideasinc.followthrough`, Kotlin / Jetpack Compose). These flows are meant to be merged into that codebase. FollowThru is the app name everywhere — no renames, alternate logos, or taglines that alter identity. Where this prototype shows a check-glyph wordmark, substitute the real FollowThru app icon asset.

This prototype is a web reference (TanStack Start + React + Tailwind v4). State is in-memory only (no backend, no localStorage), mirroring the native app's `local_only` storage posture.

> **Copy fork to decide (not a bug):** This prototype's Welcome pane 1 (section 8) uses solution-first reassurance — "Following through is easy — you just need the right cue at the right moment" — and explicitly instructs against "following through is the hard part." The Play Store banner/listing uses the opposite, problem-first hook — "Setting goals is easy. Following through is the hard part." This may be a deliberate funnel design (problem hooks the stranger on the store; reassurance encourages the user who already installed), but verify the store-to-app transition doesn't read as whiplash. The banner is locked separately and is unaffected by this doc.

---

## 1. Screen inventory & navigation map

The spine every screen advances along: **goal -> self-knowledge -> intention -> one cue -> reminder.**

| Route (web) | Screen | Purpose line | Native target |
|---|---|---|---|
| `/welcome` | Welcome (3 panes) | Why FollowThru exists; private by design | Onboarding NavHost graph, shown when `onboarding_completed == false` |
| `/` | Today (home) | "Your reminders for the moments that matter." | `TodayScreen` (start destination) |
| `/goals` | Goals list | "What you're working toward — and why it matters." | `GoalsScreen` |
| `/goals/{goalId}` | Goal detail | Why / Barriers / Progress notes / Reminders | `GoalDetailScreen` (list-detail pane on tablet) |
| `/about` | About You (palette) | "What you know about yourself is the raw material for cues…" | `AboutYouScreen` |
| `/settings` | Settings | "Make FollowThru comfortable to read and use." | `SettingsScreen` |
| `/builder` (+ `?goalId=`) | Reminder builder (4-step wizard) | Step purpose lines per step | `ReminderBuilder` (full-screen on phone, dialog/panel on tablet) |
| Cue-fire modal | In-app preview of the notification | Cue + full text + 3 actions | High-priority notification + full-screen intent / `CueFireActivity` |

**Navigation model.** Four primary destinations (Today · Goals · About You · Settings). Phone = bottom `NavigationBar`; tablet (WindowSizeClass Medium / Expanded) = `NavigationRail` with the same four destinations, icons, labels, order. Welcome and Builder are full-screen routes outside the bottom-bar/rail chrome. Map this to a single NavHost; switch `NavigationBar` <-> `NavigationRail` on window size class.

---

## 2. Component inventory

| Component (web file) | Props / state | Compose equivalent |
|---|---|---|
| `BrandMark` | size, showWordmark | `FollowThruLogo(size)` — swap glyph for icon drawable |
| `AppLayout` | children; reads current route for active item | `Scaffold` + `NavigationBar`/`NavigationRail` |
| `ReminderCard` | reminder: Reminder; local preview state | `ReminderCard(reminder, onAction, onPreview)` |
| `CueDisplay` | cue: Cue, size | `CueView(cue)` — paired text always rendered |
| `CueFireModal` | reminder, open, onOpenChange; local resolution, note | full-screen notification surface / `ModalBottomSheet` over OverlaySurface |
| Snackbar-with-undo | via `sonner.toast`, 8000ms + Undo action | `SnackbarHostState.showSnackbar(..., actionLabel="Undo")` with 8s duration |
| `PaletteChip` (inline in About You / builder) | label, emoji, edit/delete | `AssistChip` / `InputChip` |
| Wizard step (Section + step bodies in `builder.tsx`) | step index, validity flags | `ReminderBuilderStep` composables driven by a `BuilderViewModel` |
| Today progress line | computed from events/metrics | `WeeklyProgressBanner` (gold surface) |

---

## 3. Data model (Kotlin / Room)

Field names match the seed JSON 1:1 so the structures port directly. The web types live in `src/lib/store.tsx`.

\`\`\`kotlin
enum class CueType { emoji, phrase, photo, sound }
enum class EventAction { done, snoozed, not_today }
enum class ScheduleMode { daily, weekly }
enum class WeekDay { MON, TUE, WED, THU, FRI, SAT, SUN }

@Entity data class Profile(
    @PrimaryKey val id: String,
    val created_at: String,
    // passions_interests, learnings, settings as related tables / embedded
)

@Entity data class PassionInterest(
    @PrimaryKey val id: String, val profileId: String,
    val label: String, val emoji: String,
)

@Entity data class Learning(
    @PrimaryKey val id: String, val profileId: String, val text: String,
)

data class Settings(
    val theme: String,                 // "light" | "dark" | "system"
    val text_scale: Float,             // 1.0..2.0  -> drives fontScale
    val reduce_motion: Boolean,
    val notification_sound_enabled: Boolean,
    val onboarding_completed: Boolean,
)

@Entity data class Goal(
    @PrimaryKey val id: String,
    val title: String, val why_it_matters: String,
    val status: String, val created_at: String,
)
@Entity data class Barrier(@PrimaryKey val id: String, val goalId: String, val text: String)
@Entity data class ProgressNote(
    @PrimaryKey val id: String, val goalId: String, val date: String, val text: String,
)

@Entity data class Reminder(
    @PrimaryKey val id: String,
    val goal_id: String,
    @Embedded(prefix = "intent_") val intention: Intention,
    @Embedded(prefix = "cue_") val cue: Cue,
    @Embedded(prefix = "sched_") val schedule: Schedule,
    val full_text_always_shown: Boolean,
    val status: String, val created_at: String,
)
data class Intention(val when_moment: String, val i_will: String)
data class Cue(
    val type: CueType, val value: String, val alt_text: String?,
    val source_palette_id: String?, val is_palette_drawn: Boolean,
)
data class Schedule(
    val mode: ScheduleMode, val days: List<WeekDay>,  // TypeConverter
    val time_local: String, val timezone: String,
)

@Entity data class ReminderEvent(
    @PrimaryKey val id: String,
    val reminder_id: String, val delivered_at: String,
    val action: EventAction, val acted_at: String,
    val undone: Boolean, val undo_reason: String?, val reflection_text: String?,
)
\`\`\`

**Which fields drive which UI**

- `cue.type` selects the `CueView` rendering; `cue.alt_text` is required when `type == photo` and is the contentDescription.
- `cue.is_palette_drawn` / `source_palette_id` — set when a cue was chosen from the palette; drives the `reminders_with_palette_cue` counter.
- `full_text_always_shown` — intention text always renders beside the cue.
- `reminder_events.action` + `undone` — power the Today progress line and all metrics. Undone events are excluded from counts.

---

## 4. State & business rules

- **One-cue enforcement.** Builder step 4 allows exactly one cue object. Save is disabled until value is non-empty (and `alt_text` non-empty for photo). Picking a new type/value replaces the prior selection — never accumulates.
- **Undo + 8-second window.** Every Done/Snooze/Not-today on Today shows a snackbar with an Undo action that stays 8 seconds (or until dismissed). Undo sets `undone = true` and `undo_reason` (default `accidental_tap`) — events are never hard-deleted. Native: Snackbar duration 8s; recompute metrics on undo.
- **Palette-drawn flag.** Cue chosen from a palette chip -> `is_palette_drawn = true`, `source_palette_id = <chipId>`. Increment `reminders_with_palette_cue` accordingly (recomputed from reminders list).
- **Local metrics.** On-device only, never transmitted. Recomputed after every mutation: `reminders_created_total`, `reminders_with_palette_cue`, `delivered_total` (= non-undone events), `done_total`, `follow_through_rate` (= done/delivered, 2dp).
- **Schedule semantics.** `daily` -> all 7 days; `weekly` -> selected days at `time_local` in `timezone`. Map to AlarmManager (exact alarms for precise cue-fire) or WorkManager periodic work; deliver via a high-priority notification channel with a full-screen / heads-up intent carrying cue + full intention text + Done/Snooze/Not-today actions. Re-register alarms on `BOOT_COMPLETED`.
- **No auto-dismiss.** Welcome panes and the cue-fire modal never advance on a timer; the user always taps to move on.

---

## 5. Design tokens (as implemented)

Brand tokens live only in `src/styles.css` (mirrors `ui/theme/Color.kt` + `AppColors.kt`). Screens consume semantic classes — no raw hex literals.

### Light mode
| Token | Hex |
|---|---|
| Coral (accent) | `#B5402C` |
| Coral Tint | `#F6D9D2` |
| Background | `#FBF6EF` |
| Surface | `#FFFFFF` |
| Border | `#ECE3DA` |
| Text | `#2A2622` |
| Text Muted | `#6E645D` |
| Gold / Gold Surface / Gold Icon | `#C9A24A` / `#F8EAC7` / `#9C7A1A` |
| Error | `#C0392B` |
| On-Coral (label) | `#FFFFFF` |

### Dark mode
| Token | Hex |
|---|---|
| Coral | `#E8775F` |
| On-Coral (label) | `#1E1B19` |
| Coral Tint | `#3A2A25` |
| Background | `#1E1B19` |
| Surface | `#2A2622` |
| Border | `#3A332E` |
| Text | `#F3ECE4` |
| Text Muted | `#B5AAA1` |
| Gold / Gold Surface | `#D9B65E` / `#3A3320` |
| Error | `#FF8A80` |

### Overlays (mode-independent)
Modal scrim black @ 60%; Overlay surface `#2A2622` always; on-overlay text `#FFFFFF` (cue-fire modal & reassurance overlays).

### Material 3 colorScheme slot mapping
| Slot | Light | Dark |
|---|---|---|
| primary | Coral `#B5402C` | Coral `#E8775F` |
| onPrimary | `#FFFFFF` | `#1E1B19` (flips per mode) |
| primaryContainer | Coral Tint `#F6D9D2` | `#3A2A25` |
| tertiary / tertiaryContainer | Gold `#C9A24A` / `#F8EAC7` | `#D9B65E` / `#3A3320` |
| background / surface | `#FBF6EF` / `#FFFFFF` | `#1E1B19` / `#2A2622` |
| surfaceVariant | = surface (separate by outline, not fill) | = surface |
| outline (Border) | `#ECE3DA` | `#3A332E` |
| error | `#C0392B` | `#FF8A80` |

### AppColors brand-token names
`BrandAccentText`, `CoralTint`/`OnCoralTint`, `Border`, `Gold`/`GoldIcon`/`GoldSurface`, `Destructive`, `OverlayScrim`/`OverlaySurface`/`OnOverlaySurface`, `SwitchUncheckedTrack`.

### Typography — Poppins (Regular + SemiBold)
displayMedium 28/36 SB · headlineMedium 20/28 SB · headlineSmall 17/24 SB · bodyLarge 16/24 R · bodyMedium 14/20 R · bodySmall 12/16 R · labelLarge 14/20 R · labelMedium 12/16 R.

### Spacing / radii / motion
Cards radius 16px (`--radius: 1rem`); pills fully rounded. Generous whitespace. Motion 150-200ms ease (`ft-motion = 180ms`), fully disabled under reduce-motion.

### Breakpoints (WindowSizeClass)
- **Compact (< 600dp) = phone:** single column, bottom nav, full-width cards, full-screen wizard steps.
- **Medium / Expanded (>= 600dp) = tablet:** navigation rail, two-column card grids (max content width ~840px), list-detail goal screen, wizard as centered panel. Identical content, copy, interactions, theming, and spacing across both.

Native source of truth: `ui/theme/Color.kt`, `AppColors.kt`, `Theme.kt`, `Type.kt`.

---

## 6. Accessibility checklist (WCAG 2.1 AA acceptance criteria)

- **Contrast.** Body/UI text >= 4.5:1, large text >= 3:1, in both themes. Deeper coral `#B5402C` is intentional (brighter coral failed ~3.3:1) — do not lighten. White labels on coral (light); dark labels on coral (dark).
- **Touch targets >= 48dp** — all buttons/chips/nav items use `min-h-12` / `min-h-14` / `min-w-11`.
- **Focus.** Visible focus ring (coral, 2px) on every interactive element.
- **Names.** Every icon-only control has an accessible name (`aria-label` -> `contentDescription`).
- **Images/cues.** Photo cues require alt text; meaning never carried by color, emoji, or sound alone — paired text always present.
- **Inputs never hidden by keyboard.** Goal-detail progress-note input is pinned above its list; wizard content scrolls above the keyboard. In Compose use `imePadding()` / `Modifier.imeNestedScroll()`.
- **No timed auto-dismiss** without user control (welcome panes, cue-fire modal). Snackbar undo persists 8s and is dismissible.
- **Headings & labels** in logical order; wizard steps are screen-reader friendly with labeled fields and progress state.
- **Text scaling to 200%** (`text_scale` 1.0-2.0) reflows without truncation; supports portrait and landscape.
- **TalkBack acceptance:** navigate every screen, confirm each control announces a meaningful name + role; cue cards announce cue text + full intention; actions announce result + Undo availability.

---

## 7. Explicit exclusions (do NOT scope-creep)

Intentionally not built: AI features · account/login · cloud sync · social features · streak counters or badges · sub-goal hierarchies · analytics SDKs. State is local only. No renaming or rebranding — the app is FollowThru; do not generate alternative names, logos, or taglines. Gold is celebratory only (the "Followed through. Nice." moment + the gentle weekly progress accent), never streak mechanics and never body text.

> **Reconcile before building — progress vs. streak.** This prototype version describes a simple weekly *progress line*. The project's current target (see lean-prd.md and the streak spec) is the **flexible-streak system**: 2 auto-applied passes per week, a never-resetting lifetime follow-through count, and fresh-start copy when a week is missed. Treat the flexible-streak spec as authoritative over the simple progress line described here. If this prototype has not yet been updated to the flexible streak, that is a known gap to close — not the intended final design.

---

## 8. Onboarding copy (verbatim — Welcome 3 panes)

The `/welcome` flow (`WelcomeScreen` graph, shown when `onboarding_completed == false`) is three swipeable panes with progress dots, a Skip affordance (-> Today), Continue/Back, and a final **Create my first reminder** (-> Builder). No timed auto-advance — the user always taps. Copy below is the source of truth for the native strings.

**Pane 1 — the premise (reframed, encouraging, not daunting)**
- Headline: "Following through is easy — you just need the right cue at the right moment."
- Body: "People mostly forget at the moment they could act — it's not a willpower problem. FollowThru ties each intention to one vivid cue from your own life, so the moment itself reminds you."
- Tone note: the premise is deliberately reassuring, not a "following through is the hard part" warning. Do not reintroduce loss-framed or anticipatory-failure copy — the message is that follow-through is easy once the right cue meets the right moment. *(See the store-to-app copy fork flagged at the top of this doc.)*

**Pane 2 — the whole idea in 30 seconds (3 steps)**
- Headline: "Here's the whole idea in 30 seconds."
- The goal: "Take blood-pressure meds every morning."
- The cue from your life: "Starting the morning coffee — the pill sits right by the orange Chemex."
- The moment reminds you: "\"When I start the morning coffee, I will take the BP pill next to the Chemex.\" The text always travels with the cue."

**Pane 3 — privacy**
- Headline: "Private by design."
- Body: "Everything stays on this device. No account, no cloud, no tracking."
- CTAs: panes 1-2 = Continue; pane 3 = Create my first reminder. Skip (panes 0-2) and Back (panes 1-2) per Material 3 text-button styling.
