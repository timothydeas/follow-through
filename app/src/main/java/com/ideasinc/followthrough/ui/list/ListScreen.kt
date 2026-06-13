@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.ideasinc.followthrough.ui.list

import android.content.Context
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ideasinc.followthrough.R
import com.ideasinc.followthrough.ui.rememberIsTouchExplorationEnabled
import com.ideasinc.followthrough.ui.theme.AppColors
import kotlinx.coroutines.flow.drop
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ListScreen(
    viewModel: ListViewModel,
    onGoalClick: (String) -> Unit,
    onNewGoal: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val accentColor = AppColors.BrandAccentText
    val lazyListState = rememberLazyListState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val touchExplorationOn = rememberIsTouchExplorationEnabled()

    var moveDialogTarget: MoveDialogTarget? by remember { mutableStateOf(null) }

    // Each LazyColumn index corresponds directly to a goal's flat position.
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        viewModel.onDragMove(from.index, to.index)
    }

    // Persist on drag end. drop(1) skips the initial false emission so we only
    // react to the actual drag-stopped transition.
    LaunchedEffect(reorderableState) {
        snapshotFlow { reorderableState.isAnyItemDragging }
            .drop(1)
            .collect { isDragging ->
                if (!isDragging) viewModel.onDragEnd()
            }
    }

    // The "focus on fewer goals" message. It appears at most twice — the first
    // time the user has more than three active goals (adding or reordering into
    // that state, whichever happens first), then once more ~6 days later while
    // still over three. Driving it off the observed goal count keeps a single
    // trigger, so it never double-fires. The timing decision lives in the
    // unit-tested pure [decideFocusReminder].
    LaunchedEffect(viewModel) {
        val prefs = context.getSharedPreferences(FOCUS_PREFS_NAME, Context.MODE_PRIVATE)
        migrateLegacyFocusPref(prefs)
        snapshotFlow { uiState.goals.size }.collect { count ->
            val state = readFocusReminderState(prefs)
            val decision = decideFocusReminder(count, state, System.currentTimeMillis())
            if (decision.newState != state) writeFocusReminderState(prefs, decision.newState)
            if (decision.show) {
                snackbarHostState.showSnackbar(
                    message = "The more goals you try to focus on at once, the harder it gets to make real progress on any of them. Other goals will pull you in different directions. Pick the ones that matter most right now.",
                    duration = SnackbarDuration.Long
                )
            }
        }
    }

    moveDialogTarget?.let { target ->
        MoveToPositionDialog(
            totalCount = target.totalCount,
            onDismiss = { moveDialogTarget = null },
            onMove = { position ->
                viewModel.moveGoalToPosition(target.goalId, position)
                moveDialogTarget = null
            }
        )
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
                Icon(
                    painter = painterResource(id = R.drawable.ic_plus),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 4.dp, top = 24.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .semantics(mergeDescendants = true) {
                            heading()
                            contentDescription = "FollowThru — in every moment"
                        }
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = accentColor)) { append("FollowThru") }
                        },
                        style = MaterialTheme.typography.displayMedium
                    )
                    Text(
                        text = "in every moment",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_settings),
                        contentDescription = "Open settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

                SearchBar(
                    query = uiState.query,
                    onQueryChange = viewModel::onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                val totalCount = uiState.goals.size

                if (totalCount == 0) {
                    EmptyState(hasQuery = uiState.query.isNotBlank())
                } else {
                    LazyColumn(
                        state = lazyListState,
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(
                            uiState.goals,
                            key = { _, row -> "goal_${row.goal.id}" }
                        ) { idx, row ->
                            ReorderableItem(reorderableState, key = "goal_${row.goal.id}") { dragging ->
                                // longPressDraggableHandle() is an extension on the
                                // ReorderableScope receiver of this lambda, so it
                                // can only be constructed here.
                                DraggableGoalCard(
                                    row = row,
                                    rank = row.rank,
                                    isDragging = dragging,
                                    showA11yArrows = touchExplorationOn,
                                    canMoveUp = idx > 0,
                                    canMoveDown = idx < totalCount - 1,
                                    dragModifier = Modifier.longPressDraggableHandle(),
                                    onClick = { onGoalClick(row.goal.id) },
                                    onMoveUp = { viewModel.moveGoal(row.goal.id, -1) },
                                    onMoveDown = { viewModel.moveGoal(row.goal.id, +1) },
                                    onArrowLongPress = {
                                        moveDialogTarget = MoveDialogTarget(row.goal.id, totalCount)
                                    }
                                )
                            }
                        }
                    }
                }
        }
    }
}

private data class MoveDialogTarget(val goalId: String, val totalCount: Int)

// ─── Goal card ─────────────────────────────────────────────────────────────

@Composable
private fun DraggableGoalCard(
    row: GoalRowData,
    rank: Int?,
    isDragging: Boolean,
    showA11yArrows: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    dragModifier: Modifier,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onArrowLongPress: () -> Unit
) {
    val elevation by animateDpAsState(
        targetValue = if (isDragging) 12.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "cardElevation"
    )
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.05f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "cardScale"
    )

    // dragModifier carries the long-press handle from the ReorderableScope, so a
    // long-press anywhere on the card starts a drag. The card is a white surface
    // with a hairline border on the cream page (prototype look). Only the top
    // three goals (rank 1–3) carry a coral number badge — the "top priorities" —
    // plus a coral accent strip down their left edge; goals below sit in the same
    // list, unnumbered and unstriped.
    Surface(
        modifier = dragModifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .zIndex(if (isDragging) 1f else 0f),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, AppColors.Border),
        tonalElevation = elevation,
        shadowElevation = elevation
    ) {
        // A 4dp accent-strip column is reserved on every card — placed as the
        // first child, tight at the card's left edge, exactly like the original
        // (pre-a3594a5) — so all content, and therefore all titles, stay aligned
        // whether or not a card is ranked. It is colored coral only on the
        // top-three (ranked) cards; the content's own 16dp padding sets the same
        // strip-to-content spacing the original had. height(IntrinsicSize.Min)
        // lets the strip fillMaxHeight edge-to-edge, clipped to the card's
        // rounded corners by the Surface.
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .then(
                        if (rank != null) Modifier.background(AppColors.PriorityContainer)
                        else Modifier
                    )
            )
            Box(modifier = Modifier.weight(1f)) {
                GoalCardContent(
                    row = row,
                    rank = rank,
                    onClick = onClick,
                    showA11yArrows = showA11yArrows,
                    canMoveUp = canMoveUp,
                    canMoveDown = canMoveDown,
                    onMoveUp = onMoveUp,
                    onMoveDown = onMoveDown,
                    onArrowLongPress = onArrowLongPress
                )
            }
        }
    }
}

@Composable
private fun GoalCardContent(
    row: GoalRowData,
    rank: Int?,
    onClick: () -> Unit,
    showA11yArrows: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onArrowLongPress: () -> Unit
) {
    val hasReminder = row.hasReminder
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = "Open goal",
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Number-badge column, reserved on every card so all titles align
            // on the same left edge. The top three goals (rank 1–3) fill it with
            // their coral number badge; goals below leave the slot empty. The
            // badge's presence remains the only ranking signal. Sized and weighted
            // to feel deliberate alongside the Stats screen (coral kept).
            Box(
                modifier = Modifier.size(30.dp),
                contentAlignment = Alignment.Center
            ) {
                if (rank != null) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .semantics { contentDescription = "Priority $rank" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$rank",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = row.goal.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (hasReminder) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_bell),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(22.dp)
                        .clearAndSetSemantics {}
                )
            }
            if (showA11yArrows) {
                A11yReorderArrows(
                    canMoveUp = canMoveUp,
                    canMoveDown = canMoveDown,
                    onMoveUp = onMoveUp,
                    onMoveDown = onMoveDown,
                    onLongPress = onArrowLongPress
                )
                Spacer(Modifier.width(4.dp))
            }
            DragHandleIcon()
        }

        // Supporting line aligns under the title — indented past the reserved
        // badge column (30dp) plus its 12dp spacer so it sits flush under the title.
        val supportingIndent = 42.dp
        // Subtitle: the goal's most recent reminder intention. A gentle prompt when
        // there's no reminder yet.
        val subtitle = row.currentIntention?.takeIf { it.isNotBlank() }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = supportingIndent)
            )
        } else {
            Text(
                text = "No reminder yet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = supportingIndent)
            )
        }
    }
}

@Composable
private fun DragHandleIcon() {
    // Decorative only — the whole card is the long-press drag handle, and a11y
    // users get the explicit A11yReorderArrows. clearAndSetSemantics removes
    // this Box from the TalkBack tree so it isn't announced as actionable. The
    // 48dp box keeps a comfortable target footprint; the grip itself is the
    // modern six-dot pattern (two columns of three).
    Box(
        modifier = Modifier
            .size(48.dp)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center
    ) {
        SixDotGrip(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            dotSize = 4.dp,
            gap = 5.dp
        )
    }
}

/** A six-dot drag grip — two columns of three dots, intentional not frail. */
@Composable
private fun SixDotGrip(
    color: androidx.compose.ui.graphics.Color,
    dotSize: androidx.compose.ui.unit.Dp,
    gap: androidx.compose.ui.unit.Dp
) {
    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
        repeat(2) {
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(dotSize)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }
        }
    }
}

// ─── Accessibility arrow buttons ──────────────────────────────────────────

@Composable
private fun A11yReorderArrows(
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onLongPress: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ArrowButton(
            icon = Icons.Filled.KeyboardArrowUp,
            description = "Move up",
            enabled = canMoveUp,
            onClick = onMoveUp,
            onLongPress = onLongPress
        )
        Spacer(Modifier.width(2.dp))
        ArrowButton(
            icon = Icons.Filled.KeyboardArrowDown,
            description = "Move down",
            enabled = canMoveDown,
            onClick = onMoveDown,
            onLongPress = onLongPress
        )
    }
}

@Composable
private fun ArrowButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val tint = if (enabled)
        MaterialTheme.colorScheme.onSurfaceVariant
    else
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .combinedClickable(
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongPress,
                role = Role.Button,
                onClickLabel = description,
                onLongClickLabel = "Move to position"
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}

// ─── Move-to-position dialog ──────────────────────────────────────────────

@Composable
private fun MoveToPositionDialog(
    totalCount: Int,
    onDismiss: () -> Unit,
    onMove: (Int) -> Unit
) {
    var input by remember { mutableStateOf("") }
    val parsed = input.toIntOrNull()
    val isValid = parsed != null && parsed in 1..totalCount

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Move to position",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                Text(
                    text = "Enter a position from 1 to $totalCount.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { raw -> input = raw.filter { c -> c.isDigit() }.take(4) },
                    label = { Text("Position (1 - $totalCount)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let { onMove(it) } },
                enabled = isValid
            ) { Text("Move") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ─── Search bar / empty state / formatting ────────────────────────────────

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_search),
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search goals...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Search goals" }
                )
            }
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(hasQuery: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (hasQuery) "No goals match your search."
            else "Welcome to FollowThru. Tap the + button below to add your first goal or change you're working toward.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private val dateFormatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
private fun formatDate(epochMs: Long): String = dateFormatter.format(Date(epochMs))

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

private fun writeFocusReminderState(
    prefs: android.content.SharedPreferences,
    state: FocusReminderState
) {
    prefs.edit()
        .putLong(KEY_FOCUS_FIRST_SHOWN_AT, state.firstShownAt ?: 0L)
        .putBoolean(KEY_FOCUS_SECOND_SHOWN, state.secondShown)
        .apply()
}

/**
 * Folds the old single "shown once" boolean into the new two-show tracking: a
 * user who already saw the message under the drag-only trigger is marked fully
 * done (both showings consumed) so it never reappears for them.
 */
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
