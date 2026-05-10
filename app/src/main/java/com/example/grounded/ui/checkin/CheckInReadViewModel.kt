package com.example.grounded.ui.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.grounded.data.CheckIn
import com.example.grounded.data.CheckInDao
import com.example.grounded.data.Goal
import com.example.grounded.data.GoalDao
import com.example.grounded.data.QuestionConfig
import com.example.grounded.data.QuestionLabelDao
import com.example.grounded.data.resolveConfigs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CheckInReadUiState(
    val checkIn: CheckIn? = null,
    val goal: Goal? = null,
    val questionConfigs: List<QuestionConfig> = emptyList(),
    val isEditing: Boolean = false,
    val editGoalOrChange: String = "",
    val editAvoiding: String = "",
    val editConfidence: String = "",
    val editMadeProgress: String = "",
    val editCompetingPriority: String = "",
    val editImplementationIntention: String = "",
    val editAccountability: String = "",
    val shouldNavigateBack: Boolean = false,
    val didDelete: Boolean = false,
    val didSaveEdit: Boolean = false
)

class CheckInReadViewModel(
    private val checkInDao: CheckInDao,
    private val goalDao: GoalDao,
    private val questionLabelDao: QuestionLabelDao,
    private val checkInId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckInReadUiState())
    val uiState: StateFlow<CheckInReadUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            questionLabelDao.getAllLabels().first().let { labels ->
                _uiState.update { it.copy(questionConfigs = resolveConfigs(labels)) }
            }
        }
        viewModelScope.launch {
            checkInDao.getCheckInByIdAsFlow(checkInId).collect { checkIn ->
                _uiState.update { it.copy(checkIn = checkIn) }
                if (checkIn != null) {
                    val goal = goalDao.getGoalById(checkIn.goalId)
                    _uiState.update { it.copy(goal = goal) }
                }
            }
        }
    }

    fun startEdit() {
        val c = _uiState.value.checkIn ?: return
        _uiState.update {
            it.copy(
                isEditing = true,
                editGoalOrChange = c.goalOrChange,
                editAvoiding = c.avoiding ?: "",
                editConfidence = c.confidence ?: "",
                editMadeProgress = c.madeProgress ?: "",
                editCompetingPriority = c.competingPriority ?: "",
                editImplementationIntention = c.implementationIntention ?: "",
                editAccountability = c.accountability ?: ""
            )
        }
    }

    fun cancelEdit() = _uiState.update { it.copy(isEditing = false) }

    fun onEditGoalOrChangeChange(v: String) = _uiState.update { it.copy(editGoalOrChange = v) }
    fun onEditAvoidingChange(v: String) = _uiState.update { it.copy(editAvoiding = v) }
    fun onEditConfidenceChange(v: String) = _uiState.update { it.copy(editConfidence = v) }
    fun onEditMadeProgressChange(v: String) = _uiState.update { it.copy(editMadeProgress = v) }
    fun onEditCompetingPriorityChange(v: String) = _uiState.update { it.copy(editCompetingPriority = v) }
    fun onEditImplementationIntentionChange(v: String) = _uiState.update { it.copy(editImplementationIntention = v) }
    fun onEditAccountabilityChange(v: String) = _uiState.update { it.copy(editAccountability = v) }

    fun saveEdit() {
        viewModelScope.launch {
            val checkIn = _uiState.value.checkIn ?: return@launch
            val state = _uiState.value
            checkInDao.insertCheckIn(
                checkIn.copy(
                    goalOrChange = state.editGoalOrChange,
                    avoiding = state.editAvoiding.ifBlank { null },
                    confidence = state.editConfidence.ifBlank { null },
                    madeProgress = state.editMadeProgress.ifBlank { null },
                    competingPriority = state.editCompetingPriority.ifBlank { null },
                    implementationIntention = state.editImplementationIntention.ifBlank { null },
                    accountability = state.editAccountability.ifBlank { null },
                    updatedAt = System.currentTimeMillis()
                )
            )
            _uiState.update { it.copy(isEditing = false, didSaveEdit = true) }
        }
    }

    fun delete() {
        viewModelScope.launch {
            checkInDao.deleteById(checkInId)
            _uiState.update { it.copy(shouldNavigateBack = true, didDelete = true) }
        }
    }

    class Factory(
        private val checkInDao: CheckInDao,
        private val goalDao: GoalDao,
        private val questionLabelDao: QuestionLabelDao,
        private val checkInId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CheckInReadViewModel(checkInDao, goalDao, questionLabelDao, checkInId) as T
    }
}
