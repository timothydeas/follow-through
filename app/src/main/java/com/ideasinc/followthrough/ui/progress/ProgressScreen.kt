package com.ideasinc.followthrough.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ideasinc.followthrough.R
import com.ideasinc.followthrough.ui.theme.AppColors

/**
 * The progress block — streak, honest week ratio, record/counts, and the weekly grid — that
 * leads the Progress tab (above the "what's been working" cues). A forgiving Current streak with a
 * two-miss reserve (Milkman) plus an honest ratio + slip note (Fishbach); see
 * [[project_streak_philosophy]] / CLAUDE.md rule #5. The flame celebrates an active streak; the
 * reserve copy reassures ("an occasional miss won't reset it"); the real "this week" ratio and any
 * slip are surfaced plainly (never red, never shamed, always actionable); Longest never resets. All
 * values are local (no telemetry, rule #6).
 */
@Composable
fun ProgressSection(state: ProgressState, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Honest, kind, actionable when a completed week dipped below the user's own cadence
        // (Fishbach: surface it, don't bury it) — never guilt, always a fresh start.
        if (state.slippedLastWeek) SlipNote()

        StreakCard(current = state.currentStreak, reserveRemaining = state.reserveRemaining)

        // The honest in-progress ratio (Fishbach): how this week is actually going against the
        // user's own flexible cadence — shown plainly, framed as info, not a threat.
        ThisWeekCard(
            done = state.weekDoneDistinct,
            opportunities = state.weekOpportunities,
            target = state.weekTarget,
            onTrack = state.onTrack
        )

        // Longest is the never-resetting record that cushions a reset; the counts only climb.
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(Modifier.weight(1f), value = state.longestStreak, label = "Longest")
            StatCard(Modifier.weight(1f), value = state.monthDone, label = "This month")
            StatCard(Modifier.weight(1f), value = state.lifetimeDone, label = "All time")
        }

        WeekGridCard(state.week)
    }
}

/** The hero. A Current streak with a two-miss reserve (CLAUDE.md rule #5): each follow-through
 *  climbs it; an occasional miss is absorbed. Gold + flame only on the celebratory state (an
 *  active streak); calm otherwise. The reserve is always surfaced with reassurance, never a nag. */
@Composable
private fun StreakCard(current: Int, reserveRemaining: Int) {
    val celebratory = current > 0
    val bg = if (celebratory) AppColors.GoldSurface else MaterialTheme.colorScheme.surface
    val fg = if (celebratory) AppColors.OnGoldSurface else MaterialTheme.colorScheme.onSurface
    val unit = if (current == 1) "follow-through" else "follow-throughs"
    // Reassurance only — never "you have N misses left" / loss framing (rule #5).
    val reassurance = "An occasional miss won't reset it."
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.dp, AppColors.Border, RoundedCornerShape(16.dp))
            .padding(20.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = if (celebratory) {
                    "Streak: $current $unit. $reassurance"
                } else {
                    "No streak yet. Follow through on a cue to start one. $reassurance"
                }
            },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Streak", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = fg)
        if (celebratory) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_flame),
                    contentDescription = null,
                    // Gold Feather flame (ic_flame.xml) — the exact glyph in Tim's approved screenshot.
                    tint = AppColors.GoldIcon,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.size(10.dp))
                Text("$current", style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold), color = fg)
                Spacer(Modifier.size(8.dp))
                Text(unit, style = MaterialTheme.typography.titleMedium, color = fg)
            }
        } else {
            Text(
                "Start your streak",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = fg
            )
        }
        Text(
            if (celebratory) reassurance else "Follow through on a cue to begin — $reassurance",
            style = MaterialTheme.typography.bodyMedium,
            color = if (celebratory) fg else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** The honest week-in-progress ratio (Fishbach). Plain, informative, never a threat. */
@Composable
private fun ThisWeekCard(done: Int, opportunities: Int, target: Int, onTrack: Boolean) {
    val headline = if (opportunities == 0) {
        "No cues yet this week — you're all clear."
    } else {
        "$done of ~$opportunities this week"
    }
    val sub = when {
        opportunities == 0 -> null
        onTrack -> "On track for the week. Nice."
        else -> {
            val remaining = (target - done).coerceAtLeast(1)
            "$remaining more to match your usual week."
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "This week: $headline." + (sub?.let { " $it" } ?: "")
            },
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("This week", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
        Text(headline, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        if (sub != null) {
            Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** A completed week dipped below the user's cadence. Honest + actionable, never guilt. */
@Composable
private fun SlipNote() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("Last week dipped below your rhythm", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
        Text(
            "Fresh start this week. If a cue keeps slipping, making it easier to act on often helps more than trying harder.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatCard(modifier: Modifier, value: Int, label: String) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(16.dp))
            .padding(vertical = 16.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$label: $value" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text("$value", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** This week, Mon–Sun. A miss is a plain hollow dot — never red, never called out. */
@Composable
private fun WeekGridCard(week: List<WeekDay>) {
    val labels = listOf("M", "T", "W", "T", "F", "S", "S")
    val full = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Day by day", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            week.forEachIndexed { i, day ->
                DayCell(label = labels.getOrElse(i) { "" }, dayName = full.getOrElse(i) { "" }, state = day.state)
            }
        }
    }
}

@Composable
private fun DayCell(label: String, dayName: String, state: DayState) {
    val desc = when (state) {
        DayState.DONE -> "$dayName: followed through"
        DayState.MISS -> "$dayName: missed"
        DayState.PENDING -> "$dayName: today, not yet"
        DayState.NONE -> "$dayName: nothing scheduled"
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.clearAndSetSemantics { contentDescription = desc }
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Marker(state)
    }
}

@Composable
private fun Marker(state: DayState) {
    when (state) {
        DayState.DONE -> Column(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(AppColors.GoldSurface)
                // The pale gold fill is ~1.2:1 against the white card — near-invisible on its own.
                // A gold edge (GoldIcon, 3:1+) gives the marker a defined boundary, consistent with
                // the bordered miss/pending states, so a low-vision user can see the circle, not
                // just the check inside it.
                .border(1.dp, AppColors.GoldIcon, CircleShape),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = AppColors.OnGoldSurface,
                modifier = Modifier.size(20.dp).padding(top = 6.dp)
            )
        }
        // A miss is shown plainly: a neutral grey ring — visible (so a low-vision user can tell it
        // apart from an empty day), but never red, never weighted, never shamed.
        DayState.MISS -> Column(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
        ) {}
        // Today, not yet acted — a soft outline in the brand accent, inviting not nagging.
        DayState.PENDING -> Column(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .border(1.dp, AppColors.BrandAccentText, CircleShape)
        ) {}
        // Nothing scheduled — barely-there placeholder so the row stays aligned.
        DayState.NONE -> Column(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background)
        ) {}
    }
}
