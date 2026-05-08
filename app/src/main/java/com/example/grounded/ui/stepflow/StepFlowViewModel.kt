package com.example.grounded.ui.stepflow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.grounded.data.GroundedNote
import com.example.grounded.data.NoteDao
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class StepFlowUiState(
    val id: String = UUID.randomUUID().toString(),
    val currentStep: Int = 1,
    val reflection: String = "",
    val whatStoppedYou: String = "",
    val whatYouLearned: String = "",
    val nextSteps: String = "",
    val implementationIntention: String = "",
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val isDraftPersisted: Boolean = false,
    val showReassurance: Boolean = false,
    val shouldExit: Boolean = false
)

class StepFlowViewModel(
    private val noteDao: NoteDao,
    private val noteId: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(StepFlowUiState())
    val uiState: StateFlow<StepFlowUiState> = _uiState.asStateFlow()

    private var autoSaveJob: Job? = null

    init {
        if (noteId != null) {
            viewModelScope.launch {
                val note = noteDao.getNoteById(noteId)
                if (note != null) {
                    val intention = when {
                        note.implementationIntention.isNotBlank() -> note.implementationIntention
                        note.whenField.isNotBlank() || note.willField.isNotBlank() -> buildString {
                            if (note.whenField.isNotBlank()) append("When ${note.whenField}")
                            if (note.whenField.isNotBlank() && note.willField.isNotBlank()) append(", ")
                            if (note.willField.isNotBlank()) append("I will ${note.willField}")
                        }
                        else -> ""
                    }
                    _uiState.update {
                        StepFlowUiState(
                            id = note.id,
                            currentStep = 1,
                            reflection = note.reflection,
                            whatStoppedYou = note.whatStoppedYou,
                            whatYouLearned = note.whatYouLearned,
                            nextSteps = note.nextSteps,
                            implementationIntention = intention,
                            isPinned = note.isPinned,
                            createdAt = note.createdAt,
                            isDraftPersisted = true
                        )
                    }
                }
            }
        }
    }

    fun onReflectionChange(value: String) {
        _uiState.update { it.copy(reflection = value) }
        scheduleAutoSave()
    }

    fun onWhatStoppedYouChange(value: String) {
        _uiState.update { it.copy(whatStoppedYou = value) }
        scheduleAutoSave()
    }

    fun onWhatYouLearnedChange(value: String) {
        _uiState.update { it.copy(whatYouLearned = value) }
        scheduleAutoSave()
    }

    fun onNextStepsChange(value: String) {
        _uiState.update { it.copy(nextSteps = value) }
        scheduleAutoSave()
    }

    fun onImplementationIntentionChange(value: String) {
        _uiState.update { it.copy(implementationIntention = value) }
        scheduleAutoSave()
    }

    fun onNext() {
        val state = _uiState.value
        if (state.currentStep == 1 && !state.isDraftPersisted) {
            viewModelScope.launch {
                autoSaveJob?.cancel()
                persistNote(isDraft = true)
                _uiState.update { it.copy(isDraftPersisted = true, currentStep = 2) }
            }
        } else if (state.currentStep <= 4) {
            autoSaveJob?.cancel()
            viewModelScope.launch {
                if (state.isDraftPersisted) persistNote(isDraft = true)
                _uiState.update { it.copy(currentStep = it.currentStep + 1) }
            }
        }
    }

    fun onBack() {
        val state = _uiState.value
        if (state.currentStep == 1) {
            autoSaveJob?.cancel()
            _uiState.update { it.copy(shouldExit = true) }
        } else {
            _uiState.update { it.copy(currentStep = it.currentStep - 1) }
        }
    }

    fun onSave() {
        autoSaveJob?.cancel()
        viewModelScope.launch {
            persistNote(isDraft = false)
            _uiState.update { it.copy(shouldExit = true) }
        }
    }

    fun onReassuranceDone() {
        _uiState.update { it.copy(showReassurance = false, shouldExit = true) }
    }

    private fun scheduleAutoSave() {
        if (!_uiState.value.isDraftPersisted) return
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(300)
            persistNote(isDraft = true)
        }
    }

    private suspend fun persistNote(isDraft: Boolean) {
        val state = _uiState.value
        val existing = noteDao.getNoteById(state.id)
        val note = GroundedNote(
            id = state.id,
            title = state.reflection.lines().firstOrNull()?.trim() ?: "",
            body = state.reflection,
            tag = existing?.tag,
            isPinned = state.isPinned,
            createdAt = state.createdAt,
            updatedAt = System.currentTimeMillis(),
            type = existing?.type ?: com.example.grounded.data.NoteType.REFLECTION,
            isDraft = isDraft,
            reflection = state.reflection,
            whatStoppedYou = state.whatStoppedYou,
            whatYouLearned = state.whatYouLearned,
            nextSteps = state.nextSteps,
            whenField = existing?.whenField ?: "",
            willField = existing?.willField ?: "",
            followedThrough = existing?.followedThrough ?: false,
            followedThroughAt = existing?.followedThroughAt,
            implementationIntention = state.implementationIntention
        )
        noteDao.insertNote(note)
    }

    class Factory(
        private val noteDao: NoteDao,
        private val noteId: String?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            StepFlowViewModel(noteDao, noteId) as T
    }
}
