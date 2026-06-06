package com.ideasinc.followthrough.ui.goals

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ideasinc.followthrough.R
import com.ideasinc.followthrough.data.QuestionConfig
import com.ideasinc.followthrough.data.QuestionKeys
import com.ideasinc.followthrough.notifications.GoalReminderScheduler
import com.ideasinc.followthrough.ui.checkin.QuestionField
import com.ideasinc.followthrough.ui.checkin.ReflectMoreToggle
import com.ideasinc.followthrough.ui.checkin.ReflectionTextField
import com.ideasinc.followthrough.ui.checkin.placeholderFor
import com.ideasinc.followthrough.ui.rememberA11yAnnouncer
import com.ideasinc.followthrough.ui.theme.AppColors
import com.ideasinc.followthrough.ui.theme.DmSansFontFamily

// The reflection prompts offered behind "Strengthen your plan", in ALL_KEYS
// order. Deliberately excludes madeProgress (a brand-new goal has no progress
// to report) and the two structural fields handled above: the goal name and the
// action plan.
private val DEEPER_KEYS = listOf(
    QuestionKeys.AVOIDING,
    QuestionKeys.CONFIDENCE,
    QuestionKeys.COMPETING_PRIORITY,
    QuestionKeys.ACCOUNTABILITY
)

/**
 * Creating a goal is a single lead-light screen, mirroring the check-in: name it
 * and plan your first move (the only things that matter to start), with the
 * reflection prompts invited — not imposed — behind "Strengthen your plan". The
 * goal name is the one hard requirement; everything else can save blank.
 */
@Composable
fun NewGoalFlowScreen(
    viewModel: NewGoalFlowViewModel,
    onNavigateBack: () -> Unit,
    onGoalCreated: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val backFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val announce = rememberA11yAnnouncer()

    var reflectMore by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { runCatching { backFocus.requestFocus() } }

    LaunchedEffect(uiState.shouldExit) {
        if (uiState.shouldExit) {
            // Flow abandoned — drop any reminder the user set up in-flow so a
            // goal that never got saved leaves no orphaned alarm behind.
            GoalReminderScheduler.remove(context, viewModel.newGoalId)
            onNavigateBack()
        }
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

    // Goal name and action plan are structural to creation — always shown, even
    // if a user disabled them as reflection prompts in Customize Questions.
    val nameConfig = remember(uiState.questionConfigs) {
        uiState.questionConfigs.firstOrNull { it.key == QuestionKeys.GOAL_OR_CHANGE }
    }
    val deeperConfigs = uiState.questionConfigs.filter { it.isEnabled && it.key in DEEPER_KEYS }
    val canSave = uiState.goalOrChange.isNotBlank()

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
                        painter = painterResource(id = R.drawable.ic_arrow_left),
                        contentDescription = "Go back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "New goal",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .semantics { heading() }
                )
            }
        },
        bottomBar = {
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
                        focusManager.clearFocus()
                        viewModel.onSave()
                    },
                    enabled = canSave,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(
                        "Save goal",
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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1) The goal name — the only hard requirement.
            GoalNameSection(
                config = nameConfig,
                value = uiState.goalOrChange,
                onValueChange = viewModel::onGoalOrChangeChange,
                showRequiredHint = !canSave
            )

            // 2) The action plan — the core lever, kept as the centerpiece.
            ActionPlanSection(
                value = uiState.implementationIntention,
                onValueChange = viewModel::onImplementationIntentionChange,
                goalId = viewModel.newGoalId
            )

            // 3) Optional depth — the reflection prompts, invited not imposed.
            if (deeperConfigs.isNotEmpty()) {
                ReflectMoreToggle(
                    expanded = reflectMore,
                    onToggle = {
                        reflectMore = !reflectMore
                        if (reflectMore) announce("Showing more prompts")
                    },
                    collapsedLabel = "Strengthen your plan",
                    expandedLabel = "Show less"
                )
                Text(
                    text = "Optional — a few prompts to spot what might get in the way, so you can plan around it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (reflectMore) {
                    deeperConfigs.forEach { cfg ->
                        QuestionField(
                            config = cfg,
                            value = getFieldValue(uiState, cfg.key),
                            onValueChange = { v -> setFieldValue(viewModel, cfg.key, v) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalNameSection(
    config: QuestionConfig?,
    value: String,
    onValueChange: (String) -> Unit,
    showRequiredHint: Boolean
) {
    // resolveConfigs always supplies goalOrChange, but guard defensively.
    val label = config?.label ?: QuestionKeys.DEFAULT_LABELS[QuestionKeys.GOAL_OR_CHANGE].orEmpty()
    val placeholder = config?.let { placeholderFor(it) }
        ?: QuestionKeys.DEFAULT_PLACEHOLDERS[QuestionKeys.GOAL_OR_CHANGE].orEmpty()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = DmSansFontFamily),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() }
        )
        ReflectionTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            description = label
        )
        if (showRequiredHint) {
            // Visible + announced reason the Save button is disabled, so the
            // gate is never a silent mystery (don't rely on the disabled button).
            Text(
                text = "Name your goal to save",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
        }
    }
}

/**
 * The action plan — the core step. Two labelled inputs ("When …" / "I will …")
 * stored as the single [implementationIntention] string via [joinIntention], so
 * the reminder notification and the check-in record read one sentence. Once a
 * plan is written, the optional "Remind me for this" reminder appears. Rendered
 * flat in the same field idiom as the rest of the screen — distinguished by its
 * heading, not a popping bordered card.
 */
@Composable
private fun ActionPlanSection(
    value: String,
    onValueChange: (String) -> Unit,
    goalId: String
) {
    // Seed the two fields from the stored sentence once on entry; thereafter the
    // fields are the source of truth and we push the concatenation up to the VM.
    val initial = remember(goalId) { splitIntention(value) }
    var whenText by remember(goalId) { mutableStateOf(initial.first) }
    var actionText by remember(goalId) { mutableStateOf(initial.second) }

    fun push() = onValueChange(joinIntention(whenText, actionText))

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Your action plan",
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = DmSansFontFamily),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() }
        )
        LabeledPlanInput(
            label = "When…",
            value = whenText,
            placeholder = "e.g., I pour my morning coffee",
            onValueChange = { whenText = it; push() }
        )
        LabeledPlanInput(
            label = "I will…",
            value = actionText,
            placeholder = "e.g., make a breakfast I look forward to",
            onValueChange = { actionText = it; push() }
        )
        if (value.isNotBlank()) {
            GoalReminderControls(
                goalId = goalId,
                reminderBody = value.trim(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** A sub-label above one action-plan field, using the shared field so its
 *  keyboard/scroll behaviour matches every other input on the screen. */
@Composable
private fun LabeledPlanInput(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = DmSansFontFamily),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ReflectionTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            description = label
        )
    }
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
