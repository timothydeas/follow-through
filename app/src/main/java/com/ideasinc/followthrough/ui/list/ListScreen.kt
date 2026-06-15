package com.ideasinc.followthrough.ui.list

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import com.ideasinc.followthrough.R
import com.ideasinc.followthrough.ui.theme.AppColors

/**
 * Goals — "What you're working toward — and why it matters." A header + purpose
 * line over a card grid (two columns on tablet, one on phone). Each card shows the
 * goal title, its "why it matters" subtitle, and a coral reminder count; tapping
 * opens goal detail.
 */
@Composable
fun ListScreen(
    viewModel: ListViewModel,
    onGoalClick: (String) -> Unit,
    onNewGoal: () -> Unit,
    isExpandedWidth: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // The gentle "focus on fewer goals" message — at most twice, ~6 days apart,
    // while over three active goals. Timing decision lives in decideFocusReminder.
    LaunchedEffect(viewModel) {
        val prefs = context.getSharedPreferences(FOCUS_PREFS_NAME, Context.MODE_PRIVATE)
        migrateLegacyFocusPref(prefs)
        snapshotFlow { uiState.goals.size }.collect { count ->
            val state = readFocusReminderState(prefs)
            val decision = decideFocusReminder(count, state, System.currentTimeMillis())
            if (decision.newState != state) writeFocusReminderState(prefs, decision.newState)
            if (decision.show) {
                snackbarHostState.showSnackbar(
                    message = "The more goals you try to focus on at once, the harder it gets to make real progress on any of them. Pick the ones that matter most right now.",
                    duration = SnackbarDuration.Long
                )
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewGoal,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.semantics { contentDescription = "Add new goal" }
            ) {
                Icon(painter = painterRes(R.drawable.ic_plus), contentDescription = null, modifier = Modifier.size(24.dp))
            }
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (isExpandedWidth) 2 else 1),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Column {
                    Text(
                        "Goals",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        "What you're working toward — and why it matters.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                SearchBar(query = uiState.query, onQueryChange = viewModel::onQueryChange, modifier = Modifier.fillMaxWidth())
            }

            if (uiState.goals.isEmpty()) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    EmptyState(hasQuery = uiState.query.isNotBlank())
                }
            } else {
                items(uiState.goals, key = { it.goal.id }) { row ->
                    GoalCard(row = row, onClick = { onGoalClick(row.goal.id) })
                }
            }
        }
    }
}

@Composable
private fun GoalCard(row: GoalRowData, onClick: () -> Unit) {
    val why = row.goal.whyItMatters.trim()
    val reminderLabel = when (row.reminderCount) {
        0 -> "No reminders yet"
        1 -> "1 reminder"
        else -> "${row.reminderCount} reminders"
    }
    val a11y = buildString {
        append(row.goal.title)
        if (why.isNotEmpty()) append(". $why")
        append(". $reminderLabel")
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClickLabel = "Open goal", onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = a11y }
            .padding(start = 18.dp, end = 12.dp, top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = row.goal.title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (why.isNotEmpty()) {
                Text(
                    text = why,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = reminderLabel,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = AppColors.BrandAccentText,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(painter = painterRes(R.drawable.ic_search), contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text("Search goals...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Search goals" }
                )
            }
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyState(hasQuery: Boolean) {
    Box(modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 12.dp, end = 12.dp), contentAlignment = Alignment.Center) {
        Text(
            text = if (hasQuery) "No goals match your search."
            else "Tap + to add your first goal — what you're working toward, and why it matters.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun painterRes(id: Int) = androidx.compose.ui.res.painterResource(id = id)

// SharedPreferences backing for the focus message's two-show tracking.
private const val FOCUS_PREFS_NAME = "grounded_prefs"
private const val KEY_FOCUS_FIRST_SHOWN_AT = "priority_warning_first_shown_at"
private const val KEY_FOCUS_SECOND_SHOWN = "priority_warning_second_shown"
private const val KEY_FOCUS_LEGACY_SHOWN = "priority_warning_shown"

private fun readFocusReminderState(prefs: android.content.SharedPreferences): FocusReminderState {
    val firstAt = prefs.getLong(KEY_FOCUS_FIRST_SHOWN_AT, 0L)
    return FocusReminderState(
        firstShownAt = if (firstAt == 0L) null else firstAt,
        secondShown = prefs.getBoolean(KEY_FOCUS_SECOND_SHOWN, false)
    )
}

private fun writeFocusReminderState(prefs: android.content.SharedPreferences, state: FocusReminderState) {
    prefs.edit()
        .putLong(KEY_FOCUS_FIRST_SHOWN_AT, state.firstShownAt ?: 0L)
        .putBoolean(KEY_FOCUS_SECOND_SHOWN, state.secondShown)
        .apply()
}

private fun migrateLegacyFocusPref(prefs: android.content.SharedPreferences) {
    if (prefs.contains(KEY_FOCUS_FIRST_SHOWN_AT)) return
    if (prefs.getBoolean(KEY_FOCUS_LEGACY_SHOWN, false)) {
        prefs.edit()
            .putLong(KEY_FOCUS_FIRST_SHOWN_AT, System.currentTimeMillis())
            .putBoolean(KEY_FOCUS_SECOND_SHOWN, true)
            .remove(KEY_FOCUS_LEGACY_SHOWN)
            .apply()
    }
}
