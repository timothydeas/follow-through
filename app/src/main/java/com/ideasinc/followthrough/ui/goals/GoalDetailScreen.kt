package com.ideasinc.followthrough.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ideasinc.followthrough.R
import com.ideasinc.followthrough.data.CheckIn
import com.ideasinc.followthrough.data.CheckInType
import com.ideasinc.followthrough.notifications.CueImageStore
import com.ideasinc.followthrough.notifications.GoalReminderScheduler
import com.ideasinc.followthrough.notifications.deleteCueChannels
import com.ideasinc.followthrough.ui.ReassuranceOverlay
import com.ideasinc.followthrough.ui.rememberA11yAnnouncer
import com.ideasinc.followthrough.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GoalDetailScreen(
    viewModel: GoalDetailViewModel,
    onBack: () -> Unit,
    onNavigateToList: () -> Unit,
    onAddCheckIn: () -> Unit,
    onOpenCheckIn: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    val backFocus = remember { FocusRequester() }
    val deleteTriggerFocus = remember { FocusRequester() }
    val editTriggerFocus = remember { FocusRequester() }
    val announce = rememberA11yAnnouncer()

    LaunchedEffect(Unit) { runCatching { backFocus.requestFocus() } }

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
                    onClick = {
                        showDeleteDialog = false
                        // Drop every check-in's reminder, sound channels, cue image.
                        uiState.checkIns.forEach { c ->
                            GoalReminderScheduler.remove(context, c.id)
                            deleteCueChannels(context, c.id)
                            CueImageStore.deletePath(c.cueImagePath)
                        }
                        viewModel.deleteGoal()
                    },
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
                    colors = ButtonDefaults.textButtonColors(contentColor = AppColors.BrandAccentText)
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
                    colors = ButtonDefaults.textButtonColors(contentColor = AppColors.BrandAccentText)
                ) { Text("Cancel") }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
            val goal = uiState.goal
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (goal != null) {
                    item {
                        FollowThroughButton(
                            followedThrough = goal.followedThrough,
                            onFollowThrough = { viewModel.followThrough() }
                        )
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionLabel("CHECK-INS")
                            FilterRow(selected = uiState.filter, onSelect = viewModel::setFilter)
                        }
                    }

                    val log = uiState.filteredCheckIns
                    if (log.isEmpty()) {
                        item {
                            Text(
                                text = if (uiState.checkIns.isEmpty()) {
                                    "No check-ins yet. Tap + to add one."
                                } else {
                                    "No check-ins of this kind yet."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                            )
                        }
                    } else {
                        items(log, key = { it.id }) { checkIn ->
                            CheckInLogCard(checkIn = checkIn, onClick = { onOpenCheckIn(checkIn.id) })
                        }
                    }
                }
            }
        }

        if (uiState.showReassurance) {
            ReassuranceOverlay(
                message = "You followed through. Whatever happens next — you showed up for yourself. That's what matters.",
                onDismiss = viewModel::onReassuranceDone
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.semantics { heading() }
    )
}

@Composable
private fun FilterRow(selected: CheckInFilter, onSelect: (CheckInFilter) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterPill("All", selected == CheckInFilter.ALL) { onSelect(CheckInFilter.ALL) }
        FilterPill("Barriers", selected == CheckInFilter.BARRIER) { onSelect(CheckInFilter.BARRIER) }
        FilterPill("Progress", selected == CheckInFilter.PROGRESS) { onSelect(CheckInFilter.PROGRESS) }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = AppColors.Border,
            selectedBorderColor = MaterialTheme.colorScheme.primary
        )
    )
}

/**
 * One log entry — its type tag, the note, an intention/cue/reminder hint, the date.
 * Opens the check-in to view and edit.
 */
@Composable
private fun CheckInLogCard(checkIn: CheckIn, onClick: () -> Unit) {
    val context = LocalContext.current
    val isProgress = checkIn.type == CheckInType.PROGRESS
    val tagLabel = if (isProgress) "Progress" else "Barrier"
    val hasReminder = remember(checkIn.id, checkIn.updatedAt) {
        GoalReminderScheduler.read(context, checkIn.id)?.enabled == true
    }
    val a11y = buildString {
        append("$tagLabel. ${checkIn.note}")
        checkIn.intention.takeIf { it.isNotBlank() }?.let { append(". Intention: $it") }
        if (hasReminder) append(". Reminder on")
        append(". ${formatDate(checkIn.createdAt)}")
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClickLabel = "Open check-in", onClick = onClick)
            .padding(16.dp)
            .semantics(mergeDescendants = true) { contentDescription = a11y },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            checkIn.cueEmoji?.takeIf { it.isNotBlank() }?.let {
                Text(text = it, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(6.dp))
            }
            TypeTag(label = tagLabel, isProgress = isProgress)
            Spacer(modifier = Modifier.weight(1f))
            if (hasReminder) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_bell),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 6.dp).size(18.dp)
                )
            }
            Text(
                text = formatDate(checkIn.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = checkIn.note,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        checkIn.intention.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** A small pill tagging a log entry as a barrier or progress note. */
@Composable
private fun TypeTag(label: String, isProgress: Boolean) {
    val dotColor = if (isProgress) AppColors.Gold else MaterialTheme.colorScheme.primary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** The "I followed through" primary. Undo lives on "Your FollowThrus". */
@Composable
private fun FollowThroughButton(
    followedThrough: Boolean,
    onFollowThrough: () -> Unit
) {
    Button(
        onClick = { if (!followedThrough) onFollowThrough() },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .semantics {
                contentDescription =
                    if (followedThrough) "Followed through" else "I followed through"
            }
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_check_circle),
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (followedThrough) "Followed through" else "I followed through",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 2,
            textAlign = TextAlign.Center
        )
    }
}

private val dateFormatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
private fun formatDate(epochMs: Long): String = dateFormatter.format(Date(epochMs))
