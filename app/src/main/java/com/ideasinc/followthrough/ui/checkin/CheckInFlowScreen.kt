package com.ideasinc.followthrough.ui.checkin

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ideasinc.followthrough.data.QuestionConfig
import com.ideasinc.followthrough.data.QuestionKeys
import com.ideasinc.followthrough.ui.ConfidenceSlider
import com.ideasinc.followthrough.ui.goals.GoalReminderControls
import com.ideasinc.followthrough.ui.goals.PlanField
import com.ideasinc.followthrough.ui.goals.joinIntention
import com.ideasinc.followthrough.ui.goals.splitIntention
import com.ideasinc.followthrough.ui.rememberA11yAnnouncer
import com.ideasinc.followthrough.ui.theme.AppColors
import com.ideasinc.followthrough.ui.theme.DmSansFontFamily

/**
 * A check-in is a lead-light, per-screen flow: one focus per screen so the
 * keyboard never pushes content off-screen (the single weighted field shrinks to
 * fit the space above the IME). The lead screen is the core prompt ("made
 * progress?") and offers both "Save" (a quick note is enough) and "Reflect more",
 * which opens the deeper prompts, each on its own screen. The "update your plan"
 * step uses the same two-field "When… / I will…" editor as goal creation.
 */
@Composable
fun CheckInFlowScreen(
    viewModel: CheckInFlowViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val announce = rememberA11yAnnouncer()
    val backFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    LaunchedEffect(Unit) { runCatching { backFocus.requestFocus() } }

    LaunchedEffect(uiState.shouldExit, uiState.didSave) {
        if (uiState.didSave) {
            announce("Check-in saved")
            // Honest, non-sentiment-gated Play review prompt after genuine use.
            (context as? android.app.Activity)?.let {
                com.ideasinc.followthrough.feedback.AppReview.onCheckInSaved(it)
            }
        }
        if (uiState.shouldExit) onNavigateBack()
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

    val path = uiState.pathStepKeys()
    val core = uiState.coreStepKeys()
    val deeper = uiState.deeperStepKeys()
    val onCurrentStep = path.isNotEmpty() && uiState.currentStepIndex < path.size
    val stepIndex = uiState.currentStepIndex.coerceIn(0, (path.size - 1).coerceAtLeast(0))
    val currentKey = if (onCurrentStep) path.getOrNull(stepIndex) else null
    val currentConfig = currentKey?.let { key -> uiState.questionConfigs.firstOrNull { it.key == key } }

    // The lead screen (last core step) is the decision point: save now, or reflect
    // more. Once the deeper path is entered, that screen advances instead.
    val atDecisionPoint = onCurrentStep && stepIndex == core.size - 1 &&
        !uiState.reflectMore && deeper.isNotEmpty()
    val isLastStep = onCurrentStep && stepIndex == path.size - 1

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
                if (path.isNotEmpty()) {
                    // Progress reflects the path the user is actually on: the lead
                    // count by default, expanding once "Reflect more" is tapped.
                    val stepLabel = "Step ${stepIndex + 1} of ${path.size}"
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
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // "Reflect more" leads onward, so it sits ABOVE Save: Save must
                // read as the final action, not be tapped before the user even
                // sees the option to go deeper.
                if (atDecisionPoint) {
                    TextButton(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.onReflectMore()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = AppColors.BrandAccentText)
                    ) {
                        Text(
                            "Reflect more",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = AppColors.BrandAccentText
                        )
                    }
                }
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        if (atDecisionPoint || isLastStep) viewModel.onSave() else viewModel.onNext()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        if (atDecisionPoint || isLastStep) "Save" else "Next",
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = DmSansFontFamily)
                    )
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
                if (currentConfig.key == QuestionKeys.IMPLEMENTATION_INTENTION) {
                    // Updating the plan uses the same two-field editor + optional
                    // per-goal reminder as goal creation, pre-filled with the
                    // goal's current plan.
                    IntentionStep(
                        seedPlan = uiState.goalIntention.orEmpty(),
                        onValueChange = viewModel::onImplementationIntentionChange,
                        goalId = uiState.goalId,
                        checkInId = uiState.checkInId
                    )
                } else {
                    StepContent(
                        config = currentConfig,
                        value = getFieldValue(uiState, currentConfig.key),
                        onValueChange = { v -> setFieldValue(viewModel, currentConfig.key, v) }
                    )
                }
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
    Text(
        text = config.label,
        style = MaterialTheme.typography.headlineMedium.copy(fontFamily = DmSansFontFamily),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { heading() }
    )
    Spacer(modifier = Modifier.height(24.dp))

    if (config.key == QuestionKeys.CONFIDENCE) {
        // Confidence is a 0–100 slider, not a text field (ported prototype).
        // The question's placeholder doubles as the slider's helper line.
        Text(
            text = placeholderFor(config),
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = DmSansFontFamily),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        ConfidenceSlider(value = value, onValueChange = onValueChange)
        return
    }

    // The field stays unfocused on step entry — users tap to bring up the
    // keyboard, so the placeholder is visible when landing on the step. The
    // single weighted field fills the space above the keyboard rather than
    // being pushed off-screen behind it.
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
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .semantics { contentDescription = config.label }
    )
}

/**
 * The "update your plan" step — the same two labelled "When… / I will…" inputs and
 * optional "Remind me for this" reminder as goal creation. Pre-filled with the
 * goal's current plan ([seedPlan]) so the user edits it rather than an empty box;
 * edits are pushed up as the single `implementationIntention` string. Scrolls so
 * both fields and the reminder stay reachable with the keyboard up.
 */
@Composable
private fun ColumnScope.IntentionStep(
    seedPlan: String,
    onValueChange: (String) -> Unit,
    goalId: String,
    checkInId: String
) {
    val scrollState = rememberScrollState()
    val initial = remember(checkInId) { splitIntention(seedPlan) }
    var whenText by remember(checkInId) { mutableStateOf(initial.first) }
    var actionText by remember(checkInId) { mutableStateOf(initial.second) }

    fun push() = onValueChange(joinIntention(whenText, actionText))
    // The plan currently shown in the fields — seeded from the goal's plan, then
    // whatever the user types. Drives the reminder body and its visibility.
    val effectivePlan = joinIntention(whenText, actionText)

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Update your plan",
            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = DmSansFontFamily),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { heading() }
        )
        Spacer(modifier = Modifier.height(24.dp))
        PlanField(
            label = "When… (the moment or situation)",
            value = whenText,
            placeholder = "e.g., I pour my morning coffee",
            onValueChange = { whenText = it; push() }
        )
        Spacer(modifier = Modifier.height(16.dp))
        PlanField(
            label = "I will… (what you'll do)",
            value = actionText,
            placeholder = "e.g., make a breakfast I look forward to",
            onValueChange = { actionText = it; push() }
        )
        if (effectivePlan.isNotBlank()) {
            Spacer(modifier = Modifier.height(24.dp))
            GoalReminderControls(
                goalId = goalId,
                reminderBody = effectivePlan.trim(),
                modifier = Modifier.fillMaxWidth()
            )
        }
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

// Effective placeholder for a question — the user's custom placeholder when
// set, otherwise the built-in default. Resolution happens in resolveConfigs;
// the per-key default text lives in QuestionKeys.DEFAULT_PLACEHOLDERS.
internal fun placeholderFor(config: QuestionConfig): String = config.placeholder
