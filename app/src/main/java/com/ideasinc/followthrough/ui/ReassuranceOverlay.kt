package com.ideasinc.followthrough.ui

import android.content.Context
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ideasinc.followthrough.R
import com.ideasinc.followthrough.ui.theme.AppColors
import com.ideasinc.followthrough.ui.theme.PoppinsFontFamily

/**
 * Time the celebratory overlay stays up, scaled to its word count at ~200 wpm plus
 * a 3-second buffer, clamped to 5–10s. (The user can always tap to continue sooner.)
 */
private fun reassuranceDurationMs(text: String): Long {
    val words = text.trim().split(Regex("\\s+")).count { it.isNotBlank() }
    return ((words.toLong() * 60_000L) / 200L + 3_000L).coerceIn(5_000L, 10_000L)
}

/**
 * Full-screen brand "forge" overlay — a brief, celebratory close message: coral
 * [AppColors.ForgeBackground] with [AppColors.OnForgeBackground] text, in an
 * icon → wordmark → message → caption recipe. Used by the "I followed through"
 * reassurance moment.
 *
 * Dismisses on tap or after a short word-count-scaled auto-timer (skipped when an
 * accessibility service is active so the user can read at their own pace and dismiss
 * with a tap). [onDismiss] runs on either path.
 */
@Composable
internal fun ReassuranceOverlay(message: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    // Move accessibility focus onto the overlay as soon as it appears, so a screen-
    // reader user hears the message immediately (read from the node's
    // contentDescription) instead of hunting for it — and focus starts inside the
    // overlay rather than on the screen behind it.
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    // Auto-dismiss via a plain Handler so the countdown lives outside the
    // composition — no LaunchedEffect, no coroutine state, nothing that
    // could trigger recomposition during the countdown. TalkBack discovers
    // the message naturally via clearAndSetSemantics contentDescription.
    // When an accessibility service is active the timer is skipped entirely so
    // the user can read at their own pace and dismiss with a tap.
    DisposableEffect(Unit) {
        val a11yManager =
            context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        val handler = Handler(Looper.getMainLooper())
        val runnable = Runnable { onDismiss() }
        if (a11yManager?.isEnabled != true) {
            handler.postDelayed(runnable, reassuranceDurationMs(message))
        }
        onDispose {
            handler.removeCallbacks(runnable)
        }
    }

    // Full-screen brand "forge" treatment matching the launch-insight screen:
    // coral background (AppColors.ForgeBackground) with OnForgeBackground text,
    // the same icon → wordmark → message → caption recipe.
    val forgeBg = AppColors.ForgeBackground
    val forgeOn = AppColors.OnForgeBackground
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(forgeBg)
            // Focus target for the accessibility-focus move above.
            .focusRequester(focusRequester)
            .focusable()
            .clickable(onClickLabel = "Dismiss", onClick = onDismiss)
            // Whole overlay is one opaque accessibility node so TalkBack
            // reads only the reassurance message, not the icon/wordmark/caption.
            // onClick is re-declared to preserve double-tap-to-dismiss.
            .clearAndSetSemantics {
                contentDescription = message
                // Treat the overlay as a single traversal group placed first, so
                // TalkBack linear navigation starts here and stays on the overlay
                // while it is shown rather than reaching the screen behind. On
                // dismiss the overlay leaves the composition, so the screen behind
                // becomes navigable again (e.g. follow-through returns to Goal
                // Detail with normal traversal restored).
                isTraversalGroup = true
                traversalIndex = -1f
                onClick(label = "Dismiss") {
                    onDismiss()
                    true
                }
            }
    ) {
        // Portrait floats the message to the visual center with weight spacers;
        // landscape is too short for that, so the column scrolls with fixed
        // spacers — mirroring LaunchInsightScreen.
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
                text = message,
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
