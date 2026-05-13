package com.ideasinc.followthrough.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ideasinc.followthrough.ui.theme.DmSansFontFamily
import com.ideasinc.followthrough.ui.theme.PrimaryForge

@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit,
    showBackButton: Boolean = true
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Navigate back after save or delete
    LaunchedEffect(uiState.isSaved, uiState.isDeleted) {
        if (uiState.isSaved || uiState.isDeleted) onNavigateBack()
    }

    // Auto-save draft when app is backgrounded
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) viewModel.saveDraft()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            text = { Text("Delete this note?", style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteDialog = false; viewModel.deleteNote() },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFCC0000))
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            EditorTopBar(
                isPinned = uiState.isPinned,
                showBackButton = showBackButton,
                onBack = { viewModel.saveDraft(); onNavigateBack() },
                onPinToggle = viewModel::onPinToggle,
                onSave = viewModel::save,
                onDelete = { showDeleteDialog = true }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Title field
            PlaceholderField(
                value = uiState.title,
                placeholder = "Title",
                onValueChange = viewModel::onTitleChange,
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = DmSansFontFamily,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                placeholderStyle = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = DmSansFontFamily,
                    fontStyle = FontStyle.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tag field
            PlaceholderField(
                value = uiState.tag,
                placeholder = "Tag (optional)",
                onValueChange = viewModel::onTagChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = DmSansFontFamily,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                placeholderStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = DmSansFontFamily,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // Body field
            PlaceholderField(
                value = uiState.body,
                placeholder = "Start writing...",
                onValueChange = viewModel::onBodyChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = DmSansFontFamily,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                placeholderStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = DmSansFontFamily,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun PlaceholderField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    textStyle: androidx.compose.ui.text.TextStyle,
    placeholderStyle: androidx.compose.ui.text.TextStyle,
    singleLine: Boolean = false,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    Box(modifier = modifier) {
        if (value.isEmpty()) {
            Text(text = placeholder, style = placeholderStyle)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = textStyle,
            cursorBrush = SolidColor(PrimaryForge),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun EditorTopBar(
    isPinned: Boolean,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onPinToggle: () -> Unit,
    onSave: () -> Unit,
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
        if (showBackButton) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Go back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
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

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = Color(0xFFC0392B),
                modifier = Modifier.size(22.dp)
            )
        }

        TextButton(
            onClick = onSave,
            colors = ButtonDefaults.textButtonColors(contentColor = PrimaryForge)
        ) {
            Text(
                text = "Save",
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = DmSansFontFamily)
            )
        }
    }
}
