package com.example.grounded.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.grounded.MainActivity
import com.example.grounded.R

const val NOTIFICATION_CHANNEL_ID = "follow_through_reminders"
private const val REMINDER_NOTIFICATION_ID = 1

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        ensureChannel()
        postNotification()
        return Result.success()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
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

    private fun postNotification() {
        val nm = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val tapIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val tapPendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
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
