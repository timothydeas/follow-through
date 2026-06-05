package com.ideasinc.followthrough.ui.onboarding

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ideasinc.followthrough.R
import com.ideasinc.followthrough.ui.theme.AppColors

private const val FADE_MS = 350

private const val PREFS_ONBOARDING = "onboarding_prefs"
private const val KEY_SWIPE_HINT_SHOWN = "swipe_hint_shown"

/**
 * Two-segment progress bar — one rounded segment per slide, the active/completed
 * ones filled coral, upcoming ones a faint coral tint.
 */
@Composable
private fun SegmentedProgress(
    currentIndex: Int,
    total: Int = 2,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "Step ${currentIndex + 1} of $total"
            liveRegion = LiveRegionMode.Polite
        },
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(total) { idx ->
            val filled = idx <= currentIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (filled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primaryContainer
                    )
            )
        }
    }
}

@Composable
fun OnboardingScreen(
    onBiometricPersist: (biometricEnabled: Boolean) -> Unit,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 50.dp.toPx() }

    val biometricAvailable = remember {
        BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    var step by remember { mutableStateOf(0) }
    var biometricEnabled by remember { mutableStateOf(false) }

    // First-launch only: nudge the progress bar horizontally for ~1s to signal
    // the screen is swipeable. A SharedPreferences gate shows it exactly once.
    val onboardingPrefs = remember {
        context.getSharedPreferences(PREFS_ONBOARDING, Context.MODE_PRIVATE)
    }
    val hintOffsetPx = remember { Animatable(0f) }
    val hintAmplitudePx = with(density) { 14.dp.toPx() }
    LaunchedEffect(Unit) {
        val alreadyShown = onboardingPrefs.getBoolean(KEY_SWIPE_HINT_SHOWN, false)
        if (!alreadyShown) {
            hintOffsetPx.animateTo(-hintAmplitudePx, animationSpec = tween(350))
            hintOffsetPx.animateTo(hintAmplitudePx, animationSpec = tween(450))
            hintOffsetPx.animateTo(0f, animationSpec = tween(300))
            onboardingPrefs.edit().putBoolean(KEY_SWIPE_HINT_SHOWN, true).apply()
        }
    }

    // Biometric is persisted whenever we leave slide 0 (forward) or complete, so
    // the choice is never lost. Persisting is idempotent so re-calls are safe.
    val persist: () -> Unit = { onBiometricPersist(biometricEnabled) }
    val advanceFromSwipe: () -> Unit = {
        if (step == 0) { persist(); step = 1 }
        // step 1: swipe-forward is a no-op — only "Get started" completes.
    }
    val goBack: () -> Unit = { if (step > 0) step -= 1 }
    val complete: () -> Unit = { persist(); onComplete() }

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
                            totalDrag <= -swipeThresholdPx -> advanceFromSwipe()
                            totalDrag >= swipeThresholdPx -> goBack()
                        }
                        totalDrag = 0f
                    },
                    onDragCancel = { totalDrag = 0f }
                ) { _, dragAmount -> totalDrag += dragAmount }
            }
    ) {
        // Persistent header — wordmark + tagline, with a Skip action that
        // completes onboarding from either slide. Hairline divider beneath.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 24.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) {
                        heading()
                        contentDescription = "FollowThru — in every moment"
                    }
            ) {
                Text(
                    text = "FollowThru",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "in every moment",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(
                onClick = complete,
                modifier = Modifier.semantics { contentDescription = "Skip onboarding" }
            ) {
                Text(
                    text = "Skip",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        HorizontalDivider(color = AppColors.Border)

        SegmentedProgress(
            currentIndex = step,
            total = 2,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .graphicsLayer { translationX = hintOffsetPx.value }
        )

        // Body fills remaining height; the slide content scrolls, the action
        // buttons stay anchored at the bottom.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(FADE_MS)) togetherWith
                            fadeOut(animationSpec = tween(FADE_MS))
                    },
                    label = "onboarding-body",
                    modifier = Modifier.fillMaxSize()
                ) { currentStep ->
                    when (currentStep) {
                        0 -> SlideOneBody(
                            biometricAvailable = biometricAvailable,
                            biometricEnabled = biometricEnabled,
                            onBiometricChange = { biometricEnabled = it }
                        )
                        else -> HowItWorksBody()
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (step == 0) {
                PrimaryButton(label = "Continue", onClick = { persist(); step = 1 })
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = goBack,
                        modifier = Modifier
                            .weight(1f)
                            .semantics { contentDescription = "Back to step 1 of 2" },
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        Text(
                            text = "Back",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        PrimaryButton(label = "Get started", onClick = complete)
                    }
                }
            }
        }
    }
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = label },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

@Composable
private fun SlideOneBody(
    biometricAvailable: Boolean,
    biometricEnabled: Boolean,
    onBiometricChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Following through is the hard part.",
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 30.sp,
                lineHeight = 38.sp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = "We all set goals. FollowThru is for the moment you act on them — " +
                "naming the situation where it gets hard, and deciding your move in " +
                "advance. Everything stays on your device. No account, no cloud.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        )
        Text(
            text = "No cloud backup — your data stays only on this device, so it " +
                "won't transfer to a new phone or come back after uninstalling.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (biometricAvailable) {
            AppLockCard(checked = biometricEnabled, onCheckedChange = onBiometricChange)
        }
    }
}

@Composable
private fun AppLockCard(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(16.dp))
            .clickable(
                role = Role.Checkbox,
                onClick = { onCheckedChange(!checked) }
            )
            .padding(16.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "App lock with device PIN or biometrics"
                stateDescription = if (checked) "On" else "Off"
                role = Role.Checkbox
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                uncheckedColor = MaterialTheme.colorScheme.outline
            )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "App lock (device PIN)",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Lock FollowThru with your device PIN or biometrics.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HowItWorksBody() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "How it works",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 24.sp,
                lineHeight = 32.sp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() }
        )
        HowItWorksStep(
            iconRes = R.drawable.ic_target,
            title = "Name the moment",
            description = "The situation where following through gets hard."
        )
        HowItWorksStep(
            iconRes = R.drawable.ic_edit_3,
            title = "Decide your move",
            description = "When [cue], I will [action] — your plan for that moment."
        )
        HowItWorksStep(
            iconRes = R.drawable.ic_check_circle,
            title = "Follow through",
            description = "Act on your plan, then mark it."
        )

        // Optional worked example — kept off the required path behind a toggle so
        // it never lengthens onboarding for someone who just wants to start.
        var showExample by remember { mutableStateOf(false) }
        val revealLabel = if (showExample) "Hide example" else "See an example"
        Text(
            text = revealLabel,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(
                    role = Role.Button,
                    onClickLabel = revealLabel,
                    onClick = { showExample = !showExample }
                )
                .padding(vertical = 8.dp, horizontal = 4.dp)
        )
        if (showExample) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(16.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription =
                            "Example. Goal: take care of my health. " +
                                "When I pour my morning coffee, I will make a breakfast I actually look forward to. " +
                                "Your goal is what you're moving toward. Your plan is the exact move for the moment. " +
                                "You'll write your own."
                    },
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "GOAL",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Take care of my health",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "When I pour my morning coffee, I will make a breakfast I actually look forward to.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Your goal is what you're moving toward. Your plan is the exact move for the moment — you'll write your own.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HowItWorksStep(iconRes: Int, title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$title. $description"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(AppColors.CoralTint),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = AppColors.OnCoralTint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
