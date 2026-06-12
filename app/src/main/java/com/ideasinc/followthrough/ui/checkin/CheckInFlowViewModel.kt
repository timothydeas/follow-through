package com.ideasinc.followthrough.ui.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ideasinc.followthrough.data.CheckIn
import com.ideasinc.followthrough.data.CheckInDao
import com.ideasinc.followthrough.data.GoalDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * The recurring entry, scoped to a goal:
 *  0. Pick a type — barrier or progress (required; no untagged state).
 *  1. Write the note.
 *  2. Set the implementation intention ("when [cue], I will [action]") — optional.
 *  3. Set a reminder + its distinctive cue (emoji/label/image/sound) — optional.
 * Everything is stored ON the check-in (its reminder prefs key off this id). The
 * id is generated up front so the optional reminder/cue authored in step 3 attach
 * to the same check-in saved at the end. On save, the screen lands on Goal Detail.
 */
data class CheckInFlowUiState(
    val goalId: String = "",
    val checkInId: String = "",
    val goalTitle: String = "",
    val step: Int = 0,
    val type: String? = null,
    val note: String = "",
    val intention: String = "",
    val cueEmoji: String? = null,
    val cueLabel: String? = null,
    val cueImagePath: String? = null,
    val cueSound: String? = null,
    val showDiscardDialog: Boolean = false,
    val shouldExit: Boolean = false,
    val savedGoalId: String? = null,
    val showReassurance: Boolean = false
)

internal const val CHECKIN_STEP_COUNT = 4

internal fun CheckInFlowUiState.hasAnswers(): Boolean =
    type != null || note.isNotBlank() || intention.isNotBlank()

/** Whether the current step's requirement is met so the user can advance. */
internal fun CheckInFlowUiState.canProceed(): Boolean = when (step) {
    0 -> type != null
    1 -> note.isNotBlank()
    else -> true // intention and the reminder/cue step are both optional
}

class CheckInFlowViewModel(
    private val checkInDao: CheckInDao,
    private val goalDao: GoalDao,
    private val goalId: String
) : ViewModel() {

    private val checkInId = UUID.randomUUID().toString()

    private val _uiState = MutableStateFlow(
        CheckInFlowUiState(goalId = goalId, checkInId = checkInId)
    )
    val uiState: StateFlow<CheckInFlowUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val goal = goalDao.getGoalById(goalId)
            _uiState.update { it.copy(goalTitle = goal?.title ?: "") }
        }
    }

    fun onSelectType(type: String) = _uiState.update { it.copy(type = type) }
    fun onNoteChange(value: String) = _uiState.update { it.copy(note = value) }
    fun onIntentionChange(value: String) = _uiState.update { it.copy(intention = value) }

    // Cue authoring (step 3). Held here until the check-in is saved; the reminder
    // schedule is written live to prefs by the reminder control, keyed by checkInId.
    fun setCueEmoji(emoji: String?) = _uiState.update { it.copy(cueEmoji = emoji) }
    // Store the phrase as typed (only a fully-blank phrase counts as "unset"). No
    // live trim — trimming mid-keystroke fought the field's cursor.
    fun setCueLabel(label: String) =
        _uiState.update { it.copy(cueLabel = label.ifBlank { null }) }
    fun setCueImagePath(path: String?) = _uiState.update { it.copy(cueImagePath = path) }
    fun setCueSound(sound: String?) = _uiState.update { it.copy(cueSound = sound) }

    /** Advance to the next step; Save once on the last step. */
    fun onNext() {
        val state = _uiState.value
        if (!state.canProceed()) return
        if (state.step < CHECKIN_STEP_COUNT - 1) {
            _uiState.update { it.copy(step = state.step + 1) }
        } else {
            save()
        }
    }

    fun onBack() {
        val state = _uiState.value
        if (state.step == 0) {
            if (state.hasAnswers()) {
                _uiState.update { it.copy(showDiscardDialog = true) }
            } else {
                _uiState.update { it.copy(shouldExit = true) }
            }
        } else {
            _uiState.update { it.copy(step = state.step - 1) }
        }
    }

    fun onSystemBack() {
        if (_uiState.value.hasAnswers()) {
            _uiState.update { it.copy(showDiscardDialog = true) }
        } else {
            _uiState.update { it.copy(shouldExit = true) }
        }
    }

    fun onKeepWriting() = _uiState.update { it.copy(showDiscardDialog = false) }
    fun onDiscard() = _uiState.update { it.copy(showDiscardDialog = false, shouldExit = true) }

    fun onSave() = save()

    private fun save() {
        val state = _uiState.value
        val type = state.type ?: return
        if (state.note.isBlank()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            checkInDao.insertCheckIn(
                CheckIn(
                    id = state.checkInId,
                    goalId = goalId,
                    type = type,
                    note = state.note.trim(),
                    intention = state.intention.trim(),
                    cueEmoji = state.cueEmoji,
                    cueLabel = state.cueLabel,
                    cueImagePath = state.cueImagePath,
                    cueSound = state.cueSound,
                    createdAt = now,
                    updatedAt = now
                )
            )
            _uiState.update { it.copy(showReassurance = true) }
        }
    }

    /** The reassurance overlay was dismissed — publish the save so the screen navigates. */
    fun onReassuranceDone() =
        _uiState.update { it.copy(showReassurance = false, savedGoalId = goalId) }

    class Factory(
        private val checkInDao: CheckInDao,
        private val goalDao: GoalDao,
        private val goalId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CheckInFlowViewModel(checkInDao, goalDao, goalId) as T
    }
}
