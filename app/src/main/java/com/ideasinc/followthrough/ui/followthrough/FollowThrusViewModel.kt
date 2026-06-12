package com.ideasinc.followthrough.ui.followthrough

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ideasinc.followthrough.data.CheckInDao
import com.ideasinc.followthrough.data.GoalDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * One past follow-through, as the user's own record: the moment (the goal),
 * their own implementation intention, and — when they noted it — what they did.
 * Built entirely from existing follow-through data; no new external data.
 */
data class FollowThruRecord(
    val goalId: String,
    val title: String,
    val intention: String?,
    val whatYouDid: String?,
    val completedAt: Long?
)

data class FollowThrusUiState(
    val records: List<FollowThruRecord> = emptyList()
)

class FollowThrusViewModel(
    private val goalDao: GoalDao,
    checkInDao: CheckInDao
) : ViewModel() {

    /**
     * Undo a logged follow-through (relocated here from Goal Detail). Reverts the
     * goal to active; the record then drops out of this list automatically.
     */
    fun undoFollowThrough(goalId: String) {
        viewModelScope.launch {
            val goal = goalDao.getGoalById(goalId) ?: return@launch
            if (!goal.followedThrough) return@launch
            val now = System.currentTimeMillis()
            goalDao.updateGoal(
                goal.copy(
                    followedThrough = false,
                    followedThroughAt = null,
                    updatedAt = now
                )
            )
        }
    }

    val uiState: StateFlow<FollowThrusUiState> = combine(
        goalDao.getAllGoals(),
        checkInDao.getAllCheckIns()
    ) { goals, checkIns ->
        val checkInsByGoal = checkIns.groupBy { it.goalId }
        val records = goals
            .filter { it.followedThrough }
            .sortedByDescending { it.followedThroughAt ?: it.updatedAt }
            .map { goal ->
                val cis = checkInsByGoal[goal.id].orEmpty().sortedByDescending { it.createdAt }
                FollowThruRecord(
                    goalId = goal.id,
                    title = goal.title,
                    // The moment's plan: the most recent check-in intention.
                    intention = cis.firstNotNullOfOrNull { c ->
                        c.intention.takeIf { it.isNotBlank() }
                    },
                    whatYouDid = cis.firstNotNullOfOrNull { c ->
                        c.note.takeIf { it.isNotBlank() }
                    },
                    completedAt = goal.followedThroughAt
                )
            }
        FollowThrusUiState(records = records)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FollowThrusUiState()
    )

    class Factory(
        private val goalDao: GoalDao,
        private val checkInDao: CheckInDao
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            FollowThrusViewModel(goalDao, checkInDao) as T
    }
}
