package com.ideasinc.followthrough.ui.builder

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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

// Must fit in the lower 16 bits: MainActivity is a FragmentActivity (for BiometricPrompt),
// whose validateRequestPermissionsRequestCode rejects larger codes.
private const val REQUEST_POST_NOTIFICATIONS = 1001

/**
 * Create-cue flow — the spine of the MVP (MVP_User_Flow_IA.md §3). Four focused steps:
 * name the intention → pin the moment → design one distinctive cue → review. The cue is
 * the hero; the step is seeded with a concrete example so the user never faces a blank
 * prompt. Exactly one cue (emoji or phrase at launch). Back is always available; nothing
 * auto-advances; the keyboard never occludes inputs.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReminderBuilderScreen(
    container: AppContainer,
    reminderId: String?,
    onClose: () -> Unit,
    onSaved: () -> Unit
) {
    val appContext = LocalContext.current.applicationContext
    val vm: ReminderBuilderViewModel = viewModel(
        factory = ReminderBuilderViewModel.Factory(
            appContext, container.goalDao, container.reminderDao, reminderId
        )
    )
    val s by vm.uiState.collectAsState()
    val activity = LocalContext.current as? android.app.Activity

    LaunchedEffect(s.saved) {
        if (s.saved) {
            activity?.let { com.ideasinc.followthrough.feedback.AppReview.onReminderSaved(it) }
            onSaved()
        }
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    LaunchedEffect(s.deleted) { if (s.deleted) onClose() }
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this intention?") },
            text = { Text("It will stop reminding you and be removed from this device.") },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false; vm.delete() },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppColors.Destructive)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppColors.BrandAccentText)
                ) { Text("Cancel") }
            }
        )
    }

    // Reminder delivery needs notification permission on Android 13+. Ask once while the
    // user is creating a cue (the relevant moment); if they decline, the Settings →
    // Notifications row repairs it. Classic ActivityCompat.requestPermissions — NOT the
    // Activity-Result API: the host is a FragmentActivity (for BiometricPrompt), whose
    // >16-bit request code throws "Can only use lower 16 bits for requestCode".
    LaunchedEffect(Unit) {
        val act = activity
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && act != null &&
            ContextCompat.checkSelfPermission(act, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                act, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_POST_NOTIFICATIONS
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { if (s.step > 0) vm.back() else onClose() }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                }
                StepDots(current = s.step, modifier = Modifier.weight(1f))
                if (s.isEdit) {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete intention", tint = AppColors.Destructive)
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        },
        bottomBar = {
            BuilderNav(
                step = s.step,
                canAdvance = s.canAdvance(s.step),
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
                0 -> StepName(s, vm)
                1 -> StepMoment(s, vm)
                2 -> StepCue(s, vm)
                else -> StepReview(s)
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

// Tappable starters that fill "I will …" — defeats the blank slate (the beta's "where do
// I start?"), which an empty field reintroduces. Tapping one fills the still-editable field.
private val INTENTION_STARTERS = listOf(
    "take my medication",
    "go for a walk",
    "drink a glass of water",
    "call someone I love",
    "stretch for five minutes"
)

/** Step 0 — name the intention (the action). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepName(s: BuilderUiState, vm: ReminderBuilderViewModel) {
    StepHeader("What do you want to remember to do?", "The thing you mean to do but tend to forget in the moment.")
    OutlinedTextField(
        value = s.iWill, onValueChange = vm::onIWill,
        label = { Text("I will…") },
        placeholder = { Text("take my blood-pressure pill") },
        modifier = Modifier.fillMaxWidth()
    )
    Text("Need a starting point?", style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        INTENTION_STARTERS.forEach { starter ->
            Chip(label = starter, selected = s.iWill == starter) { vm.onIWill(starter) }
        }
    }
}

/** Step 1 — pin the moment + how often it recurs. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepMoment(s: BuilderUiState, vm: ReminderBuilderViewModel) {
    StepHeader("When will you do it?", "Name the moment you'll be able to act — and how often it comes around.")
    OutlinedTextField(
        value = s.whenMoment, onValueChange = vm::onWhenMoment,
        label = { Text("When…") },
        placeholder = { Text("I start the morning coffee") },
        supportingText = { Text("A situation you'll be in — not a clock time. The time below is just when to nudge you.") },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))
    Text("HOW OFTEN", style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip("Daily", s.scheduleMode == ScheduleMode.DAILY) { vm.setScheduleMode(ScheduleMode.DAILY) }
        Chip("Weekly", s.scheduleMode == ScheduleMode.WEEKLY) { vm.setScheduleMode(ScheduleMode.WEEKLY) }
        Chip("Just once", s.scheduleMode == ScheduleMode.ONCE) { vm.setScheduleMode(ScheduleMode.ONCE) }
    }
    if (s.scheduleMode == ScheduleMode.WEEKLY) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            WeekDay.ALL.forEach { d -> DayChip(day = d, selected = d in s.days) { vm.toggleDay(d) } }
        }
    }
    if (s.scheduleMode == ScheduleMode.ONCE) {
        DateRow(date = s.onceDate, onPick = vm::setOnceDate)
    }
    TimeRow(hour = s.hour, minute = s.minute, onPick = vm::setTime)
}

/** Step 2 — design one distinctive cue. Seeded with a concrete example so the step
 *  (the one most likely to stall) is never a blank prompt. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepCue(s: BuilderUiState, vm: ReminderBuilderViewModel) {
    StepHeader("Pick one vivid cue", "One specific thing you'll truly see or hear in that moment — the more vivid, the harder to ignore. One beats many.")

    // The seeded example — show, don't tell.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.CoralTint)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Dark text (onSurface), not coral-on-coral-tint — coral on the pale tint is ~4.2:1,
        // under AA for small text. The pale-coral card stays; the text reads at ~11:1.
        Text("For example", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
        Text("\"When I pour my morning coffee\" → ☕ the orange Chemex on the counter.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip("Emoji", s.cueType == CueType.EMOJI) { vm.setCueType(CueType.EMOJI) }
        Chip("Phrase", s.cueType == CueType.PHRASE) { vm.setCueType(CueType.PHRASE) }
    }

    OutlinedTextField(
        value = s.cueValue,
        onValueChange = { vm.onCueValue(if (s.cueType == CueType.EMOJI) it.take(4) else it) },
        label = { Text(if (s.cueType == CueType.EMOJI) "Your cue (emoji)" else "Your cue (phrase)") },
        placeholder = { Text(if (s.cueType == CueType.EMOJI) "☕" else "Biscuit's leash is the starting line") },
        modifier = Modifier.fillMaxWidth()
    )
}

/** Step 3 — review & confirm; the bottom bar's "Schedule it" saves. */
@Composable
private fun StepReview(s: BuilderUiState) {
    StepHeader("Ready?", "Here's your intention. The words always travel with the cue.")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (s.cueValue.isNotBlank()) {
            Text(
                if (s.cueType == CueType.EMOJI) s.cueValue else "“${s.cueValue}”",
                style = MaterialTheme.typography.titleMedium,
                color = if (s.cueType == CueType.PHRASE) AppColors.BrandAccentText else MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            "When ${s.whenMoment}, I will ${s.iWill}",
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface
        )
        Text(scheduleSummary(s), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun scheduleSummary(s: BuilderUiState): String {
    val time = formatTime(s.hour, s.minute)
    return when (s.scheduleMode) {
        ScheduleMode.DAILY -> "Every day · $time"
        ScheduleMode.ONCE -> "${if (s.onceDate.isBlank()) "Once" else "On ${formatDate(s.onceDate)}"} · $time"
        else -> {
            val days = WeekDay.ALL.filter { it in s.days }.joinToString(", ") { it.name.lowercase().replaceFirstChar(Char::titlecase).take(3) }
            "$days · $time"
        }
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
private fun DateRow(date: String, onPick: (Int, Int, Int) -> Unit) {
    val context = LocalContext.current
    val cal = remember(date) {
        java.util.Calendar.getInstance().also { c ->
            val parts = date.split("-")
            val y = parts.getOrNull(0)?.toIntOrNull()
            val m = parts.getOrNull(1)?.toIntOrNull()
            val d = parts.getOrNull(2)?.toIntOrNull()
            if (y != null && m != null && d != null) c.set(y, m - 1, d)
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Date", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        TextButton(onClick = {
            android.app.DatePickerDialog(
                context,
                { _, y, m, d -> onPick(y, m + 1, d) },
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH),
                cal.get(java.util.Calendar.DAY_OF_MONTH)
            ).apply { datePicker.minDate = System.currentTimeMillis() - 1000 }.show()
        }) { Text(if (date.isBlank()) "Pick a date" else formatDate(date), color = AppColors.BrandAccentText) }
    }
}

private fun formatDate(iso: String): String {
    val parts = iso.split("-")
    val y = parts.getOrNull(0)?.toIntOrNull() ?: return iso
    val m = parts.getOrNull(1)?.toIntOrNull() ?: return iso
    val d = parts.getOrNull(2)?.toIntOrNull() ?: return iso
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val mon = months.getOrNull(m - 1) ?: return iso
    return "$mon $d, $y"
}

@Composable
private fun Chip(label: String, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    val bg = if (selected && enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
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
            .heightIn(min = 48.dp)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                stateDescription = if (selected) "Selected" else "Not selected"
            }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = fg)
    }
}

@Composable
private fun DayChip(day: WeekDay, selected: Boolean, onClick: () -> Unit) {
    val letter = day.name.first().toString()
    val fullName = when (day) {
        WeekDay.MON -> "Monday"; WeekDay.TUE -> "Tuesday"; WeekDay.WED -> "Wednesday"
        WeekDay.THU -> "Thursday"; WeekDay.FRI -> "Friday"; WeekDay.SAT -> "Saturday"; WeekDay.SUN -> "Sunday"
    }
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
            .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else AppColors.Border, CircleShape)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = fullName
                stateDescription = if (selected) "Selected" else "Not selected"
            },
        contentAlignment = Alignment.Center
    ) {
        Text(letter, style = MaterialTheme.typography.labelLarge, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun StepDots(current: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = "Step ${current + 1} of 4" },
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
                if (isLastStep) "Schedule it" else "Continue",
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