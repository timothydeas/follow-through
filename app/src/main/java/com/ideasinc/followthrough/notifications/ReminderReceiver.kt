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

        val day = intent.getIntExtra(EXTRA_REMINDER_DAY, -1)

        if (intent.action == ACTION_GOAL_REMINDER) {
            // Per-goal reminder: surface this goal's implementation intention.
            val goalId = intent.getStringExtra(EXTRA_GOAL_ID)
            if (goalId == null) return
            val reminder = GoalReminderScheduler.read(context, goalId)
            val body = reminder?.body?.takeIf { it.isNotBlank() }
                ?: "Time to follow through on your plan."
            postNotification(context, body, goalNotificationId(goalId), goalId)
            if (day in Calendar.SUNDAY..Calendar.SATURDAY) {
                GoalReminderScheduler.rescheduleAfterFire(context, goalId, day)
            }
            return
        }

        // Global reminder (unchanged).
        postNotification(context, "Check in on your top goal", REMINDER_NOTIFICATION_ID)
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
            description = "FollowThru reminders"
        }
        nm.createNotificationChannel(channel)
    }

    private fun postNotification(
        context: Context,
        text: String,
        notificationId: Int,
        goalId: String? = null
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = if (goalId != null) {
                // Per-goal: reuse the running task (onNewIntent) when possible so
                // tapping deep-links to this goal without tearing the app down or
                // re-prompting biometric; a cold start still routes via onCreate.
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            } else {
                // Global reminder: unchanged.
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            if (goalId != null) putExtra(EXTRA_GOAL_ID, goalId)
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setColor(0xFF9B3A2E.toInt())
            .setContentTitle("FollowThru")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(tapPendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            nm.notify(notificationId, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted on Android 13+ — drop the notification silently.
        }
    }
}
