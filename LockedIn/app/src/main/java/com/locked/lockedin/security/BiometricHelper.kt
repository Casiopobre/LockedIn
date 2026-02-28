package com.locked.lockedin.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import javax.crypto.Cipher

object BiometricHelper {

    /** Returns true if the device supports and has enrolled strong biometrics. */
    fun isAvailable(activity: FragmentActivity): Boolean {
        val manager = BiometricManager.from(activity)
        return manager.canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Show the biometric prompt.
     *
     * @param cryptoObject  Wrap your decryption Cipher in a CryptoObject.
     * @param onSuccess     Called with the authenticated Cipher on success.
     * @param onFailure     Called with an error message on failure/cancellation.
     */
    fun showPrompt(
        activity: FragmentActivity,
        title: String = "Unlock Vault",
        subtitle: String = "Use your biometric to access your passwords",
        cryptoObject: BiometricPrompt.CryptoObject,
        onSuccess: (Cipher) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val prompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    val cipher = result.cryptoObject?.cipher
                    if (cipher != null) onSuccess(cipher)
                    else onFailure("Authentication succeeded but cipher was null.")
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onFailure(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    // Not called for cancellation — just a single bad attempt; prompt stays open
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .setNegativeButtonText("Use master key")
            .build()

        prompt.authenticate(promptInfo, cryptoObject)
    }
}