package com.ideasinc.followthrough.notifications

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import java.util.Calendar

/**
 * Next epoch-ms at which [dayOfWeek] (Calendar.SUNDAY..SATURDAY) falls at
 * [hour]:[minute], always in the future. Shared alarm-scheduling helper used by
 * [ReminderAlarmScheduler] to compute trigger times.
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
