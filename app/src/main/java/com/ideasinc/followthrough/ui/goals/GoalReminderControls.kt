package com.ideasinc.followthrough.ui.goals

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ideasinc.followthrough.notifications.GoalReminderScheduler
import com.ideasinc.followthrough.notifications.PREFS_GOAL_REMINDERS
import com.ideasinc.followthrough.notifications.canScheduleExactAlarmsCompat
import com.ideasinc.followthrough.ui.settings.ExactAlarmDialog
import com.ideasinc.followthrough.ui.settings.NotificationDeniedDialog
import com.ideasinc.followthrough.ui.settings.ReminderDayChips
import com.ideasinc.followthrough.ui.settings.ReminderTimePickerButton
import com.ideasinc.followthrough.ui.settings.areNotificationsFullyEnabled
import com.ideasinc.followthrough.ui.settings.openAppNotificationSettings
import com.ideasinc.followthrough.ui.settings.openExactAlarmSettings
import com.ideasinc.followthrough.ui.settings.runReminderPermissionGate
import com.ideasinc.followthrough.ui.theme.AppColors
import java.util.Calendar

private const val KEY_PENDING_PERMISSION = "goal_reminder_pending_goal_id"

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
    val prefs = remember { context.getSharedPreferences(PREFS_GOAL_REMINDERS, Context.MODE_PRIVATE) }

    val stored = remember(goalId) { GoalReminderScheduler.read(context, goalId) }
    var enabled by remember(goalId) { mutableStateOf(stored?.enabled == true) }
    var hour by remember(goalId) { mutableStateOf(stored?.hour ?: 9) }
    var minute by remember(goalId) { mutableStateOf(stored?.minute ?: 0) }
    var days by remember(goalId) {
        mutableStateOf(stored?.days?.takeIf { it.isNotEmpty() } ?: DEFAULT_DAYS)
    }

    var showNotificationDeniedDialog by remember { mutableStateOf(false) }
    var showExactAlarmDialog by remember { mutableStateOf(false) }

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

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && areNotificationsFullyEnabled(context)) {
            if (canScheduleExactAlarmsCompat(context)) {
                enabled = true
                scheduleNow()
                prefs.edit().remove(KEY_PENDING_PERMISSION).apply()
            } else {
                showExactAlarmDialog = true
            }
        } else {
            prefs.edit().remove(KEY_PENDING_PERMISSION).apply()
            showNotificationDeniedDialog = true
        }
    }

    // Returning from the system "Alarms & reminders" screen: if this goal's
    // reminder was awaiting exact-alarm access and it's now granted, finish
    // enabling and schedule it — mirrors the global reminder's resume handling.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, goalId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val pending = prefs.getString(KEY_PENDING_PERMISSION, null)
                if (pending == goalId && canScheduleExactAlarmsCompat(context)) {
                    enabled = true
                    scheduleNow()
                    prefs.edit().remove(KEY_PENDING_PERMISSION).apply()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showNotificationDeniedDialog) {
        NotificationDeniedDialog(
            onDismiss = { showNotificationDeniedDialog = false },
            onOpenSettings = {
                showNotificationDeniedDialog = false
                openAppNotificationSettings(context)
            }
        )
    }
    if (showExactAlarmDialog) {
        ExactAlarmDialog(
            onDismiss = { showExactAlarmDialog = false },
            onConfirm = {
                showExactAlarmDialog = false
                prefs.edit().putString(KEY_PENDING_PERMISSION, goalId).apply()
                openExactAlarmSettings(context)
            }
        )
    }

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
                        prefs.edit().putString(KEY_PENDING_PERMISSION, goalId).apply()
                        runReminderPermissionGate(
                            context = context,
                            notificationLauncher = notificationLauncher,
                            onGranted = {
                                enabled = true
                                scheduleNow()
                                prefs.edit().remove(KEY_PENDING_PERMISSION).apply()
                            },
                            onNotificationDenied = {
                                prefs.edit().remove(KEY_PENDING_PERMISSION).apply()
                                showNotificationDeniedDialog = true
                            },
                            onExactAlarmDenied = { showExactAlarmDialog = true }
                        )
                    } else {
                        enabled = false
                        GoalReminderScheduler.disable(context, goalId)
                        prefs.edit().remove(KEY_PENDING_PERMISSION).apply()
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
