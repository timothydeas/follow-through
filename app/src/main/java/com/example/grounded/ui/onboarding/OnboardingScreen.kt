package com.example.grounded.ui.onboarding

import androidx.biometric.BiometricManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.LaunchedEffect
import com.example.grounded.ui.theme.PoppinsFontFamily

private val OnboardingBackground = Color(0xFF7A9E7E)
private val OnboardingButtonBg = Color(0xFF6A8E6E)
private val OnboardingSwitchTrack = Color(0xFF5C7D60)

@Composable
fun OnboardingScreen1(onContinue: () -> Unit) {
    val continueFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { continueFocus.requestFocus() } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OnboardingBackground)
            .systemBarsPadding()
            .padding(horizontal = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Follow Through",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 32.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Keep making progress toward your goals despite setbacks, negative experiences, and self-doubt.",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 26.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }

        Button(
            onClick = onContinue,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .fillMaxWidth()
                .focusRequester(continueFocus),
            colors = ButtonDefaults.buttonColors(
                containerColor = OnboardingButtonBg,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Continue",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun OnboardingScreen3(onContinue: () -> Unit) {
    val gotItFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { gotItFocus.requestFocus() } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OnboardingBackground)
            .systemBarsPadding()
            .padding(horizontal = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Follow Through",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Your data stays with you",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Everything you write in Follow Through is stored privately on this device only. No one else can see it — not us, not anyone.\n\nTo keep your data safe, back up your device regularly through your Android backup settings.\n\nIf you switch to a new phone and restore from a backup, your data should transfer. If not, it will not be recoverable — so back up often.",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                lineHeight = 24.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }

        Button(
            onClick = onContinue,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .fillMaxWidth()
                .focusRequester(gotItFocus),
            colors = ButtonDefaults.buttonColors(
                containerColor = OnboardingButtonBg,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Got it",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun OnboardingScreen2(onUnderstand: (biometricEnabled: Boolean) -> Unit) {
    val context = LocalContext.current
    val biometricAvailable = remember {
        BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }
    var biometricEnabled by remember { mutableStateOf(false) }
    var showBiometricTooltip by remember { mutableStateOf(false) }
    val gotItFocus = remember { FocusRequester() }
    val infoTriggerFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { gotItFocus.requestFocus() } }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OnboardingBackground)
            .systemBarsPadding()
            .padding(horizontal = 40.dp)
    ) {
        // Biometric toggle — vertically centered
        if (biometricAvailable) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Lock Follow Through with Face ID or Device PIN",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { showBiometricTooltip = true },
                    modifier = Modifier
                        .size(48.dp)
                        .focusRequester(infoTriggerFocus)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "More information",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Switch(
                    checked = biometricEnabled,
                    onCheckedChange = { biometricEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = OnboardingSwitchTrack,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = OnboardingSwitchTrack.copy(alpha = 0.5f),
                        uncheckedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier.semantics {
                        contentDescription =
                            "Lock Follow Through with Face ID or Device PIN"
                        stateDescription = if (biometricEnabled) "On" else "Off"
                        role = Role.Switch
                    }
                )
            }
        }

        Button(
            onClick = { onUnderstand(biometricEnabled) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .fillMaxWidth()
                .focusRequester(gotItFocus),
            colors = ButtonDefaults.buttonColors(
                containerColor = OnboardingButtonBg,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Got it",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp
            )
        }
    }
}
