package com.ideasinc.followthrough

import android.app.Application
import android.content.Context
import com.ideasinc.followthrough.di.AppContainer
import com.ideasinc.followthrough.notifications.KEY_REMINDERS_ENABLED
import com.ideasinc.followthrough.notifications.PREFS_REMINDERS
import com.ideasinc.followthrough.ui.theme.ThemePreferences

private const val KEY_REMINDERS_RESET_V1 = "reminders_reset_v1"

class GroundedApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ThemePreferences.load(this)
        resetRemindersOnceForExactAlarmMigration()
    }

    private fun resetRemindersOnceForExactAlarmMigration() {
        val prefs = getSharedPreferences(PREFS_REMINDERS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_REMINDERS_RESET_V1, false)) return
        prefs.edit()
            .putBoolean(KEY_REMINDERS_ENABLED, false)
            .putBoolean(KEY_REMINDERS_RESET_V1, true)
            .apply()
    }
}
