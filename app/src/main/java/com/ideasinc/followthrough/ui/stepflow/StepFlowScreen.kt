package com.ideasinc.followthrough.ui.stepflow

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ideasinc.followthrough.ui.rememberA11yAnnouncer
import com.ideasinc.followthrough.ui.theme.DmSansFontFamily
import com.ideasinc.followthrough.ui.theme.PrimaryForge
import kotlinx.coroutines.delay

@Composable
fun StepFlowScreen(
    viewModel: StepFlowViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val announce = rememberA11yAnnouncer()

    LaunchedEffect(uiState.shouldExit) {
        if (uiState.shouldExit) onNavigateBack()
    }

    LaunchedEffect(uiState.showReassurance) {
        if (uiState.showReassurance) {
            announce("You followed through. That's what matters. Regardless of the outcome.")
            delay(3000)
            viewModel.onReassuranceDone()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                StepTopBar(
                    currentStep = uiState.currentStep,
                    onBack = viewModel::onBack
                )
            },
            bottomBar = {
                StepBottomBar(
                    currentStep = uiState.currentStep,
                    onNext = viewModel::onNext,
                    onSave = viewModel::onSave
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 24.dp)
                    .padding(top = 32.dp, bottom = 24.dp)
            ) {
                StepContent(
                    step = uiState.currentStep,
                    reflection = uiState.reflection,
                    whatStoppedYou = uiState.whatStoppedYou,
                    whatYouLearned = uiState.whatYouLearned,
                    nextSteps = uiState.nextSteps,
                    implementationIntention = uiState.implementationIntention,
                    onReflectionChange = viewModel::onReflectionChange,
                    onWhatStoppedYouChange = viewModel::onWhatStoppedYouChange,
                    onWhatYouLearnedChange = viewModel::onWhatYouLearnedChange,
                    onNextStepsChange = viewModel::onNextStepsChange,
                    onImplementationIntentionChange = viewModel::onImplementationIntentionChange
                )
            }
        }

        if (uiState.showReassurance) {
            ReassuranceOverlay(onDismiss = viewModel::onReassuranceDone)
        }
    }
}

@Composable
private fun StepTopBar(currentStep: Int, onBack: () -> Unit) {
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
        Text(
            text = "Step $currentStep of 5",
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = DmSansFontFamily,
                letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(end = 16.dp)
                .semantics {
                    contentDescription = "Step $currentStep of 5"
                    liveRegion = LiveRegionMode.Polite
                }
        )
    }
}

@Composable
private fun StepBottomBar(currentStep: Int, onNext: () -> Unit, onSave: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Button(
            onClick = if (currentStep == 5) onSave else onNext,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryForge)
        ) {
            Text(
                text = if (currentStep == 5) "Save" else "Next",
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = DmSansFontFamily)
            )
        }
    }
}

private data class StepMeta(val label: String, val placeholder: String, val tooltip: String)

private val stepMeta = listOf(
    StepMeta(
        label = "Reflection",
        placeholder = "What you noticed, felt, or experienced",
        tooltip = "What did you notice, feel, or experience? This is your raw awareness — no judgment, just what happened."
    ),
    StepMeta(
        label = "What stopped you",
        placeholder = "Honest naming of what got in the way",
        tooltip = "Name what got in the way honestly. This is where real learning begins. Naming the barrier without ego is how you grow past it."
    ),
    StepMeta(
        label = "What you've learned",
        placeholder = "The insight or growth captured",
        tooltip = "What insight or growth came from this? Capture it here so it stays with you."
    ),
    StepMeta(
        label = "Next steps",
        placeholder = "What you plan to do with the learning",
        tooltip = "What do you plan to do with this learning? Be specific."
    ),
    StepMeta(
        label = "Implementation Intention",
        placeholder = "When I sit down for my interview, I will tell my story concisely and confidently without relying on a framework",
        tooltip = "Link a specific moment to a specific action. The more vivid and specific the cue, the more likely you are to follow through when that moment arrives."
    )
)

@Composable
private fun StepContent(
    step: Int,
    reflection: String,
    whatStoppedYou: String,
    whatYouLearned: String,
    nextSteps: String,
    implementationIntention: String,
    onReflectionChange: (String) -> Unit,
    onWhatStoppedYouChange: (String) -> Unit,
    onWhatYouLearnedChange: (String) -> Unit,
    onNextStepsChange: (String) -> Unit,
    onImplementationIntentionChange: (String) -> Unit
) {
    val meta = stepMeta[step - 1]
    var showTooltip by remember(step) { mutableStateOf(false) }

    if (showTooltip) {
        AlertDialog(
            onDismissRequest = { showTooltip = false },
            text = { Text(meta.tooltip, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = { showTooltip = false }) { Text("Got it") }
            }
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = meta.label,
            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = DmSansFontFamily),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        IconButton(
            onClick = { showTooltip = true },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "More information",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    val value = when (step) {
        1 -> reflection
        2 -> whatStoppedYou
        3 -> whatYouLearned
        4 -> nextSteps
        5 -> implementationIntention
        else -> ""
    }
    val onChange: (String) -> Unit = when (step) {
        1 -> onReflectionChange
        2 -> onWhatStoppedYouChange
        3 -> onWhatYouLearnedChange
        4 -> onNextStepsChange
        5 -> onImplementationIntentionChange
        else -> { _ -> }
    }
    LargeTextField(
        value = value,
        placeholder = meta.placeholder,
        onValueChange = onChange
    )
}

@Composable
private fun LargeTextField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    Box(modifier = modifier) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = DmSansFontFamily,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = DmSansFontFamily,
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(PrimaryForge),
            modifier = Modifier.fillMaxWidth()
        )
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
