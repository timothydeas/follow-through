package com.ideasinc.followthrough.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ideasinc.followthrough.data.EventAction
import com.ideasinc.followthrough.data.ReminderDao
import com.ideasinc.followthrough.data.ReminderEvent
import com.ideasinc.followthrough.data.ReminderEventDao
import com.ideasinc.followthrough.data.ScheduleMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

/** A single day's status in the weekly grid. MISS is shown plainly — never red, never shamed. */
enum class DayState { DONE, MISS, PENDING, NONE }

data class WeekDay(val dayStartMillis: Long, val state: DayState)

/**
 * The forgiving-streak surface (CLAUDE.md rule #5, [[project_streak_philosophy]]).
 *
 * [currentStreak] is a **Current streak with a two-miss reserve** (Milkman): each follow-through
 * opportunity (one per intention per day) either climbs the streak or is a miss. A follow-through
 * refills the reserve; the first two consecutive misses are absorbed and the streak holds; a third
 * consecutive miss resets Current to 0 (an honest, gentle reset). [reserveRemaining] (0–2) is how
 * many misses are still absorbed. [longestStreak] (the best run ever) and [lifetimeDone] never
 * reset, cushioning a reset so a lapse never erases history. Only reminders the user actually SET
 * count — reminderless intentions (ScheduleMode.NONE) are excluded (`excludeReminderIds`).
 *
 * The week's true ratio ([weekDoneDistinct] of ~[weekOpportunities]) and [slippedLastWeek] are
 * surfaced honestly (Fishbach) so "gentle" never tips into ignorance-is-bliss — but framed as
 * information and course-correction, never guilt.
 */
data class ProgressState(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val reserveRemaining: Int = 2,
    val weekDoneDistinct: Int = 0,
    val weekOpportunities: Int = 0,
    val weekTarget: Int = 0,
    val onTrack: Boolean = false,
    val slippedLastWeek: Boolean = false,
    val monthDone: Int = 0,
    val lifetimeDone: Int = 0,
    val week: List<WeekDay> = emptyList(),
    val loaded: Boolean = false
)

private enum class WeekClass { WIN, OFF, NONE }

class ProgressViewModel(
    private val eventDao: ReminderEventDao,
    private val reminderDao: ReminderDao
) : ViewModel() {

    val uiState: StateFlow<ProgressState> =
        combine(eventDao.getLiveEvents(), reminderDao.getAllReminders()) { events, reminders ->
            // Only reminders the user actually SET count toward the streak — exclude reminderless
            // (ScheduleMode.NONE) intentions ("if a reminder was set at all").
            val excluded = reminders.filter { it.scheduleMode == ScheduleMode.NONE }.map { it.id }.toSet()
            computeProgress(events, System.currentTimeMillis(), excluded)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressState())

    class Factory(
        private val eventDao: ReminderEventDao,
        private val reminderDao: ReminderDao
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ProgressViewModel(eventDao, reminderDao) as T
    }
}

/**
 * Pure streak/grid computation, isolated so the rule is easy to read and reason about.
 *
 * The **streak** walks follow-through opportunities (one per intention per day) oldest→newest. A
 * follow-through (Did it) climbs [currentStreak] and refills the reserve; a miss (a cue delivered,
 * or a day that should have been acted, with no Did it) spends reserve. The first two consecutive
 * misses are absorbed (the streak holds); a third consecutive miss resets Current to 0. Only
 * RESOLVED opportunities count — today's not-yet-acted cue is pending, never a miss — so the
 * number never flickers. [longestStreak] is the best run ever and never resets.
 *
 * The weekly grid and the in-progress "this week" ratio (≥ ~half the days you were nudged =
 * on track) are reported alongside the streak as honest context, never as the streak rule itself.
 *
 * [excludeReminderIds] drops reminderless intentions (ScheduleMode.NONE) — only reminders the
 * user actually set count. Opportunities and follow-throughs are de-duplicated per (intention,
 * day) so a snoozed re-fire or a double tap can't distort the count.
 */
fun computeProgress(
    events: List<ReminderEvent>,
    now: Long,
    excludeReminderIds: Set<String> = emptySet()
): ProgressState {
    val done = events.filter { it.action == EventAction.DONE && it.reminderId !in excludeReminderIds }
    val delivered = events.filter { it.action == EventAction.DELIVERED && it.reminderId !in excludeReminderIds }

    // weekStart -> distinct (intentionId, day) keys, for opportunities and follow-throughs.
    val doneByWeek = done.groupBy { mondayStartOf(it.deliveredAt) }
        .mapValues { (_, evs) -> evs.map { it.reminderId to startOfDay(it.deliveredAt) }.toSet() }
    val deliveredByWeek = delivered.groupBy { mondayStartOf(it.deliveredAt) }
        .mapValues { (_, evs) -> evs.map { it.reminderId to startOfDay(it.deliveredAt) }.toSet() }

    // A week's follow-through opportunities = the (intention, day) pairs the cue fired on (delivered)
    // OR the user acted on (done). Done-without-delivered is normal — an in-app "Did it" logs DONE
    // but no DELIVERED — so the denominator MUST union both, or the ratio can read a nonsensical
    // "5 of ~1". Mirrors the streak's own opportunityKeys below.
    fun weekOppKeys(weekStart: Long): Set<Pair<String, Long>> =
        deliveredByWeek[weekStart].orEmpty() + doneByWeek[weekStart].orEmpty()

    fun targetFor(opportunities: Int): Int = maxOf(1, (opportunities + 1) / 2)
    fun classify(weekStart: Long): WeekClass {
        val opp = weekOppKeys(weekStart).size
        val dn = doneByWeek[weekStart]?.size ?: 0
        return when {
            opp == 0 && dn == 0 -> WeekClass.NONE
            dn >= targetFor(opp) -> WeekClass.WIN
            else -> WeekClass.OFF
        }
    }

    val currentWeekStart = mondayStartOf(now)
    val today = startOfDay(now)

    // --- Current streak: a two-miss reserve over follow-through opportunities (CLAUDE.md rule
    // #5). One opportunity per (intention, day). A follow-through climbs Current and refills the
    // reserve; the first two consecutive misses are absorbed (Current holds); a third consecutive
    // miss resets Current to 0. Longest is the best run ever and never resets. Only RESOLVED
    // opportunities count — today's not-yet-acted cue is pending, never a miss. ---
    val doneKeys = done.map { it.reminderId to startOfDay(it.deliveredAt) }.toSet()
    val opportunityKeys = (
        delivered.map { it.reminderId to startOfDay(it.deliveredAt) } +
            done.map { it.reminderId to startOfDay(it.deliveredAt) }
        ).distinct()
        .filter { it.second < today || it in doneKeys }
        .sortedWith(compareBy({ it.second }, { it.first }))
    var current = 0
    var longest = 0
    var consecutiveMisses = 0
    for (key in opportunityKeys) {
        if (key in doneKeys) {
            current++
            consecutiveMisses = 0          // any follow-through refills the reserve
            if (current > longest) longest = current
        } else {
            consecutiveMisses++
            if (consecutiveMisses >= 3) {  // first two misses absorbed; the third resets
                current = 0
                consecutiveMisses = 0
            }
        }
    }
    val reserveRemaining = (2 - consecutiveMisses).coerceIn(0, 2)

    // The most recent completed (non-empty) week that dipped below cadence — for the honest,
    // never-guilt slip note. Decoupled from the streak number.
    val slipped = (doneByWeek.keys + deliveredByWeek.keys)
        .filter { it < currentWeekStart && classify(it) != WeekClass.NONE }
        .maxOrNull()?.let { classify(it) == WeekClass.OFF } ?: false

    val weekOpportunities = weekOppKeys(currentWeekStart).size
    val weekDoneDistinct = doneByWeek[currentWeekStart]?.size ?: 0
    val weekTarget = targetFor(weekOpportunities)
    val doneDays = done.map { startOfDay(it.deliveredAt) }.toSet()
    val deliveredDays = delivered.map { startOfDay(it.deliveredAt) }.toSet()

    val week = (0..6).map { i ->
        val d = addDays(currentWeekStart, i)
        val state = when {
            d > today -> DayState.NONE
            d in doneDays -> DayState.DONE
            d == today && d in deliveredDays -> DayState.PENDING
            d in deliveredDays -> DayState.MISS
            else -> DayState.NONE
        }
        WeekDay(d, state)
    }

    val monthStart = monthStartOf(now)
    // Count one follow-through per (intention, day) — same de-dup the streak uses — so duplicate
    // DONE rows (double-tap, or the shade + in-app paths) can't make these contradict the streak.
    return ProgressState(
        currentStreak = current,
        longestStreak = longest,
        reserveRemaining = reserveRemaining,
        weekDoneDistinct = weekDoneDistinct,
        weekOpportunities = weekOpportunities,
        weekTarget = weekTarget,
        onTrack = weekDoneDistinct >= weekTarget && weekDoneDistinct >= 1,
        slippedLastWeek = slipped,
        monthDone = doneKeys.count { it.second >= monthStart },
        lifetimeDone = doneKeys.size,
        week = week,
        loaded = true
    )
}

private fun startOfDay(ts: Long): Long {
    val c = Calendar.getInstance().apply { timeInMillis = ts }
    c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

private fun mondayStartOf(ts: Long): Long {
    val c = Calendar.getInstance().apply { timeInMillis = startOfDay(ts) }
    while (c.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) c.add(Calendar.DAY_OF_MONTH, -1)
    return c.timeInMillis
}

private fun monthStartOf(ts: Long): Long {
    val c = Calendar.getInstance().apply { timeInMillis = ts }
    c.set(Calendar.DAY_OF_MONTH, 1)
    c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

private fun addDays(dayStart: Long, n: Int): Long {
    val c = Calendar.getInstance().apply { timeInMillis = dayStart }
    c.add(Calendar.DAY_OF_MONTH, n)
    return c.timeInMillis
}
