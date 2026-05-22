package com.ideasinc.followthrough.ui.theme

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Manual light/dark override. SYSTEM defers to the OS setting. */
enum class ThemeMode { LIGHT, DARK, SYSTEM }

private const val PREFS_NAME = "grounded_prefs"
private const val KEY_THEME_MODE = "theme_mode"

/**
 * App-wide light/dark mode preference. Backed by SharedPreferences and exposed
 * as an observable [StateFlow] so [GroundedTheme] recomposes the instant the
 * user changes the setting. Initialised once from GroundedApplication.
 */
object ThemePreferences {
    private val _mode = MutableStateFlow(ThemeMode.SYSTEM)
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    /** Loads the persisted preference. Call once at app startup. */
    fun load(context: Context) {
        _mode.value = read(context)
    }

    /** Persists [mode] and immediately applies it to the running UI. */
    fun setMode(context: Context, mode: ThemeMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_MODE, mode.name)
            .apply()
        _mode.value = mode
    }

    private fun read(context: Context): ThemeMode {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        return runCatching { ThemeMode.valueOf(raw ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)
    }
}

/**
 * Effective dark-mode flag for the resolved theme. Unlike `isSystemInDarkTheme()`
 * this honours the manual [ThemeMode] override, so brand tokens in [AppColors]
 * stay consistent with the active color scheme.
 */
val LocalAppDarkTheme = staticCompositionLocalOf { false }
