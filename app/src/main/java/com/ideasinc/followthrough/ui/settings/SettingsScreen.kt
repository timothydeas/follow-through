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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ideasinc.followthrough.BuildConfig
import com.ideasinc.followthrough.di.AppContainer
import com.ideasinc.followthrough.notifications.PREFS_REMINDERS
import com.ideasinc.followthrough.notifications.ReminderScheduler
import com.ideasinc.followthrough.notifications.canScheduleExactAlarmsCompat
import com.ideasinc.followthrough.ui.theme.PoppinsFontFamily
import com.ideasinc.followthrough.ui.theme.PrimaryForge
import java.util.Calendar
import java.util.Locale
import com.ideasinc.followthrough.navigation.KEY_BIOMETRIC_ENABLED
import com.ideasinc.followthrough.navigation.PREFS_NAME

private const val KEY_REMINDERS_PENDING_PERMISSION = "reminders_pending_permission"

@Composable
fun SettingsScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onCustomizeQuestions: () -> Unit
) {
    val context = LocalContext.current
    val settingsVm: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(container.questionLabelDao, context)
    )
    val uiState by settingsVm.uiState.collectAsState()

    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val remindersPrefs = remember { context.getSharedPreferences(PREFS_REMINDERS, Context.MODE_PRIVATE) }
    var biometricEnabled by remember { mutableStateOf(prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)) }
    var showBiometricTooltip by remember { mutableStateOf(false) }
    var showRemindersTooltip by remember { mutableStateOf(false) }
    val backFocus = remember { FocusRequester() }
    val biometricInfoFocus = remember { FocusRequester() }
    val remindersInfoFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { backFocus.requestFocus() } }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val pending = remindersPrefs.getBoolean(KEY_REMINDERS_PENDING_PERMISSION, false)
                if (pending && canScheduleExactAlarmsCompat(context)) {
                    val currentState = settingsVm.uiState.value
                    settingsVm.setRemindersEnabled(true)
                    try {
                        ReminderScheduler.scheduleReminders(
                            context.applicationContext,
                            currentState.reminderHour,
                            currentState.reminderMinute,
                            currentState.reminderDays
                        )
                    } catch (_: Exception) { /* scheduling failed — leave toggle on */ }
                    remindersPrefs.edit().putBoolean(KEY_REMINDERS_PENDING_PERMISSION, false).apply()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showNotificationDeniedDialog by remember { mutableStateOf(false) }
    var showExactAlarmDialog by remember { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && areNotificationsFullyEnabled(context)) {
            if (canScheduleExactAlarmsCompat(context)) {
                settingsVm.setRemindersEnabled(true)
                try {
                    ReminderScheduler.scheduleReminders(
                        context.applicationContext,
                        uiState.reminderHour,
                        uiState.reminderMinute,
                        uiState.reminderDays
                    )
                } catch (_: Exception) { /* scheduling failed — leave toggle off */ }
            } else {
                showExactAlarmDialog = true
            }
        } else {
            showNotificationDeniedDialog = true
        }
    }

    if (showNotificationDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationDeniedDialog = false },
            title = { Text("Enable notifications", style = MaterialTheme.typography.titleMedium) },
            text = {
                Text(
                    "To receive reminders, please enable notifications for Follow Through in your device Settings.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showNotificationDeniedDialog = false
                    openAppNotificationSettings(context)
                }) { Text("Open Notification Settings") }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationDeniedDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showExactAlarmDialog) {
        AlertDialog(
            onDismissRequest = { showExactAlarmDialog = false },
            title = { Text("Allow exact alarms", style = MaterialTheme.typography.titleMedium) },
            text = {
                Text(
                    "For accurate reminders, Follow Through needs permission to schedule exact alarms. Tap below to enable it in Settings — it takes just a second.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showExactAlarmDialog = false
                    remindersPrefs.edit().putBoolean(KEY_REMINDERS_PENDING_PERMISSION, true).apply()
                    openExactAlarmSettings(context)
                }) { Text("Enable in Settings") }
            },
            dismissButton = {
                TextButton(onClick = { showExactAlarmDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showRemindersTooltip) {
        val confirmFocus = remember { FocusRequester() }
        LaunchedEffect(Unit) { runCatching { confirmFocus.requestFocus() } }
        AlertDialog(
            onDismissRequest = {
                showRemindersTooltip = false
                runCatching { remindersInfoFocus.requestFocus() }
            },
            text = {
                Text(
                    "Reminders require notifications to be enabled for this app. Some older Android devices may deliver reminders at slightly different times due to battery optimization settings.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemindersTooltip = false
                        runCatching { remindersInfoFocus.requestFocus() }
                    },
                    modifier = Modifier.focusRequester(confirmFocus)
                ) { Text("Got it") }
            }
        )
    }

    if (showBiometricTooltip) {
        val confirmFocus = remember { FocusRequester() }
        LaunchedEffect(Unit) { runCatching { confirmFocus.requestFocus() } }
        AlertDialog(
            onDismissRequest = {
                showBiometricTooltip = false
                runCatching { biometricInfoFocus.requestFocus() }
            },
            text = {
                Text(
                    "This uses your phone's existing security — face recognition, fingerprint, or PIN. No new password is created. If biometrics aren't set up on your device, you'll be prompted for your device PIN instead.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBiometricTooltip = false
                        runCatching { biometricInfoFocus.requestFocus() }
                    },
                    modifier = Modifier.focusRequester(confirmFocus)
                ) { Text("Got it") }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.focusRequester(backFocus)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Go back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .semantics { heading() }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            // Biometric section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Lock Follow Through with Face ID or Device PIN",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = PoppinsFontFamily),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { showBiometricTooltip = true },
                        modifier = Modifier
                            .size(48.dp)
                            .focusRequester(biometricInfoFocus)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "More information",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Switch(
                    checked = biometricEnabled,
                    onCheckedChange = { enabled ->
                        biometricEnabled = enabled
                        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
                    },
                    modifier = Modifier.semantics {
                        contentDescription =
                            "Lock Follow Through with Face ID or Device PIN"
                        stateDescription = if (biometricEnabled) "On" else "Off"
                        role = Role.Switch
                    }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            // Customize questions row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClickLabel = "Open Customize Questions",
                        role = Role.Button,
                        onClick = onCustomizeQuestions
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Customize questions",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = PoppinsFontFamily),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            // Reminders section
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Text(
                        text = "Reminders",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .semantics { heading() }
                    )
                    IconButton(
                        onClick = { showRemindersTooltip = true },
                        modifier = Modifier
                            .size(48.dp)
                            .focusRequester(remindersInfoFocus)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "More information",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Enable reminders",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = uiState.remindersEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                enableReminders(
                                    context = context,
                                    settingsVm = settingsVm,
                                    uiState = uiState,
                                    notificationLauncher = notificationLauncher,
                                    onNotificationDenied = { showNotificationDeniedDialog = true },
                                    onExactAlarmDenied = { showExactAlarmDialog = true }
                                )
                            } else {
                                settingsVm.setRemindersEnabled(false)
                                remindersPrefs.edit().putBoolean(KEY_REMINDERS_PENDING_PERMISSION, false).apply()
                                try {
                                    ReminderScheduler.cancelReminders(context.applicationContext)
                                } catch (_: Exception) { /* ignore */ }
                            }
                        },
                        modifier = Modifier.semantics {
                            contentDescription = "Enable reminders"
                            stateDescription = if (uiState.remindersEnabled) "On" else "Off"
                            role = Role.Switch
                        }
                    )
                }

                if (uiState.remindersEnabled) {
                    Column(
                        modifier = Modifier.padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Time picker
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
                            TimePickerButton(
                                hour = uiState.reminderHour,
                                minute = uiState.reminderMinute,
                                onTimeSelected = { h, m ->
                                    settingsVm.setReminderTime(h, m)
                                    if (uiState.remindersEnabled) {
                                        try {
                                            ReminderScheduler.scheduleReminders(
                                                context.applicationContext, h, m, uiState.reminderDays
                                            )
                                        } catch (_: Exception) { /* ignore */ }
                                    }
                                }
                            )
                        }

                        // Day selector
                        Text(
                            text = "Days",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                            days.forEach { (day, label, fullName) ->
                                val selected = day in uiState.reminderDays
                                TextButton(
                                    onClick = {
                                        settingsVm.toggleDay(day)
                                        if (uiState.remindersEnabled) {
                                            val newDays = uiState.reminderDays.toMutableSet()
                                            if (selected) newDays.remove(day) else newDays.add(day)
                                            try {
                                                ReminderScheduler.scheduleReminders(
                                                    context.applicationContext,
                                                    uiState.reminderHour,
                                                    uiState.reminderMinute,
                                                    newDays
                                                )
                                            } catch (_: Exception) { /* ignore */ }
                                        }
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) PrimaryForge else MaterialTheme.colorScheme.surfaceVariant)
                                        .semantics {
                                            contentDescription = fullName
                                            stateDescription = if (selected) "Selected" else "Not selected"
                                        },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(
                    text = "About",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .semantics { heading() }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Version ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            onClickLabel = "Open privacy policy",
                            role = Role.Button,
                            onClick = {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://timothydeas.github.io/follow-through/privacy-policy.html")
                                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                runCatching { context.startActivity(intent) }
                            }
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Privacy Policy",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "All data stays on your device. We collect nothing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }
    }
}

private fun enableReminders(
    context: Context,
    settingsVm: SettingsViewModel,
    uiState: SettingsUiState,
    notificationLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    onNotificationDenied: () -> Unit,
    onExactAlarmDenied: () -> Unit
) {
    // Eager gate: on the very first toggle tap, decide synchronously whether
    // notifications are usable. The toggle never flips on (and the time/day
    // picker never appears) unless this returns true.
    if (areNotificationsFullyEnabled(context)) {
        if (!canScheduleExactAlarmsCompat(context)) {
            onExactAlarmDenied()
            return
        }
        settingsVm.setRemindersEnabled(true)
        try {
            ReminderScheduler.scheduleReminders(
                context.applicationContext,
                uiState.reminderHour,
                uiState.reminderMinute,
                uiState.reminderDays
            )
        } catch (_: Exception) { /* scheduling failed — leave toggle on */ }
        return
    }

    // Notifications are not fully enabled. If we can still ask for runtime
    // permission (Android 13+, not yet granted), launch the system prompt.
    // Otherwise the user has disabled notifications in system settings —
    // surface the deep-link dialog immediately.
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

private fun openExactAlarmSettings(context: Context) {
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

private fun areNotificationsFullyEnabled(context: Context): Boolean {
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

private fun openAppNotificationSettings(context: Context) {
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
private fun TimePickerButton(hour: Int, minute: Int, onTimeSelected: (Int, Int) -> Unit) {
    val context = LocalContext.current
    TextButton(onClick = {
        try {
            TimePickerDialog(
                context,
                { _, h, m -> onTimeSelected(h, m) },
                hour, minute, false
            ).show()
        } catch (_: Exception) { /* OEM theme issue — silent */ }
    }) {
        Text(
            text = formatTime(hour, minute),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun formatTime(hour: Int, minute: Int): String {
    val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    val ampm = if (hour < 12) "AM" else "PM"
    return String.format(Locale.getDefault(), "%d:%02d %s", h, minute, ampm)
}
