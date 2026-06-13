package com.ideasinc.followthrough

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.ideasinc.followthrough.navigation.AppNavigation
import com.ideasinc.followthrough.navigation.CURRENT_ONBOARDING_VERSION
import com.ideasinc.followthrough.navigation.KEY_BIOMETRIC_ENABLED
import com.ideasinc.followthrough.navigation.KEY_ONBOARDING_VERSION
import com.ideasinc.followthrough.navigation.PREFS_NAME
import com.ideasinc.followthrough.notifications.EXTRA_CHECKIN_ID
import com.ideasinc.followthrough.ui.launch.LaunchInsightGate
import com.ideasinc.followthrough.ui.theme.GroundedTheme

class MainActivity : FragmentActivity() {

    private var authCleared by mutableStateOf(false)

    // A check-in reminder tap arrives as an EXTRA_CHECKIN_ID on the launch intent
    // (cold start) or the new intent (already running). AppNavigation observes
    // this and deep-links to that specific check-in, synthesising Home underneath.
    private var pendingCheckInId by mutableStateOf<String?>(null)

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge so the launch insight screen's brown background paints
        // under the status and navigation bars for a true full-screen takeover.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val container = (application as GroundedApplication).container

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val savedVersion = prefs.getInt(KEY_ONBOARDING_VERSION, 0)
        val onboardingComplete = savedVersion >= CURRENT_ONBOARDING_VERSION
        val biometricEnabled = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        val alreadyAuthenticated = savedInstanceState?.getBoolean(KEY_AUTH_DONE, false) == true

        val needsBiometric = onboardingComplete && biometricEnabled && !alreadyAuthenticated
        authCleared = !needsBiometric

        pendingCheckInId = checkInIdOf(intent)
        // A cold start from a reminder tap skips the launch-insight screen so the
        // deep-link opens the check-in directly. Only set on this cold-start path;
        // a warm open (onNewIntent) isn't showing the insight, so it leaves it
        // alone and a later normal launch still sees the insight.
        LaunchInsightGate.skipForNotificationOpen = pendingCheckInId != null

        setContent {
            GroundedTheme {
                // Adaptive layout. On EXPANDED widths (≥840dp — most tablets in
                // landscape, large foldables) Home becomes a two-pane goals-list +
                // goal-detail layout; every other screen, and the whole phone
                // experience, stays single-column, capped and centred against the
                // cream page (the cap is applied per-destination in AppNavigation).
                val windowSizeClass = calculateWindowSizeClass(this)
                // Compact (< 600dp) = phone → bottom NavigationBar, single column.
                // Medium / Expanded (≥ 600dp) = tablet → left NavigationRail.
                // Expanded (≥ 840dp) additionally promotes the Goals tab to the
                // two-pane list-detail layout.
                val widthClass = windowSizeClass.widthSizeClass
                val useNavRail = widthClass != WindowWidthSizeClass.Compact
                val isExpandedWidth = widthClass == WindowWidthSizeClass.Expanded
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    if (authCleared) {
                        AppNavigation(
                            container = container,
                            pendingCheckInId = pendingCheckInId,
                            onCheckInConsumed = { pendingCheckInId = null },
                            isExpandedWidth = isExpandedWidth,
                            useNavRail = useNavRail
                        )
                    }
                }
            }
        }

        if (!authCleared) {
            showBiometricPrompt()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Adopt the new intent as the activity's current intent, then surface any
        // per-goal deep link to the navigation layer.
        setIntent(intent)
        checkInIdOf(intent)?.let { pendingCheckInId = it }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_AUTH_DONE, authCleared)
    }

    private fun checkInIdOf(intent: Intent?): String? =
        intent?.getStringExtra(EXTRA_CHECKIN_ID)?.takeIf { it.isNotBlank() }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                authCleared = true
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                finish()
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock FollowThru")
            .setSubtitle("confirm your identity to continue")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        BiometricPrompt(this, executor, callback).authenticate(promptInfo)
    }

    companion object {
        private const val KEY_AUTH_DONE = "auth_done"
    }
}