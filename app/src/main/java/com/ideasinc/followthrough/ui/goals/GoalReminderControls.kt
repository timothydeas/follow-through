package com.ideasinc.followthrough.ui.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.ideasinc.followthrough.notifications.GoalReminderScheduler
import com.ideasinc.followthrough.ui.settings.ReminderDayChips
import com.ideasinc.followthrough.ui.settings.ReminderTimePickerButton
import com.ideasinc.followthrough.ui.settings.rememberReminderPermissionFlow
import com.ideasinc.followthrough.ui.theme.AppColors
import java.util.Calendar

private val DEFAULT_DAYS: Set<Int> = setOf(
    Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
    Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
)

/**
 * "Remind me for this" — an optional, per-goal implementation-intention reminder.
 * Reuses the global reminder's exact-alarm scheduler, the shared permission gate
 * (notifications + exact alarms) and the shared time/day controls; nothing new is
 * added to the alarm or permission infrastructure. The notification surfaces this
 * goal's implementation intention ([reminderBody]).
 */
@Composable
fun GoalReminderControls(
    goalId: String,
    reminderBody: String,
    modifier: Modifier = Modifier,
    toggleLabel: String = "Remind me for this"
) {
    val context = LocalContext.current

    val stored = remember(goalId) { GoalReminderScheduler.read(context, goalId) }
    var enabled by remember(goalId) { mutableStateOf(stored?.enabled == true) }
    var hour by remember(goalId) { mutableStateOf(stored?.hour ?: 9) }
    var minute by remember(goalId) { mutableStateOf(stored?.minute ?: 0) }
    var days by remember(goalId) {
        mutableStateOf(stored?.days?.takeIf { it.isNotEmpty() } ?: DEFAULT_DAYS)
    }

    // Keep the stored notification body in sync if the user later edits the
    // goal's implementation intention.
    LaunchedEffect(goalId, reminderBody, enabled) {
        if (enabled && reminderBody.isNotBlank()) {
            GoalReminderScheduler.updateBody(context, goalId, reminderBody)
        }
    }

    fun scheduleNow() {
        GoalReminderScheduler.schedule(context, goalId, hour, minute, days, reminderBody)
    }

    // Persists the "wants reminder on" intent and walks notifications + exact
    // alarms in one flow; finalizes the toggle ON when every permission is in
    // place (including automatically, on return from Settings).
    val permissionFlow = rememberReminderPermissionFlow(
        intentKey = goalId,
        onEnabled = {
            enabled = true
            scheduleNow()
        }
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = toggleLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = enabled,
                onCheckedChange = { wantOn ->
                    if (wantOn) {
                        permissionFlow.start()
                    } else {
                        enabled = false
                        GoalReminderScheduler.disable(context, goalId)
                        permissionFlow.cancel()
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    uncheckedTrackColor = AppColors.SwitchUncheckedTrack,
                    uncheckedBorderColor = Color.Transparent
                ),
                modifier = Modifier.semantics {
                    contentDescription = "Remind me for this goal"
                    stateDescription = if (enabled) "On" else "Off"
                    role = Role.Switch
                }
            )
        }

        Text(
            text = "A local notification with this goal's plan — no internet, no data leaves your device.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )

        if (enabled) {
            Column(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Remind me at",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    ReminderTimePickerButton(
                        hour = hour,
                        minute = minute,
                        onTimeSelected = { h, m ->
                            hour = h
                            minute = m
                            scheduleNow()
                        }
                    )
                }

                Text(
                    text = "Days",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                ReminderDayChips(selectedDays = days) { day, selected ->
                    days = days.toMutableSet().apply {
                        if (selected) remove(day) else add(day)
                    }
                    scheduleNow()
                }
            }
        }
    }
}
