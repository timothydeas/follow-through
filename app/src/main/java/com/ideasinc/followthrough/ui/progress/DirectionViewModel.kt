package com.ideasinc.followthrough.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ideasinc.followthrough.data.DirectionCheckIn
import com.ideasinc.followthrough.data.DirectionCheckInDao
import com.ideasinc.followthrough.data.DirectionFeeling
import com.ideasinc.followthrough.data.Goal
import com.ideasinc.followthrough.data.GoalDao
import com.ideasinc.followthrough.data.Reminder
import com.ideasinc.followthrough.data.ReminderDao
import com.ideasinc.followthrough.data.ReminderEvent
import com.ideasinc.followthrough.data.ReminderEventDao
import com.ideasinc.followthrough.data.ReminderStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/** The one direction whose check-in is due right now (null = none). [intention] names which
 *  intention it's about so the card never reads as an orphaned, context-free prompt. */
data class DueCheckIn(val goalId: String, val reminderId: String, val intention: String, val direction: String)

/** A past own-words learning, for the reusable "things you've learned" list. [direction] is the
 *  parent direction it came from, carried so "make this an intention" can pre-fill it. */
data class DirectionInsight(val id: String, val text: String, val direction: String = "")

data class DirectionUiState(
    val due: DueCheckIn? = null,
    val learnings: List<DirectionInsight> = emptyList(),
    val loaded: Boolean = false
)

private const val DAY = 86_400_000L
const val FIRST_CHECKIN_MIN_AGE = 7 * DAY      // earliest first check-in (before the ~2-week cliff)
const val FIRST_CHECKIN_BACKSTOP = 14 * DAY    // ask by here even without logged activity
const val RECHECK_INTERVAL = 21 * DAY          // ~3 weeks between answered check-ins
const val DISMISS_SNOOZE = 7 * DAY             // "ask me later" resurfaces after ~1 week

/**
 * The Direction surface (Direction_Feature_Spec.md): the periodic "is this direction working?"
 * check-in and the reusable learnings, shown on the Progress tab. All local (rule #6).
 */
class DirectionViewModel(
    private val goalDao: GoalDao,
    private val reminderDao: ReminderDao,
    private val eventDao: ReminderEventDao,
    private val checkInDao: DirectionCheckInDao
) : ViewModel() {

    val uiState: StateFlow<DirectionUiState> =
        combine(
            goalDao.getAllGoals(),
            reminderDao.getAllReminders(),
            eventDao.getLiveEvents(),
            checkInDao.getAll()
        ) { goals, reminders, events, checkIns ->
            val directionByGoal = goals.associate { it.id to it.whyItMatters.trim() }
            DirectionUiState(
                due = computeDueCheckIn(goals, reminders, events, checkIns, System.currentTimeMillis()),
                // Only genuine "Learned something" notes surface as reusable learnings — the
                // optional "what's in the way?" note from the Not-really path is a private signal,
                // not a thing the user chose to remember.
                learnings = checkIns
                    .filter { it.feeling == DirectionFeeling.LEARNED && !it.noteText.isNullOrBlank() }
                    .map { DirectionInsight(it.id, it.noteText!!.trim(), directionByGoal[it.goalId].orEmpty()) },
                loaded = true
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DirectionUiState())

    /** Record a progress answer (optionally with a learning note). Resets the cadence. */
    fun answer(goalId: String, feeling: String, note: String? = null) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            checkInDao.insert(
                DirectionCheckIn(
                    id = UUID.randomUUID().toString(),
                    goalId = goalId,
                    askedAt = now,
                    answeredAt = now,
                    feeling = feeling,
                    noteText = note?.trim()?.ifBlank { null }
                )
            )
        }
    }

    /** "Ask me later" — snoozes the prompt (~1 week), not a progress answer. */
    fun dismiss(goalId: String) = answer(goalId, DirectionFeeling.DISMISSED, null)

    class Factory(
        private val goalDao: GoalDao,
        private val reminderDao: ReminderDao,
        private val eventDao: ReminderEventDao,
        private val checkInDao: DirectionCheckInDao
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DirectionViewModel(goalDao, reminderDao, eventDao, checkInDao) as T
    }
}

/**
 * Pure: which direction's check-in is due now (or null). Only goals with a direction
 * ([Goal.whyItMatters]) and at least one intention to adjust are eligible. The first check-in
 * lands early (≥7 days old + some activity, or ≥14 days as a backstop) — before the ~2-week
 * uninstall cliff; later ones are rare (~3 weeks apart) after a positive answer, but only ~1 week
 * after a "not really" (a flagged direction earns a sooner re-check). "Ask me later" snoozes ~1 week.
 * When several are due, the most overdue (oldest last-answered, else oldest goal) wins, so only
 * one is ever surfaced at a time.
 */
fun computeDueCheckIn(
    goals: List<Goal>,
    reminders: List<Reminder>,
    events: List<ReminderEvent>,
    checkIns: List<DirectionCheckIn>,
    now: Long
): DueCheckIn? {
    val remindersByGoal = reminders.groupBy { it.goalId }
    val checkInsByGoal = checkIns.groupBy { it.goalId }
    val activeReminderIds = events.mapTo(HashSet()) { it.reminderId }

    var best: DueCheckIn? = null
    var bestSortKey = Long.MAX_VALUE

    for (goal in goals) {
        if (goal.whyItMatters.isBlank()) continue
        val goalReminders = remindersByGoal[goal.id].orEmpty()
        if (goalReminders.isEmpty()) continue
        val reminder = goalReminders.firstOrNull { it.status == ReminderStatus.ACTIVE } ?: goalReminders.first()

        val goalCheckIns = checkInsByGoal[goal.id].orEmpty()
        val lastAnswer = goalCheckIns.filter { DirectionFeeling.isProgressAnswer(it.feeling) }.maxByOrNull { it.answeredAt }
        val lastAnswered = lastAnswer?.answeredAt
        val lastDismissed = goalCheckIns.filter { it.feeling == DirectionFeeling.DISMISSED }.maxOfOrNull { it.answeredAt }

        if (lastDismissed != null && now - lastDismissed < DISMISS_SNOOZE) continue

        val due = if (lastAnswered == null) {
            val age = now - goal.createdAt
            val hasActivity = goalReminders.any { it.id in activeReminderIds }
            age >= FIRST_CHECKIN_MIN_AGE && (hasActivity || age >= FIRST_CHECKIN_BACKSTOP)
        } else {
            // A "not really" answer flags the direction as not working — re-check soon (like a
            // snooze), so a deferred problem resurfaces in ~1 week, not after the full quiet
            // interval a positive answer earns. Keeps "Not now" honest: it means "ask me again soon".
            val interval = if (lastAnswer.feeling == DirectionFeeling.NOT_REALLY) DISMISS_SNOOZE else RECHECK_INTERVAL
            now - lastAnswered >= interval
        }
        if (!due) continue

        val sortKey = lastAnswered ?: goal.createdAt
        if (sortKey < bestSortKey) {
            bestSortKey = sortKey
            best = DueCheckIn(
                goalId = goal.id,
                reminderId = reminder.id,
                intention = reminder.intentionText,
                direction = goal.whyItMatters.trim()
            )
        }
    }
    return best
}
