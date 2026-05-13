package com.ideasinc.followthrough.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ideasinc.followthrough.data.QuestionConfig
import com.ideasinc.followthrough.data.QuestionKeys
import com.ideasinc.followthrough.data.QuestionLabel
import com.ideasinc.followthrough.data.QuestionLabelDao
import com.ideasinc.followthrough.data.resolveConfigs
import com.ideasinc.followthrough.notifications.KEY_REMINDERS_ENABLED
import com.ideasinc.followthrough.notifications.KEY_REMINDER_DAYS
import com.ideasinc.followthrough.notifications.KEY_REMINDER_HOUR
import com.ideasinc.followthrough.notifications.KEY_REMINDER_MINUTE
import com.ideasinc.followthrough.notifications.PREFS_REMINDERS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class SettingsUiState(
    val questionConfigs: List<QuestionConfig> = emptyList(),
    val editingKey: String? = null,
    val editingLabel: String = "",
    val remindersEnabled: Boolean = false,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val reminderDays: Set<Int> = emptySet()
)

class SettingsViewModel(
    private val questionLabelDao: QuestionLabelDao,
    context: Context
) : ViewModel() {

    private val prefs = context.getSharedPreferences(PREFS_REMINDERS, Context.MODE_PRIVATE)
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        questionLabelDao.getAllLabels()
            .onEach { labels ->
                _uiState.update { it.copy(questionConfigs = resolveConfigs(labels)) }
            }
            .launchIn(viewModelScope)

        val enabled = prefs.getBoolean(KEY_REMINDERS_ENABLED, false)
        val hour = prefs.getInt(KEY_REMINDER_HOUR, 9)
        val minute = prefs.getInt(KEY_REMINDER_MINUTE, 0)
        val dayStrings = prefs.getStringSet(KEY_REMINDER_DAYS, emptySet()) ?: emptySet()
        val days = dayStrings.mapNotNull { it.toIntOrNull() }.toSet()
        _uiState.update {
            it.copy(
                remindersEnabled = enabled,
                reminderHour = hour,
                reminderMinute = minute,
                reminderDays = days
            )
        }
    }

    fun startEditing(key: String) {
        val current = _uiState.value.questionConfigs.firstOrNull { it.key == key } ?: return
        _uiState.update { it.copy(editingKey = key, editingLabel = current.label) }
    }

    fun onEditingLabelChange(value: String) = _uiState.update { it.copy(editingLabel = value) }

    fun saveLabel() {
        viewModelScope.launch {
            val key = _uiState.value.editingKey ?: return@launch
            val label = _uiState.value.editingLabel.trim().ifBlank { return@launch }
            val existing = questionLabelDao.getLabelForKey(key)
            questionLabelDao.insertLabel(
                QuestionLabel(
                    id = existing?.id ?: UUID.randomUUID().toString(),
                    questionKey = key,
                    customLabel = label,
                    isEnabled = existing?.isEnabled ?: true
                )
            )
            _uiState.update { it.copy(editingKey = null) }
        }
    }

    fun resetLabel(key: String) {
        viewModelScope.launch {
            questionLabelDao.deleteByKey(key)
        }
    }

    fun cancelEditing() = _uiState.update { it.copy(editingKey = null) }

    fun toggleQuestion(key: String, enabled: Boolean) {
        viewModelScope.launch {
            val existing = questionLabelDao.getLabelForKey(key)
            val defaultLabel = QuestionKeys.DEFAULT_LABELS[key] ?: key
            questionLabelDao.insertLabel(
                QuestionLabel(
                    id = existing?.id ?: UUID.randomUUID().toString(),
                    questionKey = key,
                    customLabel = existing?.customLabel ?: defaultLabel,
                    isEnabled = enabled
                )
            )
        }
    }

    fun setRemindersEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REMINDERS_ENABLED, enabled).apply()
        _uiState.update { it.copy(remindersEnabled = enabled) }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        prefs.edit().putInt(KEY_REMINDER_HOUR, hour).putInt(KEY_REMINDER_MINUTE, minute).apply()
        _uiState.update { it.copy(reminderHour = hour, reminderMinute = minute) }
    }

    fun toggleDay(day: Int) {
        val current = _uiState.value.reminderDays.toMutableSet()
        if (current.contains(day)) current.remove(day) else current.add(day)
        val dayStrings = current.map { it.toString() }.toSet()
        prefs.edit().putStringSet(KEY_REMINDER_DAYS, dayStrings).apply()
        _uiState.update { it.copy(reminderDays = current) }
    }

    class Factory(
        private val questionLabelDao: QuestionLabelDao,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(questionLabelDao, context) as T
    }
}
