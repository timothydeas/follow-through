package com.ideasinc.followthrough.ui.builder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ideasinc.followthrough.di.AppContainer

/**
 * Reminder Builder — the 4-step spine wizard (goal → draw from yourself → intention
 * → one cue + schedule). Placeholder shell for now; the full wizard, one-cue
 * enforcement, palette chips, and emoji/phrase-only flagging land in slice 6.
 */
@Composable
fun ReminderBuilderScreen(
    container: AppContainer,
    goalId: String?,
    reminderId: String?,
    onClose: () -> Unit,
    onSaved: (goalId: String) -> Unit
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Reminder Builder", style = MaterialTheme.typography.headlineMedium)
            Text(
                "The guided 4-step builder is coming next.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onClose) { Text("Close") }
        }
    }
}
