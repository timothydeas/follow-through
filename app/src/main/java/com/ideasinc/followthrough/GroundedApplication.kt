package com.ideasinc.followthrough

import android.app.Application
import android.content.Context
import com.ideasinc.followthrough.debug.DemoSeed
import com.ideasinc.followthrough.di.AppContainer
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

        // DEBUG ONLY: populate demo content for screenshots when the DB is empty.
        // Strictly gated to debug builds — release ships the normal empty first-run.
        if (BuildConfig.DEBUG) {
            CoroutineScope(Dispatchers.IO).launch { DemoSeed.seedIfEmpty(container) }
        }
    }
}
