package com.ideasinc.followthrough.ui.aboutyou

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ideasinc.followthrough.data.Learning
import com.ideasinc.followthrough.data.PaletteDao
import com.ideasinc.followthrough.data.PassionInterest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Drives the About You palette: person-level passions/interests and learnings.
 * Lightweight capture, never interrogation — entries exist only to make cues more
 * distinctive in the builder.
 */
class AboutYouViewModel(private val paletteDao: PaletteDao) : ViewModel() {

    val passions: StateFlow<List<PassionInterest>> =
        paletteDao.getPassions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val learnings: StateFlow<List<Learning>> =
        paletteDao.getLearnings().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addPassion(emoji: String, label: String) {
        val cleanLabel = label.trim()
        if (cleanLabel.isEmpty()) return
        val cleanEmoji = emoji.trim().ifEmpty { "✨" }
        viewModelScope.launch {
            paletteDao.upsertPassion(
                PassionInterest(
                    id = UUID.randomUUID().toString(),
                    label = cleanLabel,
                    emoji = cleanEmoji,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun updatePassion(existing: PassionInterest, emoji: String, label: String) {
        val cleanLabel = label.trim()
        if (cleanLabel.isEmpty()) return
        viewModelScope.launch {
            paletteDao.updatePassion(
                existing.copy(label = cleanLabel, emoji = emoji.trim().ifEmpty { "✨" })
            )
        }
    }

    fun deletePassion(id: String) {
        viewModelScope.launch { paletteDao.deletePassion(id) }
    }

    fun addLearning(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            paletteDao.upsertLearning(
                Learning(
                    id = UUID.randomUUID().toString(),
                    text = clean,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun updateLearning(existing: Learning, text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch { paletteDao.updateLearning(existing.copy(text = clean)) }
    }

    fun deleteLearning(id: String) {
        viewModelScope.launch { paletteDao.deleteLearning(id) }
    }

    class Factory(private val paletteDao: PaletteDao) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AboutYouViewModel(paletteDao) as T
    }
}
