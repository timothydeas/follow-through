package com.example.grounded.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

const val PREFS_REMINDERS = "grounded_reminders"
const val KEY_REMINDERS_ENABLED = "reminders_enabled"
const val KEY_REMINDER_HOUR = "reminder_hour"
const val KEY_REMINDER_MINUTE = "reminder_minute"
const val KEY_REMINDER_DAYS = "reminder_days"

private const val WORK_NAME_PREFIX = "follow_through_reminder_day_"
private const val WEEKLY_INTERVAL_DAYS = 7L

object ReminderScheduler {

    fun scheduleReminders(context: Context, hour: Int, minute: Int, days: Set<Int>) {
        cancelReminders(context)
        if (days.isEmpty()) return

        val wm = WorkManager.getInstance(context.applicationContext)
        for (day in days) {
            val initialDelayMs = computeInitialDelayMs(day, hour, minute)
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(
                WEEKLY_INTERVAL_DAYS, TimeUnit.DAYS
            )
                .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
                .addTag(TAG_REMINDER)
                .build()

            wm.enqueueUniquePeriodicWork(
                WORK_NAME_PREFIX + day,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }

    fun cancelReminders(context: Context) {
        val wm = WorkManager.getInstance(context.applicationContext)
        for (day in Calendar.SUNDAY..Calendar.SATURDAY) {
            wm.cancelUniqueWork(WORK_NAME_PREFIX + day)
        }
    }

    private fun computeInitialDelayMs(dayOfWeek: Int, hour: Int, minute: Int): Long {
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
        return target.timeInMillis - now.timeInMillis
    }

    private const val TAG_REMINDER = "follow_through_reminder"
}
