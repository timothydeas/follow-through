package com.ideasinc.followthrough.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.content.Intent
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ideasinc.followthrough.R
import com.ideasinc.followthrough.data.CueType
import com.ideasinc.followthrough.data.EventAction
import com.ideasinc.followthrough.data.Reminder
import com.ideasinc.followthrough.data.ScheduleMode
import com.ideasinc.followthrough.di.AppContainer
import com.ideasinc.followthrough.navigation.KEY_REMINDERS_PAUSED
import com.ideasinc.followthrough.navigation.PREFS_NAME
import com.ideasinc.followthrough.ui.theme.AppColors
import kotlinx.coroutines.launch

/**
 * Intentions — the home tab. The user's active intentions, each showing its cue PLUS the
 * full intention text. The single response is **Did it** (undoable via an 8-second
 * snackbar). No streak, no score — a missed cue is a non-event, never logged against the
 * user (MVP_User_Flow_IA.md). The FAB starts a new intention.
 */
@Composable
fun TodayScreen(
    container: AppContainer,
    onNewReminder: () -> Unit,
    onEditReminder: (String) -> Unit = {},
    onOpenProgress: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val appContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val vm: TodayViewModel = viewModel(
        factory = TodayViewModel.Factory(appContext, container.reminderDao, container.reminderEventDao, container.goalDao)
    )
    val state by vm.uiState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    // Re-read on each composition so it refreshes when the user returns from settings.
    val notificationsOn = NotificationManagerCompat.from(ctx).areNotificationsEnabled()
    val remindersPaused = ctx.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getBoolean(KEY_REMINDERS_PAUSED, false)

    fun onDid(reminder: Reminder) {
        vm.act(reminder, EventAction.DONE) { eventId ->
            scope.launch {
                // 8-second Undo window; auto-dismisses after 8s or on an earlier tap.
                val result = kotlinx.coroutines.withTimeoutOrNull(8_000) {
                    snackbarHost.showSnackbar(
                        message = "Followed through. Nice.", actionLabel = "Undo", duration = SnackbarDuration.Indefinite
                    )
                }
                if (result == SnackbarResult.ActionPerformed) vm.undo(eventId)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHost) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewReminder,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                // Circular to echo the round launcher icon (Tim's call).
                shape = CircleShape,
                modifier = Modifier.semantics { contentDescription = "New intention" }
            ) { Icon(Icons.Rounded.Add, contentDescription = null) }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Intentions", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.semantics { heading() })
            }
            item {
                Text("What you'll remember, when it matters.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (!notificationsOn) {
                item {
                    NotificationOffBanner(onClick = {
                        ctx.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                        )
                    })
                }
            }

            if (remindersPaused) {
                item { PausedBanner(onClick = onOpenSettings) }
            }

            if (state.loaded && (state.streak > 0 || state.progress.weeklyDone > 0 || state.progress.lifetimeDone > 0)) {
                item { StreakChip(streak = state.streak, weeklyDone = state.progress.weeklyDone, onClick = onOpenProgress) }
            }

            if (state.loaded && state.items.isEmpty()) {
                item {
                    Text(
                        "No intentions yet. Tap + to create your first one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }

            items(state.items, key = { it.reminder.id }) { item ->
                IntentionCard(
                    reminder = item.reminder,
                    direction = item.direction,
                    paused = remindersPaused,
                    onEdit = { onEditReminder(item.reminder.id) },
                    onDid = { onDid(item.reminder) }
                )
            }
        }
    }
}

@Composable
private fun IntentionCard(
    reminder: Reminder,
    direction: String,
    paused: Boolean,
    onEdit: () -> Unit,
    onDid: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(16.dp))
            // Tap the card to edit this intention (time, days, wording, cue). The Did it
            // button below consumes its own taps, so it's unaffected. onClickLabel names the
            // action for TalkBack WITHOUT overriding the card's text (cue + intention), so each
            // card is still announced distinctly.
            .clickable(onClickLabel = "Edit intention", onClick = onEdit)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            if (reminder.cueType == CueType.EMOJI && reminder.cueValue.isNotBlank()) {
                Text(reminder.cueValue, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                if (reminder.cueType == CueType.PHRASE && reminder.cueValue.isNotBlank()) {
                    Text("“${reminder.cueValue}”", style = MaterialTheme.typography.titleMedium, color = AppColors.BrandAccentText)
                }
                Text(reminder.intentionText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Show "Paused" on scheduled intentions while reminders are paused.
                    val label = if (paused && reminder.scheduleMode != ScheduleMode.NONE) "Paused" else scheduleLabel(reminder)
                    Text(label, style = MaterialTheme.typography.bodySmall, color = AppColors.BrandAccentText)
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = null,
                        tint = AppColors.BrandAccentText,
                        modifier = Modifier.size(14.dp)
                    )
                }
                // The direction this intention serves (optional). Subtle — meaning, not a label.
                if (direction.isNotBlank()) {
                    Text(
                        "Toward: $direction",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
        // The single response. No response means not done — logged neutrally, no penalty.
        OutlinedButton(
            onClick = onDid,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) {
            Text("Did it", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium), color = AppColors.BrandAccentText)
        }
    }
}

/** Compact streak chip (Milkman two-miss reserve): a flame + the current follow-through streak, or
 *  a gentle nudge before one exists. Gold only on the celebratory state (active streak). Taps
 *  through to the full Progress screen (streak, honest ratio, record, weekly grid). */
@Composable
private fun StreakChip(streak: Int, weeklyDone: Int, onClick: () -> Unit) {
    val celebratory = streak > 0
    val bg = if (celebratory) AppColors.GoldSurface else MaterialTheme.colorScheme.surface
    val fg = if (celebratory) AppColors.OnGoldSurface else MaterialTheme.colorScheme.onSurface
    val label = when {
        streak == 1 -> "1 follow-through streak"
        streak > 1 -> "$streak follow-through streak"
        weeklyDone > 0 -> "$weeklyDone this week — keep it going"
        else -> "See your progress"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.dp, AppColors.Border, RoundedCornerShape(16.dp))
            .clickable(onClickLabel = "See your progress", onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Flame celebrates an active streak only (CLAUDE.md rule #5) — hidden in the calm,
        // streak-0 state so it never reads as a nag or an empty trophy.
        if (celebratory) {
            Icon(
                painter = painterResource(R.drawable.ic_flame),
                contentDescription = null,
                // Gold Feather flame (ic_flame.xml) — the exact glyph in Tim's approved screenshot.
                tint = AppColors.GoldIcon,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = fg, modifier = Modifier.weight(1f))
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = if (celebratory) fg else AppColors.BrandAccentText,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** Shown when notifications are blocked — for a reminders app this is the biggest retention
 *  leak (cues silently never fire). Taps through to the system notification settings. */
@Composable
private fun NotificationOffBanner(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.CoralTint)
            .clickable(onClickLabel = "Turn on notifications", onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("Notifications are off", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
            Text("Your reminders can't reach you. Tap to turn them on.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = AppColors.BrandAccentText, modifier = Modifier.size(20.dp))
    }
}

/** Shown when reminders are paused (e.g. vacation) — calm, not an alert: nothing fires until
 *  resumed, and the streak is safe. Taps through to Settings to turn them back on. */
@Composable
private fun PausedBanner(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(16.dp))
            .clickable(onClickLabel = "Resume reminders in Settings", onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("Reminders paused", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
            Text("They won't fire until you turn them back on — your streak is safe. Tap to manage.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = AppColors.BrandAccentText, modifier = Modifier.size(20.dp))
    }
}

/** "HH:mm" 24h → a friendly 12h label, e.g. "06:45" → "6:45 AM". */
private fun formatTime(hhmm: String): String {
    val parts = hhmm.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: return hhmm
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val period = if (h < 12) "AM" else "PM"
    val h12 = when { h == 0 -> 12; h > 12 -> h - 12; else -> h }
    return "%d:%02d %s".format(h12, m, period)
}

/** When an intention fires, shown on its card: e.g. "Every day · 6:45 AM",
 *  "Mon, Wed, Fri · 5:30 PM", or "Mar 5 · 4:00 PM" for a one-off. */
private fun scheduleLabel(r: Reminder): String {
    val time = formatTime(r.scheduleTimeLocal)
    return when (r.scheduleMode) {
        ScheduleMode.NONE -> "No reminder · tap to add"
        ScheduleMode.DAILY -> "Every day · $time"
        ScheduleMode.ONCE -> r.scheduleDate?.takeIf { it.isNotBlank() }?.let { "${shortDate(it)} · $time" } ?: "Once · $time"
        else -> {
            val days = r.days.joinToString(", ") { it.name.lowercase().replaceFirstChar(Char::titlecase).take(3) }
            if (days.isBlank()) time else "$days · $time"
        }
    }
}

/** "yyyy-MM-dd" → "Mar 5". */
private fun shortDate(iso: String): String {
    val p = iso.split("-")
    val m = p.getOrNull(1)?.toIntOrNull() ?: return iso
    val d = p.getOrNull(2)?.toIntOrNull() ?: return iso
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    return "${months.getOrNull(m - 1) ?: return iso} $d"
}
