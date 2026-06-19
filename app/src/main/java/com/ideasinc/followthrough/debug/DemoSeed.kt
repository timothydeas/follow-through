package com.ideasinc.followthrough.debug

import com.ideasinc.followthrough.data.CueType
import com.ideasinc.followthrough.data.DirectionCheckIn
import com.ideasinc.followthrough.data.DirectionFeeling
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
        val tenDaysAgo = now - 10L * 24 * 60 * 60 * 1000
        val tz = TimeZone.getDefault().id

        // Goals are the internal container each intention belongs to (never shown).
        val g1 = Goal("goal_001", "Make headway on my course", createdAt = now, updatedAt = now)
        val g2 = Goal("goal_002", "Floss every day", createdAt = now, updatedAt = now)
        val g3 = Goal("goal_003", "Grow my savings", createdAt = now, updatedAt = now)
        // g4 has a DIRECTION and is backdated ~10 days with activity, so its direction check-in is
        // due immediately on the Progress tab (lets you test the check-in without waiting a week).
        val g4 = Goal(
            "goal_004", "Walk to work more",
            whyItMatters = "save money and clear my head",
            createdAt = tenDaysAgo, updatedAt = tenDaysAgo
        )
        listOf(g1, g2, g3, g4).forEach { container.goalDao.insertGoal(it) }

        // Intentions — emoji + phrase cues only (photo/sound are launch-off).
        val rem1 = Reminder(
            id = "rem_001", goalId = g1.id,
            whenMoment = "it's Tuesday or Thursday at five",
            iWill = "put in an hour on my online course",
            cueType = CueType.PHRASE, cueValue = "Headphones on means class is in",
            scheduleMode = ScheduleMode.WEEKLY, scheduleDays = WeekDay.toCsv(listOf(WeekDay.TUE, WeekDay.THU)),
            scheduleTimeLocal = "17:00", scheduleTimezone = tz,
            status = ReminderStatus.ACTIVE, createdAt = now, updatedAt = now
        )
        val rem2 = Reminder(
            id = "rem_002", goalId = g2.id,
            whenMoment = "I finish brushing at night",
            iWill = "floss before I turn in",
            cueType = CueType.EMOJI, cueValue = "🦷",
            scheduleMode = ScheduleMode.DAILY, scheduleDays = WeekDay.toCsv(WeekDay.ALL),
            scheduleTimeLocal = "21:30", scheduleTimezone = tz,
            status = ReminderStatus.ACTIVE, createdAt = now, updatedAt = now
        )
        val rem3 = Reminder(
            id = "rem_003", goalId = g3.id,
            whenMoment = "my Friday paycheck lands",
            iWill = "move a little into savings before I spend a thing",
            cueType = CueType.PHRASE, cueValue = "Pay myself first",
            scheduleMode = ScheduleMode.WEEKLY, scheduleDays = WeekDay.toCsv(listOf(WeekDay.FRI)),
            scheduleTimeLocal = "09:00", scheduleTimezone = tz,
            status = ReminderStatus.ACTIVE, createdAt = now, updatedAt = now
        )
        val rem4 = Reminder(
            id = "rem_004", goalId = g4.id,
            whenMoment = "it's mild and dry when I head out",
            iWill = "walk to work instead of driving",
            cueType = CueType.EMOJI, cueValue = "🚶",
            scheduleMode = ScheduleMode.DAILY, scheduleDays = WeekDay.toCsv(WeekDay.ALL),
            scheduleTimeLocal = "08:00", scheduleTimezone = tz,
            status = ReminderStatus.ACTIVE, createdAt = tenDaysAgo, updatedAt = tenDaysAgo
        )
        listOf(rem1, rem2, rem3, rem4).forEach { container.reminderDao.upsert(it) }

        // A realistic, lived-in history for screenshots — strong but deliberately IMPERFECT: an
        // active streak with one recent miss, an honest "6 of 9 this week", and an earlier lapse
        // that reset a longer run (so Longest reads higher than Current). DONE = a follow-through;
        // DELIVERED-without-DONE = a neutral miss (rule #5). Every value reconciles with
        // computeProgress (ProgressViewModel):
        //   • Current streak 14   • Longest 16   • Lifetime 30   • This week 6 of ~9 (on track)
        //   • Weekly grid: Mon done, Tue done, Wed miss, today (Thu) pending
        // Offsets are days-ago; seeded on a Thursday this maps to the current Mon–Thu.
        var n = 0
        val events = buildList {
            fun done(remId: String, daysAgo: Long) {
                val ts = now - daysAgo * 24 * 60 * 60 * 1000
                add(ReminderEvent("ev_${n++}", remId, deliveredAt = ts, action = EventAction.DONE, actedAt = ts + 120_000))
            }
            // A cue that fired but was never acted on — reconciled into a neutral miss (actedAt 0).
            fun missed(remId: String, daysAgo: Long) {
                val ts = now - daysAgo * 24 * 60 * 60 * 1000
                add(ReminderEvent("ev_${n++}", remId, deliveredAt = ts, action = EventAction.DELIVERED, actedAt = 0L))
            }

            // Older run — 16 follow-throughs (floss + walk, daily) → sets Longest = 16.
            for (d in 15L..22L) { done(rem2.id, d); done(rem4.id, d) }
            // A three-miss lapse resets Current to 0 (Longest stays 16) — shows streaks recover.
            missed(rem2.id, 14); missed(rem4.id, 14); missed(rem2.id, 13)

            // Last week — a clean win that rebuilds the streak.
            done(rem2.id, 10); done(rem4.id, 10)
            done(rem1.id, 9); done(rem2.id, 9); done(rem4.id, 9)
            done(rem2.id, 8); done(rem4.id, 8)
            done(rem3.id, 6)                                       // Friday payday → savings (on schedule)

            // This week (today = Thu): Mon + Tue done, Wed missed, today still pending → 6 of ~9.
            done(rem1.id, 3); done(rem2.id, 3); done(rem4.id, 3)  // Mon
            done(rem1.id, 2); done(rem2.id, 2); done(rem4.id, 2)  // Tue
            missed(rem2.id, 1)                                     // Wed — a neutral miss in the grid
            // Activity for g4 (walk) keeps its direction check-in due; today's two are delivered
            // but not yet acted, so today shows as PENDING, never a miss.
            missed(rem2.id, 0); missed(rem4.id, 0)
        }
        events.forEach { container.reminderEventDao.upsert(it) }

        // One past learning (on a goal WITHOUT a direction, so it doesn't reset g4's check-in
        // cadence) so the "Things you've learned" list shows a real entry.
        container.directionCheckInDao.insert(
            DirectionCheckIn(
                id = "dci_demo_1", goalId = g1.id,
                askedAt = now - 2L * 24 * 60 * 60 * 1000, answeredAt = now - 2L * 24 * 60 * 60 * 1000,
                feeling = DirectionFeeling.LEARNED,
                noteText = "A short focused block sticks better than one long cram."
            )
        )
    }
}
