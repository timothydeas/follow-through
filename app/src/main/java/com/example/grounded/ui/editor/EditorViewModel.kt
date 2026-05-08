package com.example.grounded.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.grounded.data.GroundedNote
import com.example.grounded.data.NoteDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class EditorUiState(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val body: String = "",
    val tag: String = "",
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val isNewNote: Boolean = true,
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false
)

class EditorViewModel(
    private val noteDao: NoteDao,
    private val noteId: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    init {
        if (noteId != null) {
            viewModelScope.launch {
                val note = noteDao.getNoteById(noteId)
                if (note != null) {
                    _uiState.update {
                        EditorUiState(
                            id = note.id,
                            title = note.title,
                            body = note.body,
                            tag = note.tag ?: "",
                            isPinned = note.isPinned,
                            createdAt = note.createdAt,
                            isNewNote = false
                        )
                    }
                }
            }
        }
    }

    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value) }
    fun onBodyChange(value: String) = _uiState.update { it.copy(body = value) }
    fun onTagChange(value: String) = _uiState.update { it.copy(tag = value) }
    fun onPinToggle() = _uiState.update { it.copy(isPinned = !it.isPinned) }

    fun saveDraft() {
        viewModelScope.launch { persistNote(isDraft = true) }
    }

    fun save() {
        viewModelScope.launch {
            persistNote(isDraft = false)
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    private suspend fun persistNote(isDraft: Boolean) {
        val state = _uiState.value
        val note = GroundedNote(
            id = state.id,
            title = state.title,
            body = state.body,
            tag = state.tag.ifBlank { null },
            isPinned = state.isPinned,
            createdAt = state.createdAt,
            updatedAt = System.currentTimeMillis(),
            isDraft = isDraft
        )
        noteDao.insertNote(note)
    }

    fun deleteNote() {
        val id = _uiState.value.id
        viewModelScope.launch {
            noteDao.deleteById(id)
            _uiState.update { it.copy(isDeleted = true) }
        }
    }

    class Factory(
        private val noteDao: NoteDao,
        private val noteId: String?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            EditorViewModel(noteDao, noteId) as T
    }
}
