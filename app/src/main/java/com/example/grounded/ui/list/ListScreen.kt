@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.grounded.ui.list

import android.content.Context
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
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
import com.example.grounded.ui.rememberIsTouchExplorationEnabled
import com.example.grounded.ui.theme.PrimaryForge
import com.example.grounded.ui.theme.PrimaryForgeAccessible
import com.example.grounded.ui.theme.PrimaryForgeLight
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
    onSettingsClick: () -> Unit,
    onStatsClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val accentColor = MaterialTheme.colorScheme.primary
    val lazyListState = rememberLazyListState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val touchExplorationOn = rememberIsTouchExplorationEnabled()

    var moveDialogTarget: MoveDialogTarget? by remember { mutableStateOf(null) }

    // The reorderable library's onMove gives us LazyColumn indices. The
    // "Your focus goals" label sits at LazyColumn index 0 as a non-reorderable
    // item, so each goal's LazyColumn index is offset by 1 from its flat
    // position. The label is non-reorderable, so the library never targets it
    // for drops; the null-guard here is defensive.
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val fromFlat = lazyToFlat(from.index) ?: return@rememberReorderableLazyListState
        val toFlat = lazyToFlat(to.index) ?: return@rememberReorderableLazyListState
        viewModel.onDragMove(fromFlat, toFlat)
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

    LaunchedEffect(viewModel) {
        viewModel.priorityLimitWarning.collect {
            val prefs = context.getSharedPreferences("grounded_prefs", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("priority_warning_shown", false)) {
                prefs.edit().putBoolean("priority_warning_shown", true).apply()
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
                Icon(Icons.Default.Add, contentDescription = null)
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
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = accentColor)) { append("Follow Through") }
                    },
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() }
                )
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Open settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (uiState.streakDays > 0 || uiState.totalFollowThroughs > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            onClickLabel = "Tap to see full stats",
                            role = Role.Button,
                            onClick = onStatsClick
                        )
                        .semantics { contentDescription = "Tap to see full stats" }
                        .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatChip(
                        emoji = "🔥",
                        value = uiState.streakDays,
                        label = "Check-In Streak",
                        a11y = "Check-In Streak ${uiState.streakDays}"
                    )
                    StatChip(
                        emoji = "✓",
                        value = uiState.totalFollowThroughs,
                        label = "Follow Throughs",
                        a11y = "Follow Throughs ${uiState.totalFollowThroughs}"
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            SearchBar(
                query = uiState.query,
                onQueryChange = viewModel::onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
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
                    item(key = "focus_goals_label") {
                        FocusGoalsLabel()
                    }

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

@Composable
private fun StatChip(emoji: String, value: Int, label: String, a11y: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.semantics { contentDescription = a11y }
    ) {
        Text(
            text = "$emoji $value",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class MoveDialogTarget(val goalId: String, val totalCount: Int)

// LazyColumn structure during drag:
//   index 0                 : "focus_goals_label"  (non-reorderable)
//   index 1..uiState.goals.size : goal items       (flat 0..size-1)
private fun lazyToFlat(lazyIndex: Int): Int? =
    if (lazyIndex == 0) null else lazyIndex - 1

// ─── Focus goals label ─────────────────────────────────────────────────────

@Composable
private fun FocusGoalsLabel() {
    var showTooltip by remember { mutableStateOf(false) }

    if (showTooltip) {
        AlertDialog(
            onDismissRequest = { showTooltip = false },
            text = {
                Text(
                    "The more goals competing for your attention, the harder it gets to make real progress on any of them. These are the ones you've chosen to focus on right now.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showTooltip = false }) { Text("Got it") }
            }
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp)
    ) {
        Text(
            text = "Your focus goals",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { heading() }
        )
        IconButton(
            onClick = { showTooltip = true },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "About focus goals",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

// ─── Goal card ─────────────────────────────────────────────────────────────

@Composable
private fun DraggableGoalCard(
    row: GoalRowData,
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
    val isPriority = row.rank != null

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

    val baseBg = if (isPriority)
        PrimaryForgeLight.copy(alpha = 0.35f)
    else
        MaterialTheme.colorScheme.surfaceVariant

    // dragModifier carries the long-press handle from the ReorderableScope.
    // Applying it to the whole Surface lets the user long-press anywhere on
    // the card to start dragging.
    Surface(
        modifier = dragModifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .zIndex(if (isDragging) 1f else 0f),
        shape = RoundedCornerShape(14.dp),
        color = baseBg,
        tonalElevation = elevation,
        shadowElevation = elevation
    ) {
        GoalCardContent(
            row = row,
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

@Composable
private fun GoalCardContent(
    row: GoalRowData,
    onClick: () -> Unit,
    showA11yArrows: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onArrowLongPress: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = "Open goal",
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (row.rank != null) {
                val rankColor = PrimaryForge
                Text(
                    text = "${row.rank}",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = rankColor
                    ),
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .semantics { contentDescription = "Priority rank ${row.rank}" }
                )
            }
            Text(
                text = row.goal.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
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

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "${row.checkInCount} check-in${if (row.checkInCount == 1) "" else "s"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            row.latestCheckInDate?.let {
                Text(
                    text = formatDate(it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DragHandleIcon() {
    Box(
        modifier = Modifier
            .size(36.dp)
            .semantics { contentDescription = "Drag to reorder" },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.DragHandle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.size(20.dp)
        )
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
    val tint = if (enabled) LocalContentColor.current.copy(alpha = 0.7f)
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    Box(
        modifier = Modifier
            .size(36.dp)
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
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search goals…",
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
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = if (hasQuery) "No goals match your search."
            else "Nothing here yet.\nTap + to add a goal.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private val dateFormatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
private fun formatDate(epochMs: Long): String = dateFormatter.format(Date(epochMs))
