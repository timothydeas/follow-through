package com.example.grounded

import android.app.Application
import android.content.Context
import com.example.grounded.di.AppContainer
import com.example.grounded.notifications.KEY_REMINDERS_ENABLED
import com.example.grounded.notifications.PREFS_REMINDERS

private const val KEY_REMINDERS_RESET_V1 = "reminders_reset_v1"

class GroundedApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
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
