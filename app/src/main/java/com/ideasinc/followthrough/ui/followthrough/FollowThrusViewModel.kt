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
    goalDao: GoalDao,
    checkInDao: CheckInDao
) : ViewModel() {

    val uiState: StateFlow<FollowThrusUiState> = combine(
        goalDao.getAllGoals(),
        checkInDao.getAllCheckIns()
    ) { goals, checkIns ->
        val byGoal = checkIns.groupBy { it.goalId }
        val records = goals
            .filter { it.followedThrough }
            .sortedByDescending { it.followedThroughAt ?: it.updatedAt }
            .map { goal ->
                val cis = byGoal[goal.id].orEmpty().sortedByDescending { it.createdAt }
                FollowThruRecord(
                    goalId = goal.id,
                    title = goal.title,
                    intention = cis.firstNotNullOfOrNull { c ->
                        c.implementationIntention?.takeIf { it.isNotBlank() }
                    },
                    whatYouDid = cis.firstNotNullOfOrNull { c ->
                        c.madeProgress?.takeIf { it.isNotBlank() }
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
