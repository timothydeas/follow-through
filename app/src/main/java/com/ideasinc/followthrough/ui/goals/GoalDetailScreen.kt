package com.ideasinc.followthrough.ui.goals

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ideasinc.followthrough.data.CheckIn
import com.ideasinc.followthrough.data.QuestionConfig
import com.ideasinc.followthrough.data.QuestionKeys
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
                        onClick = { if (!followedThrough) viewModel.followThrough() },
                        enabled = !followedThrough,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            // Explicit disabled colors match enabled so the visual
                            // state doesn't change — the button stays solid red with
                            // a checkmark; only TalkBack reports it as disabled.
                            disabledContainerColor = MaterialTheme.colorScheme.primary,
                            disabledContentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .semantics {
                                contentDescription =
                                    if (followedThrough) "Followed through"
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
            }
            if (uiState.checkIns.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No check-ins yet.\nTap + to add one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
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
