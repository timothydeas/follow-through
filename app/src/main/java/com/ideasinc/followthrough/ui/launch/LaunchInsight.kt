package com.ideasinc.followthrough.ui.launch

import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ideasinc.followthrough.ui.theme.DmSansFontFamily
import kotlinx.coroutines.delay

const val KEY_LAST_INSIGHT_INDEX = "last_insight_index"

val LAUNCH_INSIGHTS = listOf(
    "You're a work in progress — always learning and improving.",
    "It's human to look away from things that might hurt to see. But the things we avoid tend to cost us more than the things we face.",
    "Believing you can figure something out matters more than whether you already know how. That belief is always available to you.",
    "Willpower runs low by the end of the day. If you can plan ahead for the moments when you're tired or depleted, you give yourself a real fighting chance.",
    "Doing the same thing in the same situation, over and over, is how a choice stops feeling like a choice. That's how habits form.",
    "Whatever you decide to do today, imagine doing it every single time you face this same moment. Your future self is shaped by what you do right now.",
    "Negative feedback isn't a reflection of who you are. The less your ego is tangled up in it, the more you can actually learn from it and keep moving.",
    "The more personally meaningful a goal feels — even a necessary one — the more likely you are to follow through on it. Finding your own reason to want it makes all the difference.",
    "What you expect from yourself has a way of becoming what you do. Believing change is possible is often what makes it possible."
)

/**
 * Pick a random insight, never repeating the previously shown one.
 * The chosen index is persisted so the next launch can avoid it.
 */
fun pickLaunchInsight(prefs: SharedPreferences): String {
    val last = prefs.getInt(KEY_LAST_INSIGHT_INDEX, -1)
    val candidates = LAUNCH_INSIGHTS.indices.filter { it != last }
    val pool = if (candidates.isEmpty()) LAUNCH_INSIGHTS.indices.toList() else candidates
    val pick = pool.random()
    prefs.edit().putInt(KEY_LAST_INSIGHT_INDEX, pick).apply()
    return LAUNCH_INSIGHTS[pick]
}

/**
 * Time the insight needs to be on screen, scaled to its word count at an
 * average reading speed of 200 wpm, plus a 1-second buffer so the user is
 * never rushed. Clamped to 3–8 seconds.
 */
internal fun insightDisplayDurationMs(text: String): Long {
    val wordCount = text.trim().split(Regex("\\s+")).count { it.isNotBlank() }
    val readingMs = (wordCount.toLong() * 60_000L) / 200L
    val withBuffer = readingMs + 1_000L
    return withBuffer.coerceIn(3_000L, 8_000L)
}

@Composable
fun LaunchInsightOverlay(text: String, onDismiss: () -> Unit) {
    LaunchedEffect(text) {
        delay(insightDisplayDurationMs(text))
        onDismiss()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(onClickLabel = "Dismiss", onClick = onDismiss)
            .semantics {
                contentDescription = text
                liveRegion = LiveRegionMode.Assertive
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .padding(32.dp)
                .background(Color(0xFF2C2C28), RoundedCornerShape(20.dp))
                .padding(horizontal = 32.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = DmSansFontFamily,
                    color = Color.White
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}
