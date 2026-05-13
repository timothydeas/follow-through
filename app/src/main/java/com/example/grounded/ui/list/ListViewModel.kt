package com.example.grounded.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.grounded.data.CheckIn
import com.example.grounded.data.CheckInDao
import com.example.grounded.data.Goal
import com.example.grounded.data.GoalDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

// Number of leading positions in the unified list that are treated as
// "priority" — highlighted in sage green and rank-numbered. Position 0 → rank
// 1, position 1 → rank 2.
private const val PRIORITY_SLOT_COUNT = 2

data class GoalRowData(
    val goal: Goal,
    val checkInCount: Int,
    val latestCheckInDate: Long?,
    val rank: Int?  // 1 or 2 for the top two positions; null for everything below
)

data class ListUiState(
    val goals: List<GoalRowData> = emptyList(),
    val query: String = "",
    val streakDays: Int = 0,
    val streakFlexDayUsed: Boolean = false,
    val totalFollowThroughs: Int = 0
)

/**
 * Transient state held during a drag gesture. [orderedIds] is the in-flight
 * unified ordering; [originalOrderedIds] is the snapshot from when the drag
 * started, used at drop time to detect whether a goal was promoted from
 * below the priority cutoff into the top two slots.
 */
private data class DragState(
    val orderedIds: List<String>,
    val originalOrderedIds: List<String>
)

class ListViewModel(
    private val goalDao: GoalDao,
    private val checkInDao: CheckInDao
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _dragState = MutableStateFlow<DragState?>(null)

    private val _priorityLimitWarning = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val priorityLimitWarning: SharedFlow<Unit> = _priorityLimitWarning.asSharedFlow()

    val uiState: StateFlow<ListUiState> = combine(
        goalDao.getAllGoals(),
        checkInDao.getAllCheckIns(),
        _query,
        _dragState
    ) { goals, allCheckIns, query, dragState ->
        val checkInsByGoal = allCheckIns.groupBy { it.goalId }
        val total = goals.count { it.followedThrough }
        val streak = computeStreakWithFlex(allCheckIns.map { it.createdAt })

        val filtered = if (query.isBlank()) goals
        else goals.filter { it.title.lowercase().contains(query.lowercase()) }
        val isFiltered = query.isNotBlank()

        val ordered: List<Goal> = if (dragState != null) {
            val byId = filtered.associateBy { it.id }
            val knownIds = dragState.orderedIds.toSet()
            val draggedOrder = dragState.orderedIds.mapNotNull { byId[it] }
            // Goals that became visible mid-drag (e.g. query change) get
            // appended to the bottom rather than dropped.
            val extras = filtered.filter { it.id !in knownIds }
                .sortedWith(unifiedComparator)
            draggedOrder + extras
        } else {
            filtered.sortedWith(unifiedComparator)
        }

        val rows = ordered.mapIndexed { idx, goal ->
            val rank = if (!isFiltered && idx < PRIORITY_SLOT_COUNT) idx + 1 else null
            rowFor(goal, rank, checkInsByGoal)
        }

        ListUiState(
            goals = rows,
            query = query,
            streakDays = streak.days,
            streakFlexDayUsed = streak.flexDayUsed,
            totalFollowThroughs = total
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ListUiState()
    )

    fun onQueryChange(value: String) { _query.value = value }

    /**
     * Accessibility up/down arrow handler. Swaps the target goal with its
     * neighbour in the unified ordering and rewrites priority values 1..N
     * across the full list so position is durable.
     */
    fun moveGoal(goalId: String, delta: Int) {
        if (delta != -1 && delta != +1) return
        viewModelScope.launch(Dispatchers.IO) {
            val all = goalDao.getAllGoals().first()
            val sorted = all.sortedWith(unifiedComparator).toMutableList()
            val from = sorted.indexOfFirst { it.id == goalId }
            if (from < 0) return@launch
            val to = from + delta
            if (to < 0 || to >= sorted.size) return@launch

            val warn = from >= PRIORITY_SLOT_COUNT && to < PRIORITY_SLOT_COUNT
            val item = sorted.removeAt(from)
            sorted.add(to, item)
            persistOrder(sorted, all)
            if (warn) _priorityLimitWarning.tryEmit(Unit)
        }
    }

    /**
     * "Move to position" accessibility dialog. 1-indexed position into the
     * unified list.
     */
    fun moveGoalToPosition(goalId: String, targetPosition: Int) {
        if (targetPosition < 1) return
        viewModelScope.launch(Dispatchers.IO) {
            val all = goalDao.getAllGoals().first()
            val sorted = all.sortedWith(unifiedComparator).toMutableList()
            val from = sorted.indexOfFirst { it.id == goalId }
            if (from < 0) return@launch
            val to = (targetPosition - 1).coerceIn(0, sorted.size - 1)
            if (from == to) return@launch

            val warn = from >= PRIORITY_SLOT_COUNT && to < PRIORITY_SLOT_COUNT
            val item = sorted.removeAt(from)
            sorted.add(to, item)
            persistOrder(sorted, all)
            if (warn) _priorityLimitWarning.tryEmit(Unit)
        }
    }

    /**
     * Called by the reorderable drag library on every drag delta. Both
     * indices are positions in the unified flat list. The first call seeds
     * the transient drag state from the current uiState ordering.
     */
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

    /**
     * Called when the drag gesture ends. Persists priorities 1..N across the
     * full ordered list so the on-screen order is durable. Fires the warning
     * if any goal was moved from position >= 2 into the top two slots.
     */
    fun onDragEnd() {
        val state = _dragState.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val allGoals = goalDao.getAllGoals().first()
            val byId = allGoals.associateBy { it.id }
            val origIndexById = state.originalOrderedIds.withIndex()
                .associate { (i, id) -> id to i }

            val newOrdered = state.orderedIds
            val movedFromBelowToTop = newOrdered.take(PRIORITY_SLOT_COUNT).any { id ->
                (origIndexById[id] ?: 0) >= PRIORITY_SLOT_COUNT
            }

            val expectedPriorities = newOrdered.withIndex()
                .associate { (idx, id) -> id to (idx + 1) }

            var anyUpdate = false
            newOrdered.forEachIndexed { idx, id ->
                val goal = byId[id] ?: return@forEachIndexed
                val newPriority = idx + 1
                if (goal.priority != newPriority) {
                    goalDao.updateGoal(goal.copy(priority = newPriority))
                    anyUpdate = true
                }
            }
            // Wait for Room's flow to reflect the new priorities before
            // clearing dragState; otherwise the combine briefly recomputes
            // with stale priorities and the items snap back to their old
            // positions before settling.
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
            if (movedFromBelowToTop) {
                _priorityLimitWarning.tryEmit(Unit)
            }
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            goalDao.deleteById(goalId)
        }
    }

    private fun seedDragState(): DragState? {
        val ids = uiState.value.goals.map { it.goal.id }
        if (ids.isEmpty()) return null
        val seeded = DragState(orderedIds = ids, originalOrderedIds = ids)
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

    private fun rowFor(goal: Goal, rank: Int?, checkInsByGoal: Map<String, List<CheckIn>>): GoalRowData {
        val cis = checkInsByGoal[goal.id] ?: emptyList()
        return GoalRowData(
            goal = goal,
            checkInCount = cis.size,
            latestCheckInDate = cis.maxOfOrNull { it.createdAt },
            rank = rank
        )
    }

    class Factory(
        private val goalDao: GoalDao,
        private val checkInDao: CheckInDao
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ListViewModel(goalDao, checkInDao) as T
    }
}

// Only positive integers count as a real ranked priority. Null, 0, and any
// other unexpected value are treated as unranked, so legacy rows behave
// exactly like fresh ones in the drag-and-drop and arrow flows.
private fun isValidPriority(priority: Int?): Boolean = priority != null && priority > 0

// Sort goals into a single unified order: goals with explicit priority come
// first (lowest priority value = highest rank), goals without an explicit
// priority follow, sorted by createdAt so newly-created goals land at the
// bottom of the list.
private val unifiedComparator: Comparator<Goal> = Comparator { a, b ->
    val aPri = if (isValidPriority(a.priority)) a.priority!! else Int.MAX_VALUE
    val bPri = if (isValidPriority(b.priority)) b.priority!! else Int.MAX_VALUE
    when {
        aPri != bPri -> aPri.compareTo(bPri)
        else -> a.createdAt.compareTo(b.createdAt)
    }
}

private fun startOfDay(ts: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = ts
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

data class StreakResult(val days: Int, val flexDayUsed: Boolean)

/**
 * Streak counts consecutive days ending today (or yesterday, if nothing
 * happened today yet) on which the user did the activity. Up to two
 * consecutive missed days in the window are allowed (the "flex" buffer) —
 * the streak only resets when three or more consecutive missed days are
 * encountered. The buffer resets after each hit. flexDayUsed is true when
 * the flex actually bridged a miss back to a hit (extending the streak).
 */
internal fun computeStreakWithFlex(timestamps: List<Long>): StreakResult {
    val dayMs = 24L * 60 * 60 * 1000

    val activityDays = timestamps.map { startOfDay(it) }.toHashSet()
    if (activityDays.isEmpty()) return StreakResult(0, false)

    val today = startOfDay(System.currentTimeMillis())
    val yesterday = today - dayMs
    val dayBeforeYesterday = today - 2 * dayMs

    val startDay = when {
        activityDays.contains(today) -> today
        activityDays.contains(yesterday) -> yesterday
        activityDays.contains(dayBeforeYesterday) -> dayBeforeYesterday
        else -> return StreakResult(0, false)
    }

    var hitCount = 0
    var flexUsed = false
    var current = startDay
    var consecutiveMisses = 0

    while (true) {
        val isHit = activityDays.contains(current)
        if (isHit) {
            if (consecutiveMisses > 0) {
                flexUsed = true
                consecutiveMisses = 0
            }
            hitCount++
            current -= dayMs
        } else {
            if (consecutiveMisses >= 2) break
            consecutiveMisses++
            current -= dayMs
        }
    }
    return StreakResult(hitCount, flexUsed)
}
