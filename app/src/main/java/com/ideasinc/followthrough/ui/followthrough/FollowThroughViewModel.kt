package com.ideasinc.followthrough.ui.followthrough

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ideasinc.followthrough.data.FollowThroughDao
import com.ideasinc.followthrough.data.FollowThroughEntry
import com.ideasinc.followthrough.data.NoteDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class FollowThroughUiState(
    val noteTitle: String = "",
    val body: String = "",
    val isSaved: Boolean = false
)

class FollowThroughViewModel(
    private val noteDao: NoteDao,
    private val followThroughDao: FollowThroughDao,
    private val noteId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(FollowThroughUiState())
    val uiState: StateFlow<FollowThroughUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val note = noteDao.getNoteById(noteId)
            if (note != null) {
                _uiState.update { it.copy(noteTitle = note.title) }
            }
        }
    }

    fun onBodyChange(value: String) = _uiState.update { it.copy(body = value) }

    fun save() {
        viewModelScope.launch {
            val entry = FollowThroughEntry(
                id = UUID.randomUUID().toString(),
                noteId = noteId,
                body = _uiState.value.body.trim(),
                createdAt = System.currentTimeMillis()
            )
            followThroughDao.insert(entry)
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    class Factory(
        private val noteDao: NoteDao,
        private val followThroughDao: FollowThroughDao,
        private val noteId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            FollowThroughViewModel(noteDao, followThroughDao, noteId) as T
    }
}
