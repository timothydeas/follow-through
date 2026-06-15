package com.ideasinc.followthrough.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ideasinc.followthrough.data.CueType
import com.ideasinc.followthrough.data.EventAction
import com.ideasinc.followthrough.data.Reminder
import com.ideasinc.followthrough.di.AppContainer
import com.ideasinc.followthrough.ui.progress.WeeklyProgress
import com.ideasinc.followthrough.ui.theme.AppColors
import kotlinx.coroutines.launch

/**
 * Home — Today. The reminders due today, each showing its cue PLUS the full
 * intention text, with three judgment-free one-tap actions (Done · Snooze 1h · Not
 * today), every action undoable via an 8-second snackbar. A gentle weekly progress
 * line (§4a) sits on top; the FAB starts a new reminder.
 */
@Composable
fun TodayScreen(
    container: AppContainer,
    onNewReminder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val vm: TodayViewModel = viewModel(
        factory = TodayViewModel.Factory(appContext, container.reminderDao, container.reminderEventDao, container.goalDao)
    )
    val state by vm.uiState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun onAction(reminder: Reminder, action: String, label: String) {
        vm.act(reminder, action) { eventId ->
            scope.launch {
                // 8-second Undo window (handoff §4); auto-dismisses after 8s or on
                // an earlier tap. withTimeoutOrNull cancels showSnackbar at 8s.
                val result = kotlinx.coroutines.withTimeoutOrNull(8_000) {
                    snackbarHost.showSnackbar(
                        message = label, actionLabel = "Undo", duration = SnackbarDuration.Indefinite
                    )
                }
                if (result == SnackbarResult.ActionPerformed) vm.undo(eventId)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHost) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewReminder,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.semantics { contentDescription = "New reminder" }
            ) { Icon(Icons.Default.Add, contentDescription = null) }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Today", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.semantics { heading() })
            }
            item {
                Text("Your reminders for the moments that matter.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item { WeeklyProgressLine(state.progress) }

            if (state.loaded && state.items.isEmpty()) {
                item {
                    Text(
                        "Nothing scheduled for today. Tap + to add a reminder.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }

            items(state.items, key = { it.reminder.id }) { item ->
                ReminderCard(
                    reminder = item.reminder,
                    goalTitle = item.goalTitle,
                    onDone = { onAction(item.reminder, EventAction.DONE, "Followed through. Nice.") },
                    onSnooze = { onAction(item.reminder, EventAction.SNOOZED, "Snoozed an hour.") },
                    onNotToday = { onAction(item.reminder, EventAction.NOT_TODAY, "Forgiven and rescheduled.") }
                )
            }
        }
    }
}

@Composable
private fun WeeklyProgressLine(progress: WeeklyProgress) {
    val bg = if (progress.isCelebratory) AppColors.GoldSurface else MaterialTheme.colorScheme.surface
    val fg = if (progress.isCelebratory) AppColors.OnGoldSurface else MaterialTheme.colorScheme.onSurface
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.dp, AppColors.Border, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(progress.line, style = MaterialTheme.typography.bodyMedium, color = fg)
        progress.lifetimeLine?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = if (progress.isCelebratory) fg else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReminderCard(
    reminder: Reminder,
    goalTitle: String,
    onDone: () -> Unit,
    onSnooze: () -> Unit,
    onNotToday: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            if (reminder.cueType == CueType.EMOJI && reminder.cueValue.isNotBlank()) {
                Text(reminder.cueValue, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                if (reminder.cueType == CueType.PHRASE && reminder.cueValue.isNotBlank()) {
                    Text("“${reminder.cueValue}”", style = MaterialTheme.typography.titleMedium, color = AppColors.BrandAccentText)
                }
                Text(reminder.intentionText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Row(modifier = Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(formatTime(reminder.scheduleTimeLocal), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (goalTitle.isNotBlank()) {
                        Text("· $goalTitle", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton("Done", onDone, Modifier.weight(1f))
            ActionButton("Snooze", onSnooze, Modifier.weight(1f))
            ActionButton("Not today", onNotToday, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ActionButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.heightIn(min = 44.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium), color = AppColors.BrandAccentText, maxLines = 1)
    }
}

/** "HH:mm" 24h → a friendly 12h label, e.g. "06:45" → "6:45 AM". */
private fun formatTime(hhmm: String): String {
    val parts = hhmm.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: return hhmm
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val period = if (h < 12) "AM" else "PM"
    val h12 = when { h == 0 -> 12; h > 12 -> h - 12; else -> h }
    return "%d:%02d %s".format(h12, m, period)
}
