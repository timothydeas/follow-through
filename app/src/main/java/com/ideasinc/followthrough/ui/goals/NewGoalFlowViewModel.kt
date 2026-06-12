package com.ideasinc.followthrough.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ideasinc.followthrough.data.Goal
import com.ideasinc.followthrough.data.GoalDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Creating a goal is now just naming it. On save the goal is persisted and the
 * screen flows straight into the goal's first check-in (where the type, note, and
 * implementation intention are captured).
 */
data class NewGoalFlowUiState(
    val goalName: String = "",
    val showDiscardDialog: Boolean = false,
    val shouldExit: Boolean = false,
    val savedGoalId: String? = null
)

class NewGoalFlowViewModel(
    private val goalDao: GoalDao
) : ViewModel() {

    private val goalId = UUID.randomUUID().toString()
    private val _uiState = MutableStateFlow(NewGoalFlowUiState())
    val uiState: StateFlow<NewGoalFlowUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) = _uiState.update { it.copy(goalName = value) }

    fun onBack() {
        if (_uiState.value.goalName.isNotBlank()) {
            _uiState.update { it.copy(showDiscardDialog = true) }
        } else {
            _uiState.update { it.copy(shouldExit = true) }
        }
    }

    fun onSystemBack() {
        if (_uiState.value.goalName.isNotBlank()) {
            _uiState.update { it.copy(showDiscardDialog = true) }
        } else {
            _uiState.update { it.copy(shouldExit = true) }
        }
    }

    fun onKeepWriting() = _uiState.update { it.copy(showDiscardDialog = false) }
    fun onDiscard() = _uiState.update { it.copy(showDiscardDialog = false, shouldExit = true) }

    fun onSave() {
        val title = _uiState.value.goalName.trim()
        if (title.isBlank()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            goalDao.insertGoal(
                Goal(
                    id = goalId,
                    title = title,
                    createdAt = now,
                    updatedAt = now,
                    priority = null
                )
            )
            _uiState.update { it.copy(savedGoalId = goalId) }
        }
    }

    class Factory(
        private val goalDao: GoalDao
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            NewGoalFlowViewModel(goalDao) as T
    }
}
