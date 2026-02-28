package com.locked.lockedin.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Stores and retrieves the vault's AES key, protected by biometric authentication.
 *
 * Flow:
 *  - [saveKeyForBiometric]: encrypts the vault key with a hardware-backed KeyStore key
 *    and stores the ciphertext. Call this after the user unlocks with master key and opts in.
 *  - [getCipherForDecryption]: returns an authenticated Cipher; pass this to BiometricPrompt.
 *  - [decryptVaultKey]: call this in the BiometricPrompt success callback to recover the key.
 */
class BiometricKeyManager(context: Context) {

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    private val prefs = EncryptedSharedPreferences.create(
        "biometric_key_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /** True if the user has previously enabled biometric unlock. */
    fun isBiometricKeyStored(): Boolean =
        prefs.getString(KEY_ENCRYPTED_VAULT_KEY, null) != null

    /**
     * Encrypts [vaultKey] with the hardware-backed KeyStore key and stores the result.
     * Call this when the user opts in to biometric unlock (after master key auth).
     */
    fun saveKeyForBiometric(vaultKey: SecretKeySpec) {
        val keystoreKey = getOrCreateBiometricKeystoreKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey)

        val iv = cipher.iv
        val encrypted = cipher.doFinal(vaultKey.encoded)
        val combined = iv + encrypted

        prefs.edit()
            .putString(KEY_ENCRYPTED_VAULT_KEY, Base64.encodeToString(combined, Base64.DEFAULT))
            .apply()
    }

    /**
     * Returns a Cipher initialized for ENCRYPTION (step 1 of enrollment).
     * Pass this to BiometricPrompt. The IV is embedded inside the cipher state.
     */
    fun getCipherForEncryption(): Cipher {
        val keystoreKey = getOrCreateBiometricKeystoreKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey)
        return cipher
    }

    /**
     * Use the authenticated [cipher] (from BiometricPrompt success callback)
     * to encrypt and store the vault key. Call this instead of saveKeyForBiometric().
     */
    fun encryptAndSaveKey(cipher: Cipher, vaultKey: SecretKeySpec) {
        val iv = cipher.iv
        val encrypted = cipher.doFinal(vaultKey.encoded)
        val combined = iv + encrypted
        prefs.edit()
            .putString(KEY_ENCRYPTED_VAULT_KEY, Base64.encodeToString(combined, Base64.DEFAULT))
            .apply()
    }

    /**
     * Returns a Cipher initialized for decryption with the stored IV.
     * Pass this to [androidx.biometric.BiometricPrompt.CryptoObject].
     * Returns null if no biometric key is stored.
     */
    fun getCipherForDecryption(): Cipher? {
        val combined = prefs.getString(KEY_ENCRYPTED_VAULT_KEY, null)
            ?.let { Base64.decode(it, Base64.DEFAULT) } ?: return null

        val iv = combined.sliceArray(0 until IV_LENGTH)
        val keystoreKey = getBiometricKeystoreKey() ?: return null

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, keystoreKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return cipher
    }

    /**
     * Use the authenticated [cipher] (from BiometricPrompt success callback)
     * to decrypt and return the vault key.
     */
    fun decryptVaultKey(cipher: Cipher): SecretKeySpec {
        val combined = prefs.getString(KEY_ENCRYPTED_VAULT_KEY, null)
            ?.let { Base64.decode(it, Base64.DEFAULT) }
            ?: error("No biometric-protected key found.")

        val encrypted = combined.sliceArray(IV_LENGTH until combined.size)
        val keyBytes = cipher.doFinal(encrypted)
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Removes the stored biometric key (e.g. when user disables biometric unlock).
     */
    fun clearBiometricKey() {
        prefs.edit().remove(KEY_ENCRYPTED_VAULT_KEY).apply()
        // Also remove the KeyStore key
        try {
            KeyStore.getInstance(ANDROID_KEYSTORE).apply {
                load(null)
                deleteEntry(BIOMETRIC_KEY_ALIAS)
            }
        } catch (_: Exception) {}
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private fun getOrCreateBiometricKeystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        keyStore.getKey(BIOMETRIC_KEY_ALIAS, null)?.let { return it as SecretKey }

        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGen.init(
            KeyGenParameterSpec.Builder(
                BIOMETRIC_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(true)
                // Key invalidated if new biometrics are enrolled — security best practice
                .setInvalidatedByBiometricEnrollment(true)
                .build()
        )
        return keyGen.generateKey()
    }

    private fun getBiometricKeystoreKey(): SecretKey? {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.getKey(BIOMETRIC_KEY_ALIAS, null) as? SecretKey
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val BIOMETRIC_KEY_ALIAS = "vault_biometric_key"
        private const val KEY_ENCRYPTED_VAULT_KEY = "encrypted_vault_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
    }
}