package com.ideasinc.followthrough.ui.progress

import com.ideasinc.followthrough.data.DirectionCheckIn
import com.ideasinc.followthrough.data.DirectionFeeling
import com.ideasinc.followthrough.data.EventAction
import com.ideasinc.followthrough.data.Goal
import com.ideasinc.followthrough.data.Reminder
import com.ideasinc.followthrough.data.ReminderEvent
import com.ideasinc.followthrough.data.ReminderStatus
import com.ideasinc.followthrough.data.ScheduleMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [computeDueCheckIn] — when a direction's check-in surfaces. Pins the cadence:
 * needs a direction + an intention; first one early (≥7d + activity, or ≥14d backstop); then
 * ~3 weeks apart; "ask me later" snoozes ~1 week; one (the most overdue) at a time.
 */
class DirectionDueTest {

    private val now = 1_700_000_000_000L
    private val day = 86_400_000L
    private var seq = 0

    private fun goal(id: String, direction: String, ageDays: Long) =
        Goal(id = id, title = "g", whyItMatters = direction, createdAt = now - ageDays * day, updatedAt = now)

    private fun reminder(goalId: String, status: String = ReminderStatus.ACTIVE) = Reminder(
        id = "r${seq++}", goalId = goalId, whenMoment = "w", iWill = "i",
        cueType = "emoji", cueValue = "x", scheduleMode = ScheduleMode.DAILY,
        scheduleDays = "MON", scheduleTimeLocal = "08:00", scheduleTimezone = "UTC",
        status = status, createdAt = now, updatedAt = now
    )

    private fun event(reminderId: String) = ReminderEvent(
        id = "e${seq++}", reminderId = reminderId, deliveredAt = now, action = EventAction.DELIVERED, actedAt = 0L
    )

    private fun answered(goalId: String, daysAgo: Long, feeling: String = DirectionFeeling.GETTING_THERE) =
        DirectionCheckIn(id = "c${seq++}", goalId = goalId, askedAt = now - daysAgo * day, answeredAt = now - daysAgo * day, feeling = feeling)

    @Test fun goalWithoutDirectionIsNeverDue() {
        val g = goal("g1", "", 30)
        val r = reminder("g1")
        val due = computeDueCheckIn(listOf(g), listOf(r), listOf(event(r.id)), emptyList(), now)
        assertNull(due)
    }

    @Test fun goalWithoutAnyIntentionIsNeverDue() {
        val g = goal("g1", "be calmer", 30)
        assertNull(computeDueCheckIn(listOf(g), emptyList(), emptyList(), emptyList(), now))
    }

    @Test fun notDueBeforeMinAge() {
        val g = goal("g1", "be calmer", 3)
        val r = reminder("g1")
        assertNull(computeDueCheckIn(listOf(g), listOf(r), listOf(event(r.id)), emptyList(), now))
    }

    @Test fun firstCheckInDueAfterAWeekWithActivity() {
        val g = goal("g1", "be calmer", 8)
        val r = reminder("g1")
        val due = computeDueCheckIn(listOf(g), listOf(r), listOf(event(r.id)), emptyList(), now)
        assertNotNull(due)
        assertEquals("g1", due!!.goalId)
        assertEquals(r.id, due.reminderId)
        assertEquals("be calmer", due.direction)
    }

    @Test fun notDueAtAWeekWithoutActivity() {
        val g = goal("g1", "be calmer", 8)
        val r = reminder("g1")
        assertNull(computeDueCheckIn(listOf(g), listOf(r), emptyList(), emptyList(), now))
    }

    @Test fun backstopMakesItDueByTwoWeeksEvenWithoutActivity() {
        val g = goal("g1", "be calmer", 15)
        val r = reminder("g1")
        assertNotNull(computeDueCheckIn(listOf(g), listOf(r), emptyList(), emptyList(), now))
    }

    @Test fun recurringDueOnlyAfterThreeWeeks() {
        val g = goal("g1", "be calmer", 60)
        val r = reminder("g1")
        // Answered 10 days ago → not yet due again.
        assertNull(computeDueCheckIn(listOf(g), listOf(r), listOf(event(r.id)), listOf(answered("g1", 10)), now))
        // Answered 22 days ago → due again.
        assertNotNull(computeDueCheckIn(listOf(g), listOf(r), listOf(event(r.id)), listOf(answered("g1", 22)), now))
    }

    @Test fun askMeLaterSnoozesForAboutAWeek() {
        val g = goal("g1", "be calmer", 30)
        val r = reminder("g1")
        // Dismissed 3 days ago → snoozed.
        assertNull(computeDueCheckIn(listOf(g), listOf(r), listOf(event(r.id)), listOf(answered("g1", 3, DirectionFeeling.DISMISSED)), now))
        // Dismissed 8 days ago → due again.
        assertNotNull(computeDueCheckIn(listOf(g), listOf(r), listOf(event(r.id)), listOf(answered("g1", 8, DirectionFeeling.DISMISSED)), now))
    }

    @Test fun mostOverdueIsPicked() {
        val older = goal("old", "be calmer", 90)
        val newer = goal("new", "get strong", 40)
        val rOld = reminder("old")
        val rNew = reminder("new")
        // Both due via recurring; "old" answered longer ago → more overdue.
        val checkIns = listOf(answered("old", 40), answered("new", 25))
        val due = computeDueCheckIn(listOf(newer, older), listOf(rOld, rNew), listOf(event(rOld.id), event(rNew.id)), checkIns, now)
        assertEquals("old", due!!.goalId)
    }
}
