package com.ideasinc.followthrough.ui.stats

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ideasinc.followthrough.R
import com.ideasinc.followthrough.di.AppContainer
import com.ideasinc.followthrough.ui.theme.AppColors
import com.ideasinc.followthrough.ui.theme.PoppinsFontFamily

@Composable
fun StatsScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit,
    onOpenFollowThrus: () -> Unit
) {
    val vm: StatsViewModel = viewModel(
        factory = StatsViewModel.Factory(container.goalDao, container.checkInDao)
    )
    val state by vm.uiState.collectAsState()
    val backFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { backFocus.requestFocus() } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.focusRequester(backFocus)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_left),
                        contentDescription = "Go back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Your progress",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                        .semantics { heading() }
                )
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_settings),
                        contentDescription = "Open settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        val isEmpty = state.checkInTotal == 0 && state.followThroughTotal == 0
        if (isEmpty) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No stats yet — your progress will appear after your first check-in.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "A gentle look at your own rhythm. Looking anyway, gently, is how we learn.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            CheckInStreakCard(streak = state.checkInCurrentStreak)

            SectionLabel("CHECK-INS")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("${state.checkInCurrentStreak}", "Current", Modifier.weight(1f))
                StatTile("${state.checkInLongestStreak}", "Longest", Modifier.weight(1f))
                StatTile("${state.checkInTotal}", "Total", Modifier.weight(1f))
            }
            // Split of the total by check-in type.
            Text(
                text = "Barriers ${state.checkInBarrierTotal}  ·  Progress ${state.checkInProgressTotal}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (state.checkInCurrentStreakFlexUsed) {
                Text(
                    text = "Missed a day or two? Your streak is protected — up to two missed days won't reset your progress.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SectionLabel("FOLLOW-THROUGH")
            if (state.followThroughTotal == 0) {
                Text(
                    text = "No follow-throughs yet — tap “I followed through” on any goal when you're ready.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile("${state.followThroughTotal}", "Total", Modifier.weight(1f))
                    StatTile("${state.followThroughThisWeek}", "This week", Modifier.weight(1f))
                    StatTile("${state.followThroughThisMonth}", "This month", Modifier.weight(1f))
                }
            }

            // "Your FollowThrus" — always reachable from Stats, never auto-surfaced.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, AppColors.Border, RoundedCornerShape(16.dp))
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "Open Your FollowThrus",
                        onClick = onOpenFollowThrus
                    )
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your FollowThrus",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * The Check-in Streak card — the one gold surface in the app. Check-ins are the
 * consistent, frequent behavior worth surfacing as the headline streak (follow-
 * throughs are slow and infrequent, so they stay as their own count below, not as
 * the streak). Gold flame + a large dark number (14:1 on the pale-gold surface),
 * with a reassurance line that reflects the two-day flex buffer.
 */
@Composable
private fun CheckInStreakCard(streak: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.GoldSurface)
            .padding(20.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "Check-in Streak. $streak ${if (streak == 1) "day" else "days"} in a row. " +
                    "An occasional miss won't reset it."
            },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Check-in Streak",
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
            color = AppColors.OnGoldSurface
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = R.drawable.ic_flame),
                contentDescription = null,
                tint = AppColors.GoldIcon,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "$streak",
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.SemiBold),
                color = AppColors.OnGoldSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (streak == 1) "day in a row" else "days in a row",
                style = MaterialTheme.typography.bodyLarge,
                color = AppColors.OnGoldSurface
            )
        }
        Text(
            text = "An occasional miss won't reset it.",
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.OnGoldSurface
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.semantics { heading() }
    )
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(16.dp))
            .padding(vertical = 18.dp, horizontal = 12.dp)
            .clearAndSetSemantics { contentDescription = "$label $value" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
