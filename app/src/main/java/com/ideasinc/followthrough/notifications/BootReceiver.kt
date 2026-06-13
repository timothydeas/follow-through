package com.ideasinc.followthrough.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON"
        ) return

        val appContext = context.applicationContext
        // Re-register Reminder-model alarms (read from Room) off the main thread;
        // goAsync keeps the broadcast alive until the DB read completes.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ReminderAlarmScheduler.rescheduleAllActive(appContext)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
