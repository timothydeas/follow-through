package com.ideasinc.followthrough.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Brand-specific tokens layered on top of MaterialTheme.colorScheme. Centralized
// here so screen files don't carry raw Color(0xFF...) literals or inline
// isSystemInDarkTheme() conditionals. Light/dark adaptation lives in this file.

private val OverlayScrimBlack = Color(0xFF000000)
private val OverlayContainerSurface = Color(0xFF2A2622)

object AppColors {
    // Priority goal card accent (coral). Used as the 4dp left strip on priority
    // cards — same coral in both modes, carries no text.
    val PriorityContainer: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary

    val OnPriorityContainer: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onPrimary

    // The one coral for text/links/icons/small accents. Both modes pass AA:
    // light #B5402C on cream 5.2:1, dark #E8775F on dark surface 5.9:1.
    val BrandAccentText: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary

    // Pale-coral circle backing for "How it works" glyphs and example cards.
    // Decorative only — the glyph on it uses [OnCoralTint] (coral), never white.
    val CoralTint: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primaryContainer

    val OnCoralTint: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onPrimaryContainer

    // Hairline border / divider color (#ECE3DA light, #3A332E dark).
    val Border: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.outline

    // Gold — celebratory streak only. Never body text.
    val Gold: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.tertiary

    // Darker gold reserved for the streak flame icon so it meets 3:1 on the pale
    // gold surface. Light uses #9C7A1A (~3.4:1); dark keeps GoldDark (~6.5:1 on
    // the dark gold surface). Distinct from [Gold] so the streak number and any
    // other gold accent are unchanged.
    val GoldIcon: Color
        @Composable
        @ReadOnlyComposable
        get() = if (LocalAppDarkTheme.current) GoldDark else GoldIconLight

    val GoldSurface: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.tertiaryContainer

    val OnGoldSurface: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onTertiaryContainer

    // Destructive (delete) accents. Dark mode lifts to a brighter red so it meets
    // WCAG AA against the dark surface.
    val Destructive: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.error

    // Full-screen brand background (the celebratory ReassuranceOverlay). Coral in
    // both modes — it's a brand moment, not a surface. Label uses OnForgeBackground.
    val ForgeBackground: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary

    val OnForgeBackground: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onPrimary

    // Modal scrim — the dim layer behind an overlay card. Always black @ 60%.
    val OverlayScrim: Color = OverlayScrimBlack.copy(alpha = 0.6f)

    // Dark overlay card used by the reassurance overlay — always dark so white
    // text reads clearly regardless of the underlying theme.
    val OverlaySurface: Color = OverlayContainerSurface

    val OnOverlaySurface: Color = White

    // Switch unchecked track: a neutral muted tone (works in both modes).
    val SwitchUncheckedTrack: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
}
