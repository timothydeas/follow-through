package com.ideasinc.followthrough.ui.builder

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ideasinc.followthrough.data.CueType
import com.ideasinc.followthrough.data.Goal
import com.ideasinc.followthrough.data.GoalDao
import com.ideasinc.followthrough.data.Reminder
import com.ideasinc.followthrough.data.ReminderDao
import com.ideasinc.followthrough.data.ReminderStatus
import com.ideasinc.followthrough.data.ScheduleMode
import com.ideasinc.followthrough.data.WeekDay
import com.ideasinc.followthrough.notifications.ReminderAlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.TimeZone
import java.util.UUID

/**
 * Create-cue flow state. The user creates an **Intention** directly — there is no goal
 * step in the experience (MVP_User_Flow_IA.md). The four steps are:
 *   0  Name the intention   — "What do you want to remember to do?"  (the action)
 *   1  Pin the moment       — when / where you'll act + how often      (whenMoment + schedule)
 *   2  Design the cue       — one distinctive cue, seeded with an example
 *   3  Review & confirm     — restate, then it's scheduled
 *
 * Internally each new Intention still gets a minimal `goals` container row so the existing
 * `Reminder.goalId` foreign key stays valid (no schema change, no migration). The container
 * is never shown or named.
 */
data class BuilderUiState(
    val step: Int = 0,
    val isEdit: Boolean = false,
    val loaded: Boolean = false,
    // Step 0 — the action ("I will …").
    val iWill: String = "",
    // Step 1 — the moment + how often it recurs.
    val whenMoment: String = "",
    val scheduleMode: String = ScheduleMode.WEEKLY,
    val days: Set<WeekDay> = WeekDay.ALL.toSet(),
    val onceDate: String = "", // "yyyy-MM-dd" when scheduleMode == ONCE
    val hour: Int = 8,
    val minute: Int = 0,
    // Step 2 — one cue (emoji or phrase).
    val cueType: String = CueType.EMOJI,
    val cueValue: String = "",
    val cueSourcePaletteId: String? = null,
    // Internal container goal (reused on edit; created on save for a new intention).
    val goalId: String? = null,
    // Result — flips true once saved + scheduled, or once deleted.
    val saved: Boolean = false,
    val deleted: Boolean = false
) {
    val step0Valid: Boolean get() = iWill.isNotBlank()
    val step1Valid: Boolean
        get() = whenMoment.isNotBlank() && when (scheduleMode) {
            ScheduleMode.DAILY -> true
            ScheduleMode.ONCE -> onceDate.isNotBlank()
            else -> days.isNotEmpty()
        }
    // One cue, non-empty. Photo/sound are off at launch, so only emoji/phrase can be saved.
    val step2Valid: Boolean
        get() = cueValue.isNotBlank() && CueType.isEnabledAtLaunch(cueType)

    fun canAdvance(s: Int): Boolean = when (s) {
        0 -> step0Valid
        1 -> step1Valid
        2 -> step2Valid
        else -> true
    }
}

class ReminderBuilderViewModel(
    private val appContext: Context,
    private val goalDao: GoalDao,
    private val reminderDao: ReminderDao,
    private val editReminderId: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(BuilderUiState())
    val uiState: StateFlow<BuilderUiState> = _uiState.asStateFlow()

    private var editingId: String? = null
    private var editingCreatedAt: Long = 0L

    init {
        viewModelScope.launch {
            val existing = editReminderId?.let { reminderDao.getReminderById(it) }
            if (existing != null) {
                editingId = existing.id
                editingCreatedAt = existing.createdAt
                _uiState.update {
                    it.copy(
                        step = 0,
                        isEdit = true,
                        loaded = true,
                        iWill = existing.iWill,
                        whenMoment = existing.whenMoment,
                        scheduleMode = existing.scheduleMode,
                        onceDate = existing.scheduleDate ?: "",
                        days = existing.days.toSet().ifEmpty { WeekDay.ALL.toSet() },
                        hour = existing.scheduleTimeLocal.substringBefore(":").toIntOrNull() ?: 8,
                        minute = existing.scheduleTimeLocal.substringAfter(":").toIntOrNull() ?: 0,
                        cueType = existing.cueType,
                        cueValue = existing.cueValue,
                        cueSourcePaletteId = existing.cueSourcePaletteId,
                        goalId = existing.goalId
                    )
                }
            } else {
                _uiState.update { it.copy(loaded = true) }
            }
        }
    }

    // Four steps now: 0 = action, 1 = moment + schedule, 2 = cue, 3 = review.
    fun goToStep(step: Int) = _uiState.update { it.copy(step = step.coerceIn(0, 3)) }
    fun next() = _uiState.update { it.copy(step = (it.step + 1).coerceAtMost(3)) }
    fun back() = _uiState.update { it.copy(step = (it.step - 1).coerceAtLeast(0)) }

    fun onIWill(v: String) = _uiState.update { it.copy(iWill = v) }
    fun onWhenMoment(v: String) = _uiState.update { it.copy(whenMoment = v) }

    // One-cue enforcement: setting a type/value replaces the prior selection wholesale.
    fun setCueType(type: String) = _uiState.update {
        if (CueType.isEnabledAtLaunch(type)) it.copy(cueType = type, cueValue = "", cueSourcePaletteId = null) else it
    }
    fun onCueValue(v: String) = _uiState.update { it.copy(cueValue = v, cueSourcePaletteId = null) }

    fun setScheduleMode(mode: String) = _uiState.update {
        it.copy(scheduleMode = mode, days = if (mode == ScheduleMode.DAILY) WeekDay.ALL.toSet() else it.days)
    }
    fun setOnceDate(year: Int, month1to12: Int, day: Int) = _uiState.update {
        it.copy(onceDate = "%04d-%02d-%02d".format(year, month1to12, day))
    }
    fun toggleDay(day: WeekDay) = _uiState.update {
        val next = if (day in it.days) it.days - day else it.days + day
        it.copy(days = next)
    }
    fun setTime(hour: Int, minute: Int) = _uiState.update { it.copy(hour = hour, minute = minute) }

    fun save() {
        val s = _uiState.value
        if (!s.step0Valid || !s.step1Valid || !s.step2Valid) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            // Resolve the internal container (reuse on edit; create a minimal one otherwise).
            // It is never shown — its title mirrors the action so any internal listing reads sensibly.
            val goalId = s.goalId ?: run {
                val id = UUID.randomUUID().toString()
                goalDao.insertGoal(
                    Goal(
                        id = id,
                        title = s.iWill.trim().take(80),
                        createdAt = now,
                        updatedAt = now
                    )
                )
                id
            }
            val orderedDays = WeekDay.ALL.filter { it in s.days }
            val reminder = Reminder(
                id = editingId ?: UUID.randomUUID().toString(),
                goalId = goalId,
                whenMoment = s.whenMoment.trim(),
                iWill = s.iWill.trim(),
                cueType = s.cueType,
                cueValue = s.cueValue.trim(),
                cueAltText = null,
                cueSourcePaletteId = s.cueSourcePaletteId,
                cueIsPaletteDrawn = s.cueSourcePaletteId != null,
                scheduleMode = s.scheduleMode,
                scheduleDays = when (s.scheduleMode) {
                    ScheduleMode.DAILY -> WeekDay.toCsv(WeekDay.ALL)
                    ScheduleMode.ONCE -> ""
                    else -> WeekDay.toCsv(orderedDays)
                },
                scheduleTimeLocal = "%02d:%02d".format(s.hour, s.minute),
                scheduleTimezone = TimeZone.getDefault().id,
                scheduleDate = if (s.scheduleMode == ScheduleMode.ONCE) s.onceDate else null,
                fullTextAlwaysShown = true,
                status = ReminderStatus.ACTIVE,
                createdAt = editingCreatedAt.takeIf { it > 0 } ?: now,
                updatedAt = now
            )
            reminderDao.upsert(reminder)
            // Schedule (or reschedule, on edit) the exact alarms for this intention.
            ReminderAlarmScheduler.schedule(appContext, reminder)
            _uiState.update { it.copy(saved = true) }
        }
    }

    /** Delete this intention (edit mode only): cancel its alarm, remove the row, signal done. */
    fun delete() {
        val id = editingId ?: return
        viewModelScope.launch {
            ReminderAlarmScheduler.cancel(appContext, id)
            reminderDao.deleteById(id)
            _uiState.update { it.copy(deleted = true) }
        }
    }

    class Factory(
        private val appContext: Context,
        private val goalDao: GoalDao,
        private val reminderDao: ReminderDao,
        private val editReminderId: String?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ReminderBuilderViewModel(appContext, goalDao, reminderDao, editReminderId) as T
    }
}
