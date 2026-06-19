package com.ideasinc.followthrough.ui.worked

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ideasinc.followthrough.data.CueType
import com.ideasinc.followthrough.data.EventAction
import com.ideasinc.followthrough.data.Reminder
import com.ideasinc.followthrough.data.ReminderDao
import com.ideasinc.followthrough.data.ReminderEventDao
import com.ideasinc.followthrough.data.DirectionFeeling
import com.ideasinc.followthrough.di.AppContainer
import com.ideasinc.followthrough.ui.progress.DirectionViewModel
import com.ideasinc.followthrough.ui.progress.DueCheckIn
import com.ideasinc.followthrough.ui.progress.ProgressSection
import com.ideasinc.followthrough.ui.progress.ProgressViewModel
import com.ideasinc.followthrough.ui.theme.AppColors
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** One cue that has driven follow-through, with how many times it worked. */
data class WorkedItem(val reminder: Reminder, val followThroughs: Int)

/**
 * An optional, one-tap "what's getting in the way?" reason on the Not-really check-in path — the
 * moment a user is most likely to drift. [note] is a quiet local signal stored on the check-in
 * (rule #6, never surfaced as a "learning"); [encouragement] is a kind, specific next step that
 * points at tweaking the setup rather than trying harder.
 */
private data class BlockerReason(val label: String, val note: String, val encouragement: String)

private val BLOCKER_REASONS = listOf(
    BlockerReason(
        "Wrong time of day",
        "In the way: the timing",
        "Worth a try: move it to a moment you've actually got a minute free."
    ),
    BlockerReason(
        "Easy to miss the cue",
        "In the way: the cue",
        "Worth a try: pick a cue that's harder to walk past."
    ),
    BlockerReason(
        "Feels too big",
        "In the way: the step felt too big",
        "Worth a try: shrink it to a two-minute version you can't say no to."
    ),
    BlockerReason(
        "Just been busy",
        "In the way: a busy stretch",
        "A busy stretch isn't a failure — the cue brings you back when things settle."
    )
)

data class WhatWorkedUiState(
    val items: List<WorkedItem> = emptyList(),
    val loaded: Boolean = false
)

/**
 * "What worked" — the self-discovery surface (MVP_User_Flow_IA.md). Not a streak and not
 * a score: the cues that actually drove follow-through, so the user can see what works for
 * them and reuse it. Computed live from the response log; a miss is simply absent here,
 * never called out.
 */
class WhatWorkedViewModel(
    private val reminderDao: ReminderDao,
    private val eventDao: ReminderEventDao
) : ViewModel() {

    val uiState: StateFlow<WhatWorkedUiState> =
        // All reminders (not just active) so a completed one-off — archived after it's done —
        // still shows its follow-through here.
        combine(reminderDao.getAllReminders(), eventDao.getLiveEvents()) { reminders, events ->
            // One follow-through per (cue, day) — matches the streak — so "Followed through N times"
            // can't be inflated by a double-tap or the shade-plus-in-app paths logging two rows.
            val doneByReminder = events
                .filter { it.action == EventAction.DONE }
                .map { it.reminderId to startOfDayMillis(it.deliveredAt) }
                .toSet()
                .groupingBy { it.first }
                .eachCount()
            val items = reminders
                .mapNotNull { r -> doneByReminder[r.id]?.let { WorkedItem(r, it) } }
                .sortedByDescending { it.followThroughs }
            WhatWorkedUiState(items = items, loaded = true)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WhatWorkedUiState())

    class Factory(
        private val reminderDao: ReminderDao,
        private val eventDao: ReminderEventDao
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            WhatWorkedViewModel(reminderDao, eventDao) as T
    }
}

/**
 * The **Progress** tab — the single "how am I doing" surface (distinct from Intentions, which is
 * the "do" surface). Leads with the flexible streak + honest week ratio + weekly grid
 * ([ProgressSection]), then "What's been working" — the cues that actually drove follow-through,
 * so the user can see what works for them and reuse it. Combining the two avoids the earlier IA
 * smell of Intentions appearing as two near-identical lists.
 */
@Composable
fun WhatWorkedScreen(
    container: AppContainer,
    onReuse: (String) -> Unit = {},
    onMakeIntention: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val vm: WhatWorkedViewModel = viewModel(
        factory = WhatWorkedViewModel.Factory(container.reminderDao, container.reminderEventDao)
    )
    val progressVm: ProgressViewModel = viewModel(
        factory = ProgressViewModel.Factory(container.reminderEventDao, container.reminderDao)
    )
    val directionVm: DirectionViewModel = viewModel(
        factory = DirectionViewModel.Factory(
            container.goalDao, container.reminderDao, container.reminderEventDao, container.directionCheckInDao
        )
    )
    val state by vm.uiState.collectAsState()
    val progress by progressVm.uiState.collectAsState()
    val direction by directionVm.uiState.collectAsState()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(innerPadding).imePadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Progress",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() }
                )
            }
            item {
                Text(
                    "How you're doing — your streak and this week — and the cues that actually get you to follow through.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // The occasional direction check-in — the most actionable thing when present.
            direction.due?.let { due ->
                item {
                    DirectionCheckInCard(
                        due = due,
                        onAnswer = directionVm::answer,
                        onDismiss = { directionVm.dismiss(due.goalId) },
                        onAdjust = { onReuse(due.reminderId) },
                        onMakeIntention = onMakeIntention
                    )
                }
            }

            if (progress.loaded) {
                item { ProgressSection(progress) }
            }

            item {
                Text(
                    "What's been working",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp).semantics { heading() }
                )
            }

            // One honest self-discovery line (not a dashboard): name the cue that's earned the most
            // follow-throughs, so the section reads as "which cue works for me", not a generic log.
            // Only when there's a genuine standout (top strictly beats the runner-up).
            if (state.loaded && state.items.size >= 2 &&
                state.items[0].followThroughs > state.items[1].followThroughs
            ) {
                item {
                    val top = state.items.first().reminder
                    val cueLabel = if (top.cueType == CueType.EMOJI) top.cueValue else "“${top.cueValue}”"
                    Text(
                        "Working best for you: $cueLabel",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (state.loaded && state.items.isEmpty()) {
                item {
                    Text(
                        "Once you follow through on a cue, it shows up here — so you can see what works for you and use it again.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(state.items, key = { it.reminder.id }) { item ->
                WorkedCard(item = item, onClick = { onReuse(item.reminder.id) })
            }

            // Things you've learned — reusable insights from past check-ins, each able to spark
            // a new intention.
            if (direction.learnings.isNotEmpty()) {
                item {
                    Text(
                        "Things you've learned",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp).semantics { heading() }
                    )
                }
                items(direction.learnings, key = { it.id }) { learning ->
                    LearningCard(text = learning.text, onMakeIntention = { onMakeIntention(learning.text, learning.direction) })
                }
            }
        }
    }
}

/**
 * The occasional direction check-in (Direction_Feature_Spec.md §5). Warm, three options, never a
 * survey: getting there / not really (→ adjust) / learned something (→ note, optionally a new
 * intention). Local sub-state so a chosen path expands inline; the terminal action logs via the
 * VM, after which the card retires (the next due, if any, takes its place).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DirectionCheckInCard(
    due: DueCheckIn,
    onAnswer: (String, String, String?) -> Unit,
    onDismiss: () -> Unit,
    onAdjust: () -> Unit,
    onMakeIntention: (String, String) -> Unit
) {
    // "ask" → "not_really" → "learning"; reset if a different direction becomes due.
    var mode by remember(due.goalId) { mutableStateOf("ask") }
    var note by remember(due.goalId) { mutableStateOf("") }
    var reason by remember(due.goalId) { mutableStateOf<BlockerReason?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.GoldSurface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "A quick check-in",
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.OnGoldSurface,
                modifier = Modifier.weight(1f)
            )
            if (mode == "ask") {
                // Default IconButton keeps the 48dp touch target (WCAG); icon stays small.
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Ask me later", tint = AppColors.OnGoldSurface, modifier = Modifier.size(18.dp))
                }
            }
        }

        when (mode) {
            "ask" -> {
                // Name the intention first, so the prompt is never a context-free "how's it going?".
                Text(
                    "“${due.intention}”",
                    style = MaterialTheme.typography.titleMedium,
                    color = AppColors.OnGoldSurface
                )
                Text(
                    if (due.direction.isBlank()) "Feeling like you're getting there?"
                    else "You set this up to ${due.direction}. Feeling like you're getting there?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.OnGoldSurface
                )
                Button(
                    onClick = { onAnswer(due.goalId, DirectionFeeling.GETTING_THERE, null) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) { Text("Yeah, getting there") }
                OutlinedButton(
                    onClick = { mode = "not_really" },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) { Text("Not really", color = AppColors.BrandAccentText) }
                OutlinedButton(
                    onClick = { mode = "learning" },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) { Text("Learned something", color = AppColors.BrandAccentText) }
            }
            "not_really" -> {
                Text(
                    "Good to know — that's the useful signal. What's getting in the way?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.OnGoldSurface
                )
                // Optional, never required: one tap tailors a kind, specific suggestion. Picking a
                // reason isn't a survey — it just helps us point at the right tweak.
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BLOCKER_REASONS.forEach { r ->
                        ReasonChip(
                            label = r.label,
                            selected = reason == r,
                            onClick = { reason = if (reason == r) null else r }
                        )
                    }
                }
                // Always a warm line; specific once a reason is chosen — never blame, always a tweak.
                Text(
                    reason?.encouragement
                        ?: "Most gains come from tweaking the cue or the moment, not more willpower.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.OnGoldSurface
                )
                Button(
                    onClick = { onAnswer(due.goalId, DirectionFeeling.NOT_REALLY, reason?.note); onAdjust() },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) { Text("Adjust this intention") }
                TextButton(
                    onClick = { onAnswer(due.goalId, DirectionFeeling.NOT_REALLY, reason?.note) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Not now", color = AppColors.BrandAccentText) }
            }
            "learning" -> {
                // On the gold card, the default field (transparent fill + `outline` border)
                // washes out — users miss it. Give it a contrasting filled container and
                // OnGoldSurface-toned border/label/text so it reads as an obvious input (AA).
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("What did you learn?") },
                    placeholder = { Text("e.g. mornings work better than evenings for this") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = AppColors.BrandAccentText,
                        unfocusedBorderColor = AppColors.OnGoldSurface,
                        focusedLabelColor = AppColors.BrandAccentText,
                        unfocusedLabelColor = AppColors.OnGoldSurface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                Button(
                    onClick = { onAnswer(due.goalId, DirectionFeeling.LEARNED, note) },
                    enabled = note.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) { Text("Save note") }
                TextButton(
                    onClick = { onAnswer(due.goalId, DirectionFeeling.LEARNED, note); onMakeIntention(note, due.direction) },
                    enabled = note.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save & make it an intention", color = AppColors.BrandAccentText) }
            }
        }
    }
}

/** A compact, optional reason pill (matches the builder's chips): a ≥48dp toggle that announces
 *  its label and selected state to TalkBack. */
@Composable
private fun ReasonChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val fg = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val border = if (selected) MaterialTheme.colorScheme.primary else AppColors.Border
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(50))
            .clickable(onClick = onClick)
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

/** A past learning, with a quiet way to turn it into a new intention. */
@Composable
private fun LearningCard(text: String, onMakeIntention: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        // A real button → correct role + 48dp touch target for TalkBack/switch access.
        TextButton(onClick = onMakeIntention, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)) {
            Text("Make this an intention", style = MaterialTheme.typography.labelLarge, color = AppColors.BrandAccentText)
        }
    }
}

@Composable
private fun WorkedCard(item: WorkedItem, onClick: () -> Unit) {
    val r = item.reminder
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(16.dp))
            .clickable(onClickLabel = "Open this intention", onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // The CUE leads this surface (it's about which CUES work for the user), but a phrase is
        // words: show it as a quoted line in normal text, not a big brand-coloured heading. An
        // emoji is a visual object, so it can still be shown larger.
        if (r.cueValue.isNotBlank()) {
            if (r.cueType == CueType.EMOJI) {
                Text(r.cueValue, style = MaterialTheme.typography.headlineMedium)
            } else {
                Text(
                    "“${r.cueValue}”",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Text(
            r.intentionText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        // The whole card opens the intention (to review or refine it) — a neutral "Open"
        // that reads correctly whether the intention is still recurring or a finished
        // one-off, not "reuse" (which implies it's done). Count is muted; action is coral.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                followThroughLabel(item.followThroughs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                "Open",
                style = MaterialTheme.typography.labelLarge,
                color = AppColors.BrandAccentText
            )
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = AppColors.BrandAccentText,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun followThroughLabel(n: Int): String =
    if (n == 1) "Followed through once" else "Followed through $n times"

/** Device-local start-of-day for [ts] — the (cue, day) bucket key for de-duping follow-throughs. */
private fun startOfDayMillis(ts: Long): Long {
    val c = java.util.Calendar.getInstance().apply { timeInMillis = ts }
    c.set(java.util.Calendar.HOUR_OF_DAY, 0); c.set(java.util.Calendar.MINUTE, 0)
    c.set(java.util.Calendar.SECOND, 0); c.set(java.util.Calendar.MILLISECOND, 0)
    return c.timeInMillis
}