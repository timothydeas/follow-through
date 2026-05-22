package com.ideasinc.followthrough.ui.onboarding

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ideasinc.followthrough.R
import com.ideasinc.followthrough.ui.theme.AppColors
import com.ideasinc.followthrough.ui.theme.PoppinsFontFamily

private const val FADE_MS = 350

private const val PREFS_ONBOARDING = "onboarding_prefs"
private const val KEY_SWIPE_HINT_SHOWN = "swipe_hint_shown"

@Composable
private fun ProgressDots(
    currentIndex: Int,
    total: Int = 4,
    modifier: Modifier = Modifier
) {
    val activeColor = AppColors.ForgeBackground
    val inactiveColor = AppColors.ForgeBackground.copy(alpha = 0.65f)
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "Step ${currentIndex + 1} of $total"
            liveRegion = LiveRegionMode.Polite
        },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { idx ->
            val active = idx == currentIndex
            val color by animateColorAsState(
                targetValue = if (active) activeColor else inactiveColor,
                animationSpec = tween(FADE_MS),
                label = "dot-color-$idx"
            )
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(color)
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

    val buttonFocus = remember { FocusRequester() }
    LaunchedEffect(step) { runCatching { buttonFocus.requestFocus() } }

    // First-launch only: animate the progress-dots row horizontally for ~1s to
    // signal the screen is swipeable. SharedPreferences gate ensures it shows
    // exactly once across installs.
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

    // Swipe forward — mirrors Continue button behavior, but swiping past the last step is a no-op.
    val advanceFromSwipe: () -> Unit = {
        when (step) {
            0 -> step = 1
            1 -> step = 2
            2 -> {
                onBiometricPersist(biometricEnabled)
                step = 3
            }
            // step 3: swipe-forward is a no-op — only the explicit "Got it" tap can complete onboarding.
        }
    }
    val goBack: () -> Unit = {
        if (step > 0) step -= 1
    }
    val onPrimaryClick: () -> Unit = {
        when (step) {
            0 -> step = 1
            1 -> step = 2
            2 -> {
                onBiometricPersist(biometricEnabled)
                step = 3
            }
            3 -> onComplete()
        }
    }
    val buttonDescription = when (step) {
        0 -> "Continue to step 2"
        1 -> "Continue to step 3"
        2 -> "Continue to step 4"
        else -> "Complete onboarding"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.ForgeBackground)
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
                ) { _, dragAmount ->
                    totalDrag += dragAmount
                }
            }
    ) {
        // Fixed header — forge background, icon + title, ~58% of screen, never animates
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.58f)
                .statusBarsPadding()
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(130.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Follow Thru",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    color = AppColors.OnForgeBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() }
                )
            }
        }

        // Content section — light surface (always), body crossfades between steps
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.42f)
                .background(AppColors.OnboardingBodySurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(horizontal = 32.dp)
                    .padding(top = 8.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = step,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(FADE_MS)) togetherWith
                                fadeOut(animationSpec = tween(FADE_MS))
                        },
                        label = "onboarding-body",
                        modifier = Modifier.fillMaxWidth()
                    ) { currentStep ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            when (currentStep) {
                                0 -> GoalsBody()
                                1 -> HowItWorksBody()
                                2 -> BiometricBody(
                                    biometricAvailable = biometricAvailable,
                                    biometricEnabled = biometricEnabled,
                                    onBiometricChange = { biometricEnabled = it }
                                )
                                3 -> PrivacyBody()
                            }
                        }
                    }
                }

                ProgressDots(
                    currentIndex = step,
                    modifier = Modifier.graphicsLayer { translationX = hintOffsetPx.value }
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onPrimaryClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(buttonFocus)
                        .semantics(mergeDescendants = true) {
                            contentDescription = buttonDescription
                        },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.ForgeBackground,
                        contentColor = AppColors.OnForgeBackground
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    AnimatedContent(
                        targetState = step == 3,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(FADE_MS)) togetherWith
                                fadeOut(animationSpec = tween(FADE_MS))
                        },
                        label = "button-label"
                    ) { isLastStep ->
                        Text(
                            text = if (isLastStep) "Got it" else "Continue",
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalsBody() {
    Text(
        text = "Goals & Changes",
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp,
        color = AppColors.OnOnboardingBodySurface,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun HowItWorksBody() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "How it works",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = AppColors.OnOnboardingBodySurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = "1. Add a goal or change you're working toward.\n\n" +
                "2. Check in regularly. Each check-in walks you through reflection " +
                "questions based on behavioral science.\n\n" +
                "3. Mark when you follow through. Your progress and follow-throughs " +
                "are tracked over time.",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            color = AppColors.OnOnboardingBodySurface,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun BiometricBody(
    biometricAvailable: Boolean,
    biometricEnabled: Boolean,
    onBiometricChange: (Boolean) -> Unit
) {
    if (biometricAvailable) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Lock Follow Thru with Face ID or Device PIN",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = AppColors.OnOnboardingBodySurface,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = biometricEnabled,
                onCheckedChange = onBiometricChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    uncheckedTrackColor = AppColors.SwitchUncheckedTrack,
                    uncheckedBorderColor = androidx.compose.ui.graphics.Color.Transparent
                ),
                modifier = Modifier.semantics {
                    contentDescription = "Lock Follow Thru with biometrics"
                    stateDescription = if (biometricEnabled) "On" else "Off"
                    role = Role.Switch
                }
            )
        }
    }
}

@Composable
private fun PrivacyBody() {
    Text(
        text = "Your data is stored only on your device. We do not access it.",
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp,
        color = AppColors.OnOnboardingBodySurface,
        textAlign = TextAlign.Center
    )
}
