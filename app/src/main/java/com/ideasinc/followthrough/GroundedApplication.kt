package com.ideasinc.followthrough

import android.app.Application
import com.ideasinc.followthrough.di.AppContainer
import com.ideasinc.followthrough.notifications.GoalReminderScheduler
import com.ideasinc.followthrough.ui.theme.ThemePreferences

class GroundedApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ThemePreferences.load(this)
        // One-time: clear stale goal/plan-keyed reminders so the store is keyed by
        // check-in (matching the v33→v34 revert). Idempotent.
        GoalReminderScheduler.clearForCheckInRekey(this)
    }
}
