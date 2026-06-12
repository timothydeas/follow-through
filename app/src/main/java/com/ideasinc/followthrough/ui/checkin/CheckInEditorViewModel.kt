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

data class CheckInEditorUiState(
    val checkIn: CheckIn? = null,
    val goalTitle: String = "",
    val didDelete: Boolean = false
)

/**
 * Opens a past check-in to view and edit: its note, implementation intention, its
 * distinctive cue, and its reminder (the reminder controls own the prefs directly).
 * Every edit persists immediately to the [CheckIn] row.
 */
class CheckInEditorViewModel(
    private val checkInDao: CheckInDao,
    private val goalDao: GoalDao,
    private val checkInId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckInEditorUiState())
    val uiState: StateFlow<CheckInEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            checkInDao.getCheckInByIdFlow(checkInId).collect { checkIn ->
                val title = checkIn?.let { goalDao.getGoalById(it.goalId)?.title } ?: ""
                _uiState.update { it.copy(checkIn = checkIn, goalTitle = title) }
            }
        }
    }

    fun updateNote(text: String) = persist { it.copy(note = text) }
    fun updateIntention(text: String) = persist { it.copy(intention = text) }
    fun setCueEmoji(emoji: String?) = persist { it.copy(cueEmoji = emoji) }
    // Store the phrase as typed (only a fully-blank phrase counts as "unset"); no
    // live trim — trimming mid-keystroke fought the field's cursor.
    fun setCueLabel(label: String) = persist { it.copy(cueLabel = label.ifBlank { null }) }
    fun setCueImagePath(path: String?) = persist { it.copy(cueImagePath = path) }
    fun setCueSound(sound: String?) = persist { it.copy(cueSound = sound) }

    private fun persist(transform: (CheckIn) -> CheckIn) {
        viewModelScope.launch {
            val checkIn = _uiState.value.checkIn ?: return@launch
            checkInDao.update(transform(checkIn).copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun delete() {
        viewModelScope.launch {
            checkInDao.deleteById(checkInId)
            _uiState.update { it.copy(didDelete = true) }
        }
    }

    class Factory(
        private val checkInDao: CheckInDao,
        private val goalDao: GoalDao,
        private val checkInId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CheckInEditorViewModel(checkInDao, goalDao, checkInId) as T
    }
}
