package com.ideasinc.followthrough.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ideasinc.followthrough.MainActivity
import com.ideasinc.followthrough.R
import com.ideasinc.followthrough.data.GroundedDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

const val NOTIFICATION_CHANNEL_ID = "follow_through_reminders"

/**
 * Creates the default reminder notification channel (idempotent). Used by every
 * reminder that has no custom cue sound.
 */
internal fun ensureReminderChannel(context: Context) {
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

/**
 * Sound is per-channel on Android O+ and a channel's sound is fixed once created,
 * so a goal with a custom cue sound gets its own channel keyed by (goal, sound):
 * id = "ft_cue_<goalHash>_<soundHash>". Changing the sound yields a new id (a new
 * channel); the goal's previous cue channels are deleted so they don't pile up.
 * Returns the channel id to post on. IMPORTANCE_DEFAULT keeps normal vibration and
 * respects system DND/silent (bypassDnd is never set).
 */
internal fun ensureCueChannel(context: Context, goalId: String, soundUri: Uri): String {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        ?: return NOTIFICATION_CHANNEL_ID
    val prefix = "ft_cue_${goalId.hashCode()}_"
    val desiredId = prefix + soundUri.toString().hashCode()
    // Remove this goal's stale cue channels (a previously-chosen sound).
    nm.notificationChannels
        .filter { it.id.startsWith(prefix) && it.id != desiredId }
        .forEach { nm.deleteNotificationChannel(it.id) }
    if (nm.getNotificationChannel(desiredId) == null) {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        nm.createNotificationChannel(
            NotificationChannel(desiredId, "Reminder cue", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "A distinctive reminder sound for one goal"
                setSound(soundUri, attrs)
            }
        )
    }
    return desiredId
}

/** Deletes all of a goal's custom cue channels (on sound clear or goal delete). */
fun deleteCueChannels(context: Context, goalId: String) {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        ?: return
    val prefix = "ft_cue_${goalId.hashCode()}_"
    nm.notificationChannels
        .filter { it.id.startsWith(prefix) }
        .forEach { nm.deleteNotificationChannel(it.id) }
}

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_GOAL_REMINDER) return
        val checkInId = intent.getStringExtra(EXTRA_CHECKIN_ID) ?: return
        val day = intent.getIntExtra(EXTRA_REMINDER_DAY, -1)
        val appContext = context.applicationContext

        // Do the DB read + notification build off the main thread. goAsync() keeps
        // the broadcast alive until pendingResult.finish(); the suspend Room queries
        // run on a background coroutine, so the main thread is never blocked.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ensureReminderChannel(appContext)
                val db = GroundedDatabase.getInstance(appContext)
                val checkIn = db.checkInDao().getCheckInById(checkInId) ?: return@launch
                val goal = db.goalDao().getGoalById(checkIn.goalId)

                // ── Channel (carries the cue sound, if any) ──
                val soundUri = CueImageStore.uriOrNull(checkIn.cueSound)
                val channelId = if (soundUri != null) {
                    ensureCueChannel(appContext, checkInId, soundUri)
                } else {
                    deleteCueChannels(appContext, checkInId) // sound cleared — drop stale channels
                    NOTIFICATION_CHANNEL_ID
                }

                // ── The reminder shows ONLY this check-in's implementation
                // intention (the "When …, I will …" wording from step 3). This text
                // alone conveys the full reminder (WCAG 1.1.1 / 1.4.1). ──
                val intention = checkIn.intention?.takeIf { it.isNotBlank() }
                    ?: goal?.title?.takeIf { it.isNotBlank() }
                    ?: "FollowThru"

                postNotification(
                    appContext, channelId, intention,
                    checkInNotificationId(checkInId), checkInId
                )

                if (day in Calendar.SUNDAY..Calendar.SATURDAY) {
                    GoalReminderScheduler.rescheduleAfterFire(appContext, checkInId, day)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun postNotification(
        context: Context,
        channelId: String,
        intention: String,
        notificationId: Int,
        checkInId: String
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            // Reuse the running task (onNewIntent) when possible so tapping opens
            // THIS check-in without tearing the app down or re-prompting biometric;
            // a cold start still routes via onCreate.
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_CHECKIN_ID, checkInId)
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // The notification carries only the implementation intention. BigTextStyle
        // lets a longer intention expand fully on the lock/home screen.
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setColor(0xFFB5402C.toInt())
            .setContentTitle(intention)
            .setContentIntent(tapPendingIntent)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(intention))

        try {
            nm.notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted on Android 13+ — drop the notification silently.
        }
    }
}
