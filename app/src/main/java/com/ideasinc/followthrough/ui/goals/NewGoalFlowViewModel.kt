package com.ideasinc.followthrough.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ideasinc.followthrough.data.CheckIn
import com.ideasinc.followthrough.data.CheckInDao
import com.ideasinc.followthrough.data.Goal
import com.ideasinc.followthrough.data.GoalDao
import com.ideasinc.followthrough.data.QuestionConfig
import com.ideasinc.followthrough.data.QuestionLabelDao
import com.ideasinc.followthrough.data.resolveConfigs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

sealed class NewGoalPhase {
    data class CheckIn(val stepIndex: Int) : NewGoalPhase()
}

data class NewGoalFlowUiState(
    val phase: NewGoalPhase = NewGoalPhase.CheckIn(0),
    val goalOrChange: String = "",
    val avoiding: String = "",
    val confidence: String = "",
    val madeProgress: String = "",
    val competingPriority: String = "",
    val implementationIntention: String = "",
    val accountability: String = "",
    val questionConfigs: List<QuestionConfig> = emptyList(),
    val showDiscardDialog: Boolean = false,
    val shouldExit: Boolean = false,
    val savedGoalId: String? = null
)

/** True once the user has typed any answer — used to gate the discard prompt. */
internal fun NewGoalFlowUiState.hasAnswers(): Boolean =
    goalOrChange.isNotBlank() || avoiding.isNotBlank() || confidence.isNotBlank() ||
        madeProgress.isNotBlank() || competingPriority.isNotBlank() ||
        implementationIntention.isNotBlank() || accountability.isNotBlank()

class NewGoalFlowViewModel(
    private val goalDao: GoalDao,
    private val checkInDao: CheckInDao,
    private val questionLabelDao: QuestionLabelDao
) : ViewModel() {

    private val goalId = UUID.randomUUID().toString()
    // Exposed so the optional in-flow reminder can be attached to the same id the
    // goal will be saved under. If the flow is discarded the screen removes it.
    val newGoalId: String get() = goalId
    // Initialize with defaults so the UI is ready before DB query completes
    private val _uiState = MutableStateFlow(NewGoalFlowUiState(questionConfigs = resolveConfigs(emptyList())))
    val uiState: StateFlow<NewGoalFlowUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val labels = questionLabelDao.getAllLabels().first()
            val configs = resolveConfigs(labels)
            _uiState.update { it.copy(questionConfigs = configs) }
        }
    }

    private val enabledStepIndices: List<Int>
        get() = _uiState.value.questionConfigs.indices
            .filter { _uiState.value.questionConfigs[it].isEnabled }

    fun onGoalOrChangeChange(value: String) = _uiState.update { it.copy(goalOrChange = value) }
    fun onAvoidingChange(value: String) = _uiState.update { it.copy(avoiding = value) }
    fun onConfidenceChange(value: String) = _uiState.update { it.copy(confidence = value) }
    fun onMadeProgressChange(value: String) = _uiState.update { it.copy(madeProgress = value) }
    fun onCompetingPriorityChange(value: String) = _uiState.update { it.copy(competingPriority = value) }
    fun onImplementationIntentionChange(value: String) = _uiState.update { it.copy(implementationIntention = value) }
    fun onAccountabilityChange(value: String) = _uiState.update { it.copy(accountability = value) }

    fun onNextCheckInStep() {
        val state = _uiState.value
        val phase = state.phase as? NewGoalPhase.CheckIn ?: return
        // The first step now plays the role the title used to play —
        // it becomes the goal's name, so block advancing past it blank.
        if (phase.stepIndex == 0 && state.goalOrChange.trim().isBlank()) return
        val enabled = enabledStepIndices
        val nextIndex = phase.stepIndex + 1
        if (nextIndex < enabled.size) {
            _uiState.update { it.copy(phase = NewGoalPhase.CheckIn(nextIndex)) }
        } else {
            save()
        }
    }

    fun onBack() {
        val state = _uiState.value
        when (val phase = state.phase) {
            is NewGoalPhase.CheckIn -> {
                if (phase.stepIndex == 0) {
                    // Backing out of the flow — confirm only if answers would be lost.
                    if (state.hasAnswers()) {
                        _uiState.update { it.copy(showDiscardDialog = true) }
                    } else {
                        _uiState.update { it.copy(shouldExit = true) }
                    }
                } else {
                    _uiState.update { it.copy(phase = NewGoalPhase.CheckIn(phase.stepIndex - 1)) }
                }
            }
        }
    }

    fun onKeepWriting() = _uiState.update { it.copy(showDiscardDialog = false) }

    fun onDiscard() = _uiState.update { it.copy(showDiscardDialog = false, shouldExit = true) }

    /** Android system back gesture — confirm before discarding typed answers. */
    fun onSystemBack() {
        if (_uiState.value.hasAnswers()) {
            _uiState.update { it.copy(showDiscardDialog = true) }
        }
    }

    fun onSave() {
        if (_uiState.value.goalOrChange.trim().isBlank()) return
        save()
    }

    private fun save() {
        viewModelScope.launch {
            val state = _uiState.value
            val now = System.currentTimeMillis()
            val title = state.goalOrChange.trim()
            // priority = null and createdAt = now → goal lands at the end of
            // the unprioritized list (sorted by createdAt ascending).
            val goal = Goal(
                id = goalId,
                title = title,
                createdAt = now,
                updatedAt = now,
                priority = null
            )
            goalDao.insertGoal(goal)

            val checkIn = CheckIn(
                id = UUID.randomUUID().toString(),
                goalId = goalId,
                goalOrChange = title,
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
            _uiState.update { it.copy(savedGoalId = goalId) }
        }
    }

    class Factory(
        private val goalDao: GoalDao,
        private val checkInDao: CheckInDao,
        private val questionLabelDao: QuestionLabelDao
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            NewGoalFlowViewModel(goalDao, checkInDao, questionLabelDao) as T
    }
}
