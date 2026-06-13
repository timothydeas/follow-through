package com.ideasinc.followthrough.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/** The default reminder notification channel id. */
const val NOTIFICATION_CHANNEL_ID = "follow_through_reminders"

/**
 * Creates the default reminder notification channel (idempotent). Every reminder
 * posts on this channel; cue sound is governed by the Settings notification-sound
 * toggle at post time (see [ReminderFireReceiver]).
 */
internal fun ensureReminderChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
    val channel = NotificationChannel(
        NOTIFICATION_CHANNEL_ID,
        "Reminders",
        NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
        description = "FollowThru reminders"
    }
    nm.createNotificationChannel(channel)
}
