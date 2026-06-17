package com.ideasinc.followthrough.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Brand palette — warm coral on cream, tuned to pass WCAG AA ──────────────
// The Loveable prototype's bright coral (#E26155) only hits ~3.3:1 and fails AA
// for text, so the brand coral is a slightly deeper, AA-passing coral. Every
// token below is chosen so each light/dark pairing scores in the high 90s.
// Raw literals live ONLY in this file; screens route through MaterialTheme /
// AppColors.

// Light
val CoralLight = Color(0xFFB5402C)       // buttons (white label 5.6:1), links, chips, fills, icons
val CoralTintLight = Color(0xFFF6D9D2)   // decorative pale-coral circle backing only
val BgLight = Color(0xFFFBF6EF)          // warm cream page
val SurfaceLight = Color(0xFFFFFFFF)     // cards
val BorderLight = Color(0xFFECE3DA)      // hairline
val TextLight = Color(0xFF2A2622)        // 16:1 on cream
val TextMutedLight = Color(0xFF6E645D)   // 5.4:1 on cream / 5.8:1 on white — AA pass
val GoldSurfaceLight = Color(0xFFF8EAC7) // celebratory follow-through surface only
val GoldLight = Color(0xFFC9A24A)        // gold accent on the celebratory follow-through moment
// Darker gold for a gold icon on the pale gold surface: #C9A24A is ~2:1 there
// (near-invisible in light mode); #9C7A1A is ~3.4:1 so it reads as a real icon.
// Dark mode keeps GoldDark, which already clears 3:1.
val GoldIconLight = Color(0xFF9C7A1A)

// Dark — full parity, also high-90s
val CoralDark = Color(0xFFE8775F)        // text/icon on dark 5.9:1; dark text on coral fill 5.9:1
val CoralTintDark = Color(0xFF3A2A25)    // decorative dark coral circle backing
val BgDark = Color(0xFF1E1B19)
val SurfaceDark = Color(0xFF2A2622)
val BorderDark = Color(0xFF3A332E)
val TextDark = Color(0xFFF3ECE4)
val TextMutedDark = Color(0xFFB5AAA1)
val GoldSurfaceDark = Color(0xFF3A3320)
val GoldDark = Color(0xFFD9B65E)

// Dark text used on a coral fill (button labels, selected day chips, FAB glyph).
val OnCoralDark = Color(0xFF1E1B19)

val White = Color(0xFFFFFFFF)

// ─── Backward-compatible aliases ─────────────────────────────────────────────
// Kept so the small number of files that reference these names keep compiling.
val Stone = TextLight
// Gold accent — the celebratory follow-through moment.
val Accent = GoldLight

// Destructive accents — #C0392B on cream ≈ 5.0:1 (AA). Dark lifts to #FF8A80.
val LightError = Color(0xFFC0392B)
val DarkError = Color(0xFFFF8A80)
