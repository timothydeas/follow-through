package com.ideasinc.followthrough.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ideasinc.followthrough.MainActivity
import com.ideasinc.followthrough.R
import java.util.Calendar

const val NOTIFICATION_CHANNEL_ID = "follow_through_reminders"
private const val REMINDER_NOTIFICATION_ID = 1

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        ensureChannel(context)
        postNotification(context)

        val day = intent.getIntExtra(EXTRA_REMINDER_DAY, -1)
        if (day in Calendar.SUNDAY..Calendar.SATURDAY) {
            ReminderScheduler.rescheduleAfterFire(context, day)
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Follow Through reminders"
        }
        nm.createNotificationChannel(channel)
    }

    private fun postNotification(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setColor(0xFFA8431E.toInt())
            .setContentTitle("Follow Through")
            .setContentText("Check in on your top goal")
            .setContentIntent(tapPendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            nm.notify(REMINDER_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted on Android 13+ — drop the notification silently.
        }
    }
}
