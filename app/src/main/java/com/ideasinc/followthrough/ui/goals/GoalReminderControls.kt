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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.ideasinc.followthrough.notifications.GoalReminderScheduler
import com.ideasinc.followthrough.notifications.ensureReminderChannel
import com.ideasinc.followthrough.ui.rememberA11yAnnouncer
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
 * A single plan's reminder — its own enabled flag, time and days. Keyed by
 * [checkInId] (each plan reminds independently). The notification's title, intention
 * text, and cue are read live from the plan at fire time, so this control only
 * owns the schedule. Uses the shared exact-alarm scheduler, permission gate, and
 * time/day controls.
 */
@Composable
fun GoalReminderControls(
    checkInId: String,
    modifier: Modifier = Modifier,
    toggleLabel: String = "Remind me about this check-in",
    onToggleTap: () -> Unit = {}
) {
    val context = LocalContext.current
    val announce = rememberA11yAnnouncer()

    val stored = remember(checkInId) { GoalReminderScheduler.read(context, checkInId) }
    var enabled by remember(checkInId) { mutableStateOf(stored?.enabled == true) }
    var hour by remember(checkInId) { mutableStateOf(stored?.hour ?: 9) }
    var minute by remember(checkInId) { mutableStateOf(stored?.minute ?: 0) }
    var days by remember(checkInId) {
        mutableStateOf(stored?.days?.takeIf { it.isNotEmpty() } ?: DEFAULT_DAYS)
    }

    fun scheduleNow() {
        GoalReminderScheduler.schedule(context, checkInId, hour, minute, days)
    }

    val permissionFlow = rememberReminderPermissionFlow(
        intentKey = checkInId,
        onEnabled = {
            enabled = true
            ensureReminderChannel(context)
            scheduleNow()
            announce("Reminder on. Time and days controls are now available below.")
        }
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {},
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
                    onToggleTap()
                    if (wantOn) {
                        permissionFlow.start()
                    } else {
                        enabled = false
                        GoalReminderScheduler.disable(context, checkInId)
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
                    stateDescription = if (enabled) "On" else "Off"
                    role = Role.Switch
                }
            )
        }

        Text(
            text = "A local nudge with this check-in's cue — no internet, no data leaves your device.",
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
