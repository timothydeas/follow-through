package com.ideasinc.followthrough.ui.goals

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ideasinc.followthrough.data.CheckIn
import com.ideasinc.followthrough.data.QuestionConfig
import com.ideasinc.followthrough.data.QuestionKeys
import com.ideasinc.followthrough.data.Step
import com.ideasinc.followthrough.ui.launch.insightDisplayDurationMs
import com.ideasinc.followthrough.ui.rememberA11yAnnouncer
import com.ideasinc.followthrough.ui.theme.AppColors
import com.ideasinc.followthrough.ui.theme.DmSansFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GoalDetailScreen(
    viewModel: GoalDetailViewModel,
    onBack: () -> Unit,
    onNavigateToList: () -> Unit,
    onAddCheckIn: () -> Unit,
    onCheckInClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showUndoDialog by remember { mutableStateOf(false) }
    val backFocus = remember { FocusRequester() }
    val deleteTriggerFocus = remember { FocusRequester() }
    val editTriggerFocus = remember { FocusRequester() }
    val announce = rememberA11yAnnouncer()

    LaunchedEffect(Unit) {
        runCatching { backFocus.requestFocus() }
    }

    LaunchedEffect(uiState.shouldNavigateToList) {
        if (uiState.shouldNavigateToList) {
            announce("Deleted")
            onNavigateToList()
        }
    }

    if (showDeleteDialog) {
        val confirmFocus = remember { FocusRequester() }
        LaunchedEffect(Unit) { runCatching { confirmFocus.requestFocus() } }
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                runCatching { deleteTriggerFocus.requestFocus() }
            },
            text = {
                Text(
                    "Delete this goal and all its check-ins?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteDialog = false; viewModel.deleteGoal() },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppColors.Destructive),
                    modifier = Modifier.focusRequester(confirmFocus)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        runCatching { deleteTriggerFocus.requestFocus() }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = AppColors.BrandAccentText
                    )
                ) { Text("Cancel") }
            }
        )
    }

    if (uiState.showEditDialog) {
        val titleFocus = remember { FocusRequester() }
        LaunchedEffect(Unit) { runCatching { titleFocus.requestFocus() } }
        AlertDialog(
            onDismissRequest = {
                viewModel.dismissEditDialog()
                runCatching { editTriggerFocus.requestFocus() }
            },
            title = { Text("Edit goal", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Column {
                    OutlinedTextField(
                        value = uiState.editTitle,
                        onValueChange = viewModel::onEditTitleChange,
                        label = { Text("Goal title") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(titleFocus)
                    )
                    if (uiState.editTitle.isBlank()) {
                        Text(
                            text = "Title cannot be empty.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.saveGoalEdit()
                        runCatching { editTriggerFocus.requestFocus() }
                    },
                    enabled = uiState.editTitle.isNotBlank(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = AppColors.BrandAccentText,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissEditDialog()
                        runCatching { editTriggerFocus.requestFocus() }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = AppColors.BrandAccentText
                    )
                ) { Text("Cancel") }
            }
        )
    }

    if (showUndoDialog) {
        AlertDialog(
            onDismissRequest = { showUndoDialog = false },
            text = {
                Text(
                    "Undo follow-through?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUndoDialog = false
                        viewModel.undoFollowThrough()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = AppColors.BrandAccentText
                    )
                ) { Text("Undo") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showUndoDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = AppColors.BrandAccentText
                    )
                ) { Text("Cancel") }
            }
        )
    }

    if (uiState.showStepDialog) {
        val stepFocus = remember { FocusRequester() }
        LaunchedEffect(Unit) { runCatching { stepFocus.requestFocus() } }
        val isEditingStep = uiState.stepEditorTargetId != null
        AlertDialog(
            onDismissRequest = viewModel::dismissStepDialog,
            title = {
                Text(
                    if (isEditingStep) "Edit step" else "Add step",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                OutlinedTextField(
                    value = uiState.stepDialogText,
                    onValueChange = viewModel::onStepDialogTextChange,
                    label = { Text("Step") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(stepFocus)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::saveStep,
                    enabled = uiState.stepDialogText.isNotBlank(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = AppColors.BrandAccentText,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(
                    onClick = viewModel::dismissStepDialog,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = AppColors.BrandAccentText
                    )
                ) { Text("Cancel") }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                    text = uiState.goal?.title ?: "",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .semantics { heading() }
                )
                IconButton(
                    onClick = viewModel::showEditDialog,
                    modifier = Modifier.focusRequester(editTriggerFocus)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.focusRequester(deleteTriggerFocus)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete goal",
                        tint = AppColors.Destructive,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddCheckIn,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.semantics { contentDescription = "Add check-in" }
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val goal = uiState.goal
            if (goal != null) {
                val followedThrough = goal.followedThrough
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = {
                            if (followedThrough) showUndoDialog = true
                            else viewModel.followThrough()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .semantics {
                                // Stays tappable once followed through — a tap then
                                // opens the undo confirmation dialog.
                                contentDescription =
                                    if (followedThrough)
                                        "Followed through. Double tap to undo."
                                    else "I followed through"
                            }
                    ) {
                        Icon(
                            imageVector = if (followedThrough) Icons.Filled.CheckCircle
                                else Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (followedThrough) "Followed through ✓" else "I followed through",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = DmSansFontFamily
                            )
                        )
                    }
                }

                // Sticky Steps summary. Sits above the check-ins LazyColumn so it
                // stays visible regardless of how many check-ins have accumulated.
                // Collapsed by default — only the progress summary shows. Tap to
                // expand and reveal the full Steps list with add/edit/delete.
                var stepsExpanded by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    StepsSummary(
                        steps = uiState.steps,
                        expanded = stepsExpanded,
                        onToggleExpanded = { stepsExpanded = !stepsExpanded },
                        onAddStep = viewModel::showAddStepDialog,
                        onToggleStep = viewModel::toggleStep,
                        onEditStep = viewModel::showEditStepDialog,
                        onDeleteStep = viewModel::deleteStep
                    )
                }
            }
            // Check-ins are the primary content and live in their own scroller
            // below the sticky Steps summary above.
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 4.dp,
                    bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (uiState.checkIns.isEmpty()) {
                    item {
                        Text(
                            text = "No check-ins yet. Tap + to add one.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                        )
                    }
                } else {
                    items(uiState.checkIns, key = { it.id }) { checkIn ->
                        CheckInCard(
                            checkIn = checkIn,
                            configs = uiState.questionConfigs,
                            onClick = { onCheckInClick(checkIn.id) }
                        )
                    }
                }
            }
        }
    }

    if (uiState.showReassurance) {
        ReassuranceOverlay(onDismiss = viewModel::onReassuranceDone)
    }
    }
}

@Composable
private fun CheckInCard(
    checkIn: CheckIn,
    configs: List<QuestionConfig>,
    onClick: () -> Unit
) {
    fun labelFor(key: String) = configs.firstOrNull { it.key == key }?.label
        ?: QuestionKeys.DEFAULT_LABELS[key] ?: key

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(
                onClickLabel = "Open check-in to edit",
                role = Role.Button,
                onClick = onClick
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!checkIn.madeProgress.isNullOrBlank()) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = labelFor(QuestionKeys.MADE_PROGRESS),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = checkIn.madeProgress,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Tap to edit",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = formatDate(checkIn.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Steps (sub-goals) ─────────────────────────────────────────────────────

@Composable
private fun StepsSummary(
    steps: List<Step>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onAddStep: () -> Unit,
    onToggleStep: (Step) -> Unit,
    onEditStep: (Step) -> Unit,
    onDeleteStep: (String) -> Unit
) {
    val completed = steps.count { it.isCompleted }
    val total = steps.size

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        if (total == 0) {
            // Empty state — a quiet "+ Add steps" link inside the summary card.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(
                        onClickLabel = "Add steps",
                        role = Role.Button,
                        onClick = onAddStep
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Add steps",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val progressLabel = "$completed of $total steps completed"
            val toggleLabel = if (expanded) "Collapse steps" else "Expand steps"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(
                        onClickLabel = toggleLabel,
                        role = Role.Button,
                        onClick = onToggleExpanded
                    )
                    .semantics(mergeDescendants = true) {
                        contentDescription = "$progressLabel. $toggleLabel."
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(completed.toFloat() / total)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = progressLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp
                        else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (expanded) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Steps",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = DmSansFontFamily
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .semantics { heading() }
                        )
                        IconButton(
                            onClick = onAddStep,
                            modifier = Modifier
                                .size(36.dp)
                                .semantics { contentDescription = "Add step" }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    steps.forEach { step ->
                        StepRow(
                            step = step,
                            onToggle = { onToggleStep(step) },
                            onEdit = { onEditStep(step) },
                            onDelete = { onDeleteStep(step.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepRow(
    step: Step,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .toggleable(
                    value = step.isCompleted,
                    role = Role.Checkbox,
                    onValueChange = { onToggle() }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = step.isCompleted,
                onCheckedChange = null
            )
            Text(
                text = step.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (step.isCompleted)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    MaterialTheme.colorScheme.onSurface,
                textDecoration = if (step.isCompleted)
                    TextDecoration.LineThrough else null,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        IconButton(
            onClick = onEdit,
            modifier = Modifier.semantics { contentDescription = "Edit step" }
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier.semantics { contentDescription = "Delete step" }
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = AppColors.Destructive,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ReassuranceOverlay(onDismiss: () -> Unit) {
    val message = "You followed through. Whatever happens next — you showed up for yourself. That's what matters."
    val context = LocalContext.current

    // Auto-dismiss via a plain Handler so the countdown lives outside the
    // composition — no LaunchedEffect, no coroutine state, nothing that
    // could trigger recomposition during the countdown. TalkBack discovers
    // the message naturally via clearAndSetSemantics contentDescription.
    // When an accessibility service is active the timer is skipped entirely so
    // the user can read at their own pace and dismiss with a tap.
    DisposableEffect(Unit) {
        val a11yManager =
            context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        val handler = Handler(Looper.getMainLooper())
        val runnable = Runnable { onDismiss() }
        if (a11yManager?.isEnabled != true) {
            handler.postDelayed(runnable, insightDisplayDurationMs(message))
        }
        onDispose {
            handler.removeCallbacks(runnable)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.OverlayScrim)
            .clickable(onClickLabel = "Dismiss", onClick = onDismiss)
            // Whole overlay is one opaque accessibility node so TalkBack
            // reads only the reassurance message, not the inner card/Text.
            // onClick is re-declared to preserve double-tap-to-dismiss.
            .clearAndSetSemantics {
                contentDescription = message
                onClick(label = "Dismiss") {
                    onDismiss()
                    true
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .padding(32.dp)
                .background(AppColors.OverlaySurface, RoundedCornerShape(20.dp))
                .padding(horizontal = 32.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = DmSansFontFamily,
                    color = AppColors.OnOverlaySurface
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

private val dateFormatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
private fun formatDate(epochMs: Long): String = dateFormatter.format(Date(epochMs))
