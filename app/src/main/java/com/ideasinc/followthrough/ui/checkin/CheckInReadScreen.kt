package com.ideasinc.followthrough.ui.checkin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ideasinc.followthrough.data.CheckIn
import com.ideasinc.followthrough.data.QuestionConfig
import com.ideasinc.followthrough.data.QuestionKeys
import com.ideasinc.followthrough.ui.rememberA11yAnnouncer
import com.ideasinc.followthrough.ui.theme.AppColors
import com.ideasinc.followthrough.ui.theme.DmSansFontFamily

@Composable
fun CheckInReadScreen(
    viewModel: CheckInReadViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    val backFocus = remember { FocusRequester() }
    val deleteTriggerFocus = remember { FocusRequester() }
    val announce = rememberA11yAnnouncer()

    LaunchedEffect(Unit) { runCatching { backFocus.requestFocus() } }

    LaunchedEffect(uiState.shouldNavigateBack, uiState.didDelete, uiState.didSaveEdit) {
        if (uiState.didDelete) announce("Deleted")
        if (uiState.didSaveEdit) announce("Check-in saved")
        if (uiState.shouldNavigateBack) onNavigateBack()
    }

    if (showDeleteDialog) {
        val confirmFocus = remember { FocusRequester() }
        LaunchedEffect(Unit) { runCatching { confirmFocus.requestFocus() } }
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                runCatching { deleteTriggerFocus.requestFocus() }
            },
            text = { Text("Delete this check-in?", style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteDialog = false; viewModel.delete() },
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
                    onClick = {
                        if (uiState.isEditing) viewModel.cancelEdit() else onNavigateBack()
                    },
                    modifier = Modifier.focusRequester(backFocus)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Go back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                // Goal title (muted)
                Text(
                    text = uiState.goal?.title ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .semantics { heading() }
                )
                if (!uiState.isEditing) {
                    IconButton(onClick = viewModel::startEdit) {
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
                            contentDescription = "Delete",
                            tint = AppColors.Destructive,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val checkIn = uiState.checkIn
        if (checkIn != null) {
            if (uiState.isEditing) {
                EditView(
                    uiState = uiState,
                    viewModel = viewModel,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            } else {
                ReadView(
                    checkIn = checkIn,
                    configs = uiState.questionConfigs,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun ReadView(
    checkIn: CheckIn,
    configs: List<QuestionConfig>,
    modifier: Modifier = Modifier
) {
    val allBlank = checkIn.goalOrChange.isBlank() &&
        checkIn.avoiding.isNullOrBlank() &&
        checkIn.confidence.isNullOrBlank() &&
        checkIn.madeProgress.isNullOrBlank() &&
        checkIn.competingPriority.isNullOrBlank() &&
        checkIn.implementationIntention.isNullOrBlank() &&
        checkIn.accountability.isNullOrBlank()

    if (allBlank) {
        Box(
            modifier = modifier.padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No answers recorded for this check-in.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 24.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        fun labelFor(key: String) = configs.firstOrNull { it.key == key }?.label
            ?: QuestionKeys.DEFAULT_LABELS[key] ?: key

        ReadField(label = labelFor(QuestionKeys.GOAL_OR_CHANGE), content = checkIn.goalOrChange)
        ReadField(label = labelFor(QuestionKeys.AVOIDING), content = checkIn.avoiding ?: "")
        ReadField(label = labelFor(QuestionKeys.CONFIDENCE), content = checkIn.confidence ?: "")
        ReadField(label = labelFor(QuestionKeys.MADE_PROGRESS), content = checkIn.madeProgress ?: "")
        ReadField(label = labelFor(QuestionKeys.COMPETING_PRIORITY), content = checkIn.competingPriority ?: "")
        ReadField(label = labelFor(QuestionKeys.IMPLEMENTATION_INTENTION), content = checkIn.implementationIntention ?: "")
        ReadField(label = labelFor(QuestionKeys.ACCOUNTABILITY), content = checkIn.accountability ?: "")
    }
}

@Composable
private fun EditView(
    uiState: CheckInReadUiState,
    viewModel: CheckInReadViewModel,
    modifier: Modifier = Modifier
) {
    fun labelFor(key: String) = uiState.questionConfigs.firstOrNull { it.key == key }?.label
        ?: QuestionKeys.DEFAULT_LABELS[key] ?: key

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 24.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        EditField(
            label = labelFor(QuestionKeys.GOAL_OR_CHANGE),
            value = uiState.editGoalOrChange,
            placeholder = placeholderFor(QuestionKeys.GOAL_OR_CHANGE),
            onValueChange = viewModel::onEditGoalOrChangeChange
        )
        EditField(
            label = labelFor(QuestionKeys.AVOIDING),
            value = uiState.editAvoiding,
            placeholder = placeholderFor(QuestionKeys.AVOIDING),
            onValueChange = viewModel::onEditAvoidingChange
        )
        EditField(
            label = labelFor(QuestionKeys.CONFIDENCE),
            value = uiState.editConfidence,
            placeholder = placeholderFor(QuestionKeys.CONFIDENCE),
            onValueChange = viewModel::onEditConfidenceChange
        )
        EditField(
            label = labelFor(QuestionKeys.MADE_PROGRESS),
            value = uiState.editMadeProgress,
            placeholder = placeholderFor(QuestionKeys.MADE_PROGRESS),
            onValueChange = viewModel::onEditMadeProgressChange
        )
        EditField(
            label = labelFor(QuestionKeys.COMPETING_PRIORITY),
            value = uiState.editCompetingPriority,
            placeholder = placeholderFor(QuestionKeys.COMPETING_PRIORITY),
            onValueChange = viewModel::onEditCompetingPriorityChange
        )
        EditField(
            label = labelFor(QuestionKeys.IMPLEMENTATION_INTENTION),
            value = uiState.editImplementationIntention,
            placeholder = placeholderFor(QuestionKeys.IMPLEMENTATION_INTENTION),
            onValueChange = viewModel::onEditImplementationIntentionChange
        )
        EditField(
            label = labelFor(QuestionKeys.ACCOUNTABILITY),
            value = uiState.editAccountability,
            placeholder = placeholderFor(QuestionKeys.ACCOUNTABILITY),
            onValueChange = viewModel::onEditAccountabilityChange
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(
                onClick = viewModel::cancelEdit,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "Cancel",
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = DmSansFontFamily)
                )
            }
            Button(
                onClick = viewModel::saveEdit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "Save",
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = DmSansFontFamily)
                )
            }
        }
    }
}

@Composable
private fun EditField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                letterSpacing = 1.0.sp,
                fontFamily = DmSansFontFamily
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            if (value.isEmpty() && placeholder.isNotBlank()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = DmSansFontFamily,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = DmSansFontFamily,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            )
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.surfaceVariant,
            thickness = 1.dp,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun ReadField(label: String, content: String) {
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

