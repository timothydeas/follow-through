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
import java.util.Calendar
import java.util.TimeZone
import java.util.UUID

/**
 * Create-cue flow state. The user creates an **Intention** directly. The five steps are:
 *   0  Name the intention   — "What do you want to remember to do?"  (the action)
 *   1  Your goal            — the bigger aim it serves (skippable)    (direction)
 *   2  Pin the moment       — when / where you'll act + how often      (whenMoment + schedule)
 *   3  Design the cue       — one distinctive cue, seeded with an example
 *   4  Review & confirm     — restate, then it's scheduled
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
    // Step 1 — optional "direction" this intention serves (stored on the goal's whyItMatters).
    val direction: String = "",
    // Step 2 — the moment + (optional) reminder. The reminder (schedule + cue + notification) is
    // optional: the moment is part of the intention, but a user can save without a reminder.
    val whenMoment: String = "",
    val wantsReminder: Boolean = true,
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
    val nameValid: Boolean get() = iWill.isNotBlank()

    /** A one-off's date+time must be in the future, or the alarm would never fire (the scheduler
     *  skips a past trigger). True for non-ONCE modes. */
    private val onceInFuture: Boolean
        get() {
            if (scheduleMode != ScheduleMode.ONCE) return true
            if (onceDate.isBlank()) return false
            val parts = onceDate.split("-")
            val y = parts.getOrNull(0)?.toIntOrNull() ?: return false
            val mo = parts.getOrNull(1)?.toIntOrNull() ?: return false
            val d = parts.getOrNull(2)?.toIntOrNull() ?: return false
            val cal = Calendar.getInstance().apply {
                set(y, mo - 1, d, hour, minute); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis > System.currentTimeMillis()
        }

    /** A date is picked but the chosen date+time has already passed — surface a clear reason
     *  rather than just a disabled button. */
    val oncePastSelected: Boolean
        get() = scheduleMode == ScheduleMode.ONCE && onceDate.isNotBlank() && !onceInFuture

    private val scheduleValid: Boolean
        get() = when (scheduleMode) {
            ScheduleMode.DAILY -> true
            ScheduleMode.ONCE -> onceDate.isNotBlank() && onceInFuture
            else -> days.isNotEmpty()
        }
    // The moment is required (it's the intention). The schedule only matters with a reminder.
    val momentValid: Boolean get() = whenMoment.isNotBlank() && (!wantsReminder || scheduleValid)
    // One cue, non-empty. Photo/sound are off at launch, so only emoji/phrase can be saved.
    val cueValid: Boolean
        get() = cueValue.isNotBlank() && CueType.isEnabledAtLaunch(cueType)

    /**
     * The ordered step indices actually shown — all five, always. The cue step (2) is shown even
     * without a reminder: a cue you carry or place is itself the trigger (RTA), so a reminderless
     * or one-off intention can still have one — you see it, and you act. It's just optional without
     * a reminder (see [canAdvance]); with a reminder it's required (it's what the notification shows).
     */
    val activeSteps: List<Int> get() = listOf(0, 1, 2, 3, 4)

    val isLastStep: Boolean get() = step == activeSteps.last()

    fun canAdvance(s: Int): Boolean = when (s) {
        0 -> nameValid
        1 -> momentValid
        2 -> !wantsReminder || cueValid // cue required only with a reminder; optional otherwise
        else -> true // goal (3) is skippable; review (4) always advances to save
    }
}

class ReminderBuilderViewModel(
    private val appContext: Context,
    private val goalDao: GoalDao,
    private val reminderDao: ReminderDao,
    private val editReminderId: String?,
    private val seedIWill: String? = null,
    private val seedDirection: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(BuilderUiState())
    val uiState: StateFlow<BuilderUiState> = _uiState.asStateFlow()

    private var editingId: String? = null
    private var editingCreatedAt: Long = 0L

    init {
        viewModelScope.launch {
            val existing = editReminderId?.let { reminderDao.getReminderById(it) }
            if (existing != null) {
                // Load the goal's direction (whyItMatters) so edit shows and preserves it.
                val direction = goalDao.getGoalById(existing.goalId)?.whyItMatters ?: ""
                editingId = existing.id
                editingCreatedAt = existing.createdAt
                _uiState.update {
                    it.copy(
                        step = 0,
                        isEdit = true,
                        loaded = true,
                        iWill = existing.iWill,
                        direction = direction,
                        whenMoment = existing.whenMoment,
                        wantsReminder = existing.scheduleMode != ScheduleMode.NONE,
                        scheduleMode = existing.scheduleMode.takeIf { it != ScheduleMode.NONE } ?: ScheduleMode.WEEKLY,
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
                // New intention — optionally seeded (e.g. from a "make this an intention" learning).
                // A learning carries its parent direction, so pre-fill it instead of leaving the
                // field blank (which read as "re-pick the goal" — Tim's feedback).
                _uiState.update {
                    it.copy(
                        loaded = true,
                        iWill = seedIWill?.trim().orEmpty(),
                        direction = seedDirection?.trim().orEmpty()
                    )
                }
            }
        }
    }

    // Five steps: 0 = action, 1 = moment + schedule, 2 = cue, 3 = goal, 4 = review.
    // Navigation walks `activeSteps` (all five; the cue is optional without a reminder).
    fun goToStep(step: Int) = _uiState.update { it.copy(step = step.coerceIn(0, 4)) }
    fun next() = _uiState.update { st ->
        val steps = st.activeSteps
        val i = steps.indexOf(st.step)
        st.copy(step = steps.getOrElse(i + 1) { steps.last() })
    }
    fun back() = _uiState.update { st ->
        val steps = st.activeSteps
        val i = steps.indexOf(st.step)
        st.copy(step = steps.getOrElse((i - 1).coerceAtLeast(0)) { steps.first() })
    }

    fun onIWill(v: String) = _uiState.update { it.copy(iWill = v) }
    fun onDirection(v: String) = _uiState.update { it.copy(direction = v) }
    fun onWhenMoment(v: String) = _uiState.update { it.copy(whenMoment = v) }

    // One-cue enforcement: setting a type/value replaces the prior selection wholesale.
    fun setCueType(type: String) = _uiState.update {
        if (CueType.isEnabledAtLaunch(type)) it.copy(cueType = type, cueValue = "", cueSourcePaletteId = null) else it
    }
    fun onCueValue(v: String) = _uiState.update { it.copy(cueValue = v, cueSourcePaletteId = null) }

    fun setWantsReminder(wants: Boolean) = _uiState.update { it.copy(wantsReminder = wants) }
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
        // A cue is only required when the user wants a reminder.
        if (!s.nameValid || !s.momentValid || (s.wantsReminder && !s.cueValid)) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            // Resolve the internal container (reuse on edit; create a minimal one otherwise).
            // It is never shown — its title mirrors the action so any internal listing reads
            // sensibly. The optional "direction" the intention serves lives on whyItMatters.
            val direction = s.direction.trim()
            val goalId = s.goalId ?: run {
                val id = UUID.randomUUID().toString()
                goalDao.insertGoal(
                    Goal(
                        id = id,
                        title = s.iWill.trim().take(80),
                        whyItMatters = direction,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                id
            }
            // On edit (reused goal), keep the direction in sync.
            if (s.goalId != null) {
                goalDao.getGoalById(s.goalId)?.let { g ->
                    if (g.whyItMatters != direction) goalDao.updateGoal(g.copy(whyItMatters = direction, updatedAt = now))
                }
            }
            val orderedDays = WeekDay.ALL.filter { it in s.days }
            // Reminderless: store ScheduleMode.NONE (the cue, if any, is kept); scheduler no-ops on NONE.
            val mode = if (s.wantsReminder) s.scheduleMode else ScheduleMode.NONE
            val reminder = Reminder(
                id = editingId ?: UUID.randomUUID().toString(),
                goalId = goalId,
                whenMoment = s.whenMoment.trim(),
                iWill = s.iWill.trim(),
                cueType = s.cueType,
                // A cue is kept whether or not there's a reminder — a cue you carry or place is the
                // trigger (RTA). Optional without a reminder, required with one.
                cueValue = s.cueValue.trim(),
                cueAltText = null,
                cueSourcePaletteId = s.cueSourcePaletteId,
                cueIsPaletteDrawn = s.cueSourcePaletteId != null,
                scheduleMode = mode,
                scheduleDays = when (mode) {
                    ScheduleMode.DAILY -> WeekDay.toCsv(WeekDay.ALL)
                    ScheduleMode.WEEKLY -> WeekDay.toCsv(orderedDays)
                    else -> "" // ONCE / NONE
                },
                scheduleTimeLocal = "%02d:%02d".format(s.hour, s.minute),
                scheduleTimezone = TimeZone.getDefault().id,
                scheduleDate = if (mode == ScheduleMode.ONCE) s.onceDate else null,
                fullTextAlwaysShown = true,
                status = ReminderStatus.ACTIVE,
                createdAt = editingCreatedAt.takeIf { it > 0 } ?: now,
                updatedAt = now
            )
            reminderDao.upsert(reminder)
            // (Re)schedule alarms; a no-op + cancel for ScheduleMode.NONE, so editing a reminder
            // down to "no reminder" clears its old alarms.
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
        private val editReminderId: String?,
        private val seedIWill: String? = null,
        private val seedDirection: String? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ReminderBuilderViewModel(appContext, goalDao, reminderDao, editReminderId, seedIWill, seedDirection) as T
    }
}
