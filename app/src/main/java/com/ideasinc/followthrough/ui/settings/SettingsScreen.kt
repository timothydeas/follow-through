package com.ideasinc.followthrough.ui.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ideasinc.followthrough.BuildConfig
import com.ideasinc.followthrough.R
import com.ideasinc.followthrough.data.GroundedDatabase
import com.ideasinc.followthrough.di.AppContainer
import com.ideasinc.followthrough.feedback.AppReview
import com.ideasinc.followthrough.notifications.ReminderAlarmScheduler
import com.ideasinc.followthrough.ui.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.ideasinc.followthrough.ui.theme.PoppinsFontFamily
import com.ideasinc.followthrough.ui.theme.ThemeMode
import com.ideasinc.followthrough.ui.theme.ThemePreferences
import com.ideasinc.followthrough.navigation.KEY_BIOMETRIC_ENABLED
import com.ideasinc.followthrough.navigation.KEY_REMINDERS_PAUSED
import com.ideasinc.followthrough.navigation.PREFS_NAME


@Composable
fun SettingsScreen(
    container: AppContainer,
    onReplayIntro: () -> Unit = {}
) {
    val context = LocalContext.current

    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var biometricEnabled by remember { mutableStateOf(prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)) }
    var remindersPaused by remember { mutableStateOf(prefs.getBoolean(KEY_REMINDERS_PAUSED, false)) }
    val scope = rememberCoroutineScope()
    var showDeleteData by remember { mutableStateOf(false) }

    if (showDeleteData) {
        AlertDialog(
            onDismissRequest = { showDeleteData = false },
            title = { Text("Delete all your data?") },
            text = { Text("This permanently removes every intention, cue, and follow-through record from this device. It can't be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteData = false
                        // Cancel any scheduled alarms first, then wipe every table.
                        scope.launch(Dispatchers.IO) {
                            val reminders = container.reminderDao.getActiveReminders().first()
                            reminders.forEach { ReminderAlarmScheduler.cancel(context, it.id) }
                            GroundedDatabase.getInstance(context).clearAllTables()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppColors.Destructive)
                ) { Text("Delete everything") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteData = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppColors.BrandAccentText)
                ) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Title styled to match Intentions and Progress. Settings is a top-level tab, so
            // there's no back arrow — the bottom bar / nav rail is the way back.
            Text(
                text = "Settings",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp)
                    .semantics { heading() }
            )
            HorizontalDivider(color = AppColors.Border)

            // Pause reminders — for a real break (e.g. vacation). Stops all firing without losing
            // the streak (paused weeks have no nudges, so the streak math skips them).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pause reminders",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = PoppinsFontFamily),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Going away? Stop reminders firing — your streak won't be affected. Turn back on anytime.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Switch(
                    checked = remindersPaused,
                    onCheckedChange = { paused ->
                        remindersPaused = paused
                        prefs.edit().putBoolean(KEY_REMINDERS_PAUSED, paused).apply()
                        // Re-run schedule() for every active reminder: with the pref now set it
                        // either re-arms (resumed) or cancels + no-ops (paused).
                        scope.launch(Dispatchers.IO) {
                            container.reminderDao.getActiveReminders().first()
                                .forEach { ReminderAlarmScheduler.schedule(context, it) }
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
                        contentDescription = "Pause reminders"
                        stateDescription = if (remindersPaused) "On" else "Off"
                        role = Role.Switch
                    }
                )
            }

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

            // Notifications section — cue sound.
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(
                    text = "Notifications",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp).semantics { heading() }
                )
                // The actual gate on whether reminders reach the user. Opens the system
                // app-notification settings so they can allow/repair the permission —
                // without it, fired reminders are silently dropped.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClickLabel = "Open notification settings", role = Role.Button) {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            )
                        }
                        .heightIn(min = 48.dp)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Reminder notifications",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = PoppinsFontFamily),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Allow notifications so your intentions can reach you in the moment. Tap to open system settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                val soundOn by SettingsPreferences.notificationSound.collectAsState()
                SettingsSwitchRow(
                    label = "Notification sound",
                    checked = soundOn,
                    onCheckedChange = { SettingsPreferences.setNotificationSound(context, it) }
                )
            }

            HorizontalDivider(color = AppColors.Border)

            // Replay the intro — re-shows the Welcome on next entry. No data reset.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClickLabel = "Replay the intro", role = Role.Button, onClick = onReplayIntro)
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Replay the intro",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = PoppinsFontFamily),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
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
                    text = "Everything stays only on your device — never uploaded anywhere, and not even we can see it. If you uninstall, reset your phone, or switch devices, your data can't be recovered.",
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
            }

            HorizontalDivider(color = AppColors.Border)

            // Your data — delete everything from this device (MVP_User_Flow_IA.md "My data").
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClickLabel = "Delete all my data", role = Role.Button, onClick = { showDeleteData = true })
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Delete all my data",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = PoppinsFontFamily),
                        color = AppColors.Destructive
                    )
                    Text(
                        text = "Permanently remove every intention, cue, and record from this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            HorizontalDivider(color = AppColors.Border)
        }
    }
}

/** A labelled ≥48dp switch row that announces label + on/off state to TalkBack. */
@Composable
private fun SettingsSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onPrimary,
                uncheckedTrackColor = AppColors.SwitchUncheckedTrack,
                uncheckedBorderColor = Color.Transparent
            ),
            modifier = Modifier.semantics {
                contentDescription = label
                stateDescription = if (checked) "On" else "Off"
                role = Role.Switch
            }
        )
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

