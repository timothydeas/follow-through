package com.ideasinc.followthrough.ui.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ideasinc.followthrough.BuildConfig
import com.ideasinc.followthrough.R
import com.ideasinc.followthrough.di.AppContainer
import com.ideasinc.followthrough.feedback.AppReview
import com.ideasinc.followthrough.ui.theme.AppColors
import com.ideasinc.followthrough.ui.theme.PoppinsFontFamily
import com.ideasinc.followthrough.ui.theme.ThemeMode
import com.ideasinc.followthrough.ui.theme.ThemePreferences
import kotlinx.coroutines.launch
import com.ideasinc.followthrough.navigation.KEY_BIOMETRIC_ENABLED
import com.ideasinc.followthrough.navigation.PREFS_NAME


@Composable
fun SettingsScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onReplayIntro: () -> Unit = {}
) {
    val context = LocalContext.current

    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var biometricEnabled by remember { mutableStateOf(prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)) }
    val backFocus = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { runCatching { backFocus.requestFocus() } }

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

            // Display section — text size (100–200%), reduce motion.
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(
                    text = "Display",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp).semantics { heading() }
                )
                val persistedScale by SettingsPreferences.textScale.collectAsState()
                // Drive the thumb from local state so dragging is smooth; reflow text
                // live by updating the in-memory scale every frame, and persist to
                // disk only when the drag ends (per-frame disk writes broke the drag).
                var sliderValue by remember { mutableStateOf(persistedScale) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Text size",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${(sliderValue * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                androidx.compose.material3.Slider(
                    value = sliderValue,
                    onValueChange = {
                        sliderValue = it
                        SettingsPreferences.setTextScaleLive(it)
                    },
                    onValueChangeFinished = { SettingsPreferences.setTextScale(context, sliderValue) },
                    valueRange = 1.0f..2.0f,
                    modifier = Modifier.semantics {
                        contentDescription = "Text size, ${(sliderValue * 100).toInt()} percent"
                    }
                )
                Text(
                    text = "Aa — preview at this size",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val reduceMotion by SettingsPreferences.reduceMotion.collectAsState()
                SettingsSwitchRow(
                    label = "Reduce motion",
                    checked = reduceMotion,
                    onCheckedChange = { SettingsPreferences.setReduceMotion(context, it) }
                )
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
                    painter = painterResource(id = R.drawable.ic_chevron_right),
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

