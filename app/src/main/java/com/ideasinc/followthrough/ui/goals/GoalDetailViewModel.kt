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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class GoalDetailUiState(
    val goal: Goal? = null,
    val checkIns: List<CheckIn> = emptyList(),
    val questionConfigs: List<QuestionConfig> = emptyList(),
    val shouldNavigateToList: Boolean = false,
    val showEditDialog: Boolean = false,
    val editTitle: String = "",
    val showReassurance: Boolean = false,
    // Intention editor (the "YOUR INTENTION" card / pencil). When [structured]
    // the two "When… / I will…" fields drive the value; otherwise the single
    // [intentionText] field holds a legacy/free-form plan verbatim.
    val showIntentionEditor: Boolean = false,
    val intentionStructured: Boolean = true,
    val intentionWhen: String = "",
    val intentionAction: String = "",
    val intentionText: String = "",
    val intentionTargetCheckInId: String? = null
)

private data class GoalDetailData(
    val goal: Goal?,
    val checkIns: List<CheckIn>,
    val configs: List<QuestionConfig>
)

class GoalDetailViewModel(
    private val goalDao: GoalDao,
    private val checkInDao: CheckInDao,
    private val questionLabelDao: QuestionLabelDao,
    private val goalId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalDetailUiState())
    val uiState: StateFlow<GoalDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                goalDao.getGoalByIdAsFlow(goalId),
                checkInDao.getCheckInsForGoal(goalId),
                questionLabelDao.getAllLabels()
            ) { goal, checkIns, labels ->
                GoalDetailData(goal, checkIns, resolveConfigs(labels))
            }.collect { data ->
                _uiState.update {
                    it.copy(
                        goal = data.goal,
                        checkIns = data.checkIns,
                        questionConfigs = data.configs
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

    /**
     * Opens the intention editor for this goal. The plan lives on a check-in's
     * `implementationIntention` string; we target the most recent check-in that
     * carries one, falling back to the most recent check-in so a goal without a
     * plan yet can still add one. The stored string decides the editor shape:
     * a "When …, I will …" sentence opens the two fields; anything else (legacy
     * or free-form) loads into a single field so nothing is lost.
     */
    fun startEditIntention() {
        val checkIns = _uiState.value.checkIns
        val target = checkIns.firstOrNull { !it.implementationIntention.isNullOrBlank() }
            ?: checkIns.firstOrNull()
        val current = target?.implementationIntention?.trim().orEmpty()
        val structured = current.isBlank() || isStructuredIntention(current)
        val (whenText, actionText) = if (structured) splitIntention(current) else "" to ""
        _uiState.update {
            it.copy(
                showIntentionEditor = true,
                intentionTargetCheckInId = target?.id,
                intentionStructured = structured,
                intentionWhen = whenText,
                intentionAction = actionText,
                intentionText = if (structured) "" else current
            )
        }
    }

    fun dismissIntentionEditor() = _uiState.update { it.copy(showIntentionEditor = false) }

    fun onIntentionWhenChange(value: String) = _uiState.update { it.copy(intentionWhen = value) }

    fun onIntentionActionChange(value: String) = _uiState.update { it.copy(intentionAction = value) }

    fun onIntentionTextChange(value: String) = _uiState.update { it.copy(intentionText = value) }

    fun saveIntention() {
        viewModelScope.launch {
            val state = _uiState.value
            val newValue = if (state.intentionStructured) {
                joinIntention(state.intentionWhen, state.intentionAction)
            } else {
                state.intentionText.trim()
            }
            val stored = newValue.ifBlank { null }
            val now = System.currentTimeMillis()

            val target = state.intentionTargetCheckInId
                ?.let { id -> state.checkIns.firstOrNull { it.id == id } }
            when {
                target != null ->
                    checkInDao.insertCheckIn(
                        target.copy(implementationIntention = stored, updatedAt = now)
                    )
                // No check-in to host the plan yet — create a minimal one so the
                // goal is never left uneditable. (New goals always have one, so
                // this is only a safety net for an empty legacy goal.)
                stored != null -> {
                    val goal = state.goal
                    if (goal != null) {
                        checkInDao.insertCheckIn(
                            CheckIn(
                                id = UUID.randomUUID().toString(),
                                goalId = goalId,
                                goalOrChange = goal.title,
                                implementationIntention = stored,
                                createdAt = now,
                                updatedAt = now
                            )
                        )
                    }
                }
            }
            _uiState.update { it.copy(showIntentionEditor = false) }
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

    class Factory(
        private val goalDao: GoalDao,
        private val checkInDao: CheckInDao,
        private val questionLabelDao: QuestionLabelDao,
        private val goalId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            GoalDetailViewModel(goalDao, checkInDao, questionLabelDao, goalId) as T
    }
}
