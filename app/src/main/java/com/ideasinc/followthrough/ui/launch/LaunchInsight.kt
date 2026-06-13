package com.ideasinc.followthrough.ui.launch

import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ideasinc.followthrough.R
import com.ideasinc.followthrough.ui.theme.AppColors
import com.ideasinc.followthrough.ui.theme.PoppinsFontFamily

const val KEY_LAST_INSIGHT_INDEX = "last_insight_index"
const val KEY_LAST_INSIGHT_DAY = "last_insight_day"

/**
 * The insight is a once-a-day moment, not an every-launch interstitial. Returns
 * true only when it hasn't already been shown today (device-local calendar day).
 */
fun shouldShowInsightToday(prefs: SharedPreferences): Boolean =
    prefs.getInt(KEY_LAST_INSIGHT_DAY, -1) != todayKey()

/** Records that today's insight has been shown, so later launches today skip it. */
fun markInsightShownToday(prefs: SharedPreferences) {
    prefs.edit().putInt(KEY_LAST_INSIGHT_DAY, todayKey()).apply()
}

private fun todayKey(): Int {
    val c = java.util.Calendar.getInstance()
    return c.get(java.util.Calendar.YEAR) * 1000 + c.get(java.util.Calendar.DAY_OF_YEAR)
}

/**
 * One-shot signal that the current cold start originated from a reminder-tap
 * deep-link, so the launch-insight start destination should render nothing and
 * let the deep-link open Goal Detail directly — no insight flash first. Set by
 * MainActivity in onCreate and consumed once by [LaunchInsightScreen].
 */
object LaunchInsightGate {
    @Volatile
    var skipForNotificationOpen: Boolean = false
}

val LAUNCH_INSIGHTS = listOf(
    "You're a work in progress — always learning and improving.",
    "It's human to look away from things that might hurt to see. But the things we avoid tend to cost us more than the things we face.",
    "Believing you can figure something out matters more than whether you already know how. That belief is always available to you.",
    "Willpower runs low by the end of the day. If you can plan ahead for the moments when you're tired or depleted, you give yourself a real fighting chance.",
    "Doing the same thing in the same situation, over and over, is how a choice stops feeling like a choice. That's how habits form.",
    "Whatever you decide to do today, imagine doing it every single time you face this same moment. Your future self is shaped by what you do right now.",
    "Negative feedback isn't a reflection of who you are. The less your ego is tangled up in it, the more you can actually learn from it and keep moving.",
    "The more personally meaningful a goal feels — even a necessary one — the more likely you are to follow through on it. Finding your own reason to want it makes all the difference.",
    "What you expect from yourself has a way of becoming what you do. Believing change is possible is often what makes it possible.",
    "The moment you feel like quitting is usually the moment right before the breakthrough. Don't let the obvious insights of hindsight be the only time you recognize your own progress.",
    "Persist through the unknown until it becomes the obvious.",
    "To achieve your goals, remember how they feel. Draw on the genuine emotions of past experiences that make you feel strong and capable, and bring that genuine energy to the task.",
    "What you practice becomes muscle memory. Every deliberate choice builds the reflex your future self will reach for.",
    "Off days are part of it, not the end of it. Begin again whenever you're ready.",
    // Themes carried over from the retired reflection questions, now surfaced as
    // rotating launch insights: progress, avoiding, confidence, appraisal.
    "Notice what's moved, even a little — progress counts before it's finished.",
    "What are you avoiding? Naming it is the first step out of the sand.",
    "Confidence grows from doing, not before it.",
    "What's in the way — the situation itself, or how you're seeing it?"
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
 * average reading speed of 200 wpm, plus a 3-second buffer so the user is
 * never rushed (2s longer than the prior buffer, matching the clamp shift
 * below). Clamped to 5–10 seconds.
 */
internal fun insightDisplayDurationMs(text: String): Long {
    val wordCount = text.trim().split(Regex("\\s+")).count { it.isNotBlank() }
    val readingMs = (wordCount.toLong() * 60_000L) / 200L
    val withBuffer = readingMs + 3_000L
    return withBuffer.coerceIn(5_000L, 10_000L)
}

@Composable
fun LaunchInsightScreen(text: String, onDismiss: () -> Unit) {
    // If this launch came from a reminder tap, render nothing: the deep-link in the
    // navigation layer pops this start destination and routes straight to Goal
    // Detail, so the insight never flashes. Decided once and remembered so a
    // recomposition before the pop doesn't suddenly reveal the insight.
    val skip = remember {
        LaunchInsightGate.skipForNotificationOpen.also {
            if (it) LaunchInsightGate.skipForNotificationOpen = false
        }
    }
    if (skip) return

    val forgeBg = AppColors.ForgeBackground
    val forgeOn = AppColors.OnForgeBackground
    val insightText = remember { text }

    // No timed auto-dismiss (beta complaint + the no-timed-dismiss rule): the user
    // always tap- or swipe-continues at their own pace. System back is a no-op.
    BackHandler(enabled = true) { /* intentionally empty */ }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(forgeBg)
            .clickable(onClickLabel = "Continue", onClick = onDismiss)
            // Swipe in any direction also continues.
            .pointerInput(Unit) {
                detectDragGestures(onDragEnd = { onDismiss() }) { _, _ -> }
            }
            // Whole screen is one opaque accessibility node so TalkBack reads only
            // the insight text — not the icon, title, or caption.
            .clearAndSetSemantics {
                contentDescription = insightText
                onClick(label = "Continue") {
                    onDismiss()
                    true
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Contained centered moment: the content is capped at a comfortable reading
        // width and centred on the full-bleed coral field, so on a tablet it reads
        // as one focused moment rather than text stretched edge-to-edge. Type scales
        // gently; landscape becomes scrollable for short screens.
        Column(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .systemBarsPadding()
                .then(if (isLandscape) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(if (isLandscape) 88.dp else 112.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "FollowThru",
                color = forgeOn,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 26.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(40.dp))
            Text(
                text = insightText,
                color = forgeOn,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp,
                lineHeight = 28.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(40.dp))
            Text(
                text = "Tap or swipe to continue",
                color = forgeOn,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
