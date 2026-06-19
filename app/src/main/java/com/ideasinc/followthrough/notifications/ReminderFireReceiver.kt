package com.ideasinc.followthrough.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ideasinc.followthrough.InTheMomentActivity
import com.ideasinc.followthrough.R
import com.ideasinc.followthrough.data.CueType
import com.ideasinc.followthrough.data.EventAction
import com.ideasinc.followthrough.data.GroundedDatabase
import com.ideasinc.followthrough.data.Reminder
import com.ideasinc.followthrough.data.ReminderStatus
import com.ideasinc.followthrough.data.ScheduleMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Cue-fire for the new Reminder model. Two responsibilities:
 *  - ACTION_REMINDER_FIRE: post a high-priority notification carrying the cue PLUS
 *    the full intention text (the text always carries complete meaning — WCAG
 *    1.1.1 / 1.4.1 and the brief's multi-sense principle), with the single
 *    judgment-free action: Did it. Log a 'delivered' marker (the follow-through
 *    opportunity, for the forgiving streak), then re-arm next week.
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
            ACTION_REMINDER_UNDO -> handleUndo(appContext, intent)
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
                val posted = postReminderNotification(appContext, reminder)
                // Log the follow-through opportunity ONLY if the cue was actually shown. Never
                // count a miss against the streak for a reminder the user couldn't see (e.g.
                // notifications off) — that would be dishonest and unfair (rules #5/#6).
                if (posted) recordDelivered(db.reminderEventDao(), reminderId, System.currentTimeMillis())
                if (reminder.scheduleMode == ScheduleMode.ONCE) {
                    // One-off: the moment has passed — archive it so it drops off the
                    // Intentions list. Don't re-arm. The user can still tap Did it on the
                    // notification / in-the-moment screen to log the follow-through.
                    db.reminderDao().update(reminder.copy(status = ReminderStatus.ARCHIVED, updatedAt = System.currentTimeMillis()))
                } else if (day in Calendar.SUNDAY..Calendar.SATURDAY) {
                    // Re-arm the weekly cadence for the day that just fired (skip the
                    // one-shot snooze slot, day 0).
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
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = GroundedDatabase.getInstance(appContext)
                val eventId = recordReminderEvent(
                    eventDao = db.reminderEventDao(),
                    reminderId = reminderId,
                    action = action!!,
                    deliveredAt = System.currentTimeMillis()
                )
                if (action == EventAction.DONE) {
                    // Replace the reminder with an undoable confirmation so a mistaken shade tap
                    // is recoverable (CLAUDE.md rule #3) — the parallel to the in-app undo.
                    postDoneConfirmation(appContext, eventId, notificationId)
                } else {
                    val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    nm?.cancel(notificationId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    /** Undo a shade "Did it": remove the logged DONE event and clear the confirmation. */
    private fun handleUndo(appContext: Context, intent: Intent) {
        val eventId = intent.getStringExtra(EXTRA_EVENT_ID) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = GroundedDatabase.getInstance(appContext)
                undoReminderEvent(db.reminderEventDao(), eventId)
                if (notificationId != -1) {
                    val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    nm?.cancel(notificationId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    /** A quiet confirmation that replaces the fired notification after a shade "Did it", carrying
     *  an Undo action. Times out on its own (like the in-app undo snackbar) so it never lingers. */
    private fun postDoneConfirmation(context: Context, eventId: String, notificationId: Int) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val undoIntent = Intent(context, ReminderFireReceiver::class.java).apply {
            action = ACTION_REMINDER_UNDO
            putExtra(EXTRA_EVENT_ID, eventId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val undoPending = PendingIntent.getBroadcast(
            context, eventId.hashCode() and 0x00FFFFFF, undoIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setColor(0xFFB5402C.toInt())
            .setContentTitle("Followed through")
            .setContentText("Tap undo if that wasn't right.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setTimeoutAfter(30_000)
            .addAction(0, "Undo", undoPending)
        try {
            nm.notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted — the DONE is still recorded; just no confirmation.
        }
    }

    private fun responsePending(context: Context, reminderId: String, action: String, notificationId: Int): PendingIntent {
        val intent = Intent(context, ReminderFireReceiver::class.java).apply {
            this.action = ACTION_REMINDER_RESPONSE
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_RESPONSE, action)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val rc = (reminderId.hashCode() and 0x000FFFFF) * 8 + action.hashCode().and(0x7)
        return PendingIntent.getBroadcast(
            context, rc, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Returns true only if the notification was actually shown (so the caller can decide whether
     *  to log a follow-through opportunity). */
    private fun postReminderNotification(context: Context, reminder: Reminder): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return false
        // Notifications blocked (permission off on 13+, or the channel disabled) → nothing is shown.
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
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

        // Tapping the notification opens the focused full-screen in-the-moment screen
        // (cue + intention + the single Did it response).
        val tapIntent = Intent(context, InTheMomentActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_REMINDER_ID, reminder.id)
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
            // The single response, available from the shade for convenience. It's UNDOABLE
            // (CLAUDE.md rule #3): tapping it replaces this notification with a "Followed
            // through · Undo" confirmation, so an accidental tap is reversible. Tapping the
            // notification body instead opens the in-the-moment screen (also undoable). No
            // "Snooze"/"Not yet" — no response simply means not done, logged neutrally.
            .addAction(0, "Did it", responsePending(context, reminder.id, EventAction.DONE, notificationId))
        if (body.isNotBlank()) {
            builder.setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        }

        // Privacy on the lock screen. If the user enabled the app lock (biometric/PIN)
        // they've signaled they don't want others seeing their reminders — so on a secure
        // lock screen show only a discreet line, revealing the cue + intention once
        // unlocked (publicVersion). Without the app lock, keep full content on the lock
        // screen so the RTA cue is visible at the moment. Read from prefs directly — this
        // runs in a background broadcast where the in-memory state may be cold.
        val appLocked = context.getSharedPreferences("grounded_prefs", Context.MODE_PRIVATE)
            .getBoolean("biometric_enabled", false)
        if (appLocked) {
            val publicVersion = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setColor(0xFFB5402C.toInt())
                .setContentTitle("FollowThru reminder")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
            builder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setPublicVersion(publicVersion)
        } else {
            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        }

        // Honor the Settings notification-sound toggle (read straight from prefs —
        // this runs in a background broadcast where the in-memory flow may be cold).
        val soundOn = context.getSharedPreferences("grounded_prefs", Context.MODE_PRIVATE)
            .getBoolean("notification_sound_enabled", true)
        if (!soundOn) builder.setSilent(true)

        return try {
            nm.notify(notificationId, builder.build())
            true
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted on Android 13+ — dropped; report not-shown.
            false
        }
    }

}
