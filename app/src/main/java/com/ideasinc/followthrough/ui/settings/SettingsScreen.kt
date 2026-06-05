package com.ideasinc.followthrough.ui.settings

import android.content.Context
import android.util.Log
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ideasinc.followthrough.BuildConfig
import com.ideasinc.followthrough.R
import com.ideasinc.followthrough.di.AppContainer
import com.ideasinc.followthrough.feedback.AppReview
import com.ideasinc.followthrough.notifications.PREFS_REMINDERS
import com.ideasinc.followthrough.notifications.ReminderScheduler
import com.ideasinc.followthrough.notifications.canScheduleExactAlarmsCompat
import com.ideasinc.followthrough.ui.theme.AppColors
import com.ideasinc.followthrough.ui.theme.PoppinsFontFamily
import com.ideasinc.followthrough.ui.theme.ThemeMode
import com.ideasinc.followthrough.ui.theme.ThemePreferences
import kotlinx.coroutines.launch
import com.ideasinc.followthrough.navigation.KEY_BIOMETRIC_ENABLED
import com.ideasinc.followthrough.navigation.PREFS_NAME

private const val KEY_REMINDERS_PENDING_PERMISSION = "reminders_pending_permission"
private const val TAG = "SettingsScreen"

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
    val backFocus = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val permissionDeniedMessage = "Permission required. Please enable in Settings."
    LaunchedEffect(Unit) { runCatching { backFocus.requestFocus() } }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val pending = remindersPrefs.getBoolean(KEY_REMINDERS_PENDING_PERMISSION, false)
                val canSchedule = canScheduleExactAlarmsCompat(context)
                Log.d(TAG, "ON_RESUME fired — pending=$pending canScheduleExactAlarms=$canSchedule")
                if (pending && canSchedule) {
                    val currentState = settingsVm.uiState.value
                    Log.d(TAG, "Auto-enabling reminders after permission grant")
                    settingsVm.setRemindersEnabled(true)
                    try {
                        ReminderScheduler.scheduleReminders(
                            context.applicationContext,
                            currentState.reminderHour,
                            currentState.reminderMinute,
                            currentState.reminderDays
                        )
                        Log.d(TAG, "Alarm scheduled after auto-enable")
                    } catch (e: Exception) {
                        Log.w(TAG, "Scheduling failed after auto-enable", e)
                    }
                    remindersPrefs.edit().putBoolean(KEY_REMINDERS_PENDING_PERMISSION, false).apply()
                    Log.d(TAG, "Pending flag cleared after auto-enable")
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
                remindersPrefs.edit().putBoolean(KEY_REMINDERS_PENDING_PERMISSION, false).apply()
            } else {
                showExactAlarmDialog = true
            }
        } else {
            remindersPrefs.edit().putBoolean(KEY_REMINDERS_PENDING_PERMISSION, false).apply()
            showNotificationDeniedDialog = true
            snackbarScope.launch {
                snackbarHostState.showSnackbar(
                    message = permissionDeniedMessage,
                    duration = SnackbarDuration.Short
                )
            }
        }
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
                remindersPrefs.edit().putBoolean(KEY_REMINDERS_PENDING_PERMISSION, true).apply()
                openExactAlarmSettings(context)
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        painter = painterResource(id = R.drawable.ic_arrow_left),
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
            HorizontalDivider(color = AppColors.Border)

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
                        text = "Lock FollowThru with Face ID or Device PIN",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = PoppinsFontFamily),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Switch(
                    checked = biometricEnabled,
                    onCheckedChange = { enabled ->
                        biometricEnabled = enabled
                        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        uncheckedTrackColor = AppColors.SwitchUncheckedTrack,
                        uncheckedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier.semantics {
                        contentDescription =
                            "Lock FollowThru with Face ID or Device PIN"
                        stateDescription = if (biometricEnabled) "On" else "Off"
                        role = Role.Switch
                    }
                )
            }

            HorizontalDivider(color = AppColors.Border)

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
                    painter = painterResource(id = R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            HorizontalDivider(color = AppColors.Border)

            // Appearance section
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .semantics { heading() }
                )
                val themeMode by ThemePreferences.mode.collectAsState()
                Column(modifier = Modifier.selectableGroup()) {
                    ThemeOptionRow("Light", ThemeMode.LIGHT, themeMode) {
                        ThemePreferences.setMode(context, it)
                    }
                    ThemeOptionRow("Dark", ThemeMode.DARK, themeMode) {
                        ThemePreferences.setMode(context, it)
                    }
                    ThemeOptionRow("System default", ThemeMode.SYSTEM, themeMode) {
                        ThemePreferences.setMode(context, it)
                    }
                }
            }

            HorizontalDivider(color = AppColors.Border)

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
                                Log.d(TAG, "Reminders toggle tapped ON — setting pending flag")
                                remindersPrefs.edit()
                                    .putBoolean(KEY_REMINDERS_PENDING_PERMISSION, true)
                                    .apply()
                                runReminderPermissionGate(
                                    context = context,
                                    notificationLauncher = notificationLauncher,
                                    onGranted = {
                                        settingsVm.setRemindersEnabled(true)
                                        try {
                                            ReminderScheduler.scheduleReminders(
                                                context.applicationContext,
                                                uiState.reminderHour,
                                                uiState.reminderMinute,
                                                uiState.reminderDays
                                            )
                                        } catch (_: Exception) { /* scheduling failed — leave toggle on */ }
                                        remindersPrefs.edit().putBoolean(KEY_REMINDERS_PENDING_PERMISSION, false).apply()
                                    },
                                    onNotificationDenied = {
                                        showNotificationDeniedDialog = true
                                        snackbarScope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = permissionDeniedMessage,
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    },
                                    onExactAlarmDenied = {
                                        showExactAlarmDialog = true
                                        snackbarScope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = permissionDeniedMessage,
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    }
                                )
                            } else {
                                Log.d(TAG, "Reminders toggle tapped OFF — clearing pending flag")
                                settingsVm.setRemindersEnabled(false)
                                remindersPrefs.edit().putBoolean(KEY_REMINDERS_PENDING_PERMISSION, false).apply()
                                try {
                                    ReminderScheduler.cancelReminders(context.applicationContext)
                                } catch (_: Exception) { /* ignore */ }
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
                            ReminderTimePickerButton(
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
                        ReminderDayChips(selectedDays = uiState.reminderDays) { day, selected ->
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
                        }
                    }
                }
            }

            HorizontalDivider(color = AppColors.Border)

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

                val primaryColor = AppColors.BrandAccentText
                val privacyText = buildAnnotatedString {
                    withLink(
                        LinkAnnotation.Url(
                            url = "https://timothydeas.github.io/follow-through/privacy-policy.html",
                            styles = TextLinkStyles(style = SpanStyle(color = primaryColor, textDecoration = TextDecoration.Underline))
                        )
                    ) {
                        append("Privacy Policy")
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = privacyText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Text(
                    text = "FollowThru stores everything only on your device and never uploads it to any cloud. If you uninstall, reset your phone, or switch devices, your data can't be recovered.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )

                Text(
                    text = "FollowThru is not a substitute for professional medical, psychological, or coaching advice.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Text(
                    text = "FollowThru uses research-backed questions to support personal reflection. All questions are optional and fully customizable — you are always in control.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            HorizontalDivider(color = AppColors.Border)

            // Independent, always-available feedback link (opt-in, no tracking).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClickLabel = "Send feedback by email",
                        role = Role.Button,
                        onClick = { AppReview.sendFeedback(context) }
                    )
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Send feedback",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = PoppinsFontFamily),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            HorizontalDivider(color = AppColors.Border)
        }
    }
}

/**
 * One light/dark theme choice rendered as a radio row. The whole row is a
 * single ≥ 48dp selectable target with `Role.RadioButton`, so TalkBack
 * announces the label and selected state together.
 */
@Composable
private fun ThemeOptionRow(
    label: String,
    mode: ThemeMode,
    selectedMode: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    val selected = mode == selectedMode
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(
                selected = selected,
                onClick = { onSelect(mode) },
                role = Role.RadioButton
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

