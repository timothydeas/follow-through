package com.example.grounded.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.grounded.data.CheckInDao
import com.example.grounded.data.GoalDao
import com.example.grounded.ui.list.computeStreakWithFlex
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

data class StatsUiState(
    val checkInCurrentStreak: Int = 0,
    val checkInCurrentStreakFlexUsed: Boolean = false,
    val checkInLongestStreak: Int = 0,
    val checkInTotal: Int = 0,
    val followThroughCurrentStreak: Int = 0,
    val followThroughCurrentStreakFlexUsed: Boolean = false,
    val followThroughTotal: Int = 0,
    val followThroughThisWeek: Int = 0,
    val followThroughThisMonth: Int = 0
)

class StatsViewModel(
    goalDao: GoalDao,
    checkInDao: CheckInDao
) : ViewModel() {

    val uiState: StateFlow<StatsUiState> = combine(
        goalDao.getAllGoals(),
        checkInDao.getAllCheckIns()
    ) { goals, checkIns ->
        val checkInTimes = checkIns.map { it.createdAt }
        val followThroughTimes = goals
            .filter { it.followedThrough }
            .mapNotNull { it.followedThroughAt }

        val now = System.currentTimeMillis()

        val checkInStreak = computeStreakWithFlex(checkInTimes)
        val followThroughStreak = computeStreakWithFlex(followThroughTimes)

        StatsUiState(
            checkInCurrentStreak = checkInStreak.days,
            checkInCurrentStreakFlexUsed = checkInStreak.flexDayUsed,
            checkInLongestStreak = longestStreak(checkInTimes),
            checkInTotal = checkIns.size,
            followThroughCurrentStreak = followThroughStreak.days,
            followThroughCurrentStreakFlexUsed = followThroughStreak.flexDayUsed,
            followThroughTotal = goals.count { it.followedThrough },
            followThroughThisWeek = followThroughTimes.count { it >= startOfWeek(now) },
            followThroughThisMonth = followThroughTimes.count { it >= startOfMonth(now) }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatsUiState()
    )

    class Factory(
        private val goalDao: GoalDao,
        private val checkInDao: CheckInDao
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            StatsViewModel(goalDao, checkInDao) as T
    }
}

private const val DAY_MS = 24L * 60 * 60 * 1000

private fun startOfDay(ts: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = ts
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun startOfWeek(ts: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = ts
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
    return cal.timeInMillis
}

private fun startOfMonth(ts: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = ts
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    cal.set(Calendar.DAY_OF_MONTH, 1)
    return cal.timeInMillis
}

private fun longestStreak(timestamps: List<Long>): Int {
    if (timestamps.isEmpty()) return 0
    val days = timestamps.map { startOfDay(it) }.toSortedSet().toList()
    var longest = 1
    var current = 1
    for (i in 1 until days.size) {
        if (days[i] - days[i - 1] == DAY_MS) {
            current++
            if (current > longest) longest = current
        } else {
            current = 1
        }
    }
    return longest
}
