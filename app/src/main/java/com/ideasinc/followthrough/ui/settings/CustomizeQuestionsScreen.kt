package com.ideasinc.followthrough.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ideasinc.followthrough.di.AppContainer
import com.ideasinc.followthrough.ui.theme.PoppinsFontFamily

@Composable
fun CustomizeQuestionsScreen(
    container: AppContainer,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val settingsVm: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(container.questionLabelDao, context)
    )
    val uiState by settingsVm.uiState.collectAsState()

    val backFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { backFocus.requestFocus() } }

    if (uiState.editingKey != null) {
        val labelFocus = remember { FocusRequester() }
        LaunchedEffect(Unit) { runCatching { labelFocus.requestFocus() } }
        AlertDialog(
            onDismissRequest = settingsVm::cancelEditing,
            title = { Text("Edit question label", style = MaterialTheme.typography.headlineSmall) },
            text = {
                OutlinedTextField(
                    value = uiState.editingLabel,
                    onValueChange = settingsVm::onEditingLabelChange,
                    label = { Text("Label") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(labelFocus)
                )
            },
            confirmButton = {
                TextButton(onClick = settingsVm::saveLabel) { Text("Save") }
            },
            dismissButton = {
                Row {
                    val key = uiState.editingKey!!
                    TextButton(onClick = { settingsVm.resetLabel(key); settingsVm.cancelEditing() }) {
                        Text("Reset to default")
                    }
                    TextButton(onClick = settingsVm::cancelEditing) { Text("Cancel") }
                }
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
                    text = "Customize Questions",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .semantics { heading() }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                uiState.questionConfigs.forEach { config ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = config.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (config.isEnabled)
                                    MaterialTheme.colorScheme.onSurface
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { settingsVm.startEditing(config.key) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Switch(
                            checked = config.isEnabled,
                            onCheckedChange = { settingsVm.toggleQuestion(config.key, it) },
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .semantics {
                                    contentDescription = config.label
                                    stateDescription = if (config.isEnabled) "On" else "Off"
                                    role = Role.Switch
                                }
                        )
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}
