package com.example.grounded.ui.checkin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.grounded.data.QuestionConfig
import com.example.grounded.data.QuestionKeys
import com.example.grounded.ui.rememberA11yAnnouncer
import com.example.grounded.ui.theme.DmSansFontFamily
import com.example.grounded.ui.theme.PrimaryForge

@Composable
fun CheckInFlowScreen(
    viewModel: CheckInFlowViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val announce = rememberA11yAnnouncer()
    val backFocus = remember { FocusRequester() }
    var showGlobalInfo by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { runCatching { backFocus.requestFocus() } }

    LaunchedEffect(uiState.shouldExit, uiState.didSave) {
        if (uiState.didSave) announce("Check-in saved")
        if (uiState.shouldExit) onNavigateBack()
    }

    val enabled = uiState.activeStepIndices()

    if (showGlobalInfo) {
        AlertDialog(
            onDismissRequest = { showGlobalInfo = false },
            text = {
                Text(
                    "These questions are a starting point, not a checklist. Leave any blank and keep going. You can edit your answers or customize the questions in Settings.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = { TextButton(onClick = { showGlobalInfo = false }) { Text("Got it") } }
        )
    }

    val onCurrentStep = enabled.isNotEmpty() && uiState.currentStepIndex < enabled.size
    val currentConfig: QuestionConfig? =
        if (onCurrentStep) uiState.questionConfigs[enabled[uiState.currentStepIndex]] else null
    val isLast = onCurrentStep && uiState.currentStepIndex == enabled.size - 1

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
                    IconButton(
                        onClick = { showGlobalInfo = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "More information",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                if (uiState.goalTitle.isNotBlank()) {
                    Text(
                        text = uiState.goalTitle,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = DmSansFontFamily
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 8.dp)
                    )
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
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryForge)
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
private fun ColumnScope.StepContent(
    config: QuestionConfig,
    value: String,
    onValueChange: (String) -> Unit
) {
    var showStepTooltip by remember(config.key) { mutableStateOf(false) }
    val fieldFocus = remember { FocusRequester() }

    if (showStepTooltip) {
        AlertDialog(
            onDismissRequest = { showStepTooltip = false },
            text = { Text(tooltipFor(config.key), style = MaterialTheme.typography.bodyMedium) },
            confirmButton = { TextButton(onClick = { showStepTooltip = false }) { Text("Got it") } }
        )
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = config.label,
            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = DmSansFontFamily),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        val tooltip = tooltipFor(config.key)
        if (tooltip.isNotBlank()) {
            IconButton(onClick = { showStepTooltip = true }, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "More information",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(24.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { fieldFocus.requestFocus() }
    ) {
        val placeholder = placeholderFor(config.key)
        if (value.isEmpty() && placeholder.isNotBlank()) {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = DmSansFontFamily,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = DmSansFontFamily,
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(PrimaryForge),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(fieldFocus)
                .semantics { contentDescription = config.label }
        )
    }
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

internal fun tooltipFor(key: String): String = when (key) {
    QuestionKeys.GOAL_OR_CHANGE ->
        "This can be anything — a goal, a habit, a pattern, something you want to do differently. There are no wrong answers here."
    QuestionKeys.IMPLEMENTATION_INTENTION ->
        "Link a specific moment to a specific action. The more vivid and specific the cue, the more likely you are to follow through when that moment arrives."
    QuestionKeys.ACCOUNTABILITY ->
        "Naming who or what holds you accountable makes it real. It could be a person who believes in you, a past experience, or your own inner voice."
    else -> ""
}

internal fun placeholderFor(key: String): String = when (key) {
    QuestionKeys.GOAL_OR_CHANGE ->
        "A goal, a habit, something you want to change, or something you're struggling with"
    QuestionKeys.AVOIDING ->
        "Something you've been putting off looking at, even though part of you knows it matters"
    QuestionKeys.CONFIDENCE ->
        "You don't need proof you can do this before you start. What does your gut say?"
    QuestionKeys.MADE_PROGRESS ->
        "Yes, No, or describe where you feel you are right now"
    QuestionKeys.COMPETING_PRIORITY ->
        "Be honest — sometimes our perception or anticipation of a situation matters more than the situation itself."
    QuestionKeys.IMPLEMENTATION_INTENTION ->
        "When I feel the urge to back out, I will remind myself I've been here before and I know what to do."
    QuestionKeys.ACCOUNTABILITY ->
        "A person, a memory, a strategy — whatever keeps you going"
    else -> ""
}
