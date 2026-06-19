package com.ideasinc.followthrough

import android.app.Application
import android.content.Context
import com.ideasinc.followthrough.debug.DemoSeed
import com.ideasinc.followthrough.di.AppContainer
import com.ideasinc.followthrough.notifications.ReminderAlarmScheduler
import com.ideasinc.followthrough.ui.settings.SettingsPreferences
import com.ideasinc.followthrough.ui.theme.ThemePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GroundedApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ThemePreferences.load(this)
        SettingsPreferences.load(this)
        // The legacy per-check-in reminder store is retired. Clear it so any stale
        // pending alarms aren't re-registered on boot (they're not rescheduled by
        // BootReceiver anymore; already-armed ones fire once to a now-absent receiver
        // and are dropped). Idempotent and cheap.
        getSharedPreferences("grounded_goal_reminders", Context.MODE_PRIVATE).edit().clear().apply()

        // Safety net for alarm survival (CLAUDE.md rule #7). The OS clears all pending alarms when
        // the app is updated, and some OEM task-killers drop them too; the MY_PACKAGE_REPLACED
        // receiver covers the update, and this re-arms on every cold start as a belt-and-suspenders
        // so a returning user's reminders are always restored. Idempotent — schedule() cancels then
        // re-arms, and skips paused / reminderless intentions.
        CoroutineScope(Dispatchers.IO).launch {
            ReminderAlarmScheduler.rescheduleAllActive(this@GroundedApplication)
        }

        // DEBUG ONLY: populate demo content for screenshots when the DB is empty.
        // Strictly gated to debug builds — release ships the normal empty first-run.
        if (BuildConfig.DEBUG) {
            CoroutineScope(Dispatchers.IO).launch { DemoSeed.seedIfEmpty(container) }
        }
    }
}
