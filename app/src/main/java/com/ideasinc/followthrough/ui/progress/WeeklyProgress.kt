package com.ideasinc.followthrough.ui.progress

import com.ideasinc.followthrough.data.EventAction
import com.ideasinc.followthrough.data.ReminderEvent
import java.util.Calendar

/**
 * The flexible, forgiving weekly progress indicator — ANDROID_HANDOFF.md §4a. NOT a
 * badge, score, flame, or break-the-chain counter. Reads the week warmly and points
 * a hard week forward to Monday.
 *
 * - Measure: follow-through = a non-undone [ReminderEvent] with action == done.
 * - Week: Monday 00:00 → now, device-local, recomputed on read.
 * - Two passes: each week auto-forgives up to 2 misses (Sharif & Shu); 0–2 misses →
 *   kept, 3+ → not kept. Passes apply silently.
 * - Lifetime count never resets. Consecutive kept-weeks is never shown as a number.
 */
enum class WeekState { NO_DATA, ALL_MET, PASSES_USED, NOT_KEPT }

data class WeeklyProgress(
    val state: WeekState,
    val deliveredThisWeek: Int,
    val doneThisWeek: Int,
    val forgivenDays: List<String>,
    val lifetimeDone: Int
) {
    /** Gold accent only on the celebratory states. */
    val isCelebratory: Boolean get() = state == WeekState.ALL_MET || state == WeekState.PASSES_USED

    /** The single weekly line (§4a approved copy, templated). */
    val line: String
        get() = when (state) {
            WeekState.NO_DATA -> "A fresh week — your follow-throughs will show up here."
            WeekState.ALL_MET -> "Followed through on every reminder this week. Nice."
            WeekState.PASSES_USED ->
                "Followed through on $doneThisWeek of $deliveredThisWeek this week — and ${forgivenClause()} forgiven."
            WeekState.NOT_KEPT -> "Some weeks get away from us — and that's allowed. Monday's a clean start."
        }

    /** The quiet lifetime line; null when zero (omit). */
    val lifetimeLine: String?
        get() = if (lifetimeDone <= 0) null else "$lifetimeDone follow-throughs since you started."

    private fun forgivenClause(): String = when (forgivenDays.size) {
        0 -> "a miss was"
        1 -> "${forgivenDays[0]} was"
        2 -> "${forgivenDays[0]} and ${forgivenDays[1]} were"
        else -> "${forgivenDays.dropLast(1).joinToString(", ")}, and ${forgivenDays.last()} were"
    }

    companion object {
        fun empty() = WeeklyProgress(WeekState.NO_DATA, 0, 0, emptyList(), 0)
    }
}

/**
 * Computes [WeeklyProgress] from the full set of NON-UNDONE events (undone events
 * are excluded by the caller via the live-events query).
 */
fun computeWeeklyProgress(liveEvents: List<ReminderEvent>): WeeklyProgress {
    val weekStart = weekStartMillis()
    val weekEvents = liveEvents.filter { it.deliveredAt >= weekStart }
    val delivered = weekEvents.size
    val done = weekEvents.count { it.action == EventAction.DONE }
    val lifetimeDone = liveEvents.count { it.action == EventAction.DONE }

    val misses = (delivered - done).coerceAtLeast(0)
    val state = when {
        delivered == 0 -> WeekState.NO_DATA
        misses == 0 -> WeekState.ALL_MET
        misses <= 2 -> WeekState.PASSES_USED
        else -> WeekState.NOT_KEPT
    }
    // Name up to 2 forgiven days (the days the misses landed on).
    val forgivenDays = if (state == WeekState.PASSES_USED) {
        weekEvents.filter { it.action != EventAction.DONE }
            .sortedBy { it.deliveredAt }
            .take(2)
            .map { dayName(it.deliveredAt) }
    } else emptyList()

    return WeeklyProgress(state, delivered, done, forgivenDays, lifetimeDone)
}

/** Monday 00:00 local of the current week. */
private fun weekStartMillis(): Long {
    val c = Calendar.getInstance()
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    // Walk back to Monday regardless of the locale's first-day-of-week.
    while (c.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
        c.add(Calendar.DAY_OF_MONTH, -1)
    }
    return c.timeInMillis
}

private val dayFormatter = java.text.SimpleDateFormat("EEEE", java.util.Locale.getDefault())
private fun dayName(epochMs: Long): String = dayFormatter.format(java.util.Date(epochMs))
