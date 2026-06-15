package com.ideasinc.followthrough.ui.aboutyou

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ideasinc.followthrough.data.Learning
import com.ideasinc.followthrough.data.PassionInterest
import com.ideasinc.followthrough.di.AppContainer
import com.ideasinc.followthrough.ui.theme.AppColors

/**
 * About You — the self-knowledge palette. Two sections (Passions & interests,
 * Learnings) the user grows over time; entries become the raw material for
 * distinctive cues in the Reminder Builder. Add / edit / delete each, with a gentle
 * empty state. No interrogation, no scoring.
 */
@Composable
fun AboutYouScreen(
    container: AppContainer,
    modifier: Modifier = Modifier
) {
    val vm: AboutYouViewModel = viewModel(factory = AboutYouViewModel.Factory(container.paletteDao))
    val passions by vm.passions.collectAsState()
    val learnings by vm.learnings.collectAsState()

    var editingPassion by remember { mutableStateOf<PassionInterest?>(null) }
    var showAddPassion by remember { mutableStateOf(false) }
    var editingLearning by remember { mutableStateOf<Learning?>(null) }
    var showAddLearning by remember { mutableStateOf(false) }

    if (showAddPassion || editingPassion != null) {
        PassionDialog(
            existing = editingPassion,
            onDismiss = { showAddPassion = false; editingPassion = null },
            onSave = { emoji, label ->
                val current = editingPassion
                if (current == null) vm.addPassion(emoji, label) else vm.updatePassion(current, emoji, label)
                showAddPassion = false; editingPassion = null
            }
        )
    }

    if (showAddLearning || editingLearning != null) {
        LearningDialog(
            existing = editingLearning,
            onDismiss = { showAddLearning = false; editingLearning = null },
            onSave = { text ->
                val current = editingLearning
                if (current == null) vm.addLearning(text) else vm.updateLearning(current, text)
                showAddLearning = false; editingLearning = null
            }
        )
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "About You",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() }
                )
            }
            item {
                Text(
                    "What you know about yourself is the raw material for cues that actually catch your eye.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (passions.isEmpty() && learnings.isEmpty()) {
                item {
                    Text(
                        "Add one thing you love or one thing you've learned about yourself — you'll pick cues from here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }

            item { SectionLabel("PASSIONS & INTERESTS", Modifier.padding(top = 12.dp)) }
            items(passions, key = { it.id }) { passion ->
                PaletteRow(
                    leading = passion.emoji,
                    text = passion.label,
                    onClick = { editingPassion = passion },
                    onDelete = { vm.deletePassion(passion.id) },
                    deleteLabel = "Remove ${passion.label}"
                )
            }
            item {
                AddButton(text = "Add a passion or interest") { showAddPassion = true }
            }

            item { SectionLabel("LEARNINGS", Modifier.padding(top = 16.dp)) }
            items(learnings, key = { it.id }) { learning ->
                PaletteRow(
                    leading = null,
                    text = learning.text,
                    onClick = { editingLearning = learning },
                    onDelete = { vm.deleteLearning(learning.id) },
                    deleteLabel = "Remove learning"
                )
            }
            item {
                AddButton(text = "Add something you've learned") { showAddLearning = true }
            }
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

/** One palette entry: optional emoji, the text, tap to edit, X to remove. */
@Composable
private fun PaletteRow(
    leading: String?,
    text: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    deleteLabel: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(16.dp))
            .clickable(onClickLabel = "Edit", onClick = onClick)
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!leading.isNullOrBlank()) {
            Text(text = leading, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 12.dp)
        )
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Close,
                contentDescription = deleteLabel,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun AddButton(text: String, onClick: () -> Unit) {
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
private fun PassionDialog(
    existing: PassionInterest?,
    onDismiss: () -> Unit,
    onSave: (emoji: String, label: String) -> Unit
) {
    var emoji by remember { mutableStateOf(existing?.emoji ?: "") }
    var label by remember { mutableStateOf(existing?.label ?: "") }
    val labelFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { labelFocus.requestFocus() } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (existing == null) "Add a passion or interest" else "Edit",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = emoji,
                    onValueChange = { emoji = it.take(4) },
                    label = { Text("Emoji (optional)") },
                    placeholder = { Text("☕") },
                    singleLine = true,
                    modifier = Modifier.width(140.dp)
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("What is it?") },
                    placeholder = { Text("Specialty coffee — the orange Chemex") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(labelFocus)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(emoji, label) },
                enabled = label.isNotBlank(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = AppColors.BrandAccentText,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = AppColors.BrandAccentText)) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun LearningDialog(
    existing: Learning?,
    onDismiss: () -> Unit,
    onSave: (text: String) -> Unit
) {
    var text by remember { mutableStateOf(existing?.text ?: "") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (existing == null) "Something you've learned" else "Edit",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("What's worked before?") },
                placeholder = { Text("I follow through when it's staged by the door the night before.") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus)
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
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = AppColors.BrandAccentText)) {
                Text("Cancel")
            }
        }
    )
}
