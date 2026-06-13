package com.ideasinc.followthrough.ui.aboutyou

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ideasinc.followthrough.di.AppContainer

/**
 * About You — the self-knowledge palette. "What you know about yourself is the raw
 * material for cues that actually catch your eye." Passions/interests + learnings
 * CRUD lands in slice 4; this is the destination shell for the navigation spine.
 */
@Composable
fun AboutYouScreen(
    container: AppContainer,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("About You", style = MaterialTheme.typography.displayMedium)
        Text(
            "What you know about yourself is the raw material for cues that actually catch your eye.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
