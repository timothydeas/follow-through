package com.ideasinc.followthrough.ui.launch

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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
    "What you practice becomes what you reach for when there's no time to think. Firefighters drill the same motions over and over so that in the chaos of an emergency, the right response comes automatically. The same is true for any goal — every time you make the deliberate choice in a hard moment, you're building the reflex your future self will rely on.",
    "What you practice becomes muscle memory. Every deliberate choice builds the reflex your future self will reach for."
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
    val forgeBg = AppColors.ForgeBackground
    val forgeOn = AppColors.OnForgeBackground
    // Capture the insight once so the timer keys off a stable value.
    val insightText = remember { text }
    val context = LocalContext.current

    // Auto-dismiss via a plain Handler so the countdown lives outside the
    // composition — no LaunchedEffect, no coroutine state, nothing that
    // could trigger recomposition during the countdown. TalkBack discovers
    // the content naturally via clearAndSetSemantics contentDescription.
    // When an accessibility service is active the timer is skipped entirely so
    // the user can read at their own pace and dismiss with a tap.
    DisposableEffect(Unit) {
        val a11yManager =
            context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        val handler = Handler(Looper.getMainLooper())
        val runnable = Runnable { onDismiss() }
        if (a11yManager?.isEnabled != true) {
            handler.postDelayed(runnable, insightDisplayDurationMs(insightText))
        }
        onDispose {
            handler.removeCallbacks(runnable)
        }
    }

    // System back is a no-op on this screen.
    BackHandler(enabled = true) { /* intentionally empty */ }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(forgeBg)
            .clickable(onClickLabel = "Continue", onClick = onDismiss)
            // Whole screen is one opaque accessibility node so TalkBack
            // reads only the insight text — not the icon, title, or caption.
            .clearAndSetSemantics {
                contentDescription = insightText
                onClick(label = "Continue") {
                    onDismiss()
                    true
                }
            }
    ) {
        // In portrait the weight(1f) spacers float the insight to the visual
        // center and pin the caption near the bottom. In landscape the screen
        // is too short for that arrangement, so the column becomes scrollable
        // with fixed spacers and the content flows top-to-bottom.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .then(
                    if (isLandscape) Modifier.verticalScroll(rememberScrollState())
                    else Modifier
                )
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(if (isLandscape) 24.dp else 96.dp))
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(120.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "FollowThru",
                color = forgeOn,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp,
                textAlign = TextAlign.Center
            )
            if (isLandscape) Spacer(Modifier.height(32.dp)) else Spacer(Modifier.weight(1f))
            Text(
                text = insightText,
                color = forgeOn,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp,
                lineHeight = 28.sp,
                textAlign = TextAlign.Center
            )
            if (isLandscape) Spacer(Modifier.height(32.dp)) else Spacer(Modifier.weight(1f))
            Text(
                text = "Tap anywhere to continue",
                color = forgeOn,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(if (isLandscape) 24.dp else 48.dp))
        }
    }
}
