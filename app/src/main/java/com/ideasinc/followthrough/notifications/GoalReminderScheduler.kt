package com.ideasinc.followthrough.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

// Per-goal reminders live in their own SharedPreferences file, keyed by goalId,
// mirroring the existing global-reminder plumbing (PREFS_REMINDERS). They reuse
// the exact-alarm scheduling, permission check, boot reschedule and notification
// code wholesale — only the persistence shape (one entry per goal) is new. No
// new permission is required.
const val PREFS_GOAL_REMINDERS = "grounded_goal_reminders"

// Distinct action so ReminderReceiver can tell a per-goal alarm apart from the
// global one. The goalId rides along as an extra; the day reuses the global
// EXTRA_REMINDER_DAY so the receiver's reschedule path is identical.
const val ACTION_GOAL_REMINDER = "com.ideasinc.followthrough.action.GOAL_REMINDER"
const val EXTRA_GOAL_ID = "com.ideasinc.followthrough.extra.GOAL_ID"

// Index of every goalId that currently has a persisted reminder, so boot can
// reschedule them all without scanning the whole key space.
private const val KEY_GOAL_IDS = "goal_reminder_ids"

private fun keyEnabled(goalId: String) = "enabled_$goalId"
private fun keyHour(goalId: String) = "hour_$goalId"
private fun keyMinute(goalId: String) = "minute_$goalId"
private fun keyDays(goalId: String) = "days_$goalId"
private fun keyBody(goalId: String) = "body_$goalId"

/** A goal's persisted reminder configuration. */
data class GoalReminder(
    val enabled: Boolean,
    val hour: Int,
    val minute: Int,
    val days: Set<Int>,
    val body: String
)

object GoalReminderScheduler {

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_GOAL_REMINDERS, Context.MODE_PRIVATE)

    /** Returns the stored reminder for [goalId], or null if none was ever saved. */
    fun read(context: Context, goalId: String): GoalReminder? {
        val p = prefs(context)
        val ids = p.getStringSet(KEY_GOAL_IDS, emptySet()) ?: emptySet()
        if (goalId !in ids) return null
        val dayStrings = p.getStringSet(keyDays(goalId), emptySet()) ?: emptySet()
        return GoalReminder(
            enabled = p.getBoolean(keyEnabled(goalId), false),
            hour = p.getInt(keyHour(goalId), 9),
            minute = p.getInt(keyMinute(goalId), 0),
            days = dayStrings.mapNotNull { it.toIntOrNull() }.toSet(),
            body = p.getString(keyBody(goalId), "") ?: ""
        )
    }

    /**
     * Persists [goalId]'s reminder as enabled and (re)schedules one exact alarm
     * per selected day. If exact-alarm permission isn't granted, the config is
     * still saved (so the toggle reflects the user's intent) but no alarm fires
     * until permission is granted — matching the global reminder's behaviour.
     */
    fun schedule(
        context: Context,
        goalId: String,
        hour: Int,
        minute: Int,
        days: Set<Int>,
        body: String
    ) {
        val p = prefs(context)
        val ids = (p.getStringSet(KEY_GOAL_IDS, emptySet()) ?: emptySet()).toMutableSet()
        ids.add(goalId)
        p.edit()
            .putStringSet(KEY_GOAL_IDS, ids)
            .putBoolean(keyEnabled(goalId), true)
            .putInt(keyHour(goalId), hour)
            .putInt(keyMinute(goalId), minute)
            .putStringSet(keyDays(goalId), days.map { it.toString() }.toSet())
            .putString(keyBody(goalId), body)
            .apply()

        cancelAlarms(context, goalId)
        if (days.isEmpty()) return
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        if (!canScheduleExactAlarmsCompat(context)) return
        for (day in days) {
            scheduleAlarmForDay(context, am, goalId, day, hour, minute)
        }
    }

    /** Refreshes the stored notification body for an existing reminder (e.g. when
     *  the goal's implementation intention is edited). No-op if the goal has no
     *  saved reminder. */
    fun updateBody(context: Context, goalId: String, body: String) {
        val p = prefs(context)
        val ids = p.getStringSet(KEY_GOAL_IDS, emptySet()) ?: emptySet()
        if (goalId !in ids) return
        p.edit().putString(keyBody(goalId), body).apply()
    }

    /** Turns a goal's reminder off: cancels its alarms but keeps the time/day
     *  selection so re-enabling restores it. */
    fun disable(context: Context, goalId: String) {
        cancelAlarms(context, goalId)
        prefs(context).edit().putBoolean(keyEnabled(goalId), false).apply()
    }

    /** Fully removes a goal's reminder — cancels alarms and drops all persisted
     *  keys plus the index entry. Called when the goal itself is deleted. */
    fun remove(context: Context, goalId: String) {
        cancelAlarms(context, goalId)
        val p = prefs(context)
        val ids = (p.getStringSet(KEY_GOAL_IDS, emptySet()) ?: emptySet()).toMutableSet()
        ids.remove(goalId)
        p.edit()
            .putStringSet(KEY_GOAL_IDS, ids)
            .remove(keyEnabled(goalId))
            .remove(keyHour(goalId))
            .remove(keyMinute(goalId))
            .remove(keyDays(goalId))
            .remove(keyBody(goalId))
            .apply()
    }

    /** Re-arms the alarm for a single [day] after it fires (next week). */
    fun rescheduleAfterFire(context: Context, goalId: String, day: Int) {
        val reminder = read(context, goalId) ?: return
        if (!reminder.enabled || day !in reminder.days) return
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        if (!canScheduleExactAlarmsCompat(context)) return
        scheduleAlarmForDay(context, am, goalId, day, reminder.hour, reminder.minute)
    }

    /** Reschedules every enabled per-goal reminder — called on device boot. */
    fun rescheduleAllFromPrefs(context: Context) {
        val p = prefs(context)
        val ids = p.getStringSet(KEY_GOAL_IDS, emptySet()) ?: emptySet()
        for (goalId in ids) {
            val reminder = read(context, goalId) ?: continue
            if (!reminder.enabled) continue
            schedule(context, goalId, reminder.hour, reminder.minute, reminder.days, reminder.body)
        }
    }

    private fun scheduleAlarmForDay(
        context: Context,
        am: AlarmManager,
        goalId: String,
        day: Int,
        hour: Int,
        minute: Int
    ) {
        val triggerAtMs = computeNextTriggerMs(day, hour, minute)
        val pi = pendingIntentFor(context, goalId, day)
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
        } catch (_: SecurityException) {
            // Permission revoked between check and call — drop silently.
        }
    }

    private fun cancelAlarms(context: Context, goalId: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        // Cancel all 7 days regardless of current selection so a deselected day
        // never leaves a stale alarm behind.
        for (day in Calendar.SUNDAY..Calendar.SATURDAY) {
            val pi = pendingIntentFor(context, goalId, day)
            am.cancel(pi)
            pi.cancel()
        }
    }

    private fun pendingIntentFor(context: Context, goalId: String, day: Int): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_GOAL_REMINDER
            putExtra(EXTRA_GOAL_ID, goalId)
            putExtra(EXTRA_REMINDER_DAY, day)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCodeFor(goalId, day),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

/**
 * Stable, per-(goal, day) PendingIntent request code. The low 3 bits hold the
 * day (1..7) and the remaining bits a masked hash of the goalId. Collisions
 * across goals would require two goalIds sharing the same low 28 bits of
 * hashCode — vanishingly unlikely for UUIDs, and even then the distinct action
 * keeps these separate from the global reminder's request codes.
 */
internal fun requestCodeFor(goalId: String, day: Int): Int =
    ((goalId.hashCode() and 0x00FFFFFF) shl 3) or (day and 0x7)

/**
 * Per-goal notification id, kept well clear of the global reminder's id (1) so a
 * goal reminder and the global reminder can both be shown at once.
 */
internal fun goalNotificationId(goalId: String): Int =
    100_000 + (goalId.hashCode() and 0xFFFF)
