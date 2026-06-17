package com.ideasinc.followthrough.ui.worked

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
import com.ideasinc.followthrough.di.AppContainer
import com.ideasinc.followthrough.ui.theme.AppColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** One cue that has driven follow-through, with how many times it worked. */
data class WorkedItem(val reminder: Reminder, val followThroughs: Int)

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
            val doneByReminder = events
                .filter { it.action == EventAction.DONE }
                .groupingBy { it.reminderId }
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

@Composable
fun WhatWorkedScreen(
    container: AppContainer,
    onReuse: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val vm: WhatWorkedViewModel = viewModel(
        factory = WhatWorkedViewModel.Factory(container.reminderDao, container.reminderEventDao)
    )
    val state by vm.uiState.collectAsState()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "What worked",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() }
                )
            }
            item {
                Text(
                    "The cues that actually got you to follow through — what's working for you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (state.loaded && state.items.isEmpty()) {
                item {
                    Text(
                        "Once you follow through on a cue, it shows up here — so you can see what works for you and use it again.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }

            items(state.items, key = { it.reminder.id }) { item ->
                WorkedCard(item = item, onClick = { onReuse(item.reminder.id) })
            }
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
        // An emoji cue is tiny, so it sits inline beside the intention; a phrase cue can
        // be long, so it gets its own line (a side-by-side Row would squeeze the intention
        // into a thin right-hand column and balloon the card height).
        if (r.cueType == CueType.EMOJI && r.cueValue.isNotBlank()) {
            Row {
                Text(r.cueValue, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(10.dp))
                Text(
                    r.intentionText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            if (r.cueType == CueType.PHRASE && r.cueValue.isNotBlank()) {
                Text(
                    "“${r.cueValue}”",
                    style = MaterialTheme.typography.titleMedium,
                    color = AppColors.BrandAccentText
                )
            }
            Text(
                r.intentionText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
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