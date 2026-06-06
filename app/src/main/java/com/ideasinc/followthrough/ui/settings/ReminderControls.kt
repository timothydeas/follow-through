package com.ideasinc.followthrough.ui.settings

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ideasinc.followthrough.notifications.canScheduleExactAlarmsCompat
import com.ideasinc.followthrough.ui.theme.AppColors
import java.util.Calendar
import java.util.Locale

// Shared reminder controls and the exact-alarm/notification permission gate.
// Both the global reminder (SettingsScreen) and per-goal reminders reuse these
// so the permission flow and day/time UI exist in exactly one place.

/**
 * The reminder permissions that are currently missing. A reminder needs BOTH
 * notification access (POST_NOTIFICATIONS on 13+, plus app-level notifications
 * enabled) AND exact-alarm access (canScheduleExactAlarms on 12+, denied by
 * default on 14+). [rememberReminderPermissionFlow] uses this to gather every
 * missing permission up front instead of discovering them one trip at a time.
 */
internal data class MissingReminderPermissions(
    val notifications: Boolean,
    val exactAlarm: Boolean
) {
    val any: Boolean get() = notifications || exactAlarm
    val count: Int get() = (if (notifications) 1 else 0) + (if (exactAlarm) 1 else 0)
}

internal fun missingReminderPermissions(context: Context): MissingReminderPermissions =
    MissingReminderPermissions(
        notifications = !areNotificationsFullyEnabled(context),
        exactAlarm = !canScheduleExactAlarmsCompat(context)
    )

/**
 * True when the POST_NOTIFICATIONS runtime prompt can still be shown (Android
 * 13+ and the permission isn't already granted). When this is false the user
 * must enable notifications from the system Settings screen instead.
 */
internal fun canRequestNotificationRuntime(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && try {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
    } catch (_: Exception) { false }

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

// ─── Permission flow ──────────────────────────────────────────────────────
// One coherent "enable a reminder" flow shared by the global reminder
// (SettingsScreen) and per-goal reminders (GoalReminderControls). It persists
// the user's "wants reminder on" intent, gathers EVERY missing permission up
// front, and drives notifications → exact alarms in a single sequence. On every
// return to the app it re-checks all permissions and either advances to the next
// missing one or finalizes the toggle ON automatically — so the user never has
// to toggle twice or take a surprise second trip to Settings.

private const val PREFS_REMINDER_FLOW = "grounded_reminder_flow"
private fun keyPending(k: String) = "pending_$k"
private fun keyNotifSettingsSent(k: String) = "notif_settings_sent_$k"
private fun keyExactSettingsSent(k: String) = "exact_settings_sent_$k"
private fun keyNotifRuntimeTried(k: String) = "notif_runtime_tried_$k"

/** Handle returned by [rememberReminderPermissionFlow]. */
internal class ReminderPermissionFlow internal constructor(
    private val onStart: () -> Unit,
    private val onCancel: () -> Unit
) {
    /** Call when the user toggles the reminder ON. Persists intent and begins
     *  walking through any missing permissions. */
    fun start() = onStart()

    /** Call when the user toggles the reminder OFF, to drop any in-flight intent
     *  so a half-finished permission walk doesn't later flip the toggle back on. */
    fun cancel() = onCancel()
}

/**
 * Creates the reminder-permission flow for one target, identified by [intentKey]
 * (a goalId for per-goal reminders, or a fixed string like "global"). [onEnabled]
 * is invoked exactly once when every required permission is in place — it should
 * flip the caller's toggle ON and schedule the alarm. The flow renders its own
 * dialogs, registers the POST_NOTIFICATIONS launcher, and observes ON_RESUME, so
 * the caller only has to wire [ReminderPermissionFlow.start]/[cancel] to its
 * Switch.
 */
@Composable
internal fun rememberReminderPermissionFlow(
    intentKey: String,
    onEnabled: () -> Unit
): ReminderPermissionFlow {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(PREFS_REMINDER_FLOW, Context.MODE_PRIVATE)
    }
    val currentOnEnabled by rememberUpdatedState(onEnabled)

    var showPreflight by remember(intentKey) { mutableStateOf(false) }
    var showNotifSettings by remember(intentKey) { mutableStateOf(false) }
    var showExactSettings by remember(intentKey) { mutableStateOf(false) }

    // The launcher is created below but referenced by advance(); a holder breaks
    // the cycle so advance() can be defined first.
    val launcherHolder = remember { mutableStateOf<ActivityResultLauncher<String>?>(null) }

    fun clearFlowState() {
        prefs.edit()
            .remove(keyPending(intentKey))
            .remove(keyNotifSettingsSent(intentKey))
            .remove(keyExactSettingsSent(intentKey))
            .remove(keyNotifRuntimeTried(intentKey))
            .apply()
    }

    // The single decision point. Idempotent: safe to call from the launcher
    // result and ON_RESUME, possibly both. Each system Settings trip is gated
    // behind a user-tapped dialog, and the runtime prompt is fired at most once,
    // so re-entrancy never double-launches anything.
    fun advance() {
        if (!prefs.getBoolean(keyPending(intentKey), false)) return
        val missing = missingReminderPermissions(context)
        if (!missing.any) {
            clearFlowState()
            currentOnEnabled()
            return
        }
        if (missing.notifications) {
            val runtimeTried = prefs.getBoolean(keyNotifRuntimeTried(intentKey), false)
            if (canRequestNotificationRuntime(context) && !runtimeTried) {
                prefs.edit().putBoolean(keyNotifRuntimeTried(intentKey), true).apply()
                val launcher = launcherHolder.value
                if (launcher != null) {
                    try {
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        return
                    } catch (_: Exception) { /* fall through to the Settings path */ }
                }
            }
            if (!prefs.getBoolean(keyNotifSettingsSent(intentKey), false)) {
                showNotifSettings = true
            } else {
                // Back from notification Settings still not granted — the user
                // declined. Stop here; the toggle stays in its true (off) state.
                clearFlowState()
            }
            return
        }
        // Notifications are satisfied; only exact-alarm access remains.
        if (!prefs.getBoolean(keyExactSettingsSent(intentKey), false)) {
            showExactSettings = true
        } else {
            clearFlowState()
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Re-evaluate regardless of the boolean result — areNotificationsFully-
        // Enabled() is the source of truth and advance() picks the next step.
        advance()
    }
    launcherHolder.value = notificationLauncher

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, intentKey) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) advance()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showPreflight) {
        ReminderPreflightDialog(
            onContinue = {
                showPreflight = false
                advance()
            },
            onCancel = {
                showPreflight = false
                clearFlowState()
            }
        )
    }
    if (showNotifSettings) {
        NotificationDeniedDialog(
            onDismiss = {
                showNotifSettings = false
                clearFlowState()
            },
            onOpenSettings = {
                showNotifSettings = false
                prefs.edit().putBoolean(keyNotifSettingsSent(intentKey), true).apply()
                openAppNotificationSettings(context)
            }
        )
    }
    if (showExactSettings) {
        ExactAlarmDialog(
            onDismiss = {
                showExactSettings = false
                clearFlowState()
            },
            onConfirm = {
                showExactSettings = false
                prefs.edit().putBoolean(keyExactSettingsSent(intentKey), true).apply()
                openExactAlarmSettings(context)
            }
        )
    }

    return remember(intentKey) {
        ReminderPermissionFlow(
            onStart = {
                // Persist intent and reset any stale stage flags from a prior
                // abandoned attempt, then gather all missing permissions up front.
                prefs.edit()
                    .putBoolean(keyPending(intentKey), true)
                    .remove(keyNotifSettingsSent(intentKey))
                    .remove(keyExactSettingsSent(intentKey))
                    .remove(keyNotifRuntimeTried(intentKey))
                    .apply()
                val missing = missingReminderPermissions(context)
                when {
                    !missing.any -> {
                        clearFlowState()
                        currentOnEnabled()
                    }
                    // More than one missing: explain the whole sequence first so
                    // the second Settings trip isn't a surprise.
                    missing.count >= 2 -> showPreflight = true
                    else -> advance()
                }
            },
            onCancel = { clearFlowState() }
        )
    }
}

@Composable
internal fun ReminderPreflightDialog(onContinue: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Turn on reminders", style = MaterialTheme.typography.titleMedium) },
        text = {
            Text(
                "Reminders need two quick permissions — to post notifications and to " +
                    "schedule exact alarms. We'll walk you through both, one screen at a time.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = { TextButton(onClick = onContinue) { Text("Continue") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Not now") } }
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
 * The seven day-of-week toggle circles. Selected = coral fill with a white (dark
 * in dark mode) label; unselected = an outlined circle. Fits one row when ≥ 336dp
 * is available, otherwise falls back to a 4 + 3 two-row layout so every circle
 * keeps a full 48 × 48dp touch target. [onToggle] receives the day and whether it
 * is currently selected (before the toggle).
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                days.forEach { (day, label, fullName) ->
                    val selected = day in selectedDays
                    ReminderDayChip(label, fullName, selected) { onToggle(day, selected) }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    days.take(4).forEach { (day, label, fullName) ->
                        val selected = day in selectedDays
                        ReminderDayChip(label, fullName, selected) { onToggle(day, selected) }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    days.drop(4).forEach { (day, label, fullName) ->
                        val selected = day in selectedDays
                        ReminderDayChip(label, fullName, selected) { onToggle(day, selected) }
                    }
                }
            }
        }
    }
}

/**
 * One day-of-week toggle circle. The 48dp outer Box is the touch target; the
 * 42dp inner circle is the visual chip — coral-filled when selected, outlined
 * otherwise.
 */
@Composable
private fun ReminderDayChip(
    label: String,
    fullName: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = fullName
                stateDescription = if (selected) "Selected" else "Not selected"
                role = Role.Button
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .then(
                    if (selected) Modifier.background(MaterialTheme.colorScheme.primary)
                    else Modifier.border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
