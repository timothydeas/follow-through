package com.ideasinc.followthrough.ui.readview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ideasinc.followthrough.data.GroundedNote
import com.ideasinc.followthrough.ui.rememberA11yAnnouncer
import com.ideasinc.followthrough.ui.theme.DmSansFontFamily
import com.ideasinc.followthrough.ui.theme.PrimaryForge
import kotlinx.coroutines.delay

@Composable
fun ReadViewScreen(
    viewModel: ReadViewViewModel,
    onNavigateBack: () -> Unit,
    onEdit: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    val announce = rememberA11yAnnouncer()

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            announce("Deleted")
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.shouldExitToList) {
        if (uiState.shouldExitToList) onNavigateBack()
    }

    LaunchedEffect(uiState.showReassurance) {
        if (uiState.showReassurance) {
            announce("You followed through. That's what matters. Regardless of the outcome.")
            delay(3000)
            viewModel.onReassuranceDone()
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            text = { Text("Delete this entry?", style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteNote()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFCC0000))
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                ReadViewTopBar(
                    isPinned = uiState.note?.isPinned ?: false,
                    onBack = onNavigateBack,
                    onPinToggle = viewModel::togglePin,
                    onEdit = { uiState.note?.id?.let { onEdit(it) } },
                    onDelete = { showDeleteDialog = true }
                )
            }
        ) { innerPadding ->
            val note = uiState.note
            if (note != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                        .padding(top = 24.dp, bottom = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    NoteField(label = "Reflection", content = note.reflection)
                    NoteField(label = "What stopped you", content = note.whatStoppedYou)
                    NoteField(label = "What you've learned", content = note.whatYouLearned)
                    NoteField(label = "Next steps", content = note.nextSteps)
                    NoteField(label = "Implementation intention", content = note.implementationIntention)
                    if (note.implementationIntention.isBlank() &&
                        (note.whenField.isNotBlank() || note.willField.isNotBlank())
                    ) {
                        WhenWillField(whenText = note.whenField, willText = note.willField)
                    }

                    FollowThroughButton(
                        followedThrough = note.followedThrough,
                        onFollowThrough = viewModel::followThrough
                    )
                }
            }
        }

        if (uiState.showReassurance) {
            ReassuranceOverlay(onDismiss = viewModel::onReassuranceDone)
        }
    }
}

@Composable
private fun FollowThroughButton(
    followedThrough: Boolean,
    onFollowThrough: () -> Unit
) {
    if (followedThrough) {
        Button(
            onClick = {},
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryForge),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Followed through" }
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = "Followed through \u2713",
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = DmSansFontFamily)
            )
        }
    } else {
        Button(
            onClick = onFollowThrough,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryForge),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Mark as followed through" }
        ) {
            Text(
                text = "I followed through",
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = DmSansFontFamily)
            )
        }
    }
}

@Composable
private fun ReassuranceOverlay(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(onClickLabel = "Dismiss", onClick = onDismiss)
            .semantics {
                contentDescription =
                    "You followed through. That's what matters. Regardless of the outcome."
                liveRegion = LiveRegionMode.Assertive
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .padding(32.dp)
                .background(Color(0xFF2C2C28), RoundedCornerShape(20.dp))
                .padding(horizontal = 32.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "You followed through!\nThat's what matters.\nRegardless of the outcome.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = DmSansFontFamily,
                    color = Color.White
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun NoteField(label: String, content: String) {
    if (content.isBlank()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                letterSpacing = 1.0.sp,
                fontFamily = DmSansFontFamily
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = DmSansFontFamily),
            color = MaterialTheme.colorScheme.onSurface
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.surfaceVariant,
            thickness = 1.dp,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun WhenWillField(whenText: String, willText: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "WHEN ___, I WILL ___",
            style = MaterialTheme.typography.labelMedium.copy(
                letterSpacing = 1.0.sp,
                fontFamily = DmSansFontFamily
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (whenText.isNotBlank()) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "When\u2026",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = whenText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = DmSansFontFamily),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            if (willText.isNotBlank()) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "I will\u2026",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = willText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = DmSansFontFamily),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.surfaceVariant,
            thickness = 1.dp,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun ReadViewTopBar(
    isPinned: Boolean,
    onBack: () -> Unit,
    onPinToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Go back",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onPinToggle) {
            Icon(
                imageVector = if (isPinned) Icons.Default.Star else Icons.Default.StarOutline,
                contentDescription = if (isPinned) "Unpin goal" else "Pin goal",
                tint = if (isPinned) Color(0xFFD4A843) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
        IconButton(onClick = onEdit) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = Color(0xFFC0392B),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
