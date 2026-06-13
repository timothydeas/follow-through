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
import com.ideasinc.followthrough.data.ScheduleMode
import com.ideasinc.followthrough.data.WeekDay
import com.ideasinc.followthrough.notifications.ReminderAlarmScheduler
import com.ideasinc.followthrough.notifications.recordReminderEvent
import com.ideasinc.followthrough.notifications.undoReminderEvent
import com.ideasinc.followthrough.ui.progress.WeeklyProgress
import com.ideasinc.followthrough.ui.progress.computeWeeklyProgress
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class TodayReminderItem(val reminder: Reminder, val goalTitle: String)

data class TodayUiState(
    val items: List<TodayReminderItem> = emptyList(),
    val progress: WeeklyProgress = WeeklyProgress.empty(),
    val loaded: Boolean = false
)

class TodayViewModel(
    private val appContext: Context,
    private val reminderDao: ReminderDao,
    private val eventDao: ReminderEventDao,
    private val goalDao: GoalDao
) : ViewModel() {

    val uiState: StateFlow<TodayUiState> =
        combine(
            reminderDao.getActiveReminders(),
            goalDao.getAllGoals(),
            eventDao.getLiveEvents()
        ) { reminders, goals, events ->
            val today = currentWeekDay()
            val goalsById = goals.associateBy { it.id }
            val items = reminders
                .filter { it.scheduleMode == ScheduleMode.DAILY || today in it.days }
                .sortedBy { it.scheduleTimeLocal }
                .map { TodayReminderItem(it, goalsById[it.goalId]?.title.orEmpty()) }
            TodayUiState(
                items = items,
                progress = computeWeeklyProgress(events),
                loaded = true
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    /** Records a response and returns the new event id so the caller can offer Undo. */
    fun act(reminder: Reminder, action: String, onRecorded: (String) -> Unit) {
        viewModelScope.launch {
            val id = recordReminderEvent(eventDao, reminder.id, action, System.currentTimeMillis())
            if (action == EventAction.SNOOZED) ReminderAlarmScheduler.snooze(appContext, reminder.id)
            onRecorded(id)
        }
    }

    fun undo(eventId: String) {
        viewModelScope.launch { undoReminderEvent(eventDao, eventId) }
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

internal fun currentWeekDay(): WeekDay = when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
    Calendar.MONDAY -> WeekDay.MON
    Calendar.TUESDAY -> WeekDay.TUE
    Calendar.WEDNESDAY -> WeekDay.WED
    Calendar.THURSDAY -> WeekDay.THU
    Calendar.FRIDAY -> WeekDay.FRI
    Calendar.SATURDAY -> WeekDay.SAT
    else -> WeekDay.SUN
}
