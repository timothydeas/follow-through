package com.ideasinc.followthrough.ui.builder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ideasinc.followthrough.data.Barrier
import com.ideasinc.followthrough.data.CueType
import com.ideasinc.followthrough.data.Goal
import com.ideasinc.followthrough.data.GoalContentDao
import com.ideasinc.followthrough.data.GoalDao
import com.ideasinc.followthrough.data.Learning
import com.ideasinc.followthrough.data.PaletteDao
import com.ideasinc.followthrough.data.PassionInterest
import com.ideasinc.followthrough.data.Reminder
import com.ideasinc.followthrough.data.ReminderDao
import com.ideasinc.followthrough.data.ReminderStatus
import com.ideasinc.followthrough.data.ScheduleMode
import com.ideasinc.followthrough.data.WeekDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.TimeZone
import java.util.UUID

/** A cue suggestion derived from a palette chip in the "working with" tray. */
data class CueSuggestion(val type: String, val value: String, val sourcePaletteId: String)

data class BuilderUiState(
    val step: Int = 0,
    val isEdit: Boolean = false,
    val loaded: Boolean = false,
    // Step 1 — goal.
    val goalId: String? = null,
    val goals: List<Goal> = emptyList(),
    val creatingNewGoal: Boolean = false,
    val newGoalTitle: String = "",
    val newGoalWhy: String = "",
    // Step 2 — draw from yourself.
    val passions: List<PassionInterest> = emptyList(),
    val learnings: List<Learning> = emptyList(),
    val barriers: List<Barrier> = emptyList(),
    val trayPaletteIds: Set<String> = emptySet(),
    // Step 3 — intention.
    val whenMoment: String = "",
    val iWill: String = "",
    // Step 4 — one cue + schedule.
    val cueType: String = CueType.EMOJI,
    val cueValue: String = "",
    val cueSourcePaletteId: String? = null,
    val scheduleMode: String = ScheduleMode.WEEKLY,
    val days: Set<WeekDay> = WeekDay.ALL.toSet(),
    val hour: Int = 8,
    val minute: Int = 0,
    // Result.
    val savedGoalId: String? = null
) {
    val resolvedGoalTitle: String
        get() = goals.firstOrNull { it.id == goalId }?.title ?: newGoalTitle

    val step1Valid: Boolean
        get() = goalId != null || (creatingNewGoal && newGoalTitle.isNotBlank())

    val step3Valid: Boolean get() = whenMoment.isNotBlank() && iWill.isNotBlank()

    // One cue, non-empty. Photo/sound are flagged off at launch, so only emoji/phrase
    // can be saved; the value must be present.
    val step4Valid: Boolean
        get() = cueValue.isNotBlank() && CueType.isEnabledAtLaunch(cueType) &&
            (scheduleMode == ScheduleMode.DAILY || days.isNotEmpty())

    /** Cue suggestions from the tray: each selected passion offers its emoji + label. */
    val cueSuggestions: List<CueSuggestion>
        get() = passions.filter { it.id in trayPaletteIds }.flatMap { p ->
            listOf(
                CueSuggestion(CueType.EMOJI, p.emoji, p.id),
                CueSuggestion(CueType.PHRASE, p.label, p.id)
            )
        }
}

class ReminderBuilderViewModel(
    private val goalDao: GoalDao,
    private val goalContentDao: GoalContentDao,
    private val paletteDao: PaletteDao,
    private val reminderDao: ReminderDao,
    private val initialGoalId: String?,
    private val editReminderId: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(BuilderUiState())
    val uiState: StateFlow<BuilderUiState> = _uiState.asStateFlow()

    private var editingId: String? = null
    private var editingCreatedAt: Long = 0L

    init {
        viewModelScope.launch {
            val goals = goalDao.getAllGoals().first()
            val passions = paletteDao.getPassions().first()
            val learnings = paletteDao.getLearnings().first()

            val existing = editReminderId?.let { reminderDao.getReminderById(it) }
            if (existing != null) {
                editingId = existing.id
                editingCreatedAt = existing.createdAt
                val barriers = goalContentDao.getBarriers(existing.goalId).first()
                _uiState.update {
                    it.copy(
                        step = 2, // jump to intention when editing
                        isEdit = true,
                        loaded = true,
                        goalId = existing.goalId,
                        goals = goals,
                        passions = passions,
                        learnings = learnings,
                        barriers = barriers,
                        whenMoment = existing.whenMoment,
                        iWill = existing.iWill,
                        cueType = existing.cueType,
                        cueValue = existing.cueValue,
                        cueSourcePaletteId = existing.cueSourcePaletteId,
                        scheduleMode = existing.scheduleMode,
                        days = existing.days.toSet().ifEmpty { WeekDay.ALL.toSet() },
                        hour = existing.scheduleTimeLocal.substringBefore(":").toIntOrNull() ?: 8,
                        minute = existing.scheduleTimeLocal.substringAfter(":").toIntOrNull() ?: 0
                    )
                }
            } else {
                val barriers = initialGoalId?.let { goalContentDao.getBarriers(it).first() } ?: emptyList()
                _uiState.update {
                    it.copy(
                        loaded = true,
                        goalId = initialGoalId,
                        goals = goals,
                        passions = passions,
                        learnings = learnings,
                        barriers = barriers,
                        // With a goal already chosen, start at "draw from yourself".
                        step = if (initialGoalId != null) 1 else 0
                    )
                }
            }
        }
    }

    fun goToStep(step: Int) = _uiState.update { it.copy(step = step.coerceIn(0, 3)) }
    fun next() = _uiState.update { it.copy(step = (it.step + 1).coerceAtMost(3)) }
    fun back() = _uiState.update { it.copy(step = (it.step - 1).coerceAtLeast(0)) }

    fun selectGoal(goalId: String) {
        viewModelScope.launch {
            val barriers = goalContentDao.getBarriers(goalId).first()
            _uiState.update { it.copy(goalId = goalId, creatingNewGoal = false, barriers = barriers) }
        }
    }

    fun startNewGoal() = _uiState.update { it.copy(creatingNewGoal = true, goalId = null) }
    fun onNewGoalTitle(v: String) = _uiState.update { it.copy(newGoalTitle = v) }
    fun onNewGoalWhy(v: String) = _uiState.update { it.copy(newGoalWhy = v) }
    fun applyTemplate(title: String) =
        _uiState.update { it.copy(creatingNewGoal = true, goalId = null, newGoalTitle = title) }

    fun toggleTray(paletteId: String) = _uiState.update {
        val next = if (paletteId in it.trayPaletteIds) it.trayPaletteIds - paletteId else it.trayPaletteIds + paletteId
        it.copy(trayPaletteIds = next)
    }

    fun onWhenMoment(v: String) = _uiState.update { it.copy(whenMoment = v) }
    fun onIWill(v: String) = _uiState.update { it.copy(iWill = v) }

    // One-cue enforcement: setting a type/value replaces the prior selection wholesale.
    fun setCueType(type: String) = _uiState.update {
        if (CueType.isEnabledAtLaunch(type)) it.copy(cueType = type, cueValue = "", cueSourcePaletteId = null) else it
    }
    fun onCueValue(v: String) = _uiState.update { it.copy(cueValue = v, cueSourcePaletteId = null) }
    fun pickSuggestion(s: CueSuggestion) = _uiState.update {
        it.copy(cueType = s.type, cueValue = s.value, cueSourcePaletteId = s.sourcePaletteId)
    }

    fun setScheduleMode(mode: String) = _uiState.update {
        it.copy(scheduleMode = mode, days = if (mode == ScheduleMode.DAILY) WeekDay.ALL.toSet() else it.days)
    }
    fun toggleDay(day: WeekDay) = _uiState.update {
        val next = if (day in it.days) it.days - day else it.days + day
        it.copy(days = next)
    }
    fun setTime(hour: Int, minute: Int) = _uiState.update { it.copy(hour = hour, minute = minute) }

    fun save() {
        val s = _uiState.value
        if (!s.step4Valid || !s.step3Valid) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            // Resolve / create the goal.
            val goalId = s.goalId ?: run {
                val id = UUID.randomUUID().toString()
                goalDao.insertGoal(
                    Goal(
                        id = id,
                        title = s.newGoalTitle.trim(),
                        createdAt = now,
                        updatedAt = now,
                        whyItMatters = s.newGoalWhy.trim()
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
                scheduleDays = WeekDay.toCsv(if (s.scheduleMode == ScheduleMode.DAILY) WeekDay.ALL else orderedDays),
                scheduleTimeLocal = "%02d:%02d".format(s.hour, s.minute),
                scheduleTimezone = TimeZone.getDefault().id,
                fullTextAlwaysShown = true,
                status = ReminderStatus.ACTIVE,
                createdAt = editingCreatedAt.takeIf { it > 0 } ?: now,
                updatedAt = now
            )
            reminderDao.upsert(reminder)
            _uiState.update { it.copy(savedGoalId = goalId) }
        }
    }

    class Factory(
        private val goalDao: GoalDao,
        private val goalContentDao: GoalContentDao,
        private val paletteDao: PaletteDao,
        private val reminderDao: ReminderDao,
        private val initialGoalId: String?,
        private val editReminderId: String?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ReminderBuilderViewModel(goalDao, goalContentDao, paletteDao, reminderDao, initialGoalId, editReminderId) as T
    }
}
