package com.ideasinc.followthrough.ui.list

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Timing rules for the "focus on fewer goals" message. The 6-day re-fire can't
 * be hand-tested on device, so the pure [decideFocusReminder] carries the logic
 * and this verifies it: show the first time the user exceeds three goals, show
 * once more ~6 days later while still over three, and never again.
 */
class FocusReminderTest {

    private val t0 = 1_000_000_000_000L // arbitrary fixed "now"
    private val sixDays = FOCUS_REMINDER_REPEAT_DELAY_MS

    @Test
    fun `not shown at or below three goals`() {
        val fresh = FocusReminderState(firstShownAt = null, secondShown = false)
        for (count in 0..3) {
            val d = decideFocusReminder(count, fresh, t0)
            assertFalse("count=$count should not show", d.show)
            assertEquals("state unchanged", fresh, d.newState)
        }
    }

    @Test
    fun `first showing fires when count first exceeds three`() {
        val fresh = FocusReminderState(firstShownAt = null, secondShown = false)
        val d = decideFocusReminder(4, fresh, t0)
        assertTrue(d.show)
        assertEquals(t0, d.newState.firstShownAt)
        assertFalse(d.newState.secondShown)
    }

    @Test
    fun `no second showing before six days elapse`() {
        val afterFirst = FocusReminderState(firstShownAt = t0, secondShown = false)
        // One millisecond shy of the window.
        val d = decideFocusReminder(5, afterFirst, t0 + sixDays - 1)
        assertFalse(d.show)
        assertEquals(afterFirst, d.newState)
    }

    @Test
    fun `second showing fires at six days while still over three`() {
        val afterFirst = FocusReminderState(firstShownAt = t0, secondShown = false)
        val d = decideFocusReminder(4, afterFirst, t0 + sixDays)
        assertTrue(d.show)
        assertTrue(d.newState.secondShown)
        assertEquals(t0, d.newState.firstShownAt)
    }

    @Test
    fun `no second showing if back to three or fewer when the window arrives`() {
        val afterFirst = FocusReminderState(firstShownAt = t0, secondShown = false)
        val d = decideFocusReminder(3, afterFirst, t0 + sixDays + 1)
        assertFalse(d.show)
        assertEquals(afterFirst, d.newState)
    }

    @Test
    fun `never shows a third time`() {
        val afterSecond = FocusReminderState(firstShownAt = t0, secondShown = true)
        // Far past the window, still many goals.
        val d = decideFocusReminder(9, afterSecond, t0 + sixDays * 10)
        assertFalse(d.show)
        assertEquals(afterSecond, d.newState)
    }

    @Test
    fun `dipping below three then back up does not reset the first showing`() {
        // First showing recorded.
        val first = decideFocusReminder(4, FocusReminderState(null, false), t0)
        assertTrue(first.show)
        // Drop to three: no show, state preserved.
        val dip = decideFocusReminder(3, first.newState, t0 + sixDays / 2)
        assertFalse(dip.show)
        assertEquals(first.newState, dip.newState)
        // Climb back above three before the window: still no early second show.
        val backUp = decideFocusReminder(5, dip.newState, t0 + sixDays - 1)
        assertFalse(backUp.show)
        // And the original first-shown timestamp is intact, so the window is honored.
        assertEquals(t0, backUp.newState.firstShownAt)
    }

    @Test
    fun `default fresh state has no first showing`() {
        assertNull(FocusReminderState(firstShownAt = null, secondShown = false).firstShownAt)
    }
}
