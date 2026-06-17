package com.ideasinc.followthrough.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ideasinc.followthrough.R
import com.ideasinc.followthrough.ui.settings.LocalReduceMotion
import com.ideasinc.followthrough.ui.theme.AppColors
import com.ideasinc.followthrough.ui.theme.CoralLight

private const val FADE_MS = 350
private const val PANE_COUNT = 3

/**
 * Welcome — three swipeable panes (handoff §8, verbatim copy). No timed auto-
 * advance; the user always taps. Skip (panes 0–2) and Back (panes 1–2); pane 3's
 * CTA is "Create my first reminder" → the builder. Completing or skipping marks
 * onboarding done.
 */
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onCreateFirstReminder: () -> Unit
) {
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 50.dp.toPx() }
    var pane by remember { mutableIntStateOf(0) }

    val goBack = { if (pane > 0) pane -= 1 }
    val goForward = { if (pane < PANE_COUNT - 1) pane += 1 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onDragEnd = {
                        when {
                            totalDrag <= -swipeThresholdPx -> goForward()
                            totalDrag >= swipeThresholdPx -> goBack()
                        }
                        totalDrag = 0f
                    },
                    onDragCancel = { totalDrag = 0f }
                ) { _, dragAmount -> totalDrag += dragAmount }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 24.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f).semantics(mergeDescendants = true) { heading() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App launcher icon. Reproduced from its adaptive-icon layers (fixed
                // brand-coral background + white foreground vector, inset 18% as the
                // mipmap does) and masked to a circle — painterResource can't load the
                // AdaptiveIconDrawable directly. Decorative; the label carries the name.
                Box(
                    modifier = Modifier.size(34.dp).clip(CircleShape).background(CoralLight),
                    contentAlignment = Alignment.Center
                ) {
                    // Tighter inset than the launcher mipmap's 18% — at this 34dp lockup
                    // size the arrow needs to read boldly, not float in the circle.
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().padding(3.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text("FollowThru", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onBackground)
            }
            TextButton(onClick = onComplete, modifier = Modifier.semantics { contentDescription = "Skip onboarding" }) {
                Text("Skip", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        HorizontalDivider(color = AppColors.Border)

        SegmentedProgress(currentIndex = pane, total = PANE_COUNT, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp))

        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).navigationBarsPadding().padding(horizontal = 24.dp).padding(bottom = 16.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                // Honor the Reduce motion setting (vestibular accessibility): snap
                // instantly between panes instead of cross-fading when it's on.
                val reduceMotion = LocalReduceMotion.current
                AnimatedContent(
                    targetState = pane,
                    transitionSpec = {
                        if (reduceMotion) fadeIn(snap()) togetherWith fadeOut(snap())
                        else fadeIn(tween(FADE_MS)) togetherWith fadeOut(tween(FADE_MS))
                    },
                    label = "welcome-body",
                    modifier = Modifier.fillMaxSize()
                ) { current ->
                    when (current) {
                        0 -> PanePremise()
                        1 -> PaneIdea()
                        else -> PanePrivacy()
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Full-width primary, with Back below (matches the prototype and keeps
            // the long "Create my first reminder" label fitting — it wraps within the
            // full-width button rather than overflowing, including at 200% text.
            when (pane) {
                0 -> PrimaryButton("Continue", showArrow = true, onClick = goForward)
                1 -> {
                    PrimaryButton("Continue", showArrow = true, onClick = goForward)
                    Spacer(Modifier.height(4.dp))
                    BackButton(onClick = goBack)
                }
                else -> {
                    PrimaryButton("Set my first intention", showArrow = false, onClick = onCreateFirstReminder)
                    Spacer(Modifier.height(4.dp))
                    BackButton(onClick = goBack)
                }
            }
        }
    }
}

/**
 * Carousel-style progress (handoff §8 "progress dots", prototype onboardingExample.png):
 * a centred row where only the current pane is a wide coral pill; the others are small
 * neutral dots. Not a cumulative stepper — earlier panes are not filled in.
 */
@Composable
private fun SegmentedProgress(currentIndex: Int, total: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { idx ->
            val active = idx == currentIndex
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(if (active) 28.dp else 8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
            )
        }
    }
}

@Composable
private fun PanePremise() {
    CenteredScrollPane {
        Text("Remember what you meant to do.", style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.semantics { heading() })
        Text(
            "People mostly forget at the moment they could act — it's not a willpower problem. FollowThru ties each intention to one vivid cue from your own life, so the moment itself reminds you. And when you miss — everyone does — nothing breaks: the cue just brings you back next time.",
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * A pane body that sits vertically centered when its content is shorter than the
 * available height, and scrolls from the top (no clipped heading) when it isn't —
 * e.g. at 200% text scale. Plain `Arrangement.Center` on a scrollable column can't
 * do the second half, so we floor the column height to the viewport.
 */
@Composable
private fun CenteredScrollPane(
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = maxHeight)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
            horizontalAlignment = horizontalAlignment,
            content = content
        )
    }
}

@Composable
private fun PaneIdea() {
    // Top-aligned (not centered): the cards can exceed the viewport on small screens,
    // where centering a scrollable column would push the top out of reach.
    // The four beats mirror the create-cue flow's steps in order (what → when → cue → Did
    // it), so the example previews exactly what the user is asked next.
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            "How it works",
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() }
        )
        IdeaRow(Icons.AutoMirrored.Rounded.DirectionsRun, "What you want to remember to do", "\"Go for a run after work\" — the thing you keep meaning to do, then forget on the couch.")
        IdeaRow(Icons.Rounded.Home, "When you'll do it", "\"When I get home and change out of my work clothes.\"")
        IdeaRow(Icons.Rounded.Bolt, "One vivid cue", "👟 Your running shoes by the door — the one thing you can't miss walking in.")
        IdeaRow(Icons.Rounded.Check, "Then tap Did it", "When the moment comes, the reminder shows your cue and your words. Tap Did it — and a missed day is no big deal.")
    }
}

/**
 * One idea step (prototype onboardingExample.png): a pale-coral circle holding a coral
 * outline icon, then a bold label and the body copy, in a white hairline-bordered card.
 * The circle icon is illustrative onboarding art (handoff §9.1 permits a matching
 * illustration here), not interface chrome — distinct from any inline emoji in [text],
 * which is verbatim §8 cue copy.
 */
@Composable
private fun IdeaRow(icon: ImageVector, label: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, AppColors.Border, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(AppColors.CoralTint),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = AppColors.OnCoralTint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PanePrivacy() {
    CenteredScrollPane(horizontalAlignment = Alignment.CenterHorizontally) {
        // Pale-coral lock circle — illustrative onboarding art (handoff §9.1 permits it),
        // matching the pane-2 idea circles. Decorative; the headline carries the meaning.
        Box(
            modifier = Modifier.size(96.dp).clip(CircleShape).background(AppColors.CoralTint),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = AppColors.OnCoralTint, modifier = Modifier.size(44.dp))
        }
        Text("Private by design.", style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
        Text("Everything stays on this device. No account, no cloud, no tracking.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun PrimaryButton(label: String, showArrow: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .semantics { contentDescription = label },
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    ) {
        // The label may wrap to a second line (long copy at large text scale) rather
        // than overflow; the row stays centred.
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center,
            maxLines = 2
        )
        if (showArrow) {
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun BackButton(onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Back" }
    ) {
        Text("Back", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.primary)
    }
}
