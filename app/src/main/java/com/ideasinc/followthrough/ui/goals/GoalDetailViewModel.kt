package com.ideasinc.followthrough.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ideasinc.followthrough.data.Barrier
import com.ideasinc.followthrough.data.CheckIn
import com.ideasinc.followthrough.data.CheckInDao
import com.ideasinc.followthrough.data.Goal
import com.ideasinc.followthrough.data.GoalContentDao
import com.ideasinc.followthrough.data.GoalDao
import com.ideasinc.followthrough.data.ProgressNote
import com.ideasinc.followthrough.data.Reminder
import com.ideasinc.followthrough.data.ReminderDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class GoalDetailUiState(
    val goal: Goal? = null,
    val barriers: List<Barrier> = emptyList(),
    val progressNotes: List<ProgressNote> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
    val shouldNavigateToList: Boolean = false,
    val showEditDialog: Boolean = false,
    val editTitle: String = "",
    val editWhy: String = "",
    val showReassurance: Boolean = false
)

class GoalDetailViewModel(
    private val goalDao: GoalDao,
    private val checkInDao: CheckInDao,
    private val goalContentDao: GoalContentDao,
    private val reminderDao: ReminderDao,
    private val goalId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalDetailUiState())
    val uiState: StateFlow<GoalDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                goalDao.getGoalByIdAsFlow(goalId),
                goalContentDao.getBarriers(goalId),
                goalContentDao.getProgressNotes(goalId),
                reminderDao.getRemindersForGoal(goalId)
            ) { goal, barriers, notes, reminders ->
                GoalDetailUiState(
                    goal = goal,
                    barriers = barriers,
                    progressNotes = notes,
                    reminders = reminders
                )
            }.collect { data ->
                _uiState.update {
                    it.copy(
                        goal = data.goal,
                        barriers = data.barriers,
                        progressNotes = data.progressNotes,
                        reminders = data.reminders
                    )
                }
            }
        }
    }

    fun showEditDialog() {
        val goal = _uiState.value.goal ?: return
        _uiState.update { it.copy(showEditDialog = true, editTitle = goal.title, editWhy = goal.whyItMatters) }
    }

    fun dismissEditDialog() = _uiState.update { it.copy(showEditDialog = false) }
    fun onEditTitleChange(value: String) = _uiState.update { it.copy(editTitle = value) }
    fun onEditWhyChange(value: String) = _uiState.update { it.copy(editWhy = value) }

    fun saveGoalEdit() {
        viewModelScope.launch {
            val goal = _uiState.value.goal ?: return@launch
            val title = _uiState.value.editTitle.trim().ifBlank { return@launch }
            goalDao.updateGoal(
                goal.copy(
                    title = title,
                    whyItMatters = _uiState.value.editWhy.trim(),
                    updatedAt = System.currentTimeMillis()
                )
            )
            _uiState.update { it.copy(showEditDialog = false) }
        }
    }

    fun addBarrier(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            goalContentDao.upsertBarrier(
                Barrier(UUID.randomUUID().toString(), goalId, clean, System.currentTimeMillis())
            )
        }
    }

    fun deleteBarrier(id: String) {
        viewModelScope.launch { goalContentDao.deleteBarrier(id) }
    }

    fun addProgressNote(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            goalContentDao.upsertProgressNote(
                ProgressNote(UUID.randomUUID().toString(), goalId, clean, System.currentTimeMillis())
            )
        }
    }

    fun deleteProgressNote(id: String) {
        viewModelScope.launch { goalContentDao.deleteProgressNote(id) }
    }

    fun followThrough() {
        viewModelScope.launch {
            val goal = _uiState.value.goal ?: return@launch
            if (goal.followedThrough) return@launch
            val now = System.currentTimeMillis()
            goalDao.updateGoal(goal.copy(followedThrough = true, followedThroughAt = now, updatedAt = now))
            _uiState.update { it.copy(showReassurance = true) }
        }
    }

    fun onReassuranceDone() = _uiState.update { it.copy(showReassurance = false) }

    /**
     * Snapshot of this goal's check-ins, for the screen to clean up any legacy
     * per-check-in reminder alarms/channels/cue images before the goal is deleted.
     * (New reminders cascade-delete in the DB; their alarms are cleaned in slice 7.)
     */
    suspend fun legacyCheckInsForCleanup(): List<CheckIn> =
        checkInDao.getCheckInsForGoal(goalId).first()

    fun deleteGoal() {
        viewModelScope.launch {
            goalDao.deleteById(goalId)
            _uiState.update { it.copy(shouldNavigateToList = true) }
        }
    }

    class Factory(
        private val goalDao: GoalDao,
        private val checkInDao: CheckInDao,
        private val goalContentDao: GoalContentDao,
        private val reminderDao: ReminderDao,
        private val goalId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            GoalDetailViewModel(goalDao, checkInDao, goalContentDao, reminderDao, goalId) as T
    }
}
