package com.privbrowse.app.privacy

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricLock {
    fun isAvailable(activity: FragmentActivity): Boolean =
        BiometricManager.from(activity).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS

    fun authenticate(activity: FragmentActivity, onSuccess: () -> Unit, onFail: (String) -> Unit) {
        if (!isAvailable(activity)) {
            onFail("Biometric authentication is not available on this device")
            return
        }
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { onSuccess() }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { onFail(errString.toString()) }
        })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock PrivBrowse")
            .setSubtitle("Use your device biometric to continue")
            .setNegativeButtonText("Cancel")
            .build()
        prompt.authenticate(info)
    }
}
