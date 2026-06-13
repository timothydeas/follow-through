package com.ideasinc.followthrough.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ideasinc.followthrough.data.Goal
import com.ideasinc.followthrough.data.GoalDao
import com.ideasinc.followthrough.data.ReminderDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Above this many active goals, the gentle "focus on fewer goals" message can show.
private const val FOCUS_SLOT_COUNT = 3

// ~6 days between the first and second (and final) showing of the focus message.
internal const val FOCUS_REMINDER_REPEAT_DELAY_MS = 6L * 24 * 60 * 60 * 1000

internal data class FocusReminderState(
    val firstShownAt: Long?,
    val secondShown: Boolean
)

internal data class FocusReminderDecision(
    val show: Boolean,
    val newState: FocusReminderState
)

/**
 * Decides whether the focus message should appear, given how many active goals
 * exist, what has been shown before, and the current time. Pure, so the 6-day
 * re-fire is unit-testable.
 */
internal fun decideFocusReminder(
    activeGoalCount: Int,
    state: FocusReminderState,
    now: Long
): FocusReminderDecision = when {
    activeGoalCount <= FOCUS_SLOT_COUNT -> FocusReminderDecision(false, state)
    state.firstShownAt == null -> FocusReminderDecision(true, state.copy(firstShownAt = now))
    !state.secondShown && now - state.firstShownAt >= FOCUS_REMINDER_REPEAT_DELAY_MS ->
        FocusReminderDecision(true, state.copy(secondShown = true))
    else -> FocusReminderDecision(false, state)
}

/** One Goals-screen card: the goal + how many reminders it has. */
data class GoalRowData(
    val goal: Goal,
    val reminderCount: Int
)

data class ListUiState(
    val goals: List<GoalRowData> = emptyList(),
    val query: String = ""
)

class ListViewModel(
    private val goalDao: GoalDao,
    private val reminderDao: ReminderDao
) : ViewModel() {

    private val _query = MutableStateFlow("")

    val uiState: StateFlow<ListUiState> = combine(
        goalDao.getAllGoals(),
        reminderDao.getActiveReminders(),
        _query
    ) { goals, reminders, query ->
        val countByGoal = reminders.groupingBy { it.goalId }.eachCount()
        val activeGoals = goals.filter { !it.followedThrough }
        val filtered = if (query.isBlank()) activeGoals
        else activeGoals.filter { it.title.lowercase().contains(query.lowercase()) }
        val rows = filtered.sortedWith(unifiedComparator)
            .map { GoalRowData(it, countByGoal[it.id] ?: 0) }
        ListUiState(goals = rows, query = query)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ListUiState()
    )

    fun onQueryChange(value: String) { _query.value = value }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch(Dispatchers.IO) { goalDao.deleteById(goalId) }
    }

    class Factory(
        private val goalDao: GoalDao,
        private val reminderDao: ReminderDao
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ListViewModel(goalDao, reminderDao) as T
    }
}

// Stable order: any drag-era priority first (ascending), then by creation time.
private fun isValidPriority(priority: Int?): Boolean = priority != null && priority > 0

private val unifiedComparator: Comparator<Goal> = Comparator { a, b ->
    val aPri = if (isValidPriority(a.priority)) a.priority!! else Int.MAX_VALUE
    val bPri = if (isValidPriority(b.priority)) b.priority!! else Int.MAX_VALUE
    when {
        aPri != bPri -> aPri.compareTo(bPri)
        else -> a.createdAt.compareTo(b.createdAt)
    }
}
