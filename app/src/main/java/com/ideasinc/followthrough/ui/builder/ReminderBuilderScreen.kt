package com.ideasinc.followthrough.ui.builder

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ideasinc.followthrough.data.CueType
import com.ideasinc.followthrough.data.ScheduleMode
import com.ideasinc.followthrough.data.WeekDay
import com.ideasinc.followthrough.di.AppContainer
import com.ideasinc.followthrough.ui.theme.AppColors

private val TEMPLATES = listOf(
    "Daily medication",
    "Move my body",
    "Call someone I love",
    "Ship the thing I keep postponing"
)

/**
 * Reminder Builder — the 4-step spine wizard: goal → draw from yourself → intention
 * → one cue + schedule. Enforces exactly one cue (emoji or phrase at launch; photo
 * and sound are shown as the target state but disabled). Back is always available;
 * nothing auto-advances.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReminderBuilderScreen(
    container: AppContainer,
    goalId: String?,
    reminderId: String?,
    onClose: () -> Unit,
    onSaved: (goalId: String) -> Unit
) {
    val appContext = LocalContext.current.applicationContext
    val vm: ReminderBuilderViewModel = viewModel(
        factory = ReminderBuilderViewModel.Factory(
            appContext, container.goalDao, container.goalContentDao, container.paletteDao, container.reminderDao, goalId, reminderId
        )
    )
    val s by vm.uiState.collectAsState()
    val activity = LocalContext.current as? android.app.Activity

    LaunchedEffect(s.savedGoalId) {
        s.savedGoalId?.let { goalId ->
            // Count genuine use for the (never sentiment-gated) Play review prompt.
            activity?.let { com.ideasinc.followthrough.feedback.AppReview.onReminderSaved(it) }
            onSaved(goalId)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { if (s.step > 0 && !s.isEdit) vm.back() else onClose() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                }
                StepDots(current = s.step, modifier = Modifier.weight(1f))
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        },
        bottomBar = {
            BuilderNav(
                step = s.step,
                canAdvance = when (s.step) {
                    0 -> s.step1Valid
                    1 -> true // draw-from-yourself is skippable
                    2 -> s.step3Valid
                    else -> s.step4Valid
                },
                isLastStep = s.step == 3,
                onBack = vm::back,
                onNext = vm::next,
                onSave = vm::save
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            when (s.step) {
                0 -> StepGoal(s, vm)
                1 -> StepDraw(s, vm)
                2 -> StepIntention(s, vm)
                else -> StepCue(s, vm)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StepHeader(title: String, purpose: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(purpose, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepGoal(s: BuilderUiState, vm: ReminderBuilderViewModel) {
    StepHeader("Which goal?", "Pick what you're working toward, or start a new one.")
    s.goals.forEach { g ->
        SelectableRow(text = g.title, selected = s.goalId == g.id && !s.creatingNewGoal) { vm.selectGoal(g.id) }
    }
    SelectableRow(text = "+ New goal", selected = s.creatingNewGoal) { vm.startNewGoal() }
    if (s.creatingNewGoal) {
        OutlinedTextField(
            value = s.newGoalTitle, onValueChange = vm::onNewGoalTitle,
            label = { Text("Goal") }, placeholder = { Text("Run a 5K without stopping by August") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = s.newGoalWhy, onValueChange = vm::onNewGoalWhy,
            label = { Text("Why it matters (optional)") },
            modifier = Modifier.fillMaxWidth()
        )
        Text("Or start from a template", style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TEMPLATES.forEach { t -> Chip(label = t, selected = s.newGoalTitle == t) { vm.applyTemplate(t) } }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepDraw(s: BuilderUiState, vm: ReminderBuilderViewModel) {
    StepHeader("Draw from yourself", "Pick what's relevant — this is the palette, not a quiz. You can skip this.")
    if (s.passions.isEmpty() && s.learnings.isEmpty() && s.barriers.isEmpty()) {
        Text(
            "Nothing here yet. Add passions and learnings on the About You tab, or barriers on the goal — they'll show up here as cue material.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    if (s.passions.isNotEmpty()) {
        Text("PASSIONS & INTERESTS", style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            s.passions.forEach { p -> Chip(label = "${p.emoji} ${p.label}", selected = p.id in s.trayPaletteIds) { vm.toggleTray(p.id) } }
        }
    }
    if (s.learnings.isNotEmpty()) {
        Text("LEARNINGS", style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            s.learnings.forEach { l -> Chip(label = l.text, selected = l.id in s.trayPaletteIds) { vm.toggleTray(l.id) } }
        }
    }
    if (s.barriers.isNotEmpty()) {
        Text("BARRIERS", style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            s.barriers.forEach { b -> Chip(label = b.text, selected = b.id in s.trayPaletteIds) { vm.toggleTray(b.id) } }
        }
    }
}

@Composable
private fun StepIntention(s: BuilderUiState, vm: ReminderBuilderViewModel) {
    StepHeader("Your intention", "Fill in the moment and the action. The whole sentence travels with the cue.")
    Text("When…", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    OutlinedTextField(
        value = s.whenMoment, onValueChange = vm::onWhenMoment,
        placeholder = { Text("I start the morning coffee") },
        modifier = Modifier.fillMaxWidth()
    )
    Text("…I will", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    OutlinedTextField(
        value = s.iWill, onValueChange = vm::onIWill,
        placeholder = { Text("take the BP pill next to the orange Chemex") },
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepCue(s: BuilderUiState, vm: ReminderBuilderViewModel) {
    StepHeader("Choose ONE cue", "The single most vivid thing for you. One beats many.")

    // Type toggle: emoji / phrase enabled; photo / sound shown as the target state.
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip("Emoji", s.cueType == CueType.EMOJI) { vm.setCueType(CueType.EMOJI) }
        Chip("Phrase", s.cueType == CueType.PHRASE) { vm.setCueType(CueType.PHRASE) }
        Chip("Photo (soon)", selected = false, enabled = false) {}
        Chip("Sound (soon)", selected = false, enabled = false) {}
    }

    if (s.cueSuggestions.isNotEmpty()) {
        Text("FROM YOUR PALETTE", style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            s.cueSuggestions.forEach { sug ->
                Chip(label = if (sug.type == CueType.EMOJI) sug.value else "“${sug.value}”", selected = s.cueValue == sug.value && s.cueType == sug.type) {
                    vm.pickSuggestion(sug)
                }
            }
        }
    }

    OutlinedTextField(
        value = s.cueValue,
        onValueChange = { vm.onCueValue(if (s.cueType == CueType.EMOJI) it.take(4) else it) },
        label = { Text(if (s.cueType == CueType.EMOJI) "Your emoji" else "Your phrase") },
        placeholder = { Text(if (s.cueType == CueType.EMOJI) "☕" else "Biscuit's leash is the starting line") },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(8.dp))
    Text("WHEN IT SURFACES", style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip("Daily", s.scheduleMode == ScheduleMode.DAILY) { vm.setScheduleMode(ScheduleMode.DAILY) }
        Chip("Weekly", s.scheduleMode == ScheduleMode.WEEKLY) { vm.setScheduleMode(ScheduleMode.WEEKLY) }
    }
    if (s.scheduleMode == ScheduleMode.WEEKLY) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            WeekDay.ALL.forEach { d ->
                DayChip(day = d, selected = d in s.days) { vm.toggleDay(d) }
            }
        }
    }
    TimeRow(hour = s.hour, minute = s.minute, onPick = vm::setTime)

    Spacer(Modifier.height(8.dp))
    ConfirmRestate(s)
}

@Composable
private fun ConfirmRestate(s: BuilderUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (s.cueValue.isNotBlank()) {
                Text(if (s.cueType == CueType.EMOJI) s.cueValue else "“${s.cueValue}”", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(10.dp))
            }
        }
        if (s.whenMoment.isNotBlank() || s.iWill.isNotBlank()) {
            Text(
                "When ${s.whenMoment}, I will ${s.iWill}",
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            "The text always travels with the cue, so the meaning is never lost.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TimeRow(hour: Int, minute: Int, onPick: (Int, Int) -> Unit) {
    val context = LocalContext.current
    val label = formatTime(hour, minute)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Time", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        TextButton(onClick = {
            TimePickerDialog(context, { _, h, m -> onPick(h, m) }, hour, minute, false).show()
        }) { Text(label, color = AppColors.BrandAccentText) }
    }
}

@Composable
private fun SelectableRow(text: String, selected: Boolean, onClick: () -> Unit) {
    val border = if (selected) MaterialTheme.colorScheme.primary else AppColors.Border
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    val bg = when {
        !enabled -> MaterialTheme.colorScheme.surface
        selected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val fg = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        selected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val border = if (selected && enabled) MaterialTheme.colorScheme.primary else AppColors.Border
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(50))
            .clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = 40.dp)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = fg)
    }
}

@Composable
private fun DayChip(day: WeekDay, selected: Boolean, onClick: () -> Unit) {
    val letter = day.name.first().toString()
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
            .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else AppColors.Border, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            letter,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StepDots(current: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
        repeat(4) { i ->
            Box(
                modifier = Modifier
                    .size(if (i == current) 9.dp else 7.dp)
                    .clip(CircleShape)
                    .background(if (i == current) MaterialTheme.colorScheme.primary else AppColors.Border)
            )
        }
    }
}

@Composable
private fun BuilderNav(
    step: Int,
    canAdvance: Boolean,
    isLastStep: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (step > 0) {
            TextButton(onClick = onBack) { Text("Back", color = AppColors.BrandAccentText) }
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = { if (isLastStep) onSave() else onNext() },
            enabled = canAdvance,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Text(
                if (isLastStep) "Save reminder" else "Continue",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

private fun formatTime(hour: Int, minute: Int): String {
    val period = if (hour < 12) "AM" else "PM"
    val h12 = when { hour == 0 -> 12; hour > 12 -> hour - 12; else -> hour }
    return "%d:%02d %s".format(h12, minute, period)
}
