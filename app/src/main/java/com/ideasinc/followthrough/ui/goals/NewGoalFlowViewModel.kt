package com.ideasinc.followthrough.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ideasinc.followthrough.data.CheckIn
import com.ideasinc.followthrough.data.CheckInDao
import com.ideasinc.followthrough.data.Goal
import com.ideasinc.followthrough.data.GoalDao
import com.ideasinc.followthrough.data.QuestionConfig
import com.ideasinc.followthrough.data.QuestionKeys
import com.ideasinc.followthrough.data.QuestionLabelDao
import com.ideasinc.followthrough.data.resolveConfigs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class NewGoalFlowUiState(
    // Index into the current path (see pathStepKeys). The path is core-only until
    // the user taps "Reflect more", after which the deeper steps join the path.
    val currentStepIndex: Int = 0,
    val reflectMore: Boolean = false,
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

// The deeper, optional reflection prompts offered behind "Reflect more", in the
// order they appear. Deliberately excludes madeProgress (a brand-new goal has no
// progress to report) and the two core steps (goal name, action plan).
internal val NEW_GOAL_DEEPER_ORDER = listOf(
    QuestionKeys.AVOIDING,
    QuestionKeys.CONFIDENCE,
    QuestionKeys.COMPETING_PRIORITY,
    QuestionKeys.ACCOUNTABILITY
)

/**
 * The core path, always shown: name the goal, then plan the first move. Both are
 * structural to creation, so they appear even if disabled as reflection prompts.
 */
internal fun NewGoalFlowUiState.coreStepKeys(): List<String> =
    listOf(QuestionKeys.GOAL_OR_CHANGE, QuestionKeys.IMPLEMENTATION_INTENTION)

/** Deeper steps the user can opt into, filtered to those still enabled. */
internal fun NewGoalFlowUiState.deeperStepKeys(): List<String> =
    NEW_GOAL_DEEPER_ORDER.filter { key ->
        questionConfigs.any { it.key == key && it.isEnabled }
    }

/**
 * The steps actually on the user's path right now — core by default, expanding to
 * include the deeper steps once "Reflect more" is tapped. Drives the progress
 * count so it reflects the path the user is really on.
 */
internal fun NewGoalFlowUiState.pathStepKeys(): List<String> =
    coreStepKeys() + if (reflectMore) deeperStepKeys() else emptyList()

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

    fun onGoalOrChangeChange(value: String) = _uiState.update { it.copy(goalOrChange = value) }
    fun onAvoidingChange(value: String) = _uiState.update { it.copy(avoiding = value) }
    fun onConfidenceChange(value: String) = _uiState.update { it.copy(confidence = value) }
    fun onMadeProgressChange(value: String) = _uiState.update { it.copy(madeProgress = value) }
    fun onCompetingPriorityChange(value: String) = _uiState.update { it.copy(competingPriority = value) }
    fun onImplementationIntentionChange(value: String) = _uiState.update { it.copy(implementationIntention = value) }
    fun onAccountabilityChange(value: String) = _uiState.update { it.copy(accountability = value) }

    /** Advance to the next step on the path; Save once there are none left. */
    fun onNext() {
        val state = _uiState.value
        val path = state.pathStepKeys()
        // The goal name is the one hard requirement — never advance past it blank.
        if (path.getOrNull(state.currentStepIndex) == QuestionKeys.GOAL_OR_CHANGE &&
            state.goalOrChange.trim().isBlank()
        ) return
        if (state.currentStepIndex < path.size - 1) {
            _uiState.update { it.copy(currentStepIndex = state.currentStepIndex + 1) }
        } else {
            save()
        }
    }

    /** Opt into the deeper prompts: extend the path and jump to its first step. */
    fun onReflectMore() {
        val state = _uiState.value
        if (state.deeperStepKeys().isEmpty()) return
        _uiState.update {
            it.copy(reflectMore = true, currentStepIndex = it.coreStepKeys().size)
        }
    }

    fun onBack() {
        val state = _uiState.value
        if (state.currentStepIndex == 0) {
            // Backing out of the flow — confirm only if answers would be lost.
            if (state.hasAnswers()) {
                _uiState.update { it.copy(showDiscardDialog = true) }
            } else {
                _uiState.update { it.copy(shouldExit = true) }
            }
        } else {
            _uiState.update { it.copy(currentStepIndex = state.currentStepIndex - 1) }
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
                // madeProgress is never asked during goal creation — a new goal
                // has no progress yet, so it is always stored blank here.
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
