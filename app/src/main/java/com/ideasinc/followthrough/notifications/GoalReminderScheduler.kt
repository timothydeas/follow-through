package com.ideasinc.followthrough.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

// Per-CHECK-IN reminders live in this SharedPreferences file, keyed by checkInId.
// Each check-in gets its own enabled flag, time and day set. The notification's
// title / intention / cue are read live from the check-in (Room) at fire time, so
// nothing is mirrored here. No special permission beyond notifications + exact
// alarms.
const val PREFS_GOAL_REMINDERS = "grounded_goal_reminders"

// Action identifying a per-check-in reminder alarm. The checkInId rides in
// EXTRA_CHECKIN_ID; the day-of-week in EXTRA_REMINDER_DAY so the receiver can
// re-arm next week.
const val ACTION_GOAL_REMINDER = "com.ideasinc.followthrough.action.GOAL_REMINDER"
// Rides on the alarm broadcast AND on the notification's tap intent to MainActivity
// (which deep-links to that specific check-in).
const val EXTRA_CHECKIN_ID = "com.ideasinc.followthrough.extra.CHECKIN_ID"

// Index of every checkInId that currently has a persisted reminder.
private const val KEY_GOAL_IDS = "goal_reminder_ids"
// One-time flag: the store has been re-keyed to check-in ids (old plan/goal keys
// cleared).
private const val KEY_KEYED_BY_CHECKIN = "reminders_keyed_by_checkin"

private fun keyEnabled(checkInId: String) = "enabled_$checkInId"
private fun keyHour(checkInId: String) = "hour_$checkInId"
private fun keyMinute(checkInId: String) = "minute_$checkInId"
private fun keyDays(checkInId: String) = "days_$checkInId"

/** A check-in's persisted reminder schedule. */
data class GoalReminder(
    val enabled: Boolean,
    val hour: Int,
    val minute: Int,
    val days: Set<Int>
)

object GoalReminderScheduler {

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_GOAL_REMINDERS, Context.MODE_PRIVATE)

    /** Returns the stored reminder for [checkInId], or null if none was ever saved. */
    fun read(context: Context, checkInId: String): GoalReminder? {
        val p = prefs(context)
        val ids = p.getStringSet(KEY_GOAL_IDS, emptySet()) ?: emptySet()
        if (checkInId !in ids) return null
        val dayStrings = p.getStringSet(keyDays(checkInId), emptySet()) ?: emptySet()
        return GoalReminder(
            enabled = p.getBoolean(keyEnabled(checkInId), false),
            hour = p.getInt(keyHour(checkInId), 9),
            minute = p.getInt(keyMinute(checkInId), 0),
            days = dayStrings.mapNotNull { it.toIntOrNull() }.toSet()
        )
    }

    /**
     * Persists [checkInId]'s reminder as enabled and (re)schedules one exact alarm
     * per selected day. If exact-alarm permission isn't granted, the config is
     * saved but no alarm fires until permission is granted.
     */
    fun schedule(context: Context, checkInId: String, hour: Int, minute: Int, days: Set<Int>) {
        val p = prefs(context)
        val ids = (p.getStringSet(KEY_GOAL_IDS, emptySet()) ?: emptySet()).toMutableSet()
        ids.add(checkInId)
        p.edit()
            .putStringSet(KEY_GOAL_IDS, ids)
            .putBoolean(keyEnabled(checkInId), true)
            .putInt(keyHour(checkInId), hour)
            .putInt(keyMinute(checkInId), minute)
            .putStringSet(keyDays(checkInId), days.map { it.toString() }.toSet())
            .apply()

        cancelAlarms(context, checkInId)
        if (days.isEmpty()) return
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        if (!canScheduleExactAlarmsCompat(context)) return
        for (day in days) scheduleAlarmForDay(context, am, checkInId, day, hour, minute)
    }

    /** Turns a check-in's reminder off: cancels alarms, keeps time/day so re-enabling restores it. */
    fun disable(context: Context, checkInId: String) {
        cancelAlarms(context, checkInId)
        prefs(context).edit().putBoolean(keyEnabled(checkInId), false).apply()
    }

    /** Fully removes a check-in's reminder — cancels alarms, drops all keys + index entry. */
    fun remove(context: Context, checkInId: String) {
        cancelAlarms(context, checkInId)
        val p = prefs(context)
        val ids = (p.getStringSet(KEY_GOAL_IDS, emptySet()) ?: emptySet()).toMutableSet()
        ids.remove(checkInId)
        p.edit()
            .putStringSet(KEY_GOAL_IDS, ids)
            .remove(keyEnabled(checkInId))
            .remove(keyHour(checkInId))
            .remove(keyMinute(checkInId))
            .remove(keyDays(checkInId))
            .apply()
    }

    /** Re-arms the alarm for a single [day] after it fires (next week). */
    fun rescheduleAfterFire(context: Context, checkInId: String, day: Int) {
        val reminder = read(context, checkInId) ?: return
        if (!reminder.enabled || day !in reminder.days) return
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        if (!canScheduleExactAlarmsCompat(context)) return
        scheduleAlarmForDay(context, am, checkInId, day, reminder.hour, reminder.minute)
    }

    /** Reschedules every enabled per-check-in reminder — called on device boot. */
    fun rescheduleAllFromPrefs(context: Context) {
        clearForCheckInRekey(context)
        val p = prefs(context)
        val ids = p.getStringSet(KEY_GOAL_IDS, emptySet()) ?: emptySet()
        for (checkInId in ids) {
            val reminder = read(context, checkInId) ?: continue
            if (!reminder.enabled) continue
            schedule(context, checkInId, reminder.hour, reminder.minute, reminder.days)
        }
    }

    /**
     * One-time cleanup for the v33→v34 revert: reminders used to be keyed by
     * goalId, then planId, neither of which exists in the check-in–keyed model.
     * Since there's no data to preserve (pre-release), cancel every stale alarm and
     * wipe the store, then mark it migrated. Idempotent (gated by a flag).
     */
    fun clearForCheckInRekey(context: Context) {
        val p = prefs(context)
        if (p.getBoolean(KEY_KEYED_BY_CHECKIN, false)) return
        val ids = (p.getStringSet(KEY_GOAL_IDS, emptySet()) ?: emptySet()).toSet()
        for (oldId in ids) cancelAlarms(context, oldId)
        p.edit().clear().putBoolean(KEY_KEYED_BY_CHECKIN, true).apply()
    }

    private fun scheduleAlarmForDay(
        context: Context,
        am: AlarmManager,
        checkInId: String,
        day: Int,
        hour: Int,
        minute: Int
    ) {
        val triggerAtMs = computeNextTriggerMs(day, hour, minute)
        val pi = pendingIntentFor(context, checkInId, day)
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
        } catch (_: SecurityException) {
            // Permission revoked between check and call — drop silently.
        }
    }

    private fun cancelAlarms(context: Context, checkInId: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        for (day in Calendar.SUNDAY..Calendar.SATURDAY) {
            val pi = pendingIntentFor(context, checkInId, day)
            am.cancel(pi)
            pi.cancel()
        }
    }

    private fun pendingIntentFor(context: Context, checkInId: String, day: Int): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_GOAL_REMINDER
            putExtra(EXTRA_CHECKIN_ID, checkInId)
            putExtra(EXTRA_REMINDER_DAY, day)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCodeFor(checkInId, day),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

/**
 * Stable, per-(check-in, day) PendingIntent request code. The low 3 bits hold the
 * day (1..7) and the remaining bits a masked hash of the checkInId.
 */
internal fun requestCodeFor(checkInId: String, day: Int): Int =
    ((checkInId.hashCode() and 0x00FFFFFF) shl 3) or (day and 0x7)

/**
 * Per-check-in notification id, derived from the checkInId so each check-in's
 * reminder occupies a distinct slot and several can be shown at once.
 */
internal fun checkInNotificationId(checkInId: String): Int =
    100_000 + (checkInId.hashCode() and 0xFFFF)
