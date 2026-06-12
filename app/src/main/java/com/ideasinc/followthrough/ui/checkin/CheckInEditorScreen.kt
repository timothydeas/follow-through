package com.ideasinc.followthrough.ui.checkin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ideasinc.followthrough.data.CheckInType
import com.ideasinc.followthrough.notifications.CueImageStore
import com.ideasinc.followthrough.notifications.GoalReminderScheduler
import com.ideasinc.followthrough.notifications.deleteCueChannels
import com.ideasinc.followthrough.ui.goals.GoalReminderControls
import com.ideasinc.followthrough.ui.rememberA11yAnnouncer
import com.ideasinc.followthrough.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Opens one check-in to view and edit — its note, implementation intention, its
 * distinctive cue, and its reminder. Deleting it also clears its reminder, cue
 * image, and custom sound channels.
 */
@Composable
fun CheckInEditorScreen(
    viewModel: CheckInEditorViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val announce = rememberA11yAnnouncer()
    val backFocus = remember { FocusRequester() }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { runCatching { backFocus.requestFocus() } }
    LaunchedEffect(uiState.didDelete) {
        if (uiState.didDelete) {
            announce("Deleted")
            onBack()
        }
    }

    val checkIn = uiState.checkIn

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            text = { Text("Delete this check-in and its reminder?", style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        checkIn?.let {
                            GoalReminderScheduler.remove(context, it.id)
                            deleteCueChannels(context, it.id)
                            CueImageStore.deletePath(it.cueImagePath)
                        }
                        viewModel.delete()
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
                    text = uiState.goalTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .semantics { heading() }
                )
                if (checkIn != null) {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete check-in",
                            tint = AppColors.Destructive,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        if (checkIn == null) return@Scaffold
        val isProgress = checkIn.type == CheckInType.PROGRESS
        val tagLabel = if (isProgress) "Progress" else "Barrier"
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Type tag + date.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isProgress) AppColors.Gold else MaterialTheme.colorScheme.primary)
                )
                Text(
                    text = "  $tagLabel · ${formatDate(checkIn.createdAt)}",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Note.
            var noteText by remember(checkIn.id) { mutableStateOf(checkIn.note) }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("NOTE")
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it; viewModel.updateNote(it) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 96.dp)
                        .semantics { contentDescription = "Note" }
                )
            }

            // Implementation intention.
            var intentionText by remember(checkIn.id) { mutableStateOf(checkIn.intention) }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("IMPLEMENTATION INTENTION")
                OutlinedTextField(
                    value = intentionText,
                    onValueChange = { intentionText = it; viewModel.updateIntention(it) },
                    placeholder = { Text("When …, I will …") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 96.dp)
                        .semantics { contentDescription = "Implementation intention" }
                )
            }

            // Reminder.
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("REMINDER")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, AppColors.Border, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    GoalReminderControls(checkInId = checkIn.id)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.semantics { heading() }
    )
}

private val dateFormatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
private fun formatDate(epochMs: Long): String = dateFormatter.format(Date(epochMs))
