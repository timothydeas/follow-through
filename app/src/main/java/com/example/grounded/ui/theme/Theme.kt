package com.example.grounded.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GroundedColorScheme = lightColorScheme(
    primary = PrimaryForge,
    onPrimary = White,
    primaryContainer = PrimaryForgeLight,
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
    error = Color(0xFFC0392B),
    onError = White,
)

private val GroundedDarkColorScheme = darkColorScheme(
    primary = PrimaryForgeDark,
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
    error = Color(0xFFC0392B),
    onError = White,
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
