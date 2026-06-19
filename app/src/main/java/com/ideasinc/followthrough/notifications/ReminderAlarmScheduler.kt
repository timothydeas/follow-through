package com.ideasinc.followthrough.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.ideasinc.followthrough.data.GroundedDatabase
import com.ideasinc.followthrough.data.Reminder
import com.ideasinc.followthrough.data.ReminderStatus
import com.ideasinc.followthrough.data.ScheduleMode
import com.ideasinc.followthrough.data.WeekDay
import kotlinx.coroutines.flow.first
import java.util.Calendar

// Reminder-model alarms. The schedule lives on the Reminder row (Room), read live
// at fire time; nothing here is mirrored to prefs. Reliability contract:
// setExactAndAllowWhileIdle, re-arm after fire, re-register on boot.
const val ACTION_REMINDER_FIRE = "com.ideasinc.followthrough.action.REMINDER_FIRE"
const val ACTION_REMINDER_RESPONSE = "com.ideasinc.followthrough.action.REMINDER_RESPONSE"
// Undo a shade "Did it" — removes the logged DONE event so an accidental tap is reversible.
const val ACTION_REMINDER_UNDO = "com.ideasinc.followthrough.action.REMINDER_UNDO"
const val EXTRA_EVENT_ID = "com.ideasinc.followthrough.extra.EVENT_ID"
const val EXTRA_REMINDER_ID = "com.ideasinc.followthrough.extra.REMINDER_ID"
const val EXTRA_FIRE_DAY = "com.ideasinc.followthrough.extra.FIRE_DAY"
const val EXTRA_RESPONSE = "com.ideasinc.followthrough.extra.RESPONSE"
const val EXTRA_NOTIFICATION_ID = "com.ideasinc.followthrough.extra.NOTIFICATION_ID"

// day == 0 is reserved for one-shot snooze alarms so they don't overwrite the
// weekly re-arm for any real weekday (1..7).
private const val SNOOZE_DAY_SLOT = 0

object ReminderAlarmScheduler {

    /** (Re)schedules one exact alarm per scheduled day for [reminder]. */
    fun schedule(context: Context, reminder: Reminder) {
        cancel(context, reminder.id)
        if (reminder.status != ReminderStatus.ACTIVE) return
        // A reminderless intention (ScheduleMode.NONE) never fires — also guards boot re-arm.
        if (reminder.scheduleMode == ScheduleMode.NONE) return
        // Reminders paused (e.g. vacation) — nothing arms; also guards boot re-arm. Resuming in
        // Settings re-runs schedule() for every active reminder.
        if (context.getSharedPreferences("grounded_prefs", Context.MODE_PRIVATE)
                .getBoolean("reminders_paused", false)
        ) return
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        // No exact-alarm gate here: armDay() falls back to an inexact alarm rather than
        // scheduling nothing, so reminders are never silently dropped.
        val (hour, minute) = parseTime(reminder.scheduleTimeLocal)
        if (reminder.scheduleMode == ScheduleMode.ONCE) {
            armOnce(context, am, reminder, hour, minute)
            return
        }
        val days = if (reminder.scheduleMode == ScheduleMode.DAILY) WeekDay.ALL else reminder.days
        for (wd in days) {
            armDay(context, am, reminder.id, calendarDay(wd), hour, minute)
        }
    }

    /** Cancels every alarm for [reminderId] (all 7 weekday slots + the snooze slot). */
    fun cancel(context: Context, reminderId: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        for (day in SNOOZE_DAY_SLOT..Calendar.SATURDAY) {
            val pi = firePendingIntent(context, reminderId, day)
            am.cancel(pi)
            pi.cancel()
        }
    }

    /** Re-arms [calendarDay]'s weekly alarm after it fires (next week). */
    fun rescheduleAfterFire(context: Context, reminder: Reminder, calendarDay: Int) {
        if (reminder.status != ReminderStatus.ACTIVE) return
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val (hour, minute) = parseTime(reminder.scheduleTimeLocal)
        armDay(context, am, reminder.id, calendarDay, hour, minute)
    }

    /** One-shot re-fire ~1 hour out after the user taps Snooze. */
    fun snooze(context: Context, reminderId: String, delayMs: Long = 60 * 60 * 1000L) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = firePendingIntent(context, reminderId, SNOOZE_DAY_SLOT)
        setBestEffortAlarm(context, am, System.currentTimeMillis() + delayMs, pi)
    }

    /** Re-registers every active reminder's alarms — called on device boot. */
    suspend fun rescheduleAllActive(context: Context) {
        val db = GroundedDatabase.getInstance(context.applicationContext)
        val reminders = db.reminderDao().getAllReminders().first()
        for (r in reminders) if (r.status == ReminderStatus.ACTIVE) schedule(context, r)
    }

    private fun armDay(context: Context, am: AlarmManager, reminderId: String, calendarDay: Int, hour: Int, minute: Int) {
        val triggerAtMs = computeNextTriggerMs(calendarDay, hour, minute)
        val pi = firePendingIntent(context, reminderId, calendarDay)
        setBestEffortAlarm(context, am, triggerAtMs, pi)
    }

    /**
     * Arms the single alarm for a one-off intention at its scheduled date + time. The alarm
     * is keyed to that date's weekday slot (1..7) so cancel() — which sweeps slots 0..7 —
     * still cleans it up; on fire, ReminderFireReceiver archives the one-off instead of
     * re-arming. A date/time already in the past is skipped (it simply won't fire).
     */
    private fun armOnce(context: Context, am: AlarmManager, reminder: Reminder, hour: Int, minute: Int) {
        val parts = (reminder.scheduleDate ?: return).split("-")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: return
        val month = parts.getOrNull(1)?.toIntOrNull() ?: return // 1..12
        val day = parts.getOrNull(2)?.toIntOrNull() ?: return
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val triggerAtMs = cal.timeInMillis
        if (triggerAtMs <= System.currentTimeMillis()) return
        val pi = firePendingIntent(context, reminder.id, cal.get(Calendar.DAY_OF_WEEK))
        setBestEffortAlarm(context, am, triggerAtMs, pi)
    }

    /**
     * Sets a wake alarm — exact (to-the-minute) when the OS allows it, otherwise
     * FALLING BACK to an inexact allow-while-idle alarm rather than dropping it. The
     * reliability contract is "the reminder fires," so a missing/revoked exact-alarm
     * permission (Android 12+) must never silently lose the reminder or a snooze.
     */
    private fun setBestEffortAlarm(context: Context, am: AlarmManager, triggerAtMs: Long, pi: PendingIntent) {
        try {
            if (canScheduleExactAlarmsCompat(context)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
            }
        } catch (_: SecurityException) {
            // Exact permission revoked between the check and the call — best-effort inexact.
            try {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
            } catch (_: SecurityException) {
            }
        }
    }

    private fun firePendingIntent(context: Context, reminderId: String, day: Int): PendingIntent {
        val intent = Intent(context, ReminderFireReceiver::class.java).apply {
            action = ACTION_REMINDER_FIRE
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_FIRE_DAY, day)
        }
        return PendingIntent.getBroadcast(
            context,
            fireRequestCode(reminderId, day),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

/** Maps a [WeekDay] to a Calendar day-of-week int (SUNDAY=1 … SATURDAY=7). */
internal fun calendarDay(day: WeekDay): Int = when (day) {
    WeekDay.SUN -> Calendar.SUNDAY
    WeekDay.MON -> Calendar.MONDAY
    WeekDay.TUE -> Calendar.TUESDAY
    WeekDay.WED -> Calendar.WEDNESDAY
    WeekDay.THU -> Calendar.THURSDAY
    WeekDay.FRI -> Calendar.FRIDAY
    WeekDay.SAT -> Calendar.SATURDAY
}

/** "HH:mm" → (hour, minute), defaulting to 08:00 on a malformed value. */
internal fun parseTime(hhmm: String): Pair<Int, Int> {
    val h = hhmm.substringBefore(":").toIntOrNull()?.coerceIn(0, 23) ?: 8
    val m = hhmm.substringAfter(":", "0").toIntOrNull()?.coerceIn(0, 59) ?: 0
    return h to m
}

/**
 * Stable per-(reminder, day) request code, salted to avoid collisions with the
 * legacy per-check-in codes. Low 3 bits hold the day (0..7).
 */
internal fun fireRequestCode(reminderId: String, day: Int): Int =
    (((reminderId.hashCode() xor 0x5A5A5A5A) and 0x00FFFFFF) shl 3) or (day and 0x7)

/** Per-reminder notification id, distinct from the legacy check-in slot range. */
internal fun reminderNotificationId(reminderId: String): Int =
    200_000 + (reminderId.hashCode() and 0xFFFF)
