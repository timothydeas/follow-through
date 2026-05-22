package com.ideasinc.followthrough.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Brand-specific tokens layered on top of MaterialTheme.colorScheme. Centralized
// here so screen files don't carry raw Color(0xFF...) literals or inline
// isSystemInDarkTheme() conditionals. Light/dark adaptation lives in this file.

private val OverlayScrimBlack = Color(0xFF000000)
private val OverlayContainerSurface = Color(0xFF2C2C28)

object AppColors {
    // Priority goal card (always TrackRed bg with white text — high-contrast brand
    // call-out in both modes).
    val PriorityContainer: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary

    val OnPriorityContainer: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onPrimary

    // Cancel/Save-style text buttons on regular surfaces. TrackRed lacks contrast
    // on the dark surface, so dark mode falls back to onSurface (white).
    val BrandAccentText: Color
        @Composable
        @ReadOnlyComposable
        get() = if (LocalAppDarkTheme.current)
            MaterialTheme.colorScheme.onSurface
        else
            MaterialTheme.colorScheme.primary

    // Destructive (delete) accents. Dark mode lifts to a brighter red so it meets
    // WCAG AA against the dark surface (#1C1C1A): #FF8A80 ≈ 6.6:1.
    val Destructive: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.error

    // Full-screen forge background (LaunchInsight, Onboarding header, follow-through
    // reassurance). Same TrackRed in both modes — it's a brand moment, not a surface.
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

    // Switch unchecked track: muted primary (works in both modes via alpha).
    val SwitchUncheckedTrack: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)

    // Onboarding body section uses an explicitly light surface even in dark mode
    // (it sits on the forge red, so a dark card would clash). Always light.
    val OnboardingBodySurface: Color = White

    val OnOnboardingBodySurface: Color = Stone
}
