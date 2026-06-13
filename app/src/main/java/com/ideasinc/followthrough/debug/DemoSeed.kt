package com.ideasinc.followthrough.debug

import com.ideasinc.followthrough.data.Barrier
import com.ideasinc.followthrough.data.CueType
import com.ideasinc.followthrough.data.EventAction
import com.ideasinc.followthrough.data.Goal
import com.ideasinc.followthrough.data.Learning
import com.ideasinc.followthrough.data.PassionInterest
import com.ideasinc.followthrough.data.ProgressNote
import com.ideasinc.followthrough.data.Reminder
import com.ideasinc.followthrough.data.ReminderEvent
import com.ideasinc.followthrough.data.ReminderStatus
import com.ideasinc.followthrough.data.ScheduleMode
import com.ideasinc.followthrough.data.WeekDay
import com.ideasinc.followthrough.di.AppContainer
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.TimeZone

/**
 * DEBUG-ONLY screenshot/demo seed. Reuses the data-model.json content (Maya,
 * Biscuit, the orange Chemex) to populate goals + reminders + the palette + a few
 * weeks of follow-through events, so screens show a real, encouraging state.
 *
 * NEVER call this outside `if (BuildConfig.DEBUG)`. Release users get the normal
 * empty first-run; the only call sites are debug-gated (GroundedApplication seed-on-
 * empty, and the debug "Reset demo data" Settings button), so R8 strips this from
 * release builds.
 */
object DemoSeed {

    /** Seeds only if there are no goals yet (debug launch into an empty DB). */
    suspend fun seedIfEmpty(container: AppContainer) {
        if (container.goalDao.getAllGoals().first().isEmpty()) seed(container)
    }

    /** Wipes all user content and re-seeds — for the debug "Reset demo data" button. */
    suspend fun reset(container: AppContainer) {
        wipe(container)
        seed(container)
    }

    private suspend fun wipe(container: AppContainer) {
        // Deleting goals cascades reminders → events, plus barriers + progress notes.
        container.goalDao.getAllGoals().first().forEach { container.goalDao.deleteById(it.id) }
        container.paletteDao.getPassions().first().forEach { container.paletteDao.deletePassion(it.id) }
        container.paletteDao.getLearnings().first().forEach { container.paletteDao.deleteLearning(it.id) }
    }

    private suspend fun seed(container: AppContainer) {
        val now = System.currentTimeMillis()
        val tz = TimeZone.getDefault().id

        // ── Self-knowledge palette (ids match the reminders' cueSourcePaletteId) ──
        listOf(
            PassionInterest("pi_001", "Trail running on the Sacagawea river path", "🏃‍♀️", now),
            PassionInterest("pi_002", "My golden retriever, Biscuit", "🐕", now),
            PassionInterest("pi_003", "Specialty coffee — the orange Chemex on my counter", "☕", now),
            PassionInterest("pi_004", "Sci-fi audiobooks on the commute", "🎧", now)
        ).forEach { container.paletteDao.upsertPassion(it) }
        listOf(
            Learning("ln_001", "I follow through when the thing is staged by the door the night before.", now),
            Learning("ln_002", "Morning me keeps promises; 9pm me negotiates them away.", now),
            Learning("ln_003", "Pairing a chore with a podcast makes me actually start it.", now)
        ).forEach { container.paletteDao.upsertLearning(it) }

        // ── Goals (with why / barriers / progress notes) ──
        val g1 = Goal("goal_001", "Run a 5K without stopping by August", createdAt = now, updatedAt = now,
            whyItMatters = "Energy back, and Biscuit needs a running buddy.")
        val g2 = Goal("goal_002", "Take blood-pressure meds every morning", createdAt = now, updatedAt = now,
            whyItMatters = "Doctor flagged it in May. Non-negotiable.")
        val g3 = Goal("goal_003", "Call Mom every Sunday", createdAt = now, updatedAt = now,
            whyItMatters = "She mentioned twice that I only text now.")
        listOf(g1, g2, g3).forEach { container.goalDao.insertGoal(it) }

        listOf(
            Barrier("br_001", g1.id, "After-work slump — I sit down and never get back up.", now),
            Barrier("br_002", g1.id, "Forget to pack running shoes when I leave for work.", now),
            Barrier("br_003", g2.id, "Mornings are chaotic; the bottle lives in a drawer I never open.", now),
            Barrier("br_004", g3.id, "Sundays blur; by the time I remember it's 9pm her time.", now)
        ).forEach { container.goalContentDao.upsertBarrier(it) }

        listOf(
            ProgressNote("pr_001", g1.id, "Ran 1.5 miles, walked the rest. Knees fine.", now - day(5)),
            ProgressNote("pr_002", g1.id, "Skipped Tuesday — barrier #1 exactly as predicted.", now - day(3)),
            ProgressNote("pr_003", g2.id, "Moved the bottle next to the coffee scale. Took it both days since.", now - day(2))
        ).forEach { container.goalContentDao.upsertProgressNote(it) }

        // ── Reminders (emoji + phrase cues only; photo/sound are launch-off) ──
        val rem1 = Reminder(
            id = "rem_001", goalId = g1.id,
            whenMoment = "I change out of work clothes on Mon/Wed/Fri",
            iWill = "put on running shoes and take Biscuit out the door — even for 10 minutes",
            cueType = CueType.PHRASE, cueValue = "Biscuit's leash is the starting line",
            cueSourcePaletteId = "pi_002", cueIsPaletteDrawn = true,
            scheduleMode = ScheduleMode.WEEKLY, scheduleDays = WeekDay.toCsv(listOf(WeekDay.MON, WeekDay.WED, WeekDay.FRI)),
            scheduleTimeLocal = "17:30", scheduleTimezone = tz,
            status = ReminderStatus.ACTIVE, createdAt = now, updatedAt = now
        )
        val rem2 = Reminder(
            id = "rem_002", goalId = g2.id,
            whenMoment = "I start the morning coffee",
            iWill = "take the BP pill sitting next to the orange Chemex",
            cueType = CueType.EMOJI, cueValue = "☕",
            cueSourcePaletteId = "pi_003", cueIsPaletteDrawn = true,
            scheduleMode = ScheduleMode.DAILY, scheduleDays = WeekDay.toCsv(WeekDay.ALL),
            scheduleTimeLocal = "06:45", scheduleTimezone = tz,
            status = ReminderStatus.ACTIVE, createdAt = now, updatedAt = now
        )
        val rem3 = Reminder(
            id = "rem_003", goalId = g3.id,
            whenMoment = "the Sunday audiobook hour ends",
            iWill = "call Mom before doing anything else",
            cueType = CueType.PHRASE, cueValue = "Mom's voice beats another text",
            cueSourcePaletteId = null, cueIsPaletteDrawn = false,
            scheduleMode = ScheduleMode.WEEKLY, scheduleDays = WeekDay.toCsv(listOf(WeekDay.SUN)),
            scheduleTimeLocal = "16:00", scheduleTimezone = tz,
            status = ReminderStatus.ACTIVE, createdAt = now, updatedAt = now
        )
        listOf(rem1, rem2, rem3).forEach { container.reminderDao.upsert(it) }

        // ── Follow-through events across ~3 weeks ──
        // §4a counts "this week" as deliveredAt >= Monday 00:00 with no upper bound,
        // so current-week events are dated from this Monday regardless of run day.
        val monday = mondayThisWeek(now)
        val events = buildList {
            var n = 0
            fun add(remId: String, ts: Long, action: String) =
                add(ReminderEvent("ev_${n++}", remId, deliveredAt = ts, action = action, actedAt = ts + 120_000))

            // This week → 3 done + 1 forgiven = "Followed through on 3 of 4 … and Wednesday was forgiven."
            add(rem2.id, monday + hour(9), EventAction.DONE)               // Mon
            add(rem2.id, monday + day(1) + hour(9), EventAction.DONE)      // Tue
            add(rem2.id, monday + day(2) + hour(9), EventAction.DONE)      // Wed
            add(rem1.id, monday + day(2) + hour(18), EventAction.NOT_TODAY) // Wed (forgiven)

            // Two prior weeks of mostly-done history → healthy lifetime count.
            for (w in 1..2) {
                val wkMon = monday - day(7L * w)
                for (d in 0..4) add(rem2.id, wkMon + day(d.toLong()) + hour(9), EventAction.DONE) // BP pill Mon–Fri
                add(rem1.id, wkMon + hour(18), EventAction.DONE)            // run Mon
                add(rem1.id, wkMon + day(2) + hour(18), EventAction.DONE)   // run Wed
            }
        }
        events.forEach { container.reminderEventDao.upsert(it) }
    }

    private fun hour(h: Long) = h * 60 * 60 * 1000
    private fun day(d: Long) = d * 24 * 60 * 60 * 1000

    private fun mondayThisWeek(now: Long): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = now
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        while (c.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) c.add(Calendar.DAY_OF_MONTH, -1)
        return c.timeInMillis
    }
}
