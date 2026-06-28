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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
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
import com.ideasinc.followthrough.ui.theme.AppColors
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** How many of the most recent history buckets show before the user taps "Show earlier". */
private const val VISIBLE_HISTORY_SECTIONS = 2

/** One cue that has driven follow-through, with how many times it worked. */
data class WorkedItem(val reminder: Reminder, val followThroughs: Int)

/**
 * One follow-through in the look-back log: an intention the user actually completed on a given day.
 * Built only from real "Did it" taps (deliberate user actions, so Doze-safe — unlike the old
 * delivery-reconciled grid). [dayLabel] is pre-formatted for its section (weekday this week, date
 * otherwise). Deduped per (intention, day) so a reminderless intention done five times in a day
 * still reads as one calm line, not five.
 */
data class HistoryEntry(
    val reminderId: String,
    val intentionText: String,
    val cueType: String,
    val cueValue: String,
    val dayStart: Long,
    val dayLabel: String
)

/** A time bucket of follow-throughs — "This week", "Earlier this month", or an older month. */
data class HistorySection(val title: String, val entries: List<HistoryEntry>)

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
    val history: List<HistorySection> = emptyList(),
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
            val byId = reminders.associateBy { it.id }
            // One follow-through per (intention, day) — so a double-tap, the shade-plus-in-app
            // paths, or a reminderless intention done several times in a day all read as one.
            val doneDays = events
                .filter { it.action == EventAction.DONE }
                .map { it.reminderId to startOfDayMillis(it.deliveredAt) }
                .toSet()
            val doneByReminder = doneDays.groupingBy { it.first }.eachCount()
            val items = reminders
                .mapNotNull { r -> doneByReminder[r.id]?.let { WorkedItem(r, it) } }
                .sortedByDescending { it.followThroughs }
            // The look-back log: every (intention, day) the user followed through, newest first.
            val historyEntries = doneDays.mapNotNull { (rid, day) -> byId[rid]?.let { it to day } }
            WhatWorkedUiState(items = items, history = buildHistory(historyEntries), loaded = true)
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
 * The **Progress** tab — the single "look back" surface (distinct from Intentions, the "do"
 * surface). A **history of what you've actually followed through on** (Tim's call 2026-06-25),
 * grouped by time, newest first — a positive log, never a streak or a calendar-with-gaps: a miss
 * is simply absent (rule #5, no guilt/loss). Built only from real "Did it" taps, so it's reliable
 * regardless of Doze (unlike the old delivery-reconciled grid). Topped by the one self-discovery
 * line — the cue working best for you — and followed by the occasional direction check-in and past
 * learnings. No running tallies (number-free). This keeps Progress distinct from Intentions, not a
 * second list of the same intentions.
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
    val directionVm: DirectionViewModel = viewModel(
        factory = DirectionViewModel.Factory(
            container.goalDao, container.reminderDao, container.reminderEventDao, container.directionCheckInDao
        )
    )
    val state by vm.uiState.collectAsState()
    val direction by directionVm.uiState.collectAsState()
    // The log can grow long; show the most recent buckets and let the user expand to see it all.
    var historyExpanded by remember { mutableStateOf(false) }

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
                    "A look back at what you've followed through on — and the cue that works best for you.",
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

            // One honest self-discovery line (not a dashboard): the cue that's earned the most
            // follow-throughs, so Progress opens with "what works for me". Shown when there's a
            // genuine standout (the only cue, or one that strictly beats the runner-up).
            // Only when there's a genuine standout to compare: two or more cues, the top strictly
            // beating the runner-up. "Best" of a single cue would read oddly.
            val standout = state.items
                .takeIf { it.size >= 2 && it[0].followThroughs > it[1].followThroughs }
                ?.first()?.reminder
            if (standout != null) {
                item { StandoutCard(standout) }
            }

            if (state.loaded && state.history.isEmpty()) {
                item {
                    Text(
                        "Once you follow through on an intention, it shows up here — a record of what you've actually done.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // The look-back log, grouped by time. Only days you followed through appear — no empty
            // days, no streak, nothing counted against you. Capped to the most recent buckets so a
            // long-time user isn't met with an endless scroll; "Show earlier" reveals the rest.
            val visibleHistory = if (historyExpanded) state.history else state.history.take(VISIBLE_HISTORY_SECTIONS)
            visibleHistory.forEach { section ->
                item(key = "history_${section.title}") { HistorySectionView(section) }
            }
            if (state.history.size > VISIBLE_HISTORY_SECTIONS) {
                item {
                    if (historyExpanded) {
                        TextButton(
                            onClick = { historyExpanded = false },
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)
                        ) { Text("Show less", style = MaterialTheme.typography.labelLarge, color = AppColors.BrandAccentText) }
                    } else {
                        val hidden = state.history.drop(VISIBLE_HISTORY_SECTIONS).sumOf { it.entries.size }
                        TextButton(
                            onClick = { historyExpanded = true },
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)
                        ) { Text("Show earlier ($hidden more)", style = MaterialTheme.typography.labelLarge, color = AppColors.BrandAccentText) }
                    }
                }
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

            // A quiet, research-grounded closing note (implementation intentions / cue-linked
            // habits) — one plain line, no jargon, no copied examples. Shown once there's a log.
            if (state.loaded && state.history.isNotEmpty()) {
                item {
                    Text(
                        "What makes these stick: tying a small action to a moment that already happens in your day.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp)
                    )
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

/** The one self-discovery callout at the top of Progress: the cue that's earned the most follow-
 *  throughs. A calm celebration, not a section header — so it doesn't compete with the log's time
 *  headings. Not interactive (Progress never edits). */
@Composable
private fun StandoutCard(reminder: Reminder) {
    val cueLabel = if (reminder.cueType == CueType.EMOJI) reminder.cueValue else "“${reminder.cueValue}”"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.GoldSurface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            "Working best for you",
            style = MaterialTheme.typography.labelMedium,
            color = AppColors.OnGoldSurface
        )
        Text(
            cueLabel,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = AppColors.OnGoldSurface
        )
    }
}

/** One time bucket of the look-back log: a heading plus the days' follow-throughs in a grouped card. */
@Composable
private fun HistorySectionView(section: HistorySection) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            section.title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 8.dp).semantics { heading() }
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, AppColors.Border, RoundedCornerShape(16.dp))
        ) {
            section.entries.forEachIndexed { i, entry ->
                if (i > 0) HorizontalDivider(color = AppColors.Border)
                HistoryRow(entry)
            }
        }
    }
}

/** One follow-through line: what you did, with a quiet day label. Not interactive — Progress is
 *  the look-back surface, never a second place to edit. Merged semantics so TalkBack reads it as
 *  one item, e.g. "Walked after lunch, Tue". */
@Composable
private fun HistoryRow(entry: HistoryEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (entry.cueType == CueType.EMOJI && entry.cueValue.isNotBlank()) {
            Text(entry.cueValue, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            // Show the phrase/object cue the user set, so the log reflects their own cue
            // wording — mirroring the emoji cue already shown for emoji cues.
            if (entry.cueType == CueType.PHRASE && entry.cueValue.isNotBlank()) {
                Text(
                    "“${entry.cueValue}”",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = AppColors.BrandAccentText
                )
            }
            Text(
                entry.intentionText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            entry.dayLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Device-local start-of-day for [ts] — the (cue, day) bucket key for de-duping follow-throughs. */
private fun startOfDayMillis(ts: Long): Long {
    val c = java.util.Calendar.getInstance().apply { timeInMillis = ts }
    c.set(java.util.Calendar.HOUR_OF_DAY, 0); c.set(java.util.Calendar.MINUTE, 0)
    c.set(java.util.Calendar.SECOND, 0); c.set(java.util.Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

/**
 * Group the user's (intention, day) follow-throughs into time buckets, newest first: "This week"
 * (since Monday), "Earlier this month", then one section per older month. Each entry gets a section-
 * appropriate label — weekday inside this week, "MMM d" otherwise. A bucket exists only if something
 * happened in it: no empty weeks, no gaps, no streak. "Now" is read at compute time (fine for a
 * foreground screen); recomputed whenever the event log changes.
 */
private fun buildHistory(entries: List<Pair<Reminder, Long>>): List<HistorySection> {
    if (entries.isEmpty()) return emptyList()
    val cal = java.util.Calendar.getInstance()
    val startOfWeek = (cal.clone() as java.util.Calendar).apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
        val dow = get(java.util.Calendar.DAY_OF_WEEK)
        val backToMonday = if (dow == java.util.Calendar.SUNDAY) 6 else dow - java.util.Calendar.MONDAY
        add(java.util.Calendar.DAY_OF_MONTH, -backToMonday)
    }.timeInMillis
    val startOfMonth = (cal.clone() as java.util.Calendar).apply {
        set(java.util.Calendar.DAY_OF_MONTH, 1)
        set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
    val thisYear = cal.get(java.util.Calendar.YEAR)
    val weekdayFmt = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())
    val dateFmt = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
    val monthFmt = java.text.SimpleDateFormat("LLLL", java.util.Locale.getDefault())

    // Bucket key = (sort order desc, title). This week / this month sort above any month; older
    // months sort by their own year-month so the newest comes first.
    fun bucketFor(day: Long): Pair<Int, String> = when {
        day >= startOfWeek -> Int.MAX_VALUE to "This week"
        day >= startOfMonth -> (Int.MAX_VALUE - 1) to "Earlier this month"
        else -> {
            val c = java.util.Calendar.getInstance().apply { timeInMillis = day }
            val y = c.get(java.util.Calendar.YEAR)
            val title = if (y == thisYear) monthFmt.format(c.time) else "${monthFmt.format(c.time)} $y"
            (y * 12 + c.get(java.util.Calendar.MONTH)) to title
        }
    }

    return entries
        .map { (r, day) ->
            val label = if (day >= startOfWeek) weekdayFmt.format(day) else dateFmt.format(day)
            bucketFor(day) to HistoryEntry(r.id, r.intentionText, r.cueType, r.cueValue, day, label)
        }
        .groupBy({ it.first }, { it.second })
        .toList()
        .sortedByDescending { it.first.first }
        .map { (bucket, list) -> HistorySection(bucket.second, list.sortedByDescending { it.dayStart }) }
}