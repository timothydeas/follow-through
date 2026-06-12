package com.ideasinc.followthrough.ui.checkin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ideasinc.followthrough.data.CheckInType
import com.ideasinc.followthrough.notifications.CueImageStore
import com.ideasinc.followthrough.notifications.GoalReminderScheduler
import com.ideasinc.followthrough.notifications.deleteCueChannels
import com.ideasinc.followthrough.ui.ReassuranceOverlay
import com.ideasinc.followthrough.ui.goals.GoalReminderControls
import com.ideasinc.followthrough.ui.rememberA11yAnnouncer
import com.ideasinc.followthrough.ui.theme.AppColors
import com.ideasinc.followthrough.ui.theme.DmSansFontFamily

/**
 * A check-in: pick a type (barrier/progress), write the note, then set the
 * implementation intention — one focus per screen so the keyboard never pushes
 * content off-screen. Save lands on the goal's detail.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CheckInFlowScreen(
    viewModel: CheckInFlowViewModel,
    onNavigateBack: () -> Unit,
    onSaved: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val announce = rememberA11yAnnouncer()
    val backFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    LaunchedEffect(Unit) { runCatching { backFocus.requestFocus() } }

    LaunchedEffect(uiState.shouldExit) {
        if (uiState.shouldExit) {
            // Abandoned without saving — drop any reminder/cue authored in step 3
            // under this not-yet-saved check-in id so nothing is orphaned.
            GoalReminderScheduler.remove(context, uiState.checkInId)
            deleteCueChannels(context, uiState.checkInId)
            CueImageStore.deletePath(uiState.cueImagePath)
            onNavigateBack()
        }
    }
    LaunchedEffect(uiState.savedGoalId) {
        uiState.savedGoalId?.let { id ->
            announce("Check-in saved")
            (context as? android.app.Activity)?.let {
                com.ideasinc.followthrough.feedback.AppReview.onCheckInSaved(it)
            }
            onSaved(id)
        }
    }

    BackHandler(enabled = true) { viewModel.onSystemBack() }

    if (uiState.showDiscardDialog) {
        AlertDialog(
            onDismissRequest = viewModel::onKeepWriting,
            text = { Text("Discard this check-in?", style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(
                    onClick = viewModel::onDiscard,
                    colors = ButtonDefaults.textButtonColors(contentColor = AppColors.Destructive)
                ) { Text("Discard") }
            },
            dismissButton = {
                TextButton(
                    onClick = viewModel::onKeepWriting,
                    colors = ButtonDefaults.textButtonColors(contentColor = AppColors.BrandAccentText)
                ) { Text("Keep Writing") }
            }
        )
    }

    val isLastStep = uiState.step == CHECKIN_STEP_COUNT - 1

    Scaffold(
        modifier = Modifier.semantics {
            if (uiState.showReassurance) invisibleToUser()
        },
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
                    onClick = viewModel::onBack,
                    modifier = Modifier.focusRequester(backFocus)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Go back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                val stepLabel = "Step ${uiState.step + 1} of $CHECKIN_STEP_COUNT"
                Text(
                    text = stepLabel,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = DmSansFontFamily,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .semantics {
                            contentDescription = stepLabel
                            liveRegion = LiveRegionMode.Polite
                        }
                )
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        if (isLastStep) viewModel.onSave() else viewModel.onNext()
                    },
                    enabled = uiState.canProceed(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(
                        if (isLastStep) "Save check-in" else "Next",
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = DmSansFontFamily)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .padding(top = 32.dp, bottom = 24.dp)
        ) {
            when (uiState.step) {
                0 -> TypeStep(
                    selected = uiState.type,
                    onSelect = viewModel::onSelectType
                )
                1 -> NoteStep(
                    type = uiState.type,
                    value = uiState.note,
                    onValueChange = viewModel::onNoteChange
                )
                2 -> IntentionStep(
                    value = uiState.intention,
                    onValueChange = viewModel::onIntentionChange
                )
                else -> CueReminderStep(
                    checkInId = uiState.checkInId
                )
            }
        }
    }

    if (uiState.showReassurance) {
        ReassuranceOverlay(
            message = "You showed up. Trust the process.",
            onDismiss = viewModel::onReassuranceDone
        )
    }
}

@Composable
private fun ColumnScope.TypeStep(
    selected: String?,
    onSelect: (String) -> Unit
) {
    Text(
        text = "What kind of check-in?",
        style = MaterialTheme.typography.headlineMedium.copy(fontFamily = DmSansFontFamily),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth().semantics { heading() }
    )
    Spacer(modifier = Modifier.height(24.dp))
    Column(
        modifier = Modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TypeOption(
            title = "Barrier",
            subtitle = "Something getting in the way.",
            selected = selected == CheckInType.BARRIER,
            onSelect = { onSelect(CheckInType.BARRIER) }
        )
        TypeOption(
            title = "Progress",
            subtitle = "Something that went well.",
            selected = selected == CheckInType.PROGRESS,
            onSelect = { onSelect(CheckInType.PROGRESS) }
        )
    }
}

@Composable
private fun TypeOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else AppColors.Border
    val containerColor =
        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
            .selectable(
                selected = selected,
                onClick = onSelect,
                role = Role.RadioButton
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$title. $subtitle" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ColumnScope.NoteStep(
    type: String?,
    value: String,
    onValueChange: (String) -> Unit
) {
    val heading = if (type == CheckInType.PROGRESS) "What went well?" else "What's getting in the way?"
    val placeholder = if (type == CheckInType.PROGRESS) {
        "e.g., I made a start, even a small one."
    } else {
        "e.g., “I keep putting it off,” or “late meetings eat my evenings.”"
    }
    Text(
        text = heading,
        style = MaterialTheme.typography.headlineMedium.copy(fontFamily = DmSansFontFamily),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth().semantics { heading() }
    )
    Spacer(modifier = Modifier.height(24.dp))
    StepField(value = value, onValueChange = onValueChange, placeholder = placeholder, label = heading)
}

@Composable
private fun ColumnScope.IntentionStep(
    value: String,
    onValueChange: (String) -> Unit
) {
    Text(
        text = "Set your implementation intention",
        style = MaterialTheme.typography.headlineMedium.copy(fontFamily = DmSansFontFamily),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth().semantics { heading() }
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "When [cue], I will [action] — optional, and you can add it later.\n" +
            "e.g., When I pour my morning coffee, I'll text one person.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(24.dp))
    StepField(
        value = value,
        onValueChange = onValueChange,
        placeholder = "When …, I will …",
        label = "Implementation intention"
    )
}

/**
 * The optional final step: set a reminder for this check-in. Optional — saving with
 * nothing set is fine, and it can be edited later in the check-in editor. Scrolls,
 * since the reminder controls can be taller than one screen.
 */
@Composable
private fun ColumnScope.CueReminderStep(
    checkInId: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Reminder (optional)",
            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = DmSansFontFamily),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth().semantics { heading() }
        )
        Text(
            text = "Set a reminder for this check-in. Optional — you can skip it and add it later.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        GoalReminderControls(checkInId = checkInId)
    }
}

@Composable
private fun ColumnScope.StepField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = DmSansFontFamily),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .semantics { contentDescription = label }
    )
}
