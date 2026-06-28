package com.ideasinc.followthrough.ui.today

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ideasinc.followthrough.data.EventAction
import com.ideasinc.followthrough.data.GoalDao
import com.ideasinc.followthrough.data.Reminder
import com.ideasinc.followthrough.data.ReminderDao
import com.ideasinc.followthrough.data.ReminderEventDao
import com.ideasinc.followthrough.data.ReminderStatus
import com.ideasinc.followthrough.data.ScheduleMode
import com.ideasinc.followthrough.notifications.ReminderAlarmScheduler
import com.ideasinc.followthrough.notifications.recordReminderEvent
import com.ideasinc.followthrough.notifications.undoReminderEvent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class IntentionItem(val reminder: Reminder, val direction: String = "")

data class TodayUiState(
    val items: List<IntentionItem> = emptyList(),
    val loaded: Boolean = false
)

/**
 * The Intentions tab (home). Lists the user's active intentions (cue + full text + schedule)
 * with the single Did-it response. No counters or score here — progress lives entirely on the
 * Progress tab as the number-free "what's been working" cues (Tim's call 2026-06-24).
 */
class TodayViewModel(
    private val appContext: Context,
    private val reminderDao: ReminderDao,
    private val eventDao: ReminderEventDao,
    private val goalDao: GoalDao
) : ViewModel() {

    val uiState: StateFlow<TodayUiState> =
        combine(
            reminderDao.getActiveReminders(),
            goalDao.getAllGoals()
        ) { reminders, goals ->
            // Map each intention to the direction it serves (goal.whyItMatters), for the card line.
            val directionByGoal = goals.associate { it.id to it.whyItMatters }
            TodayUiState(
                items = reminders.sortedBy { it.scheduleTimeLocal }
                    .map { IntentionItem(it, direction = directionByGoal[it.goalId].orEmpty()) },
                loaded = true
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    /** Records a response and returns the new event id so the caller can offer Undo. */
    fun act(reminder: Reminder, action: String, onRecorded: (String) -> Unit) {
        viewModelScope.launch {
            // A reminderless intention (ScheduleMode.NONE) is a mental cue that can be done many
            // times a day — each tap is a distinct completion, so don't collapse same-day repeats.
            // Reminder-based intentions keep the per-occasion dedupe (notification + in-app = one).
            val id = recordReminderEvent(
                eventDao, reminder.id, action, System.currentTimeMillis(),
                dedupePerDay = reminder.scheduleMode != ScheduleMode.NONE
            )
            if (action == EventAction.SNOOZED) ReminderAlarmScheduler.snooze(appContext, reminder.id)
            // A one-off is finished once you do it: cancel its pending alarm and archive it
            // so it drops off Intentions. It still shows in What worked (via its event).
            if (action == EventAction.DONE && reminder.scheduleMode == ScheduleMode.ONCE) {
                ReminderAlarmScheduler.cancel(appContext, reminder.id)
                reminderDao.update(reminder.copy(status = ReminderStatus.ARCHIVED, updatedAt = System.currentTimeMillis()))
            }
            onRecorded(id)
        }
    }

    fun undo(eventId: String) {
        viewModelScope.launch {
            undoReminderEvent(eventDao, eventId)
            // Undoing a one-off's Did it brings it back: re-activate + reschedule.
            val event = eventDao.getEventById(eventId)
            val reminder = event?.let { reminderDao.getReminderById(it.reminderId) }
            if (reminder != null && reminder.scheduleMode == ScheduleMode.ONCE && reminder.status != ReminderStatus.ACTIVE) {
                val restored = reminder.copy(status = ReminderStatus.ACTIVE, updatedAt = System.currentTimeMillis())
                reminderDao.update(restored)
                ReminderAlarmScheduler.schedule(appContext, restored)
            }
        }
    }

    class Factory(
        private val appContext: Context,
        private val reminderDao: ReminderDao,
        private val eventDao: ReminderEventDao,
        private val goalDao: GoalDao
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TodayViewModel(appContext, reminderDao, eventDao, goalDao) as T
    }
}