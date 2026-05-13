package com.ideasinc.followthrough.ui.readview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ideasinc.followthrough.data.GroundedNote
import com.ideasinc.followthrough.data.NoteDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReadViewUiState(
    val note: GroundedNote? = null,
    val isDeleted: Boolean = false,
    val showReassurance: Boolean = false,
    val shouldExitToList: Boolean = false
)

class ReadViewViewModel(
    private val noteDao: NoteDao,
    private val noteId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReadViewUiState())
    val uiState: StateFlow<ReadViewUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            noteDao.getNoteByIdAsFlow(noteId).collect { note ->
                _uiState.update { it.copy(note = note) }
            }
        }
    }

    fun togglePin() {
        viewModelScope.launch {
            val note = _uiState.value.note ?: return@launch
            noteDao.insertNote(note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteNote() {
        viewModelScope.launch {
            noteDao.deleteById(noteId)
            _uiState.update { it.copy(isDeleted = true) }
        }
    }

    fun followThrough() {
        viewModelScope.launch {
            val note = _uiState.value.note ?: return@launch
            if (note.followedThrough) return@launch
            noteDao.insertNote(
                note.copy(
                    followedThrough = true,
                    followedThroughAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
            _uiState.update { it.copy(showReassurance = true) }
        }
    }

    fun onReassuranceDone() {
        _uiState.update { it.copy(showReassurance = false, shouldExitToList = true) }
    }

    class Factory(
        private val noteDao: NoteDao,
        private val noteId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ReadViewViewModel(noteDao, noteId) as T
    }
}
