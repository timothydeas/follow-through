package com.ideasinc.followthrough

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
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
import androidx.core.view.WindowCompat
import com.ideasinc.followthrough.navigation.AppNavigation
import com.ideasinc.followthrough.navigation.CURRENT_ONBOARDING_VERSION
import com.ideasinc.followthrough.navigation.KEY_ONBOARDING_VERSION
import com.ideasinc.followthrough.navigation.PREFS_NAME
import com.ideasinc.followthrough.ui.settings.SettingsPreferences
import com.ideasinc.followthrough.ui.theme.GroundedTheme

class MainActivity : FragmentActivity() {

    private var authCleared by mutableStateOf(false)

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge so app backgrounds paint under the status and navigation bars.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // With the app lock on, keep intention content out of screenshots and the recents preview.
        if (AppLock.isEnabled(this)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        // Notification preferences load once here. Text size is honored straight from
        // the OS font-scale setting — no in-app override.
        SettingsPreferences.load(this)

        val container = (application as GroundedApplication).container

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val savedVersion = prefs.getInt(KEY_ONBOARDING_VERSION, 0)
        val onboardingComplete = savedVersion >= CURRENT_ONBOARDING_VERSION
        val alreadyAuthenticated = savedInstanceState?.getBoolean(KEY_AUTH_DONE, false) == true

        // shouldGate() is false when no credential is enrolled, so a user who turned the lock on and
        // later removed their PIN/biometric is never locked out of the app that holds the toggle.
        val needsBiometric = onboardingComplete && AppLock.shouldGate(this) && !alreadyAuthenticated
        authCleared = !needsBiometric

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
                // Text size follows the OS font-scale setting via the default LocalDensity.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    if (authCleared) {
                        AppNavigation(
                            container = container,
                            isExpandedWidth = isExpandedWidth,
                            useNavRail = useNavRail
                        )
                    }
                }
            }
        }

        if (!authCleared) {
            // A genuine deny (cancel / negative button / lockout) closes the app; a missing-
            // credential error fails open inside AppLock so the user is never permanently locked out.
            AppLock.prompt(this) { ok -> if (ok) authCleared = true else finish() }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_AUTH_DONE, authCleared)
    }

    companion object {
        private const val KEY_AUTH_DONE = "auth_done"
    }
}