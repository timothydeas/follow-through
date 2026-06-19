package com.ideasinc.followthrough

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.ideasinc.followthrough.navigation.KEY_BIOMETRIC_ENABLED
import com.ideasinc.followthrough.navigation.PREFS_NAME

/**
 * The optional "Lock FollowThru with Face ID or Device PIN" gate, shared by every surface that can
 * show intention content directly — [MainActivity] AND [InTheMomentActivity] (reached from a
 * notification, bypassing the home screen). Centralised so the lock can't be silently bypassed on
 * one entry point, and so the no-credential / hardware-unavailable cases FAIL OPEN — never locking
 * a user out of the very app that holds the toggle to turn the lock back off.
 */
object AppLock {

    private const val AUTHENTICATORS =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

    /** Whether the user turned the lock on. */
    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_BIOMETRIC_ENABLED, false)

    /** The device can actually authenticate right now (a biometric or device credential exists). */
    private fun canAuthenticate(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(AUTHENTICATORS) ==
            BiometricManager.BIOMETRIC_SUCCESS

    /**
     * True only when content must be gated: the lock is on AND the device can authenticate. If the
     * lock is on but there's no usable credential (the user removed it after enabling the lock),
     * this returns false so the app stays reachable — the in-app toggle is the only way to disable
     * the lock, so blocking here would be a permanent lockout.
     */
    fun shouldGate(context: Context): Boolean = isEnabled(context) && canAuthenticate(context)

    /**
     * Prompts for biometric / device-credential auth. [onResult] gets true to proceed, false to
     * block. A missing-credential / hardware-unavailable error fails OPEN (true); a genuine deny
     * (user cancel, negative button, lockout) fails closed (false).
     */
    fun prompt(activity: FragmentActivity, onResult: (Boolean) -> Unit) {
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) =
                onResult(true)

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                val failOpen = errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS ||
                    errorCode == BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL ||
                    errorCode == BiometricPrompt.ERROR_HW_NOT_PRESENT ||
                    errorCode == BiometricPrompt.ERROR_HW_UNAVAILABLE
                onResult(failOpen)
            }
        }
        val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback)
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock FollowThru")
            .setSubtitle("Confirm your identity to continue")
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()
        prompt.authenticate(info)
    }
}
