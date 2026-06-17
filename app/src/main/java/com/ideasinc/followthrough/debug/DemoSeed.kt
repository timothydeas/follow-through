package com.ideasinc.followthrough.debug

import com.ideasinc.followthrough.data.CueType
import com.ideasinc.followthrough.data.EventAction
import com.ideasinc.followthrough.data.Goal
import com.ideasinc.followthrough.data.Reminder
import com.ideasinc.followthrough.data.ReminderEvent
import com.ideasinc.followthrough.data.ReminderStatus
import com.ideasinc.followthrough.data.ScheduleMode
import com.ideasinc.followthrough.data.WeekDay
import com.ideasinc.followthrough.di.AppContainer
import kotlinx.coroutines.flow.first
import java.util.TimeZone

/**
 * DEBUG-ONLY screenshot/demo seed. Fills an empty DB with a few **intentions** (cue +
 * full text + schedule) and a little follow-through history, so screens show a real,
 * encouraging state for screenshots.
 *
 * NEVER call this outside `if (BuildConfig.DEBUG)`. The only call site is debug-gated
 * (GroundedApplication seed-on-empty), so R8 strips it from release builds. There is no
 * visible demo UI — it silently fills an empty DB on a debug launch.
 *
 * Aligned to the MVP: only goals (the invisible container each intention belongs to),
 * reminders, and DONE events are seeded — no palette / barriers / learnings / progress /
 * goal-motivation data (those surfaces were removed).
 */
object DemoSeed {

    /** Seeds only if there are no goals yet (debug launch into an empty DB). */
    suspend fun seedIfEmpty(container: AppContainer) {
        if (container.goalDao.getAllGoals().first().isEmpty()) seed(container)
    }

    private suspend fun seed(container: AppContainer) {
        val now = System.currentTimeMillis()
        val tz = TimeZone.getDefault().id

        // Goals are the internal container each intention belongs to (never shown).
        val g1 = Goal("goal_001", "Take Biscuit out the door", createdAt = now, updatedAt = now)
        val g2 = Goal("goal_002", "Take blood-pressure meds", createdAt = now, updatedAt = now)
        val g3 = Goal("goal_003", "Call Mom", createdAt = now, updatedAt = now)
        listOf(g1, g2, g3).forEach { container.goalDao.insertGoal(it) }

        // Intentions — emoji + phrase cues only (photo/sound are launch-off).
        val rem1 = Reminder(
            id = "rem_001", goalId = g1.id,
            whenMoment = "I change out of work clothes on Mon/Wed/Fri",
            iWill = "put on running shoes and take Biscuit out the door — even for 10 minutes",
            cueType = CueType.PHRASE, cueValue = "Biscuit's leash is the starting line",
            scheduleMode = ScheduleMode.WEEKLY, scheduleDays = WeekDay.toCsv(listOf(WeekDay.MON, WeekDay.WED, WeekDay.FRI)),
            scheduleTimeLocal = "17:30", scheduleTimezone = tz,
            status = ReminderStatus.ACTIVE, createdAt = now, updatedAt = now
        )
        val rem2 = Reminder(
            id = "rem_002", goalId = g2.id,
            whenMoment = "I start the morning coffee",
            iWill = "take the BP pill sitting next to the orange Chemex",
            cueType = CueType.EMOJI, cueValue = "☕",
            scheduleMode = ScheduleMode.DAILY, scheduleDays = WeekDay.toCsv(WeekDay.ALL),
            scheduleTimeLocal = "06:45", scheduleTimezone = tz,
            status = ReminderStatus.ACTIVE, createdAt = now, updatedAt = now
        )
        val rem3 = Reminder(
            id = "rem_003", goalId = g3.id,
            whenMoment = "the Sunday audiobook hour ends",
            iWill = "call Mom before doing anything else",
            cueType = CueType.PHRASE, cueValue = "Mom's voice beats another text",
            scheduleMode = ScheduleMode.WEEKLY, scheduleDays = WeekDay.toCsv(listOf(WeekDay.SUN)),
            scheduleTimeLocal = "16:00", scheduleTimezone = tz,
            status = ReminderStatus.ACTIVE, createdAt = now, updatedAt = now
        )
        listOf(rem1, rem2, rem3).forEach { container.reminderDao.upsert(it) }

        // A little follow-through history so "What worked" shows cues that have worked.
        var n = 0
        val events = buildList {
            fun done(remId: String, daysAgo: Long) {
                val ts = now - daysAgo * 24 * 60 * 60 * 1000
                add(ReminderEvent("ev_${n++}", remId, deliveredAt = ts, action = EventAction.DONE, actedAt = ts + 120_000))
            }
            done(rem2.id, 1); done(rem2.id, 2); done(rem2.id, 3); done(rem2.id, 4)
            done(rem1.id, 2); done(rem1.id, 5)
            done(rem3.id, 7)
        }
        events.forEach { container.reminderEventDao.upsert(it) }
    }
}
