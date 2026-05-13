package com.example.grounded.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

const val PREFS_REMINDERS = "grounded_reminders"
const val KEY_REMINDERS_ENABLED = "reminders_enabled"
const val KEY_REMINDER_HOUR = "reminder_hour"
const val KEY_REMINDER_MINUTE = "reminder_minute"
const val KEY_REMINDER_DAYS = "reminder_days"

const val ACTION_REMINDER = "com.example.grounded.action.REMINDER"
const val EXTRA_REMINDER_DAY = "com.example.grounded.extra.REMINDER_DAY"

object ReminderScheduler {

    fun scheduleReminders(context: Context, hour: Int, minute: Int, days: Set<Int>) {
        cancelReminders(context)
        if (days.isEmpty()) return

        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        if (!canScheduleExactAlarmsCompat(am)) return

        for (day in days) {
            scheduleAlarmForDay(context, am, day, hour, minute)
        }
    }

    fun cancelReminders(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        for (day in Calendar.SUNDAY..Calendar.SATURDAY) {
            val pi = pendingIntentFor(context, day)
            am.cancel(pi)
            pi.cancel()
        }
    }

    fun rescheduleAfterFire(context: Context, day: Int) {
        val prefs = context.getSharedPreferences(PREFS_REMINDERS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_REMINDERS_ENABLED, false)) return

        val dayStrings = prefs.getStringSet(KEY_REMINDER_DAYS, emptySet()) ?: emptySet()
        val days = dayStrings.mapNotNull { it.toIntOrNull() }.toSet()
        if (day !in days) return

        val hour = prefs.getInt(KEY_REMINDER_HOUR, 9)
        val minute = prefs.getInt(KEY_REMINDER_MINUTE, 0)

        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        if (!canScheduleExactAlarmsCompat(am)) return

        scheduleAlarmForDay(context, am, day, hour, minute)
    }

    fun rescheduleAllFromPrefs(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_REMINDERS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_REMINDERS_ENABLED, false)) return

        val hour = prefs.getInt(KEY_REMINDER_HOUR, 9)
        val minute = prefs.getInt(KEY_REMINDER_MINUTE, 0)
        val dayStrings = prefs.getStringSet(KEY_REMINDER_DAYS, emptySet()) ?: emptySet()
        val days = dayStrings.mapNotNull { it.toIntOrNull() }.toSet()

        scheduleReminders(context, hour, minute, days)
    }

    private fun scheduleAlarmForDay(
        context: Context,
        am: AlarmManager,
        day: Int,
        hour: Int,
        minute: Int
    ) {
        val triggerAtMs = computeNextTriggerMs(day, hour, minute)
        val pi = pendingIntentFor(context, day)
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
        } catch (_: SecurityException) {
            // Permission revoked between check and call — drop silently.
        }
    }

    private fun pendingIntentFor(context: Context, day: Int): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra(EXTRA_REMINDER_DAY, day)
        }
        return PendingIntent.getBroadcast(
            context,
            day,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun computeNextTriggerMs(dayOfWeek: Int, hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, dayOfWeek)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        while (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.WEEK_OF_YEAR, 1)
        }
        return target.timeInMillis
    }
}

fun canScheduleExactAlarmsCompat(context: Context): Boolean {
    val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
    return canScheduleExactAlarmsCompat(am)
}

private fun canScheduleExactAlarmsCompat(am: AlarmManager): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        am.canScheduleExactAlarms()
    } else {
        true
    }
}
