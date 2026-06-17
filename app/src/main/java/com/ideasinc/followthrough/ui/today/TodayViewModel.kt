package com.ideasinc.followthrough.ui.today

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ideasinc.followthrough.data.EventAction
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
import java.util.Calendar

data class IntentionItem(val reminder: Reminder)

/** Forgiving, positive progress (Milkman): this week's follow-throughs + a never-resetting
 *  lifetime total. It only counts up — a miss never breaks or resets it. */
data class WeeklyProgress(val weeklyDone: Int = 0, val lifetimeDone: Int = 0)

data class TodayUiState(
    val items: List<IntentionItem> = emptyList(),
    val progress: WeeklyProgress = WeeklyProgress(),
    val loaded: Boolean = false
)

/**
 * The Intentions tab (home). Lists the user's active intentions (cue + full text + schedule)
 * with the single Did-it response, plus a gentle, forgiving progress line — this week's
 * follow-throughs and a never-resetting lifetime total (never a rigid streak).
 */
class TodayViewModel(
    private val appContext: Context,
    private val reminderDao: ReminderDao,
    private val eventDao: ReminderEventDao
) : ViewModel() {

    val uiState: StateFlow<TodayUiState> =
        combine(reminderDao.getActiveReminders(), eventDao.getLiveEvents()) { reminders, events ->
            val monday = mondayStart(System.currentTimeMillis())
            val done = events.filter { it.action == EventAction.DONE }
            TodayUiState(
                items = reminders.sortedBy { it.scheduleTimeLocal }.map { IntentionItem(it) },
                progress = WeeklyProgress(
                    weeklyDone = done.count { it.deliveredAt >= monday },
                    lifetimeDone = done.size
                ),
                loaded = true
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    /** Records a response and returns the new event id so the caller can offer Undo. */
    fun act(reminder: Reminder, action: String, onRecorded: (String) -> Unit) {
        viewModelScope.launch {
            val id = recordReminderEvent(eventDao, reminder.id, action, System.currentTimeMillis())
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
        private val eventDao: ReminderEventDao
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TodayViewModel(appContext, reminderDao, eventDao) as T
    }
}

/** Start of the current week (Monday 00:00, device-local). Recomputed on read. */
private fun mondayStart(now: Long): Long {
    val c = Calendar.getInstance().apply { timeInMillis = now }
    c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
    while (c.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) c.add(Calendar.DAY_OF_MONTH, -1)
    return c.timeInMillis
}