package com.ideasinc.followthrough.notifications

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import java.util.Calendar

// Day-of-week extra carried by per-goal reminder alarms. Shared by
// [GoalReminderScheduler] (which sets it) and [ReminderReceiver] (which reads it
// to re-arm next week's alarm after firing).
const val EXTRA_REMINDER_DAY = "com.ideasinc.followthrough.extra.REMINDER_DAY"

/**
 * Next epoch-ms at which [dayOfWeek] (Calendar.SUNDAY..SATURDAY) falls at
 * [hour]:[minute], always in the future. Used by the per-goal reminders
 * ([GoalReminderScheduler]) to compute trigger times.
 */
internal fun computeNextTriggerMs(dayOfWeek: Int, hour: Int, minute: Int): Long {
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
