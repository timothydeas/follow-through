package com.ideasinc.followthrough.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

private val ScrimBlack = Color(0xFF000000)

// Warm coral on cream. surfaceVariant == surface so cards read as white on the
// cream page (light) / lifted surface on the near-black page (dark); hairline
// separation is carried by `outline` (AppColors.Border). Coral fills carry white
// labels in light and dark text in dark — both AA — so onPrimary flips per mode.
private val GroundedColorScheme = lightColorScheme(
    primary = CoralLight,
    onPrimary = White,
    primaryContainer = CoralTintLight,
    onPrimaryContainer = CoralLight,
    secondary = TextMutedLight,
    onSecondary = White,
    background = BgLight,
    onBackground = TextLight,
    surface = SurfaceLight,
    onSurface = TextLight,
    surfaceVariant = SurfaceLight,
    onSurfaceVariant = TextMutedLight,
    tertiary = GoldLight,
    tertiaryContainer = GoldSurfaceLight,
    onTertiaryContainer = TextLight,
    outline = BorderLight,
    error = LightError,
    onError = White,
    scrim = ScrimBlack,
)

private val GroundedDarkColorScheme = darkColorScheme(
    primary = CoralDark,
    onPrimary = OnCoralDark,
    primaryContainer = CoralTintDark,
    onPrimaryContainer = CoralDark,
    secondary = TextMutedDark,
    onSecondary = OnCoralDark,
    background = BgDark,
    onBackground = TextDark,
    surface = SurfaceDark,
    onSurface = TextDark,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = TextMutedDark,
    tertiary = GoldDark,
    tertiaryContainer = GoldSurfaceDark,
    onTertiaryContainer = TextDark,
    outline = BorderDark,
    error = DarkError,
    onError = White,
    scrim = ScrimBlack,
)

@Composable
fun GroundedTheme(content: @Composable () -> Unit) {
    // Resolve the effective dark/light mode from the user's manual preference,
    // falling back to the OS setting when ThemeMode.SYSTEM is selected.
    val themeMode by ThemePreferences.mode.collectAsState()
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (darkTheme) GroundedDarkColorScheme else GroundedColorScheme
    // Expose the resolved flag so brand tokens in AppColors stay consistent with
    // the override instead of reading isSystemInDarkTheme() directly.
    CompositionLocalProvider(LocalAppDarkTheme provides darkTheme) {
        androidx.compose.material3.MaterialTheme(
            colorScheme = colorScheme,
            typography = GroundedTypography,
            content = content
        )
    }
}
