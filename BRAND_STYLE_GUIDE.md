# FollowThru — Brand Style Guide

> Warm coral on cream. Every color pairing is tuned to pass WCAG AA. Raw color
> literals live only in `ui/theme/Color.kt`; screens consume them through
> `MaterialTheme.colorScheme` and `AppColors`.

---

## 1. Color Palette

### Light mode

| Token | Hex | Role |
|---|---|---|
| Coral | `#B5402C` | Buttons (white label, 5.6:1), links, chips, fills, icons |
| Coral Tint | `#F6D9D2` | Decorative pale-coral circle backing only |
| Background | `#FBF6EF` | Warm cream page |
| Surface | `#FFFFFF` | Cards |
| Border | `#ECE3DA` | Hairline divider |
| Text | `#2A2622` | Primary text — 16:1 on cream |
| Text Muted | `#6E645D` | Secondary text — 5.4:1 on cream / 5.8:1 on white (AA) |
| Gold Surface | `#F8EAC7` | Celebratory follow-through surface only |
| Gold | `#C9A24A` | Gold accent on the celebratory follow-through moment |
| Gold Icon | `#9C7A1A` | Celebratory follow-through icon only (3.4:1 on pale gold) |
| Error | `#C0392B` | Destructive / delete (≈5.0:1 on cream, AA) |
| White | `#FFFFFF` | Labels on coral fills |

### Dark mode (full parity, also high-90s contrast)

| Token | Hex | Role |
|---|---|---|
| Coral | `#E8775F` | Text/icon on dark 5.9:1; dark text on coral fill 5.9:1 |
| Coral Tint | `#3A2A25` | Decorative dark coral circle backing |
| Background | `#1E1B19` | Page |
| Surface | `#2A2622` | Cards |
| Border | `#3A332E` | Hairline divider |
| Text | `#F3ECE4` | Primary text |
| Text Muted | `#B5AAA1` | Secondary text |
| Gold Surface | `#3A3320` | Celebratory follow-through surface |
| Gold | `#D9B65E` | Gold accent + celebratory follow-through icon (≈6.5:1) |
| On-Coral | `#1E1B19` | Dark text/glyph on a coral fill (button labels, day chips, FAB) |
| Error | `#FF8A80` | Destructive / delete (lifted for AA on dark) |

### Overlays (mode-independent)

| Token | Value | Role |
|---|---|---|
| Overlay Scrim | Black @ 60% | Dim layer behind modal cards |
| Overlay Surface | `#2A2622` | Reassurance overlay card (always dark so white text reads) |
| On Overlay Surface | `#FFFFFF` | Text on the overlay card |

---

## 2. Semantic Token Mapping

The theme maps the raw palette onto Material 3 `colorScheme` slots. Screens
should reference these — not raw hexes.

| Material slot | Light | Dark | Used for |
|---|---|---|---|
| `primary` | Coral `#B5402C` | Coral `#E8775F` | Brand accent, buttons, links |
| `onPrimary` | White | On-Coral `#1E1B19` | Label on coral fills (flips per mode) |
| `primaryContainer` | Coral Tint `#F6D9D2` | `#3A2A25` | Decorative coral circle backing |
| `onPrimaryContainer` | Coral `#B5402C` | Coral `#E8775F` | Glyph on the coral tint (never white) |
| `secondary` | Text Muted | Text Muted | Secondary accents |
| `background` | `#FBF6EF` | `#1E1B19` | Page |
| `surface` / `surfaceVariant` | `#FFFFFF` | `#2A2622` | Cards (surfaceVariant == surface) |
| `onSurface` | Text | Text | Body text on cards |
| `onSurfaceVariant` | Text Muted | Text Muted | Muted body / switch track base |
| `tertiary` | Gold | Gold | Celebratory follow-through accent (never body text) |
| `tertiaryContainer` | Gold Surface | Gold Surface | Celebratory follow-through surface |
| `outline` | Border | Border | Hairline separation (cards read as white via outline, not fill) |
| `error` | `#C0392B` | `#FF8A80` | Destructive actions |
| `scrim` | Black | Black | Modal scrim base |

### `AppColors` brand tokens (composable accessors)

`AppColors` wraps the slots above with brand-intent names so screens carry no
raw literals or inline `isSystemInDarkTheme()` checks:

- `BrandAccentText` — the one coral for text/links/icons/small accents
- `CoralTint` / `OnCoralTint` — pale-coral glyph backing ("How it works", example cards)
- `Border` — hairline divider
- `Gold` — celebratory follow-through accent
- `GoldIcon` — darker gold reserved for the celebratory follow-through icon (meets 3:1 on pale gold)
- `GoldSurface` / `OnGoldSurface` — celebratory follow-through surface + text
- `Destructive` — delete accents
- `OverlayScrim` / `OverlaySurface` / `OnOverlaySurface` — modal overlay layer
- `SwitchUncheckedTrack` — neutral muted track (`onSurfaceVariant` @ 45%)

---

## 3. Typography

**Typeface:** Poppins (Regular + SemiBold). `LoraFontFamily` and
`DmSansFontFamily` are aliases of Poppins kept only for compile compatibility.

| Style | Weight | Size / Line height | Use |
|---|---|---|---|
| `displayMedium` | SemiBold | 28 / 36 | App name, display heading |
| `headlineMedium` | SemiBold | 20 / 28 | Note titles |
| `headlineSmall` | SemiBold | 17 / 24 | Sub-headings |
| `bodyLarge` | Regular | 16 / 24 | Primary body text |
| `bodyMedium` | Regular | 14 / 20 | Secondary body |
| `bodySmall` | Regular | 12 / 16 | Captions |
| `labelLarge` | Regular | 14 / 20 | Buttons, UI chrome |
| `labelMedium` | Regular | 12 / 16 | Small labels |

---

## 4. Principles

1. **Warm coral on cream.** Coral is the single accent — buttons, links, icons,
   small fills. One coral per mode, both AA.
2. **Accessibility is non-negotiable.** Every light/dark pairing scores in the
   high 90s. The brand coral is intentionally deeper than the prototype's bright
   `#E26155` (which failed AA at ~3.3:1).
3. **Gold is celebratory only.** Reserved for the follow-through celebration moment and the gentle weekly-progress accent — never body text.
4. **White labels on coral in light, dark labels on coral in dark.** `onPrimary`
   flips per mode so labels always pass AA.
5. **Cards separate by hairline, not fill.** `surfaceVariant == surface`; the
   `outline` token carries card separation.
6. **No raw literals in screens.** All color lives in `Color.kt` and routes
   through `MaterialTheme` / `AppColors`.

---

## 5. Iconography

Modern icons throughout the app. The whole UI uses **Material Symbols (Rounded)** —
the rounded-optical Material 3 icon set — consistently, never a mix of styles.

- **Emoji are user content only** (cue values, interest chips) — never interface
  decoration. UI affordances are always Material Symbols, never emoji.
- **Every icon-only control has a `contentDescription`** (the accessibility name).
- **Gold-tinted icons** appear only in the celebratory follow-through moment, never
  as a standard nav or action affordance.
- **Brand glyph / wordmark** = the FollowThru launcher icon (coral background + white
  up-arrow foreground), reproduced in-app wherever a logo mark appears. Never
  substitute an alternate logo.
- Decorative onboarding example art (e.g. the worked-example illustration glyphs) is
  illustrative only and is not part of this system icon set.

| Purpose | Material Symbols (Rounded) | Used in |
|---|---|---|
| Intentions (nav / home) | `bolt` | bottom bar / nav rail |
| What worked (nav) | `insights` | bottom bar / nav rail |
| Settings (nav) | `settings` | bottom bar / nav rail |
| Add (new intention) | `add` | Intentions FAB |
| Edit | `edit` | Intentions card, create flow |
| Delete | `delete` | edit (create) screen |
| Close | `close` | create flow |
| Back | `arrow_back` | create flow, Settings |
| Continue / forward | `arrow_forward` | onboarding, create flow |
| Disclosure chevron | `chevron_right` | Settings rows |
| Expand / collapse | `keyboard_arrow_up` / `keyboard_arrow_down` | accessible expanders |
| Theme: light / dark / system | `light_mode` / `dark_mode` / `brightness_auto` | Settings |

The "Did it" response is a labelled text button, not an icon. Decorative onboarding example glyphs (e.g. `directions_run`, `home`, `lock`) are illustrative art, not part of this system set. The brand launcher glyph is the only `painterResource` icon (the FollowThru app icon).

---

*Source of truth: `app/src/main/java/com/ideasinc/followthrough/ui/theme/`
(`Color.kt`, `AppColors.kt`, `Theme.kt`, `Type.kt`). Update this guide when those
files change.*
