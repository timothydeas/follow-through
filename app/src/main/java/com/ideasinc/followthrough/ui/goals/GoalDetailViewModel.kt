package com.ideasinc.followthrough.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ideasinc.followthrough.data.Barrier
import com.ideasinc.followthrough.data.Goal
import com.ideasinc.followthrough.data.GoalContentDao
import com.ideasinc.followthrough.data.GoalDao
import com.ideasinc.followthrough.data.PaletteDao
import com.ideasinc.followthrough.data.PassionInterest
import com.ideasinc.followthrough.data.ProgressNote
import com.ideasinc.followthrough.data.Reminder
import com.ideasinc.followthrough.data.ReminderDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    val editMotivation: String = "",
    val editWantTo: String = "",
    val editLinkedPassions: Set<String> = emptySet(),
    val allPassions: List<PassionInterest> = emptyList(),
    val showReassurance: Boolean = false
)

class GoalDetailViewModel(
    private val goalDao: GoalDao,
    private val goalContentDao: GoalContentDao,
    private val reminderDao: ReminderDao,
    private val paletteDao: PaletteDao,
    private val goalId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalDetailUiState())
    val uiState: StateFlow<GoalDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            paletteDao.getPassions().collect { passions ->
                _uiState.update { it.copy(allPassions = passions) }
            }
        }
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
        _uiState.update {
            it.copy(
                showEditDialog = true,
                editTitle = goal.title,
                editWhy = goal.whyItMatters,
                editMotivation = goal.motivationType,
                editWantTo = goal.wantToFraming,
                editLinkedPassions = goal.linkedPassionIds.split(",").filter { it.isNotBlank() }.toSet()
            )
        }
    }

    fun dismissEditDialog() = _uiState.update { it.copy(showEditDialog = false) }
    fun onEditTitleChange(value: String) = _uiState.update { it.copy(editTitle = value) }
    fun onEditWhyChange(value: String) = _uiState.update { it.copy(editWhy = value) }
    fun onEditMotivationChange(value: String) = _uiState.update { it.copy(editMotivation = if (it.editMotivation == value) "" else value) }
    fun onEditWantToChange(value: String) = _uiState.update { it.copy(editWantTo = value) }
    fun toggleEditPassion(id: String) = _uiState.update {
        it.copy(editLinkedPassions = if (id in it.editLinkedPassions) it.editLinkedPassions - id else it.editLinkedPassions + id)
    }

    fun saveGoalEdit() {
        viewModelScope.launch {
            val goal = _uiState.value.goal ?: return@launch
            val title = _uiState.value.editTitle.trim().ifBlank { return@launch }
            goalDao.updateGoal(
                goal.copy(
                    title = title,
                    whyItMatters = _uiState.value.editWhy.trim(),
                    motivationType = _uiState.value.editMotivation,
                    wantToFraming = _uiState.value.editWantTo.trim(),
                    linkedPassionIds = _uiState.value.editLinkedPassions.joinToString(","),
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

    fun deleteGoal() {
        viewModelScope.launch {
            goalDao.deleteById(goalId)
            _uiState.update { it.copy(shouldNavigateToList = true) }
        }
    }

    class Factory(
        private val goalDao: GoalDao,
        private val goalContentDao: GoalContentDao,
        private val reminderDao: ReminderDao,
        private val paletteDao: PaletteDao,
        private val goalId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            GoalDetailViewModel(goalDao, goalContentDao, reminderDao, paletteDao, goalId) as T
    }
}
