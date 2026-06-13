# Loveable Prompt — FollowThru: Reminders-Through-Association Prototype (single run, all-in-one)

Copy everything below the line into Loveable as one prompt.

---

Build a mobile-first web app called **FollowThru** — a privacy-first tool that helps people follow through on intentions by tying each one to a single, personally distinctive cue surfaced at the moment they can act.

**Identity is fixed:** FollowThru is an existing, live Android app (package `com.ideasinc.followthrough`, published on Play Console). This prototype is a fresh-perspective reference for new flows to refine that app — do NOT invent a new name, logo, or rebrand anywhere in the UI or copy. The app name is "FollowThru" everywhere it appears. I will upload the current FollowThru brand app icon — use that exact icon wherever an app icon or logo mark appears (welcome screen, header); do not generate a substitute logo.

## Redesign principle (apply to every element)
**Purpose visible, one cue, full meaning in text, always declinable.** Every screen states why it exists in one short line, moves the user one step along the spine (goal → self-knowledge → intention → one cue → reminder), carries complete meaning in plain text (never only in an emoji, image, color, or sound), and keeps "skip / not now / undo" one effortless tap away. No guilt language anywhere. No streak-shaming. Nothing auto-dismisses before the user chooses to move on.

## Data model + seed data (use exactly — zero placeholder/Lorem text)
Initialize the app's state from this JSON. Treat it as the single source of truth: every screen renders from this structure, and all create/edit/done/snooze/undo actions mutate it in memory (no backend, no localStorage — in-memory state for the prototype). Keep the field names exactly as written so the structure can be mirrored 1:1 as Kotlin data classes / Room entities in the native Android build.

```json
{
  "schema_version": "1.0.0",
  "storage": "local_only",
  "profile": {
    "id": "profile_local_001",
    "created_at": "2026-06-08T18:22:00-07:00",
    "passions_interests": [
      { "id": "pi_001", "label": "Trail running on the Sacagawea river path", "emoji": "🏃‍♀️" },
      { "id": "pi_002", "label": "My golden retriever, Biscuit", "emoji": "🐕" },
      { "id": "pi_003", "label": "Specialty coffee — the orange Chemex on my counter", "emoji": "☕" },
      { "id": "pi_004", "label": "Sci-fi audiobooks on the commute", "emoji": "🎧" }
    ],
    "learnings": [
      { "id": "ln_001", "text": "I follow through when the thing is staged by the door the night before." },
      { "id": "ln_002", "text": "Morning me keeps promises; 9pm me negotiates them away." },
      { "id": "ln_003", "text": "Pairing a chore with a podcast makes me actually start it." }
    ],
    "settings": {
      "theme": "system",
      "text_scale": 1.0,
      "reduce_motion": false,
      "notification_sound_enabled": true,
      "onboarding_completed": false
    }
  },
  "goals": [
    {
      "id": "goal_001",
      "title": "Run a 5K without stopping by August",
      "why_it_matters": "Energy back, and Biscuit needs a running buddy.",
      "status": "active",
      "created_at": "2026-06-08T18:31:00-07:00",
      "barriers": [
        { "id": "br_001", "text": "After-work slump — I sit down and never get back up." },
        { "id": "br_002", "text": "Forget to pack running shoes when I leave for work." }
      ],
      "progress_notes": [
        { "id": "pr_001", "date": "2026-06-09", "text": "Ran 1.5 miles, walked the rest. Knees fine." },
        { "id": "pr_002", "date": "2026-06-11", "text": "Skipped Tuesday — barrier #1 exactly as predicted." }
      ]
    },
    {
      "id": "goal_002",
      "title": "Take blood-pressure meds every morning",
      "why_it_matters": "Doctor flagged it in May. Non-negotiable.",
      "status": "active",
      "created_at": "2026-06-09T07:05:00-07:00",
      "barriers": [
        { "id": "br_003", "text": "Mornings are chaotic; the bottle lives in a drawer I never open." }
      ],
      "progress_notes": [
        { "id": "pr_003", "date": "2026-06-10", "text": "Moved the bottle next to the coffee scale. Took it both days since." }
      ]
    },
    {
      "id": "goal_003",
      "title": "Call Mom every Sunday",
      "why_it_matters": "She mentioned twice that I only text now.",
      "status": "active",
      "created_at": "2026-06-10T20:14:00-07:00",
      "barriers": [
        { "id": "br_004", "text": "Sundays blur; by the time I remember it's 9pm her time." }
      ],
      "progress_notes": []
    }
  ],
  "reminders": [
    {
      "id": "rem_001",
      "goal_id": "goal_001",
      "intention": { "when_moment": "I change out of work clothes on Mon/Wed/Fri", "i_will": "put on running shoes and take Biscuit out the door — even for 10 minutes" },
      "cue": {
        "type": "phrase",
        "value": "Biscuit's leash is the starting line",
        "source_palette_id": "pi_002",
        "is_palette_drawn": true
      },
      "schedule": { "mode": "weekly", "days": ["MON", "WED", "FRI"], "time_local": "17:30", "timezone": "America/Los_Angeles" },
      "full_text_always_shown": true,
      "status": "active",
      "created_at": "2026-06-08T18:40:00-07:00"
    },
    {
      "id": "rem_002",
      "goal_id": "goal_002",
      "intention": { "when_moment": "I start the morning coffee", "i_will": "take the BP pill sitting next to the orange Chemex" },
      "cue": {
        "type": "emoji",
        "value": "☕",
        "source_palette_id": "pi_003",
        "is_palette_drawn": true
      },
      "schedule": { "mode": "daily", "days": ["MON","TUE","WED","THU","FRI","SAT","SUN"], "time_local": "06:45", "timezone": "America/Los_Angeles" },
      "full_text_always_shown": true,
      "status": "active",
      "created_at": "2026-06-09T07:12:00-07:00"
    },
    {
      "id": "rem_003",
      "goal_id": "goal_003",
      "intention": { "when_moment": "the Sunday audiobook hour ends", "i_will": "call Mom before doing anything else" },
      "cue": {
        "type": "photo",
        "value": "mom_garden_photo",
        "alt_text": "Mom in her garden holding tomatoes",
        "source_palette_id": null,
        "is_palette_drawn": false
      },
      "schedule": { "mode": "weekly", "days": ["SUN"], "time_local": "16:00", "timezone": "America/Los_Angeles" },
      "full_text_always_shown": true,
      "status": "active",
      "created_at": "2026-06-10T20:20:00-07:00"
    }
  ],
  "reminder_events": [
    { "id": "ev_001", "reminder_id": "rem_002", "delivered_at": "2026-06-10T06:45:02-07:00", "action": "done", "acted_at": "2026-06-10T06:47:11-07:00", "undone": false, "reflection_text": null },
    { "id": "ev_002", "reminder_id": "rem_001", "delivered_at": "2026-06-10T17:30:01-07:00", "action": "not_today", "acted_at": "2026-06-10T17:31:40-07:00", "undone": false, "reflection_text": "Long meeting ran over. Tomorrow." },
    { "id": "ev_003", "reminder_id": "rem_002", "delivered_at": "2026-06-11T06:45:00-07:00", "action": "done", "acted_at": "2026-06-11T06:45:58-07:00", "undone": false, "reflection_text": null },
    { "id": "ev_004", "reminder_id": "rem_001", "delivered_at": "2026-06-11T17:30:00-07:00", "action": "done", "acted_at": "2026-06-11T17:36:05-07:00", "undone": true, "undo_reason": "accidental_tap", "reflection_text": null }
  ],
  "local_metrics": {
    "description": "On-device counters only. Never transmitted. Powers the user's own insight line on Today and validates leading indicators at survey re-run.",
    "first_reminder_created_seconds_after_install": 142,
    "reminders_created_total": 3,
    "reminders_with_palette_cue": 2,
    "delivered_total": 4,
    "done_total": 3,
    "follow_through_rate": 0.75
  }
}
```

Notes on the model: `cue.type` is one of `emoji | phrase | photo | sound`; photo cues require `alt_text`. `reminder_events.action` is one of `done | snoozed | not_today`; every action is undoable and undo sets `undone: true` with an `undo_reason`. `is_palette_drawn` flags whether a cue came from the user's palette — increment `local_metrics.reminders_with_palette_cue` accordingly when new reminders are created. For the photo cue, render a warm illustrated placeholder card labeled with the alt text (no external image fetches).

## Screens

**1. Welcome (first run, 3 panes max, skippable, re-openable from Settings).** Pane 1 headline: "Progress, not perfection." Body: "People mostly forget at the moment they could act — it's not a willpower problem. FollowThru ties each intention to one vivid cue from your own life, so the moment itself reminds you." Pane 2: a 30-second worked example using the coffee/BP-pill reminder, shown end-to-end. Pane 3: "Private by design — everything stays on this device. No account, no cloud, no tracking." Each pane advances only on user tap; nothing auto-advances or times out. Primary CTA: "Create my first reminder." Completing or skipping sets `profile.settings.onboarding_completed` to true.

**2. Home — Today.** Header "Today" with a one-line purpose ("Your reminders for the moments that matter"). Cards for today's reminders showing the cue (emoji/phrase/photo card) PLUS the full intention text, time, and goal chip. One-tap actions on each card: Done ✓, Snooze 1h, Not today — every action shows a snackbar with an Undo button that stays for 8 seconds or until dismissed. A gentle, non-streak progress line computed from `local_metrics` and `reminder_events`: "Followed through on 3 of 4 this week — and Tuesday was forgiven." Floating action button: "New reminder." Bottom nav: Today · Goals · About You · Settings.

**3. Goals list + Goal detail.** List shows the three goals with why-it-matters subtitle. Detail screen sections: Why this matters; Barriers (chips, addable); Progress notes (dated list, addable, one-line entry field that is never covered by the keyboard — pin the input above the keyboard); Reminders for this goal. "Add reminder" launches the builder pre-filled with this goal.

**4. About You (the palette).** Purpose line: "What you know about yourself is the raw material for cues that actually catch your eye." Two sections — Passions & interests (chips with emoji + label) and Learnings (short text rows). Add/edit/delete each. Empty-state copy if cleared: "Add one thing you love or one thing you've learned about yourself — you'll pick cues from here."

**5. Reminder Builder (4-step wizard, progress dots, back always available).**
- Step 1 — Goal: pick an existing goal or create one inline (title + optional why). Offer 4 starter templates to defeat the blank slate: "Daily medication," "Move my body," "Call someone I love," "Ship the thing I keep postponing."
- Step 2 — Draw from yourself: show the palette chips AND this goal's barriers; tapping a chip drops it into a "working with" tray. Helper text: "Pick what's relevant — this is the palette, not a quiz." Entire step skippable.
- Step 3 — Intention: a fill-in sentence with two visible, clearly labeled text fields: "When **[the moment]** , I will **[the action]** ." Fields are obviously typeable (visible borders, placeholder examples), and the keyboard must never occlude the field or the helper copy — scroll content above the keyboard.
- Step 4 — One cue: "Choose ONE cue — the single most vivid thing for you. One beats many." Cue types: emoji, short phrase, photo (with required alt-text field), sound. If a palette chip is in the tray, pre-suggest cues derived from it (e.g., Biscuit chip → phrase "Biscuit's leash is the starting line"). Enforce exactly one selection. Then set schedule (daily/weekly, days, time) and confirm. Confirmation restates: cue + full intention text + "The text always travels with the cue, so the meaning is never lost." Saving appends a new reminder object to state following the JSON shape above.

**6. Cue-fire (notification preview / in-app modal).** Include a "Preview this reminder" action on each reminder so the cue-fire experience is demoable. Shows the cue large, the full intention text beneath it, and three equal-weight buttons: Done · Snooze 1h · Not today. Tapping Not today reveals an optional single-line "want to note why?" field (collapsed by default — never required). After Done, one warm line: "Followed through. Nice." After Not today: "Forgiven and rescheduled — one miss doesn't sink a goal." Each action appends a `reminder_events` entry.

**7. Settings.** Theme: Light / Dark / System with true parity (verify both modes on every screen — identical layout and contrast). Text size: slider from 100% to 200%, live preview, all layouts must reflow without truncation. Reduce motion toggle. Notification sound toggle. "Replay the intro." Privacy statement: "All data lives on this device. Deleting the app deletes everything."

## Design language — use the FollowThru brand style guide exactly
Clean, modern, warm: **warm coral on cream**. Rounded 16px cards, generous whitespace, simple outlined iconography, subtle motion (150–200ms ease, fully disabled under reduce-motion). Implement the brand as design tokens / CSS variables — never raw hex literals inside screen components — mirroring the native app's rule that color lives only in `Color.kt`/`AppColors` and screens consume semantic tokens.

### Color tokens — Light mode
- **Coral `#B5402C`** — the single brand accent: buttons (white label, 5.6:1), links, chips, fills, icons, the FAB, selected nav item, priority strip.
- **Coral Tint `#F6D9D2`** — decorative pale-coral circle backing only (e.g., "How it works" glyphs, example cards); glyph on the tint is Coral, never white.
- **Background `#FBF6EF`** — warm cream page.
- **Surface `#FFFFFF`** — cards. Cards separate by hairline **Border `#ECE3DA`**, not fill (surfaceVariant == surface).
- **Text `#2A2622`** primary (16:1 on cream); **Text Muted `#6E645D`** secondary (5.4:1 on cream / 5.8:1 on white).
- **Gold `#C9A24A`** on **Gold Surface `#F8EAC7`** with **Gold Icon `#9C7A1A`** — celebratory only, never body text. In this prototype, gold is reserved for the brief "Followed through. Nice." confirmation moment and the gentle weekly progress line's accent — never streak mechanics (excluded from MVP).
- **Error `#C0392B`** — destructive/delete only. **White `#FFFFFF`** — labels on coral fills.

### Color tokens — Dark mode (full parity required)
- **Coral `#E8775F`** — text/icon on dark 5.9:1; **On-Coral `#1E1B19`** dark labels on coral fills (button labels, day chips, FAB) — on-primary flips per mode so labels always pass AA.
- **Coral Tint `#3A2A25`** — dark decorative coral backing; glyph on it is Coral `#E8775F`.
- **Background `#1E1B19`**, **Surface `#2A2622`**, **Border `#3A332E`**.
- **Text `#F3ECE4`**, **Text Muted `#B5AAA1`**.
- **Gold `#D9B65E`** (also the celebratory icon color, ≈6.5:1) on **Gold Surface `#3A3320`**.
- **Error `#FF8A80`** (lifted for AA on dark).

### Overlays (mode-independent)
Modal scrim: black @ 60%. Overlay card surface: `#2A2622` always (so white `#FFFFFF` text reads in both modes) — use for the cue-fire modal backdrop and any reassurance overlay.

### Typography — Poppins (Regular + SemiBold)
displayMedium SemiBold 28/36 (app name, display heading) · headlineMedium SemiBold 20/28 (card/reminder titles) · headlineSmall SemiBold 17/24 (sub-headings) · bodyLarge Regular 16/24 (primary body) · bodyMedium Regular 14/20 (secondary) · bodySmall Regular 12/16 (captions) · labelLarge Regular 14/20 (buttons, UI chrome) · labelMedium Regular 12/16 (small labels).

### Brand principles (enforce)
One coral per mode is the only accent — no second accent color. Every light/dark pairing must hold AA (the deeper `#B5402C` exists precisely because a brighter coral failed at ~3.3:1 — do not lighten it). Gold is celebratory only, never body text. White labels on coral in light, dark labels on coral in dark. Cards separate by hairline outline, not fill. No raw hex literals in screen components — semantic tokens only.

## Responsive layout — phone AND tablet, consistent by design
This prototype is the reference for a native Android phone + tablet app, so the two form factors must feel like one product, not two designs. Build a single responsive system, not separate layouts:
- **Phone (< 600px width):** single-column, bottom navigation bar, full-width cards, the Reminder Builder as full-screen steps.
- **Tablet (≥ 600px width):** the same components scaled with intent — navigation rail on the left replacing the bottom bar (same four destinations, same icons, same labels, same order), Today and Goals as a two-column card grid with a max content width of ~840px, Goal detail as list-detail side-by-side, the Reminder Builder as a centered dialog/panel with identical steps and copy.
- Identical content, identical copy, identical interaction patterns, identical theming and spacing scale across both. Nothing exists on one form factor that doesn't exist on the other. Make the breakpoint behavior visible when resizing the preview.

## Accessibility — WCAG 2.1 AA throughout (non-negotiable)
Text contrast ≥ 4.5:1 (3:1 for large text) in BOTH themes; touch targets ≥ 48dp; visible focus indicators; every interactive element has an accessible name; every image/photo cue requires alt text; meaning never conveyed by color, emoji, or sound alone — paired text always present; inputs never hidden behind the keyboard; no content that auto-dismisses on a timer without user control; logical heading order and screen-reader-friendly labels on the wizard steps; supports 200% text scaling and both portrait and landscape orientations without loss of function.

## Deliverable alongside the app: ANDROID_HANDOFF.md
In addition to the working prototype, generate a markdown file named **ANDROID_HANDOFF.md** in the project root documenting everything you build, written for a developer **refining the existing native Android app FollowThru** (package `com.ideasinc.followthrough`, Kotlin / Jetpack Compose) in Android Studio — these flows will be merged into that codebase, so reference the existing package and keep the FollowThru name and identity intact throughout. It must contain:
1. **Screen inventory & navigation map** — every screen/route, its purpose line, and how screens connect (mapping bottom nav / nav rail to a Compose NavHost).
2. **Component inventory** — each reusable component (reminder card, palette chip, wizard step, snackbar-with-undo, cue-fire modal, etc.) with its props/state and the Compose equivalent to target.
3. **Data model** — the full JSON schema above restated as suggested Kotlin data classes and Room entities/DAOs, with notes on which fields drive which UI.
4. **State & business rules** — the one-cue enforcement, undo behavior and 8-second window, palette-drawn flag logic, local-metrics counters, schedule semantics, and how cue-fire maps to AlarmManager/WorkManager + a high-priority notification channel in native Android.
5. **Design tokens** — restate the brand style guide as implemented: the full light/dark/overlay color tables, the Material 3 colorScheme slot mapping (primary=Coral, onPrimary flipping White/On-Coral per mode, primaryContainer=Coral Tint, tertiary/tertiaryContainer=Gold/Gold Surface, outline=Border, error per mode), the `AppColors` brand-token names (BrandAccentText, CoralTint/OnCoralTint, Border, Gold/GoldIcon/GoldSurface, Destructive, OverlayScrim/OverlaySurface/OnOverlaySurface, SwitchUncheckedTrack), the Poppins type scale, spacing, radii, motion durations, and the phone/tablet breakpoint rules (Material 3 WindowSizeClass: Compact = phone layout, Medium/Expanded = tablet layout) — noting the native source of truth is `ui/theme/Color.kt`, `AppColors.kt`, `Theme.kt`, `Type.kt`.
6. **Accessibility checklist** — the AA requirements above restated as concrete TalkBack/contrast/touch-target/text-scaling acceptance criteria.
7. **Explicit exclusions** — what was intentionally not built (below) so the native app doesn't accidentally scope-creep.

## Explicitly exclude
No AI features, no account/login, no cloud sync, no social features, no streak counters or badges, no sub-goal hierarchies, no analytics SDKs. State is local only. **No renaming or rebranding** — the app is FollowThru, full stop; do not generate alternative names, logos, or taglines that alter the identity.
