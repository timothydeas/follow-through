package com.ideasinc.followthrough.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.ideasinc.followthrough.MainActivity
import com.ideasinc.followthrough.R
import com.ideasinc.followthrough.data.CueType
import com.ideasinc.followthrough.data.EventAction
import com.ideasinc.followthrough.data.GroundedDatabase
import com.ideasinc.followthrough.data.Reminder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Cue-fire for the new Reminder model. Two responsibilities:
 *  - ACTION_REMINDER_FIRE: post a high-priority notification carrying the cue PLUS
 *    the full intention text (the text always carries complete meaning — WCAG
 *    1.1.1 / 1.4.1 and the brief's multi-sense principle), with three equal,
 *    judgment-free actions: Done · Snooze · Not today. Then re-arm next week.
 *  - ACTION_REMINDER_RESPONSE: append a ReminderEvent for the tapped action,
 *    re-fire ~1h out on Snooze, and dismiss the notification.
 *
 * All DB work runs off the main thread under goAsync(), mirroring the legacy
 * ReminderReceiver's reliability contract.
 */
class ReminderFireReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        when (intent.action) {
            ACTION_REMINDER_FIRE -> handleFire(appContext, intent)
            ACTION_REMINDER_RESPONSE -> handleResponse(appContext, intent)
        }
    }

    private fun handleFire(appContext: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val day = intent.getIntExtra(EXTRA_FIRE_DAY, -1)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ensureReminderChannel(appContext)
                val db = GroundedDatabase.getInstance(appContext)
                val reminder = db.reminderDao().getReminderById(reminderId) ?: return@launch
                postReminderNotification(appContext, reminder)
                // Re-arm the weekly cadence for the day that just fired (skip the
                // one-shot snooze slot, day 0).
                if (day in Calendar.SUNDAY..Calendar.SATURDAY) {
                    ReminderAlarmScheduler.rescheduleAfterFire(appContext, reminder, day)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleResponse(appContext: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val action = intent.getStringExtra(EXTRA_RESPONSE)
        if (!isValidResponse(action)) return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, reminderNotificationId(reminderId))
        val deliveredAt = intent.getLongExtra(EXTRA_FIRE_DAY, System.currentTimeMillis())
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = GroundedDatabase.getInstance(appContext)
                recordReminderEvent(
                    eventDao = db.reminderEventDao(),
                    reminderId = reminderId,
                    action = action!!,
                    deliveredAt = System.currentTimeMillis()
                )
                if (action == EventAction.SNOOZED) {
                    ReminderAlarmScheduler.snooze(appContext, reminderId)
                }
                val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.cancel(notificationId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun postReminderNotification(context: Context, reminder: Reminder) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val notificationId = reminderNotificationId(reminder.id)

        // Keep the notification short: the cue alone is the title, the full intention
        // text is the body. No goal name (it made notifications too long). The text
        // always carries complete meaning so it never depends on the cue alone
        // (WCAG 1.1.1 / 1.4.1). Only emoji/phrase cues are surfaced as text; photo/
        // sound cue values are launch-off and never reach here.
        val cue = if (reminder.cueType == CueType.EMOJI || reminder.cueType == CueType.PHRASE) {
            reminder.cueValue.trim()
        } else ""
        val intention = reminder.intentionText
        val title = cue.ifBlank { intention }
        // When there's no cue, the intention is already the title — don't repeat it.
        val body = if (cue.isNotBlank()) intention else ""

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val tapPending = PendingIntent.getActivity(
            context, notificationId, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setColor(0xFFB5402C.toInt())
            .setContentTitle(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(tapPending)
            .setAutoCancel(true)
            .addAction(0, "Done", responsePending(context, reminder.id, EventAction.DONE, notificationId))
            .addAction(0, "Snooze", responsePending(context, reminder.id, EventAction.SNOOZED, notificationId))
            .addAction(0, "Not today", responsePending(context, reminder.id, EventAction.NOT_TODAY, notificationId))
        if (body.isNotBlank()) {
            builder.setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        }

        // Honor the Settings notification-sound toggle (read straight from prefs —
        // this runs in a background broadcast where the in-memory flow may be cold).
        val soundOn = context.getSharedPreferences("grounded_prefs", Context.MODE_PRIVATE)
            .getBoolean("notification_sound_enabled", true)
        if (!soundOn) builder.setSilent(true)

        try {
            nm.notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted on Android 13+ — drop silently.
        }
    }

    private fun responsePending(context: Context, reminderId: String, action: String, notificationId: Int): PendingIntent {
        val intent = Intent(context, ReminderFireReceiver::class.java).apply {
            this.action = ACTION_REMINDER_RESPONSE
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_RESPONSE, action)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        // Distinct request code per (reminder, action) so the three actions don't
        // collide.
        val rc = (reminderId.hashCode() and 0x000FFFFF) * 8 + action.hashCode().and(0x7)
        return PendingIntent.getBroadcast(
            context, rc, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
