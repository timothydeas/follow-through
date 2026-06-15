package com.ideasinc.followthrough.ui.settings

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val PREFS_NAME = "grounded_prefs"
private const val KEY_REDUCE_MOTION = "reduce_motion"
private const val KEY_NOTIFICATION_SOUND = "notification_sound_enabled"

/**
 * Display + notification preferences (reduce motion, notification sound). Backed by
 * SharedPreferences and exposed as observable StateFlows so the running UI reacts
 * immediately. Mirrors [com.ideasinc.followthrough.ui.theme.ThemePreferences].
 *
 * Text size is intentionally NOT here: it follows the OS font-scale setting
 * directly (WCAG 1.4.4), so there is no in-app slider to back.
 */
object SettingsPreferences {
    private val _reduceMotion = MutableStateFlow(false)
    val reduceMotion: StateFlow<Boolean> = _reduceMotion.asStateFlow()

    private val _notificationSound = MutableStateFlow(true)
    val notificationSound: StateFlow<Boolean> = _notificationSound.asStateFlow()

    /** Loads persisted values. Call once at startup. */
    fun load(context: Context) {
        val p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _reduceMotion.value = p.getBoolean(KEY_REDUCE_MOTION, false)
        _notificationSound.value = p.getBoolean(KEY_NOTIFICATION_SOUND, true)
    }

    fun setReduceMotion(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_REDUCE_MOTION, enabled).apply()
        _reduceMotion.value = enabled
    }

    fun setNotificationSound(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_NOTIFICATION_SOUND, enabled).apply()
        _notificationSound.value = enabled
    }
}

/** Reduce-motion flag for composables to gate non-essential animation. */
val LocalReduceMotion = staticCompositionLocalOf { false }
