package com.ideasinc.followthrough.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ideasinc.followthrough.data.Goal
import com.ideasinc.followthrough.data.GoalDao
import com.ideasinc.followthrough.data.Reminder
import com.ideasinc.followthrough.data.ReminderDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Number of leading positions in the unified list that are treated as
// "priority" — highlighted and rank-numbered. Position 0 → rank 1, etc.
private const val PRIORITY_SLOT_COUNT = 3

// ~6 days between the first and second (and final) showing of the focus message.
internal const val FOCUS_REMINDER_REPEAT_DELAY_MS = 6L * 24 * 60 * 60 * 1000

/**
 * Persisted tracking for the "focus on fewer goals" message: when it was first
 * shown (null = never), and whether the single follow-up showing has happened.
 */
internal data class FocusReminderState(
    val firstShownAt: Long?,
    val secondShown: Boolean
)

/** Whether to show the message now, plus the state to persist afterward. */
internal data class FocusReminderDecision(
    val show: Boolean,
    val newState: FocusReminderState
)

/**
 * Decides whether the top-3 focus message should appear, given how many active
 * goals exist, what has been shown before, and the current time. Pure, so the
 * 6-day re-fire is unit-testable.
 */
internal fun decideFocusReminder(
    activeGoalCount: Int,
    state: FocusReminderState,
    now: Long
): FocusReminderDecision = when {
    activeGoalCount <= PRIORITY_SLOT_COUNT -> FocusReminderDecision(false, state)
    state.firstShownAt == null -> FocusReminderDecision(true, state.copy(firstShownAt = now))
    !state.secondShown && now - state.firstShownAt >= FOCUS_REMINDER_REPEAT_DELAY_MS ->
        FocusReminderDecision(true, state.copy(secondShown = true))
    else -> FocusReminderDecision(false, state)
}

data class GoalRowData(
    val goal: Goal,
    val rank: Int?,  // 1, 2, or 3 for the top three positions; null below
    // Subtitle: the goal's most recent reminder's intention ("When …, I will …").
    val currentIntention: String?,
    val hasReminder: Boolean
)

data class ListUiState(
    val goals: List<GoalRowData> = emptyList(),
    val query: String = ""
)

private data class DragState(val orderedIds: List<String>)

class ListViewModel(
    private val goalDao: GoalDao,
    private val reminderDao: ReminderDao
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _dragState = MutableStateFlow<DragState?>(null)

    val uiState: StateFlow<ListUiState> = combine(
        goalDao.getAllGoals(),
        reminderDao.getActiveReminders(),
        _query,
        _dragState
    ) { goals, reminders, query, dragState ->
        val remindersByGoal = reminders.groupBy { it.goalId }

        val activeGoals = goals.filter { !it.followedThrough }
        val filtered = if (query.isBlank()) activeGoals
        else activeGoals.filter { it.title.lowercase().contains(query.lowercase()) }
        val isFiltered = query.isNotBlank()

        val ordered: List<Goal> = if (dragState != null) {
            val byId = filtered.associateBy { it.id }
            val knownIds = dragState.orderedIds.toSet()
            val draggedOrder = dragState.orderedIds.mapNotNull { byId[it] }
            val extras = filtered.filter { it.id !in knownIds }.sortedWith(unifiedComparator)
            draggedOrder + extras
        } else {
            filtered.sortedWith(unifiedComparator)
        }

        val rows = ordered.mapIndexed { idx, goal ->
            val rank = if (!isFiltered && idx < PRIORITY_SLOT_COUNT) idx + 1 else null
            rowFor(goal, rank, remindersByGoal)
        }

        ListUiState(goals = rows, query = query)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ListUiState()
    )

    fun onQueryChange(value: String) { _query.value = value }

    fun moveGoal(goalId: String, delta: Int) {
        if (delta != -1 && delta != +1) return
        viewModelScope.launch(Dispatchers.IO) {
            val all = goalDao.getAllGoals().first()
            val sorted = all.sortedWith(unifiedComparator).toMutableList()
            val from = sorted.indexOfFirst { it.id == goalId }
            if (from < 0) return@launch
            val to = from + delta
            if (to < 0 || to >= sorted.size) return@launch
            val item = sorted.removeAt(from)
            sorted.add(to, item)
            persistOrder(sorted, all)
        }
    }

    fun moveGoalToPosition(goalId: String, targetPosition: Int) {
        if (targetPosition < 1) return
        viewModelScope.launch(Dispatchers.IO) {
            val all = goalDao.getAllGoals().first()
            val sorted = all.sortedWith(unifiedComparator).toMutableList()
            val from = sorted.indexOfFirst { it.id == goalId }
            if (from < 0) return@launch
            val to = (targetPosition - 1).coerceIn(0, sorted.size - 1)
            if (from == to) return@launch
            val item = sorted.removeAt(from)
            sorted.add(to, item)
            persistOrder(sorted, all)
        }
    }

    fun onDragMove(fromIdx: Int, toIdx: Int) {
        val state = _dragState.value ?: seedDragState() ?: return
        if (fromIdx < 0 || fromIdx >= state.orderedIds.size) return
        if (toIdx < 0 || toIdx >= state.orderedIds.size) return
        if (fromIdx == toIdx) return
        val list = state.orderedIds.toMutableList()
        val item = list.removeAt(fromIdx)
        list.add(toIdx, item)
        _dragState.value = state.copy(orderedIds = list)
    }

    fun onDragEnd() {
        val state = _dragState.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val allGoals = goalDao.getAllGoals().first()
            val byId = allGoals.associateBy { it.id }
            val newOrdered = state.orderedIds
            val expectedPriorities = newOrdered.withIndex().associate { (idx, id) -> id to (idx + 1) }

            var anyUpdate = false
            newOrdered.forEachIndexed { idx, id ->
                val goal = byId[id] ?: return@forEachIndexed
                val newPriority = idx + 1
                if (goal.priority != newPriority) {
                    goalDao.updateGoal(goal.copy(priority = newPriority))
                    anyUpdate = true
                }
            }
            if (anyUpdate) {
                goalDao.getAllGoals().first { goals ->
                    val freshById = goals.associateBy { it.id }
                    expectedPriorities.entries.all { (id, expected) ->
                        val fresh = freshById[id] ?: return@all true
                        fresh.priority == expected
                    }
                }
            }
            _dragState.value = null
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch(Dispatchers.IO) { goalDao.deleteById(goalId) }
    }

    private fun seedDragState(): DragState? {
        val ids = uiState.value.goals.map { it.goal.id }
        if (ids.isEmpty()) return null
        val seeded = DragState(orderedIds = ids)
        _dragState.value = seeded
        return seeded
    }

    private suspend fun persistOrder(ordered: List<Goal>, snapshot: List<Goal>) {
        val byId = snapshot.associateBy { it.id }
        ordered.forEachIndexed { idx, goal ->
            val newPriority = idx + 1
            val current = byId[goal.id] ?: return@forEachIndexed
            if (current.priority != newPriority) {
                goalDao.updateGoal(current.copy(priority = newPriority))
            }
        }
    }

    private fun rowFor(
        goal: Goal,
        rank: Int?,
        remindersByGoal: Map<String, List<Reminder>>
    ): GoalRowData {
        val goalReminders = remindersByGoal[goal.id].orEmpty()
        val currentIntention = goalReminders.maxByOrNull { it.createdAt }?.intentionText?.trim()
        return GoalRowData(
            goal = goal,
            rank = rank,
            currentIntention = currentIntention,
            hasReminder = goalReminders.isNotEmpty()
        )
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

// Only positive integers count as a real ranked priority.
private fun isValidPriority(priority: Int?): Boolean = priority != null && priority > 0

private val unifiedComparator: Comparator<Goal> = Comparator { a, b ->
    val aPri = if (isValidPriority(a.priority)) a.priority!! else Int.MAX_VALUE
    val bPri = if (isValidPriority(b.priority)) b.priority!! else Int.MAX_VALUE
    when {
        aPri != bPri -> aPri.compareTo(bPri)
        else -> a.createdAt.compareTo(b.createdAt)
    }
}
