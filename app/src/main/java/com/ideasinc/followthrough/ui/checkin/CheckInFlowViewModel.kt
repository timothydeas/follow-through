package com.ideasinc.followthrough.ui.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ideasinc.followthrough.data.CheckIn
import com.ideasinc.followthrough.data.CheckInDao
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

data class CheckInFlowUiState(
    val goalId: String = "",
    val checkInId: String = UUID.randomUUID().toString(),
    val questionConfigs: List<QuestionConfig> = emptyList(),
    // Read-only context: the goal and the user's own implementation intention
    // (carried from goal creation / prior check-ins). Never re-asked or stored as
    // this check-in's answer.
    val goalTitle: String = "",
    val goalIntention: String? = null,
    // Index into the current path (see pathStepKeys). The path is the lead step
    // only until the user taps "Reflect more", after which the deeper steps join.
    val currentStepIndex: Int = 0,
    val reflectMore: Boolean = false,
    val goalOrChange: String = "",
    val avoiding: String = "",
    val confidence: String = "",
    val madeProgress: String = "",
    val competingPriority: String = "",
    val implementationIntention: String = "",
    val accountability: String = "",
    val showDiscardDialog: Boolean = false,
    val shouldExit: Boolean = false,
    val didSave: Boolean = false
)

/** True once the user has typed any answer — used to gate the discard prompt. */
internal fun CheckInFlowUiState.hasAnswers(): Boolean =
    goalOrChange.isNotBlank() || avoiding.isNotBlank() || confidence.isNotBlank() ||
        madeProgress.isNotBlank() || competingPriority.isNotBlank() ||
        implementationIntention.isNotBlank() || accountability.isNotBlank()

// The deeper, optional prompts offered behind "Reflect more", in order. The
// goalOrChange question is never part of a check-in (the goal already exists).
internal val CHECKIN_DEEPER_ORDER = listOf(
    QuestionKeys.AVOIDING,
    QuestionKeys.CONFIDENCE,
    QuestionKeys.COMPETING_PRIORITY,
    QuestionKeys.IMPLEMENTATION_INTENTION,
    QuestionKeys.ACCOUNTABILITY
)

private fun CheckInFlowUiState.isEnabled(key: String): Boolean =
    questionConfigs.any { it.key == key && it.isEnabled }

/**
 * The single lead prompt: progress is the natural lead, falling back to the first
 * enabled deeper prompt if the user disabled progress in Customize Questions.
 */
internal fun CheckInFlowUiState.leadKey(): String? =
    if (isEnabled(QuestionKeys.MADE_PROGRESS)) QuestionKeys.MADE_PROGRESS
    else CHECKIN_DEEPER_ORDER.firstOrNull { isEnabled(it) }

/** The lead step, always shown (when any question is enabled at all). */
internal fun CheckInFlowUiState.coreStepKeys(): List<String> = listOfNotNull(leadKey())

/** Deeper steps the user can opt into — enabled prompts other than the lead. */
internal fun CheckInFlowUiState.deeperStepKeys(): List<String> {
    val lead = leadKey()
    return CHECKIN_DEEPER_ORDER.filter { it != lead && isEnabled(it) }
}

/**
 * The steps actually on the user's path right now — the lead by default,
 * expanding to include the deeper steps once "Reflect more" is tapped. Drives the
 * progress count so it reflects the path the user is really on.
 */
internal fun CheckInFlowUiState.pathStepKeys(): List<String> =
    coreStepKeys() + if (reflectMore) deeperStepKeys() else emptyList()

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
            val goal = goalDao.getGoalById(goalId)
            // Goal-level implementation intention = the most recent non-blank
            // intention across this goal's check-ins (the plan the user set).
            val intention = checkInDao.getCheckInsForGoal(goalId).first()
                .sortedByDescending { it.createdAt }
                .firstNotNullOfOrNull { it.implementationIntention?.takeIf { s -> s.isNotBlank() } }
            _uiState.update {
                it.copy(
                    questionConfigs = resolveConfigs(labels),
                    goalTitle = goal?.title ?: "",
                    goalIntention = intention
                )
            }
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

    fun onSave() = save()

    private fun save() {
        viewModelScope.launch {
            val state = _uiState.value
            val now = System.currentTimeMillis()
            // The plan editor is pre-filled with the goal's current plan. Only
            // record an intention on this check-in when the user actually changed
            // it — an untouched (or reverted) plan stays null so the existing
            // latest-intention carries forward unchanged.
            val editedPlan = state.implementationIntention.trim()
            val currentPlan = state.goalIntention?.trim().orEmpty()
            val intentionToStore =
                if (editedPlan.isNotBlank() && editedPlan != currentPlan) editedPlan else null
            val checkIn = CheckIn(
                id = state.checkInId,
                goalId = state.goalId,
                goalOrChange = state.goalOrChange,
                avoiding = state.avoiding.ifBlank { null },
                confidence = state.confidence.ifBlank { null },
                madeProgress = state.madeProgress.ifBlank { null },
                competingPriority = state.competingPriority.ifBlank { null },
                implementationIntention = intentionToStore,
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
