package com.ideasinc.followthrough

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
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
import com.ideasinc.followthrough.ui.theme.GroundedTheme

class MainActivity : FragmentActivity() {

    private var authCleared by mutableStateOf(false)

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

        setContent {
            GroundedTheme {
                if (authCleared) {
                    AppNavigation(container = container)
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    )
                }
            }
        }

        if (!authCleared) {
            showBiometricPrompt()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_AUTH_DONE, authCleared)
    }

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
            .setTitle("Unlock Follow Thru")
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