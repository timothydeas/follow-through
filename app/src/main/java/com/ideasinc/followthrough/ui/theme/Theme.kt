package com.ideasinc.followthrough.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// #C0392B on white = 5.1:1 (AA pass). On the dark surface (#1C1C1A) it only
// hits ~3.0:1 which fails AA for normal text. Dark mode lifts to #FF8A80
// (M3 dark error standard) ≈ 6.6:1 on the same surface — safe for delete icons
// and destructive button labels.
private val LightError = Color(0xFFC0392B)
private val DarkError = Color(0xFFFF8A80)
private val ScrimBlack = Color(0xFF000000)

private val GroundedColorScheme = lightColorScheme(
    primary = TrackRed,
    onPrimary = White,
    primaryContainer = TrackRedLight,
    onPrimaryContainer = Stone,
    secondary = MutedStone,
    onSecondary = White,
    background = Cream,
    onBackground = Stone,
    surface = Cream,
    onSurface = Stone,
    surfaceVariant = CreamDark,
    onSurfaceVariant = MutedStone,
    tertiaryContainer = Color(0xFFF2E8D4),
    outline = MutedStone,
    error = LightError,
    onError = White,
    scrim = ScrimBlack,
)

private val GroundedDarkColorScheme = darkColorScheme(
    primary = TrackRed,
    onPrimary = White,
    primaryContainer = DarkTagChip,
    onPrimaryContainer = DarkPrimaryText,
    secondary = DarkSecondaryText,
    onSecondary = White,
    background = DarkBackground,
    onBackground = DarkPrimaryText,
    surface = DarkBackground,
    onSurface = DarkPrimaryText,
    surfaceVariant = DarkCard,
    onSurfaceVariant = DarkSecondaryText,
    tertiaryContainer = DarkPinnedCard,
    outline = DarkSecondaryText,
    error = DarkError,
    onError = White,
    scrim = ScrimBlack,
)

@Composable
fun GroundedTheme(content: @Composable () -> Unit) {
    val colorScheme = if (isSystemInDarkTheme()) GroundedDarkColorScheme else GroundedColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = GroundedTypography,
        content = content
    )
}
