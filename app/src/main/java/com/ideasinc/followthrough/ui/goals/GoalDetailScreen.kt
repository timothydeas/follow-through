package com.ideasinc.followthrough.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ideasinc.followthrough.R
import com.ideasinc.followthrough.data.MotivationType
import com.ideasinc.followthrough.data.PassionInterest
import com.ideasinc.followthrough.data.Reminder
import com.ideasinc.followthrough.notifications.ReminderAlarmScheduler
import com.ideasinc.followthrough.ui.ReassuranceOverlay
import com.ideasinc.followthrough.ui.rememberA11yAnnouncer
import com.ideasinc.followthrough.ui.theme.AppColors
import androidx.compose.ui.ExperimentalComposeUiApi
import kotlinx.coroutines.launch

/**
 * Goal detail — the spine's "why" home. Sections: Why this matters · Barriers ·
 * Progress notes · Reminders for this goal. "Add reminder" launches the builder
 * pre-filled with this goal.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GoalDetailScreen(
    viewModel: GoalDetailViewModel,
    onBack: () -> Unit,
    onNavigateToList: () -> Unit,
    onAddReminder: () -> Unit,
    onOpenReminder: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showBarrierDialog by remember { mutableStateOf(false) }
    var showProgressDialog by remember { mutableStateOf(false) }
    val backFocus = remember { FocusRequester() }
    val announce = rememberA11yAnnouncer()

    LaunchedEffect(Unit) { runCatching { backFocus.requestFocus() } }
    LaunchedEffect(uiState.shouldNavigateToList) {
        if (uiState.shouldNavigateToList) {
            announce("Deleted")
            onNavigateToList()
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            text = { Text("Delete this goal and everything in it?", style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        scope.launch {
                            // Cancel this goal's reminder alarms; the rows cascade-
                            // delete with the goal.
                            uiState.reminders.forEach { r -> ReminderAlarmScheduler.cancel(context, r.id) }
                            viewModel.deleteGoal()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppColors.Destructive)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppColors.BrandAccentText)
                ) { Text("Cancel") }
            }
        )
    }

    if (uiState.showEditDialog) {
        EditGoalDialog(
            title = uiState.editTitle,
            why = uiState.editWhy,
            motivation = uiState.editMotivation,
            wantTo = uiState.editWantTo,
            allPassions = uiState.allPassions,
            linkedPassions = uiState.editLinkedPassions,
            onTitleChange = viewModel::onEditTitleChange,
            onWhyChange = viewModel::onEditWhyChange,
            onMotivationChange = viewModel::onEditMotivationChange,
            onWantToChange = viewModel::onEditWantToChange,
            onTogglePassion = viewModel::toggleEditPassion,
            onSave = viewModel::saveGoalEdit,
            onDismiss = viewModel::dismissEditDialog
        )
    }

    if (showBarrierDialog) {
        SingleFieldDialog(
            title = "What's getting in the way?",
            placeholder = "After-work slump — I sit down and never get back up.",
            onSave = { viewModel.addBarrier(it); showBarrierDialog = false },
            onDismiss = { showBarrierDialog = false }
        )
    }
    if (showProgressDialog) {
        SingleFieldDialog(
            title = "What went well?",
            placeholder = "Ran 1.5 miles, walked the rest. Knees fine.",
            onSave = { viewModel.addProgressNote(it); showProgressDialog = false },
            onDismiss = { showProgressDialog = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.semantics { if (uiState.showReassurance) invisibleToUser() },
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
                    IconButton(onClick = onBack, modifier = Modifier.focusRequester(backFocus)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Text(
                        text = uiState.goal?.title ?: "",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp).semantics { heading() }
                    )
                    IconButton(onClick = viewModel::showEditDialog) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit goal", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete goal", tint = AppColors.Destructive, modifier = Modifier.size(22.dp))
                    }
                }
            }
        ) { innerPadding ->
            val goal = uiState.goal
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (goal != null) {
                    item {
                        FollowThroughButton(goal.followedThrough) { viewModel.followThrough() }
                    }

                    // Why this matters.
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionLabel("WHY THIS MATTERS")
                            if (goal.whyItMatters.isBlank()) {
                                Text(
                                    "Tap the edit pencil to add why this goal matters.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    goal.whyItMatters,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // What pulls you (intrinsic motivation — goal-level, never the reminder).
                    val linkedIds = goal.linkedPassionIds.split(",").filter { it.isNotBlank() }.toSet()
                    val linkedPassions = uiState.allPassions.filter { it.id in linkedIds }
                    if (goal.motivationType.isNotBlank() || goal.wantToFraming.isNotBlank() || linkedPassions.isNotEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                SectionLabel("WHAT PULLS YOU")
                                if (goal.motivationType.isNotBlank()) {
                                    Text(
                                        if (goal.motivationType == MotivationType.WANT_TO) "A want-to goal."
                                        else "A have-to — with the want-to harnessed.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (goal.wantToFraming.isNotBlank()) {
                                    Text(
                                        goal.wantToFraming,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (linkedPassions.isNotEmpty()) {
                                    Text(
                                        "Draws on " + linkedPassions.joinToString(", ") { "${it.emoji} ${it.label}" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Barriers.
                    item { SectionLabel("BARRIERS") }
                    if (uiState.barriers.isEmpty()) {
                        item { EmptyHint("What might get in the way? Add one to draw from later.") }
                    } else {
                        items(uiState.barriers, key = { it.id }) { b ->
                            RemovableRow(text = b.text, removeLabel = "Remove barrier") { viewModel.deleteBarrier(b.id) }
                        }
                    }
                    item { AddRow("Add a barrier") { showBarrierDialog = true } }

                    // Progress notes.
                    item { SectionLabel("PROGRESS NOTES") }
                    if (uiState.progressNotes.isEmpty()) {
                        item { EmptyHint("Jot what went well — no scoring, just your own record.") }
                    } else {
                        items(uiState.progressNotes, key = { it.id }) { n ->
                            ProgressNoteRow(text = n.text, date = formatDate(n.createdAt)) { viewModel.deleteProgressNote(n.id) }
                        }
                    }
                    item { AddRow("Add a progress note") { showProgressDialog = true } }

                    // Reminders.
                    item { SectionLabel("REMINDERS") }
                    if (uiState.reminders.isEmpty()) {
                        item { EmptyHint("No reminders yet. Add one to tie this goal to a cue.") }
                    } else {
                        items(uiState.reminders, key = { it.id }) { r ->
                            ReminderRow(reminder = r) { onOpenReminder(r.id) }
                        }
                    }
                    item { AddRow("Add reminder") { onAddReminder() } }
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
private fun EmptyHint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun RemovableRow(text: String, removeLabel: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(16.dp))
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f).padding(vertical = 14.dp))
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Close, contentDescription = removeLabel, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ProgressNoteRow(text: String, date: String, onRemove: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(16.dp))
            .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Close, contentDescription = "Remove progress note", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 6.dp))
    }
}

@Composable
private fun ReminderRow(reminder: Reminder, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(16.dp))
            .clickable(onClickLabel = "Open reminder", onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CueGlyph(reminder)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(reminder.intentionText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text(scheduleSummary(reminder), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/** Small cue glyph: emoji shown directly; phrase shown as a bell icon stand-in. */
@Composable
private fun CueGlyph(reminder: Reminder) {
    if (reminder.cueType == com.ideasinc.followthrough.data.CueType.EMOJI && reminder.cueValue.isNotBlank()) {
        Text(reminder.cueValue, style = MaterialTheme.typography.titleMedium)
    } else {
        Icon(
            painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_bell),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun AddRow(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.BrandAccentText)
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun FollowThroughButton(followedThrough: Boolean, onFollowThrough: () -> Unit) {
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
            .semantics { contentDescription = if (followedThrough) "Followed through" else "I followed through" }
    ) {
        Icon(painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_check_circle), contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (followedThrough) "Followed through" else "I followed through",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

@Composable
private fun MotivationChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val pad = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
    if (selected) {
        Button(
            onClick = onClick,
            contentPadding = pad,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick, contentPadding = pad) { Text(label) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditGoalDialog(
    title: String,
    why: String,
    motivation: String,
    wantTo: String,
    allPassions: List<PassionInterest>,
    linkedPassions: Set<String>,
    onTitleChange: (String) -> Unit,
    onWhyChange: (String) -> Unit,
    onMotivationChange: (String) -> Unit,
    onWantToChange: (String) -> Unit,
    onTogglePassion: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val titleFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { titleFocus.requestFocus() } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit goal", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text("Goal title") },
                    modifier = Modifier.fillMaxWidth().focusRequester(titleFocus)
                )
                OutlinedTextField(
                    value = why,
                    onValueChange = onWhyChange,
                    label = { Text("Why it matters") },
                    placeholder = { Text("Energy back, and Biscuit needs a running buddy.") },
                    modifier = Modifier.fillMaxWidth()
                )
                // Intrinsic motivation — goal-level only (never the reminder/intention/cue).
                Text(
                    "Want to, or have to?",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MotivationChip("Want to", motivation == MotivationType.WANT_TO) { onMotivationChange(MotivationType.WANT_TO) }
                    MotivationChip("Have to", motivation == MotivationType.HAVE_TO) { onMotivationChange(MotivationType.HAVE_TO) }
                }
                OutlinedTextField(
                    value = wantTo,
                    onValueChange = onWantToChange,
                    label = { Text("A way you'd enjoy it") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (allPassions.isNotEmpty()) {
                    Text(
                        "Draws on",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        allPassions.forEach { p ->
                            MotivationChip("${p.emoji} ${p.label}", p.id in linkedPassions) { onTogglePassion(p.id) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = AppColors.BrandAccentText,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = AppColors.BrandAccentText)) { Text("Cancel") }
        }
    )
}

@Composable
private fun SingleFieldDialog(
    title: String,
    placeholder: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(placeholder) },
                modifier = Modifier.fillMaxWidth().focusRequester(focus)
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(text) },
                enabled = text.isNotBlank(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = AppColors.BrandAccentText,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = AppColors.BrandAccentText)) { Text("Cancel") }
        }
    )
}

private fun scheduleSummary(reminder: Reminder): String {
    val time = formatTime(reminder.scheduleTimeLocal)
    val days = reminder.days
    val daysLabel = if (days.size == 7 || reminder.scheduleMode == com.ideasinc.followthrough.data.ScheduleMode.DAILY) {
        "Daily"
    } else {
        days.joinToString(", ") { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } }
    }
    return "$daysLabel · $time"
}

/** "HH:mm" 24h → a friendly 12h label, e.g. "06:45" → "6:45 AM". */
private fun formatTime(hhmm: String): String {
    val parts = hhmm.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: return hhmm
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val period = if (h < 12) "AM" else "PM"
    val h12 = when {
        h == 0 -> 12
        h > 12 -> h - 12
        else -> h
    }
    return "%d:%02d %s".format(h12, m, period)
}

private val dateFormatter = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
private fun formatDate(epochMs: Long): String = dateFormatter.format(java.util.Date(epochMs))
