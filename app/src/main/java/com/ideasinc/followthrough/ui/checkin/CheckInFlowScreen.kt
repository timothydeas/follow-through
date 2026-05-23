package com.ideasinc.followthrough.ui.checkin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
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
import com.ideasinc.followthrough.ui.rememberA11yAnnouncer
import com.ideasinc.followthrough.ui.theme.AppColors
import com.ideasinc.followthrough.ui.theme.DmSansFontFamily

@Composable
fun CheckInFlowScreen(
    viewModel: CheckInFlowViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val announce = rememberA11yAnnouncer()
    val backFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) { runCatching { backFocus.requestFocus() } }

    LaunchedEffect(uiState.shouldExit, uiState.didSave) {
        if (uiState.didSave) announce("Check-in saved")
        if (uiState.shouldExit) onNavigateBack()
    }

    val enabled = uiState.activeStepIndices()

    val onCurrentStep = enabled.isNotEmpty() && uiState.currentStepIndex < enabled.size
    val currentConfig: QuestionConfig? =
        if (onCurrentStep) uiState.questionConfigs[enabled[uiState.currentStepIndex]] else null
    val isLast = onCurrentStep && uiState.currentStepIndex == enabled.size - 1

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
            Column(modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
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
                    if (enabled.isNotEmpty()) {
                        Text(
                            text = "Step ${uiState.currentStepIndex + 1} of ${enabled.size}",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = DmSansFontFamily,
                                letterSpacing = 0.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .semantics {
                                    contentDescription =
                                        "Step ${uiState.currentStepIndex + 1} of ${enabled.size}"
                                    liveRegion = LiveRegionMode.Polite
                                }
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (onCurrentStep) {
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
                        onClick = if (isLast) viewModel::onSave else viewModel::onNext,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            if (isLast) "Save" else "Next",
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = DmSansFontFamily)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        if (currentConfig != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(top = 32.dp, bottom = 24.dp)
            ) {
                StepContent(
                    config = currentConfig,
                    value = getFieldValue(uiState, currentConfig.key),
                    onValueChange = { v -> setFieldValue(viewModel, currentConfig.key, v) }
                )
            }
        }
    }
}

@Composable
private fun StepContent(
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
    // Fixed gap between the question and the input. The parent Column scrolls,
    // so a long question and a long answer can both grow without the field
    // ever crowding or overlapping the heading.
    Spacer(modifier = Modifier.height(28.dp))

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
            .heightIn(min = 240.dp)
    )
}

private fun getFieldValue(state: CheckInFlowUiState, key: String): String = when (key) {
    QuestionKeys.GOAL_OR_CHANGE -> state.goalOrChange
    QuestionKeys.AVOIDING -> state.avoiding
    QuestionKeys.CONFIDENCE -> state.confidence
    QuestionKeys.MADE_PROGRESS -> state.madeProgress
    QuestionKeys.COMPETING_PRIORITY -> state.competingPriority
    QuestionKeys.IMPLEMENTATION_INTENTION -> state.implementationIntention
    QuestionKeys.ACCOUNTABILITY -> state.accountability
    else -> ""
}

private fun setFieldValue(vm: CheckInFlowViewModel, key: String, value: String) = when (key) {
    QuestionKeys.GOAL_OR_CHANGE -> vm.onGoalOrChangeChange(value)
    QuestionKeys.AVOIDING -> vm.onAvoidingChange(value)
    QuestionKeys.CONFIDENCE -> vm.onConfidenceChange(value)
    QuestionKeys.MADE_PROGRESS -> vm.onMadeProgressChange(value)
    QuestionKeys.COMPETING_PRIORITY -> vm.onCompetingPriorityChange(value)
    QuestionKeys.IMPLEMENTATION_INTENTION -> vm.onImplementationIntentionChange(value)
    QuestionKeys.ACCOUNTABILITY -> vm.onAccountabilityChange(value)
    else -> {}
}

// Effective placeholder for a question — the user's custom placeholder when
// set, otherwise the built-in default. Resolution happens in resolveConfigs;
// the per-key default text lives in QuestionKeys.DEFAULT_PLACEHOLDERS.
internal fun placeholderFor(config: QuestionConfig): String = config.placeholder
