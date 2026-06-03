package com.ideasinc.followthrough.ui.settings

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ideasinc.followthrough.notifications.canScheduleExactAlarmsCompat
import com.ideasinc.followthrough.ui.theme.AppColors
import java.util.Calendar
import java.util.Locale

// Shared reminder controls and the exact-alarm/notification permission gate.
// Both the global reminder (SettingsScreen) and per-goal reminders reuse these
// so the permission flow and day/time UI exist in exactly one place.

/**
 * Runs the notification + exact-alarm permission gate. Calls exactly one of:
 *  - [onGranted] when notifications are usable AND exact alarms can be scheduled
 *  - [onExactAlarmDenied] when notifications are fine but exact-alarm access is off
 *  - [onNotificationDenied] when notifications can't be used and we can't prompt
 * When the POST_NOTIFICATIONS runtime prompt is still available it is launched
 * instead, and the caller's launcher callback re-runs the gate on the result.
 */
internal fun runReminderPermissionGate(
    context: Context,
    notificationLauncher: ActivityResultLauncher<String>,
    onGranted: () -> Unit,
    onNotificationDenied: () -> Unit,
    onExactAlarmDenied: () -> Unit
) {
    if (areNotificationsFullyEnabled(context)) {
        if (!canScheduleExactAlarmsCompat(context)) {
            onExactAlarmDenied()
            return
        }
        onGranted()
        return
    }

    val canRequestPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && run {
        try {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) { false }
    }

    if (canRequestPermission) {
        try {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } catch (_: Exception) {
            onNotificationDenied()
        }
    } else {
        onNotificationDenied()
    }
}

internal fun areNotificationsFullyEnabled(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val granted = try {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) { false }
        if (!granted) return false
    }
    return try {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    } catch (_: Exception) { false }
}

internal fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
        data = Uri.parse("package:${context.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        } catch (_: Exception) { /* nothing more to do */ }
    }
}

internal fun openAppNotificationSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        } catch (_: Exception) { /* nothing more to do */ }
    }
}

@Composable
internal fun NotificationDeniedDialog(onDismiss: () -> Unit, onOpenSettings: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enable notifications", style = MaterialTheme.typography.titleMedium) },
        text = {
            Text(
                "To receive reminders, please enable notifications for FollowThru in your device Settings.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onOpenSettings) { Text("Open Notification Settings") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
internal fun ExactAlarmDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Allow exact alarms", style = MaterialTheme.typography.titleMedium) },
        text = {
            Text(
                "For accurate reminders, FollowThru needs permission to schedule exact alarms. Tap below to enable it in Settings — it takes just a second.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Enable in Settings") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
internal fun ReminderTimePickerButton(
    hour: Int,
    minute: Int,
    onTimeSelected: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    val timeLabel = formatReminderTime(hour, minute)
    TextButton(
        onClick = {
            try {
                TimePickerDialog(
                    context,
                    { _, h, m -> onTimeSelected(h, m) },
                    hour, minute, false
                ).show()
            } catch (_: Exception) { /* OEM theme issue — silent */ }
        },
        modifier = Modifier.semantics {
            contentDescription = "Reminder time, $timeLabel, double-tap to change"
        }
    ) {
        Text(
            text = timeLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.BrandAccentText
        )
    }
}

internal fun formatReminderTime(hour: Int, minute: Int): String {
    val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    val ampm = if (hour < 12) "AM" else "PM"
    return String.format(Locale.getDefault(), "%d:%02d %s", h, minute, ampm)
}

/**
 * The seven day-of-week toggle chips. Fits one row when ≥ 336dp is available,
 * otherwise falls back to a 4 + 3 two-row layout so every chip keeps a full
 * 48 × 48dp touch target. [onToggle] receives the day and whether it is
 * currently selected (before the toggle).
 */
@Composable
internal fun ReminderDayChips(
    selectedDays: Set<Int>,
    onToggle: (day: Int, currentlySelected: Boolean) -> Unit
) {
    val days = listOf(
        Triple(Calendar.MONDAY, "M", "Monday"),
        Triple(Calendar.TUESDAY, "T", "Tuesday"),
        Triple(Calendar.WEDNESDAY, "W", "Wednesday"),
        Triple(Calendar.THURSDAY, "T", "Thursday"),
        Triple(Calendar.FRIDAY, "F", "Friday"),
        Triple(Calendar.SATURDAY, "S", "Saturday"),
        Triple(Calendar.SUNDAY, "S", "Sunday")
    )
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val singleRow = maxWidth >= 48.dp * 7
        if (singleRow) {
            Row(modifier = Modifier.fillMaxWidth()) {
                days.forEach { (day, label, fullName) ->
                    val selected = day in selectedDays
                    ReminderDayChip(label, fullName, selected) { onToggle(day, selected) }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    days.take(4).forEach { (day, label, fullName) ->
                        val selected = day in selectedDays
                        ReminderDayChip(label, fullName, selected) { onToggle(day, selected) }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    days.drop(4).forEach { (day, label, fullName) ->
                        val selected = day in selectedDays
                        ReminderDayChip(label, fullName, selected) { onToggle(day, selected) }
                    }
                    // Keep row-2 chips the same width as row 1.
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * One day-of-week toggle chip. `weight(1f)` + `heightIn(min = 48.dp)` guarantee
 * a ≥ 48 × 48dp touch target; the caller sizes the row so the weighted width
 * never drops below 48dp.
 */
@Composable
private fun RowScope.ReminderDayChip(
    label: String,
    fullName: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .semantics {
                contentDescription = fullName
                stateDescription = if (selected) "Selected" else "Not selected"
                role = Role.Button
            },
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
