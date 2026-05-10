package com.example.grounded.ui.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.grounded.data.CheckIn
import com.example.grounded.data.CheckInDao
import com.example.grounded.data.GoalDao
import com.example.grounded.data.QuestionConfig
import com.example.grounded.data.QuestionKeys
import com.example.grounded.data.QuestionLabelDao
import com.example.grounded.data.resolveConfigs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class CheckInFlowUiState(
    val goalId: String = "",
    val goalTitle: String = "",
    val checkInId: String = UUID.randomUUID().toString(),
    val questionConfigs: List<QuestionConfig> = emptyList(),
    // Index into the enabled-step list. After the last enabled step the
    // flow is finished and save() runs.
    val currentStepIndex: Int = 0,
    val goalOrChange: String = "",
    val avoiding: String = "",
    val confidence: String = "",
    val madeProgress: String = "",
    val competingPriority: String = "",
    val implementationIntention: String = "",
    val accountability: String = "",
    val shouldExit: Boolean = false,
    val didSave: Boolean = false
)

/**
 * Indices into [CheckInFlowUiState.questionConfigs] that should appear as
 * steps in this flow. The goalOrChange step is intentionally excluded —
 * a check-in is always added to an existing entry, so the goal/change
 * has already been captured at the entry level.
 */
internal fun CheckInFlowUiState.activeStepIndices(): List<Int> =
    questionConfigs.indices.filter {
        val cfg = questionConfigs[it]
        cfg.isEnabled && cfg.key != QuestionKeys.GOAL_OR_CHANGE
    }

class CheckInFlowViewModel(
    private val checkInDao: CheckInDao,
    private val questionLabelDao: QuestionLabelDao,
    private val goalDao: GoalDao,
    private val goalId: String
) : ViewModel() {

    // Initialize with defaults so UI is ready before DB query completes
    private val _uiState = MutableStateFlow(
        CheckInFlowUiState(goalId = goalId, questionConfigs = resolveConfigs(emptyList()))
    )
    val uiState: StateFlow<CheckInFlowUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val labels = questionLabelDao.getAllLabels().first()
            val configs = resolveConfigs(labels)
            val title = goalDao.getGoalById(goalId)?.title.orEmpty()
            _uiState.update { it.copy(questionConfigs = configs, goalTitle = title) }
        }
    }

    private val enabledStepIndices: List<Int>
        get() = _uiState.value.activeStepIndices()

    fun onGoalOrChangeChange(value: String) = _uiState.update { it.copy(goalOrChange = value) }
    fun onAvoidingChange(value: String) = _uiState.update { it.copy(avoiding = value) }
    fun onConfidenceChange(value: String) = _uiState.update { it.copy(confidence = value) }
    fun onMadeProgressChange(value: String) = _uiState.update { it.copy(madeProgress = value) }
    fun onCompetingPriorityChange(value: String) = _uiState.update { it.copy(competingPriority = value) }
    fun onImplementationIntentionChange(value: String) = _uiState.update { it.copy(implementationIntention = value) }
    fun onAccountabilityChange(value: String) = _uiState.update { it.copy(accountability = value) }

    fun onNext() {
        val state = _uiState.value
        val enabled = enabledStepIndices
        if (state.currentStepIndex >= enabled.size) return
        val isLast = state.currentStepIndex == enabled.size - 1
        if (isLast) {
            save()
        } else {
            _uiState.update { it.copy(currentStepIndex = state.currentStepIndex + 1) }
        }
    }

    fun onBack() {
        val state = _uiState.value
        if (state.currentStepIndex == 0) {
            _uiState.update { it.copy(shouldExit = true) }
        } else {
            _uiState.update { it.copy(currentStepIndex = state.currentStepIndex - 1) }
        }
    }

    fun onSave() = save()

    private fun save() {
        viewModelScope.launch {
            val state = _uiState.value
            val now = System.currentTimeMillis()
            val checkIn = CheckIn(
                id = state.checkInId,
                goalId = state.goalId,
                goalOrChange = state.goalOrChange,
                avoiding = state.avoiding.ifBlank { null },
                confidence = state.confidence.ifBlank { null },
                madeProgress = state.madeProgress.ifBlank { null },
                competingPriority = state.competingPriority.ifBlank { null },
                implementationIntention = state.implementationIntention.ifBlank { null },
                accountability = state.accountability.ifBlank { null },
                createdAt = now,
                updatedAt = now
            )
            checkInDao.insertCheckIn(checkIn)
            _uiState.update { it.copy(shouldExit = true, didSave = true) }
        }
    }

    class Factory(
        private val checkInDao: CheckInDao,
        private val questionLabelDao: QuestionLabelDao,
        private val goalDao: GoalDao,
        private val goalId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CheckInFlowViewModel(checkInDao, questionLabelDao, goalDao, goalId) as T
    }
}

