package com.ideasinc.followthrough.ui.goals

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ideasinc.followthrough.data.QuestionConfig
import com.ideasinc.followthrough.data.QuestionKeys
import com.ideasinc.followthrough.ui.checkin.placeholderFor
import com.ideasinc.followthrough.ui.theme.AppColors
import com.ideasinc.followthrough.ui.theme.DmSansFontFamily

@Composable
fun NewGoalFlowScreen(
    viewModel: NewGoalFlowViewModel,
    onNavigateBack: () -> Unit,
    onGoalCreated: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val backFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) { runCatching { backFocus.requestFocus() } }

    LaunchedEffect(uiState.shouldExit) {
        if (uiState.shouldExit) onNavigateBack()
    }
    LaunchedEffect(uiState.savedGoalId) {
        uiState.savedGoalId?.let { onGoalCreated(it) }
    }

    // Intercept the system back gesture only while answers exist — otherwise
    // back navigation proceeds normally and pops the flow.
    BackHandler(enabled = uiState.hasAnswers()) {
        viewModel.onSystemBack()
    }

    if (uiState.showDiscardDialog) {
        AlertDialog(
            onDismissRequest = viewModel::onKeepWriting,
            text = { Text("Discard your answers?", style = MaterialTheme.typography.bodyMedium) },
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
                val enabled = uiState.questionConfigs.indices
                    .filter { uiState.questionConfigs[it].isEnabled }
                val phase = uiState.phase as NewGoalPhase.CheckIn
                val stepLabel = "Step ${phase.stepIndex + 1} of ${enabled.size}"
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
            val phase = uiState.phase as NewGoalPhase.CheckIn
            val enabled = uiState.questionConfigs.indices
                .filter { uiState.questionConfigs[it].isEnabled }
            val isLast = phase.stepIndex == enabled.size - 1
            // Step 1 captures the goal title — block advancing while it's blank.
            val canProceed = phase.stepIndex != 0 || uiState.goalOrChange.isNotBlank()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        if (isLast) {
                            viewModel.onSave()
                        } else {
                            // Drop focus before advancing so the next step
                            // opens with the keyboard hidden and the
                            // placeholder fully visible. The user can tap
                            // the field to bring the IME back up.
                            focusManager.clearFocus()
                            viewModel.onNextCheckInStep()
                        }
                    },
                    enabled = canProceed,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(
                        if (isLast) "Save" else "Next",
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
            val phase = uiState.phase as NewGoalPhase.CheckIn
            val enabled = uiState.questionConfigs.indices
                .filter { uiState.questionConfigs[it].isEnabled }
            if (phase.stepIndex < enabled.size) {
                val config = uiState.questionConfigs[enabled[phase.stepIndex]]
                CheckInStepContent(
                    config = config,
                    value = getFieldValue(uiState, config.key),
                    onValueChange = { v -> setFieldValue(viewModel, config.key, v) }
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.CheckInStepContent(
    config: QuestionConfig,
    value: String,
    onValueChange: (String) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = config.label,
            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = DmSansFontFamily),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
    }
    Spacer(modifier = Modifier.height(24.dp))

    // The field stays unfocused on step entry — users tap to bring up the
    // keyboard, so the placeholder is visible when landing on the step.
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholderFor(config)) },
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = DmSansFontFamily),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color.Transparent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
    )
}

private fun getFieldValue(state: NewGoalFlowUiState, key: String): String = when (key) {
    QuestionKeys.GOAL_OR_CHANGE -> state.goalOrChange
    QuestionKeys.AVOIDING -> state.avoiding
    QuestionKeys.CONFIDENCE -> state.confidence
    QuestionKeys.MADE_PROGRESS -> state.madeProgress
    QuestionKeys.COMPETING_PRIORITY -> state.competingPriority
    QuestionKeys.IMPLEMENTATION_INTENTION -> state.implementationIntention
    QuestionKeys.ACCOUNTABILITY -> state.accountability
    else -> ""
}

private fun setFieldValue(vm: NewGoalFlowViewModel, key: String, value: String) = when (key) {
    QuestionKeys.GOAL_OR_CHANGE -> vm.onGoalOrChangeChange(value)
    QuestionKeys.AVOIDING -> vm.onAvoidingChange(value)
    QuestionKeys.CONFIDENCE -> vm.onConfidenceChange(value)
    QuestionKeys.MADE_PROGRESS -> vm.onMadeProgressChange(value)
    QuestionKeys.COMPETING_PRIORITY -> vm.onCompetingPriorityChange(value)
    QuestionKeys.IMPLEMENTATION_INTENTION -> vm.onImplementationIntentionChange(value)
    QuestionKeys.ACCOUNTABILITY -> vm.onAccountabilityChange(value)
    else -> {}
}

