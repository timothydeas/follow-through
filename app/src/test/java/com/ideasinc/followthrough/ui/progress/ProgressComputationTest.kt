package com.ideasinc.followthrough.ui.progress

import com.ideasinc.followthrough.data.EventAction
import com.ideasinc.followthrough.data.ReminderEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Unit tests for the forgiving Current streak ([computeProgress]) — CLAUDE.md rule #5.
 *
 * The model is a **two-miss reserve over follow-through opportunities** (one per intention per
 * day): a follow-through (Did it) climbs Current and refills the reserve; the first two
 * consecutive misses (delivered, not acted) are absorbed and Current holds; a third consecutive
 * miss resets Current to 0. Longest is the best run ever and never resets. Today's not-yet-acted
 * cue is pending, never a miss. Reminderless intentions and snoozes never build a streak.
 *
 * Anchored on real Mondays: 2024-01-01 is a Monday, so each `ts(2024, 0, dayOfMonth)` lines up
 * with the device-local calendar the production code uses. `now` is Wed 2024-01-31, so days 1–30
 * are all in the past (resolved), and day 31 is "today."
 */
class ProgressComputationTest {

    private var seq = 0
    private fun ts(year: Int, month0: Int, day: Int): Long {
        val c = Calendar.getInstance()
        c.set(year, month0, day, 12, 0, 0); c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun done(rid: String, t: Long) =
        ReminderEvent(id = "e${seq++}", reminderId = rid, deliveredAt = t, action = EventAction.DONE, actedAt = t)
    private fun delivered(rid: String, t: Long) =
        ReminderEvent(id = "e${seq++}", reminderId = rid, deliveredAt = t, action = EventAction.DELIVERED, actedAt = 0L)
    private fun snoozed(rid: String, t: Long) =
        ReminderEvent(id = "e${seq++}", reminderId = rid, deliveredAt = t, action = EventAction.SNOOZED, actedAt = t)

    /** A resolved "won" opportunity: the cue fired (delivered) and the user followed through. */
    private fun firedAndDone(rid: String, t: Long) = listOf(delivered(rid, t), done(rid, t))
    /** A resolved "missed" opportunity: the cue fired but was never acted on. */
    private fun firedNotDone(rid: String, t: Long) = listOf(delivered(rid, t))

    private fun d(day: Int) = ts(2024, 0, day)
    private val now = ts(2024, 0, 31) // Wed of the current week

    @Test fun emptyHistoryIsAllZero() {
        val s = computeProgress(emptyList(), now)
        assertEquals(0, s.currentStreak)
        assertEquals(0, s.longestStreak)
        assertEquals(0, s.lifetimeDone)
        assertEquals(2, s.reserveRemaining)
        assertEquals(7, s.week.size)
        assertTrue(s.week.all { it.state == DayState.NONE })
        assertTrue(s.loaded)
    }

    @Test fun consecutiveFollowThroughsBuildTheStreak() {
        val ev = firedAndDone("r", d(1)) + firedAndDone("r", d(2)) +
            firedAndDone("r", d(3)) + firedAndDone("r", d(4))
        val s = computeProgress(ev, now)
        assertEquals(4, s.currentStreak)
        assertEquals(4, s.longestStreak)
        assertEquals(4, s.lifetimeDone)
        assertEquals(2, s.reserveRemaining)
    }

    @Test fun twoConsecutiveMissesAreAbsorbed() {
        // done, done, miss, miss, done → the two misses are absorbed; Current holds then climbs.
        val ev = firedAndDone("r", d(1)) + firedAndDone("r", d(2)) +
            firedNotDone("r", d(3)) + firedNotDone("r", d(4)) + firedAndDone("r", d(5))
        val s = computeProgress(ev, now)
        assertEquals(3, s.currentStreak)
        assertEquals(3, s.longestStreak)
    }

    @Test fun thirdConsecutiveMissResetsCurrent() {
        // done x3, then miss x3 → the third consecutive miss resets Current; Longest holds.
        val ev = firedAndDone("r", d(1)) + firedAndDone("r", d(2)) + firedAndDone("r", d(3)) +
            firedNotDone("r", d(4)) + firedNotDone("r", d(5)) + firedNotDone("r", d(6))
        val s = computeProgress(ev, now)
        assertEquals(0, s.currentStreak)
        assertEquals(3, s.longestStreak)
        assertEquals(2, s.reserveRemaining) // reserve is refilled after a reset
    }

    @Test fun aFollowThroughRefillsTheReserve() {
        // done, miss, miss, done (refills), miss, miss, done → never hits a third consecutive miss.
        val ev = firedAndDone("r", d(1)) + firedNotDone("r", d(2)) + firedNotDone("r", d(3)) +
            firedAndDone("r", d(4)) + firedNotDone("r", d(5)) + firedNotDone("r", d(6)) +
            firedAndDone("r", d(7))
        val s = computeProgress(ev, now)
        assertEquals(3, s.currentStreak)
        assertEquals(3, s.longestStreak)
    }

    @Test fun reminderlessIntentionsAreExcluded() {
        // A reminderless intention's proactive Did it must not build a streak ("if a reminder was
        // set at all"). Only "rNone" activity exists, and it's excluded → no streak.
        val ev = firedAndDone("rNone", d(1)) + firedAndDone("rNone", d(2))
        val s = computeProgress(ev, now, excludeReminderIds = setOf("rNone"))
        assertEquals(0, s.currentStreak)
        assertEquals(0, s.lifetimeDone)
    }

    @Test fun deliveredButNotActedNeverCredits() {
        val ev = firedNotDone("r", d(1)) + firedNotDone("r", d(2)) + firedNotDone("r", d(3))
        val s = computeProgress(ev, now)
        assertEquals(0, s.currentStreak)
        assertEquals(0, s.lifetimeDone)
    }

    @Test fun snoozeDoesNotCountAsFollowThrough() {
        val ev = listOf(delivered("r", d(1)), snoozed("r", d(1)), delivered("r", d(2)), snoozed("r", d(2)))
        val s = computeProgress(ev, now)
        assertEquals(0, s.currentStreak)
        assertEquals(0, s.lifetimeDone)
    }

    @Test fun todaysUnactedCueIsPendingNotAMiss() {
        // Followed through yesterday; today's cue fired but isn't acted yet → today is pending,
        // so it must not reset or dent the streak built yesterday.
        val ev = firedAndDone("r", d(30)) + firedNotDone("r", d(31)) // d(31) == today
        val s = computeProgress(ev, now)
        assertEquals(1, s.currentStreak)
    }

    @Test fun longestStreakNeverResets() {
        // build 3, reset (3 misses), build 1 → Current drops to 1 but the record holds at 3.
        val ev = firedAndDone("r", d(1)) + firedAndDone("r", d(2)) + firedAndDone("r", d(3)) +
            firedNotDone("r", d(4)) + firedNotDone("r", d(5)) + firedNotDone("r", d(6)) +
            firedAndDone("r", d(7))
        val s = computeProgress(ev, now)
        assertEquals(1, s.currentStreak)
        assertEquals(3, s.longestStreak)
    }

    @Test fun currentWeekRatioIsReportedHonestly() {
        // This (current) week: fired Mon+Tue (2 opportunity days), followed through Mon (1).
        val ev = firedAndDone("r", d(29)) + firedNotDone("r", d(30)) // Mon, Tue of the now-week
        val s = computeProgress(ev, now)
        assertEquals(2, s.weekOpportunities)
        assertEquals(1, s.weekDoneDistinct)
        assertEquals(1, s.weekTarget) // ceil(2/2)
        assertTrue(s.onTrack)
    }

    @Test fun inAppDoneWithoutDeliveredStillCountsAsAnOpportunity() {
        // An in-app "Did it" (Intentions card) logs DONE but no DELIVERED. The week-ratio
        // denominator must union delivered+done, so it can never read "2 of ~0" / "5 of ~1".
        val ev = listOf(done("r", d(29)), done("r2", d(30))) // two in-app dids this week, no fires
        val s = computeProgress(ev, now)
        assertEquals(2, s.weekDoneDistinct)
        assertEquals(2, s.weekOpportunities)
        assertTrue(s.weekOpportunities >= s.weekDoneDistinct)
    }

    @Test fun weeklyGridReflectsDoneMissAndToday() {
        // This week: done Mon, fired-not-done Tue, today (Wed) fired-not-done = pending.
        val ev = firedAndDone("r", d(29)) + firedNotDone("r", d(30)) + firedNotDone("r", d(31))
        val week = computeProgress(ev, now).week
        assertEquals(DayState.DONE, week[0].state)     // Monday
        assertEquals(DayState.MISS, week[1].state)     // Tuesday
        assertEquals(DayState.PENDING, week[2].state)  // Wednesday (today)
        assertEquals(DayState.NONE, week[3].state)     // Thursday (future)
    }
}
