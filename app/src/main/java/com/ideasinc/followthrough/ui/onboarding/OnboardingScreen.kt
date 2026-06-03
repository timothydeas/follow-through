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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    total: Int = 2,
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

    // Two slides total. Biometric is persisted as soon as the user leaves
    // slide 0 (Security & Privacy), so slide 1 (How it works) → "Get Started"
    // doesn't need to re-persist. Swipe forward past the last slide is a no-op.
    val advanceFromSwipe: () -> Unit = {
        when (step) {
            0 -> {
                onBiometricPersist(biometricEnabled)
                step = 1
            }
            // step 1: swipe-forward is a no-op — only the explicit "Get Started" tap completes.
        }
    }
    val goBack: () -> Unit = {
        if (step > 0) step -= 1
    }
    val onPrimaryClick: () -> Unit = {
        when (step) {
            0 -> {
                onBiometricPersist(biometricEnabled)
                step = 1
            }
            1 -> onComplete()
        }
    }
    val buttonDescription = when (step) {
        0 -> "Continue to step 2 of 2"
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
        // Persistent unified header — all three elements (icon, wordmark,
        // tagline) sit on the brand-red ForgeBackground inherited from the
        // root Column. Icon foreground paths render white against the red,
        // matching the original onboarding look. Appears on both slides.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 32.dp)
                .padding(top = 16.dp, bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(96.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "FollowThru",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    color = AppColors.OnForgeBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() }
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "in every moment",
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppColors.OnForgeBackground,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Body section fills remaining height. Holds the current slide body,
        // progress dots, and the primary action button anchored at the bottom.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(AppColors.OnboardingBodySurface)
                .navigationBarsPadding()
                .padding(horizontal = 32.dp)
                .padding(top = 24.dp, bottom = 12.dp),
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
                            0 -> SecurityAndPrivacyBody(
                                biometricAvailable = biometricAvailable,
                                biometricEnabled = biometricEnabled,
                                onBiometricChange = { biometricEnabled = it }
                            )
                            1 -> HowItWorksBody()
                        }
                    }
                }
            }

            ProgressDots(
                currentIndex = step,
                total = 2,
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
                    targetState = step == 1,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(FADE_MS)) togetherWith
                            fadeOut(animationSpec = tween(FADE_MS))
                    },
                    label = "button-label"
                ) { isLastStep ->
                    Text(
                        text = if (isLastStep) "Get Started" else "Next",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SecurityAndPrivacyBody(
    biometricAvailable: Boolean,
    biometricEnabled: Boolean,
    onBiometricChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Lead framing — orients a cold user in one line: the follow-through
        // identity, stated positively (no defensive "not a task manager").
        Text(
            text = "We all set goals. Following through is the hard part — that's what FollowThru is for: acting on your intention, in the moment.",
            style = MaterialTheme.typography.bodyLarge,
            color = AppColors.OnOnboardingBodySurface,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite }
        )
        if (biometricAvailable) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "App lock",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 18.sp,
                        lineHeight = 24.sp
                    ),
                    color = AppColors.OnOnboardingBodySurface,
                    modifier = Modifier.semantics { heading() }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lock FollowThru with Face ID or Device PIN",
                        style = MaterialTheme.typography.bodyLarge,
                        color = AppColors.OnOnboardingBodySurface,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
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
                            contentDescription = "Lock FollowThru with biometrics"
                            stateDescription = if (biometricEnabled) "On" else "Off"
                            role = Role.Switch
                        }
                    )
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Your privacy",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 18.sp,
                    lineHeight = 24.sp
                ),
                color = AppColors.OnOnboardingBodySurface,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = "Your data is stored only on your device. We do not access it.",
                style = MaterialTheme.typography.bodyLarge,
                color = AppColors.OnOnboardingBodySurface
            )
        }
    }
}

@Composable
private fun HowItWorksBody() {
    // The container now expands to fit content (header is wrap_content, body
    // takes weight 1f), so the 3 cards fit naturally on Pixel 5/6 without
    // scrolling. verticalScroll remains as a defensive fallback for very small
    // phones or large-font-scale users where 4 stacked accessible-size blocks
    // could still overflow.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "How it works",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 24.sp,
                lineHeight = 32.sp
            ),
            color = AppColors.OnOnboardingBodySurface,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { heading() }
        )
        HowItWorksStep(
            number = 1,
            title = "Name the moment",
            description = "The situation where following through gets hard."
        )
        HowItWorksStep(
            number = 2,
            title = "Decide your move",
            description = "When [cue], I will [action] — your plan for that moment."
        )
        HowItWorksStep(
            number = 3,
            title = "Follow through",
            description = "Act on your plan, then mark it."
        )

        // Optional worked example — kept off the required path behind a toggle
        // so it never lengthens onboarding for someone who just wants to start.
        var showExample by remember { mutableStateOf(false) }
        val revealLabel = if (showExample) "Hide example" else "See an example"
        Text(
            text = revealLabel,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = AppColors.ForgeBackground,
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
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.OnOnboardingBodySurface.copy(alpha = 0.06f))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription =
                            "Example. Goal: take care of my health. " +
                                "Plan: when I pour my morning coffee, I will make a breakfast I actually look forward to. " +
                                "Your goal is what you're moving toward — take care of my health. " +
                                "Your plan is the exact move for the moment. You'll write your own."
                    },
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Goal: Take care of my health",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = AppColors.OnOnboardingBodySurface
                )
                Text(
                    text = "Plan: When I pour my morning coffee → I will make a breakfast I actually look forward to.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppColors.OnOnboardingBodySurface
                )
                Text(
                    text = "Your goal is what you're moving toward — “take care of my health.” " +
                        "Your plan is the exact move for the moment. You'll write your own.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.OnOnboardingBodySurface.copy(alpha = 0.75f)
                )
            }
        }
    }
}

@Composable
private fun HowItWorksStep(number: Int, title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            // Faint tint of the body text color — gives a subtle card surface
            // against the white onboarding body without introducing a raw hex.
            .background(AppColors.OnOnboardingBodySurface.copy(alpha = 0.06f))
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "Step $number. $title. $description"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(AppColors.ForgeBackground),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = AppColors.OnForgeBackground
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 18.sp,
                    lineHeight = 24.sp
                ),
                color = AppColors.OnOnboardingBodySurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = AppColors.OnOnboardingBodySurface
            )
        }
    }
}

