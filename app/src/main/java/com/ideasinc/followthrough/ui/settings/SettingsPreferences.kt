package com.ideasinc.followthrough.ui.settings

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val PREFS_NAME = "grounded_prefs"
private const val KEY_TEXT_SCALE = "text_scale"
private const val KEY_REDUCE_MOTION = "reduce_motion"
private const val KEY_NOTIFICATION_SOUND = "notification_sound_enabled"

/**
 * Display + notification preferences (handoff Settings: text size 100–200%, reduce
 * motion, notification sound). Backed by SharedPreferences and exposed as
 * observable StateFlows so the running UI reacts immediately. Mirrors
 * [com.ideasinc.followthrough.ui.theme.ThemePreferences].
 */
object SettingsPreferences {
    private val _textScale = MutableStateFlow(1.0f)
    val textScale: StateFlow<Float> = _textScale.asStateFlow()

    private val _reduceMotion = MutableStateFlow(false)
    val reduceMotion: StateFlow<Boolean> = _reduceMotion.asStateFlow()

    private val _notificationSound = MutableStateFlow(true)
    val notificationSound: StateFlow<Boolean> = _notificationSound.asStateFlow()

    /** Loads persisted values. Call once at startup. */
    fun load(context: Context) {
        val p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _textScale.value = p.getFloat(KEY_TEXT_SCALE, 1.0f).coerceIn(1.0f, 2.0f)
        _reduceMotion.value = p.getBoolean(KEY_REDUCE_MOTION, false)
        _notificationSound.value = p.getBoolean(KEY_NOTIFICATION_SOUND, true)
    }

    /**
     * Updates the in-memory scale only (no disk write) — used during a slider drag
     * so text reflows live every frame without thrashing SharedPreferences (the disk
     * write per frame was stalling the drag gesture). Persist with [setTextScale] on
     * drag end.
     */
    fun setTextScaleLive(scale: Float) {
        _textScale.value = scale.coerceIn(1.0f, 2.0f)
    }

    fun setTextScale(context: Context, scale: Float) {
        val v = scale.coerceIn(1.0f, 2.0f)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putFloat(KEY_TEXT_SCALE, v).apply()
        _textScale.value = v
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
