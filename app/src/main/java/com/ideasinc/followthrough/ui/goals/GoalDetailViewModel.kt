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
import com.ideasinc.followthrough.data.Step
import com.ideasinc.followthrough.data.StepDao
import com.ideasinc.followthrough.data.resolveConfigs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class GoalDetailUiState(
    val goal: Goal? = null,
    val checkIns: List<CheckIn> = emptyList(),
    val questionConfigs: List<QuestionConfig> = emptyList(),
    val steps: List<Step> = emptyList(),
    val shouldNavigateToList: Boolean = false,
    val showEditDialog: Boolean = false,
    val editTitle: String = "",
    val showReassurance: Boolean = false,
    // Step add/edit dialog. stepEditorTargetId == null → adding a new step;
    // non-null → editing the step with that id.
    val showStepDialog: Boolean = false,
    val stepDialogText: String = "",
    val stepEditorTargetId: String? = null
)

private data class GoalDetailData(
    val goal: Goal?,
    val checkIns: List<CheckIn>,
    val configs: List<QuestionConfig>,
    val steps: List<Step>
)

class GoalDetailViewModel(
    private val goalDao: GoalDao,
    private val checkInDao: CheckInDao,
    private val questionLabelDao: QuestionLabelDao,
    private val stepDao: StepDao,
    private val goalId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalDetailUiState())
    val uiState: StateFlow<GoalDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                goalDao.getGoalByIdAsFlow(goalId),
                checkInDao.getCheckInsForGoal(goalId),
                questionLabelDao.getAllLabels(),
                stepDao.getStepsForGoal(goalId)
            ) { goal, checkIns, labels, steps ->
                GoalDetailData(goal, checkIns, resolveConfigs(labels), steps)
            }.collect { data ->
                _uiState.update {
                    it.copy(
                        goal = data.goal,
                        checkIns = data.checkIns,
                        questionConfigs = data.configs,
                        steps = data.steps
                    )
                }
            }
        }
    }

    fun showEditDialog() {
        val goal = _uiState.value.goal ?: return
        _uiState.update {
            it.copy(showEditDialog = true, editTitle = goal.title)
        }
    }

    fun dismissEditDialog() = _uiState.update { it.copy(showEditDialog = false) }

    fun onEditTitleChange(value: String) = _uiState.update { it.copy(editTitle = value) }

    fun saveGoalEdit() {
        viewModelScope.launch {
            val goal = _uiState.value.goal ?: return@launch
            val title = _uiState.value.editTitle.trim().ifBlank { return@launch }
            // Preserve the existing accountableTo on the entity; the edit dialog
            // no longer surfaces it.
            goalDao.updateGoal(
                goal.copy(
                    title = title,
                    updatedAt = System.currentTimeMillis()
                )
            )
            _uiState.update { it.copy(showEditDialog = false) }
        }
    }

    fun followThrough() {
        viewModelScope.launch {
            val goal = _uiState.value.goal ?: return@launch
            if (goal.followedThrough) return@launch
            val now = System.currentTimeMillis()
            goalDao.updateGoal(
                goal.copy(
                    followedThrough = true,
                    followedThroughAt = now,
                    updatedAt = now
                )
            )
            _uiState.update { it.copy(showReassurance = true) }
        }
    }

    /** Reverts a followed-through goal back to the active state. */
    fun undoFollowThrough() {
        viewModelScope.launch {
            val goal = _uiState.value.goal ?: return@launch
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

    fun onReassuranceDone() = _uiState.update { it.copy(showReassurance = false) }

    fun deleteGoal() {
        viewModelScope.launch {
            goalDao.deleteById(goalId)
            _uiState.update { it.copy(shouldNavigateToList = true) }
        }
    }

    // ─── Steps (sub-goals) ───────────────────────────────────────────────

    fun showAddStepDialog() = _uiState.update {
        it.copy(showStepDialog = true, stepDialogText = "", stepEditorTargetId = null)
    }

    fun showEditStepDialog(step: Step) = _uiState.update {
        it.copy(showStepDialog = true, stepDialogText = step.title, stepEditorTargetId = step.id)
    }

    fun onStepDialogTextChange(value: String) =
        _uiState.update { it.copy(stepDialogText = value) }

    fun dismissStepDialog() = _uiState.update {
        it.copy(showStepDialog = false, stepDialogText = "", stepEditorTargetId = null)
    }

    fun saveStep() {
        viewModelScope.launch {
            val state = _uiState.value
            val title = state.stepDialogText.trim()
            if (title.isBlank()) return@launch
            val now = System.currentTimeMillis()
            val targetId = state.stepEditorTargetId
            if (targetId == null) {
                stepDao.insertStep(
                    Step(
                        id = UUID.randomUUID().toString(),
                        goalId = goalId,
                        title = title,
                        isCompleted = false,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            } else {
                val existing = state.steps.firstOrNull { it.id == targetId } ?: return@launch
                stepDao.updateStep(existing.copy(title = title, updatedAt = now))
            }
            _uiState.update {
                it.copy(showStepDialog = false, stepDialogText = "", stepEditorTargetId = null)
            }
        }
    }

    fun toggleStep(step: Step) {
        viewModelScope.launch {
            stepDao.updateStep(
                step.copy(
                    isCompleted = !step.isCompleted,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteStep(stepId: String) {
        viewModelScope.launch { stepDao.deleteById(stepId) }
    }

    class Factory(
        private val goalDao: GoalDao,
        private val checkInDao: CheckInDao,
        private val questionLabelDao: QuestionLabelDao,
        private val stepDao: StepDao,
        private val goalId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            GoalDetailViewModel(goalDao, checkInDao, questionLabelDao, stepDao, goalId) as T
    }
}
