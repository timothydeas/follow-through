package com.ideasinc.followthrough.ui.onboarding

import androidx.biometric.BiometricManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.graphics.Color
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
import com.ideasinc.followthrough.ui.theme.PoppinsFontFamily

private val ForgeColor = Color(0xFF9A3A1B)
private val CreamBackground = Color(0xFFF5F2EC)
private val BodyTextColor = Color(0xFF2C2C28)
private val OnboardingSwitchTrack = Color(0xFF9A3A1B)

private const val FADE_MS = 350

@Composable
private fun ProgressDots(
    currentIndex: Int,
    total: Int = 3,
    modifier: Modifier = Modifier
) {
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
                targetValue = if (active) ForgeColor else ForgeColor.copy(alpha = 0.25f),
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
    var showBiometricTooltip by remember { mutableStateOf(false) }

    val buttonFocus = remember { FocusRequester() }
    val infoTriggerFocus = remember { FocusRequester() }
    LaunchedEffect(step) { runCatching { buttonFocus.requestFocus() } }

    if (showBiometricTooltip) {
        val tooltipFocus = remember { FocusRequester() }
        LaunchedEffect(Unit) { runCatching { tooltipFocus.requestFocus() } }
        AlertDialog(
            onDismissRequest = {
                showBiometricTooltip = false
                runCatching { infoTriggerFocus.requestFocus() }
            },
            text = {
                Text(
                    text = "This uses your phone's existing security — face recognition, fingerprint, or PIN. No new password is created. If biometrics aren't set up on your device, you'll be prompted for your device PIN instead.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBiometricTooltip = false
                        runCatching { infoTriggerFocus.requestFocus() }
                    },
                    modifier = Modifier.focusRequester(tooltipFocus)
                ) { Text("Got it") }
            }
        )
    }

    // Swipe forward — mirrors Continue button behavior, but swiping past the last step is a no-op.
    val advanceFromSwipe: () -> Unit = {
        when (step) {
            0 -> step = 1
            1 -> {
                onBiometricPersist(biometricEnabled)
                step = 2
            }
            // step 2: swipe-forward is a no-op — only the explicit "Got it" tap can complete onboarding.
        }
    }
    val goBack: () -> Unit = {
        if (step > 0) step -= 1
    }
    val onPrimaryClick: () -> Unit = {
        when (step) {
            0 -> step = 1
            1 -> {
                onBiometricPersist(biometricEnabled)
                step = 2
            }
            2 -> onComplete()
        }
    }
    val buttonDescription = when (step) {
        0 -> "Continue to step 2"
        1 -> "Continue to step 3"
        else -> "Complete onboarding"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ForgeColor)
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
        // Fixed header — forge background, icon + title, ~35% of screen, never animates
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.35f)
                .statusBarsPadding()
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "Follow Through app icon",
                    modifier = Modifier.size(130.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Follow Through",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() }
                )
            }
        }

        // Content section — cream background, body crossfades between steps
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.65f)
                .background(CreamBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(horizontal = 32.dp)
                    .padding(top = 32.dp, bottom = 28.dp),
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
                                0 -> Step0Body()
                                1 -> Step1Body(
                                    biometricAvailable = biometricAvailable,
                                    biometricEnabled = biometricEnabled,
                                    onBiometricChange = { biometricEnabled = it },
                                    onShowTooltip = { showBiometricTooltip = true },
                                    infoTriggerFocus = infoTriggerFocus
                                )
                                2 -> Step2Body()
                            }
                        }
                    }
                }

                ProgressDots(currentIndex = step)

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
                        containerColor = ForgeColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    AnimatedContent(
                        targetState = step == 2,
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
private fun Step0Body() {
    Text(
        text = "Keep making progress toward your goals despite setbacks, negative experiences, and self-doubt.",
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp,
        color = BodyTextColor,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun Step1Body(
    biometricAvailable: Boolean,
    biometricEnabled: Boolean,
    onBiometricChange: (Boolean) -> Unit,
    onShowTooltip: () -> Unit,
    infoTriggerFocus: FocusRequester
) {
    if (biometricAvailable) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Lock Follow Through with Face ID or Device PIN",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = BodyTextColor,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onShowTooltip,
                modifier = Modifier
                    .size(48.dp)
                    .focusRequester(infoTriggerFocus)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "More information",
                    tint = BodyTextColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = biometricEnabled,
                onCheckedChange = onBiometricChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = OnboardingSwitchTrack,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = OnboardingSwitchTrack.copy(alpha = 0.5f),
                    uncheckedBorderColor = Color.Transparent
                ),
                modifier = Modifier.semantics {
                    contentDescription = "Lock Follow Through with biometrics"
                    stateDescription = if (biometricEnabled) "On" else "Off"
                    role = Role.Switch
                }
            )
        }
    }
}

@Composable
private fun Step2Body() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Your data stays with you",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            color = BodyTextColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Everything you write in Follow Through is stored privately on this device only. No one else can see it — not us, not anyone.\n\nTo keep your data safe, back up your device regularly through your Android backup settings.\n\nIf you switch to a new phone and restore from a backup, your data should transfer. If not, it will not be recoverable — so back up often.",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 24.sp,
            color = BodyTextColor,
            textAlign = TextAlign.Center
        )
    }
}
