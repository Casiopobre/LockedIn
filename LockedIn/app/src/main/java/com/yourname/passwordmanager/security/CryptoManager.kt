package com.yourname.passwordmanager.security

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

/**
 * Handles AES-256-GCM encryption/decryption of vault entries.
 *
 * The secret key is no longer stored on disk — it is derived from the master key
 * at unlock time and kept in [VaultKeyHolder] for the duration of the session.
 *
 * This means:
 *  - Even if the device is compromised, encrypted blobs cannot be decrypted
 *    without knowing the master key.
 *  - The key is wiped from memory when the process ends.
 */
class CryptoManager {

    /**
     * Encrypt a password using AES-256-GCM.
     * Requires the vault to be unlocked ([VaultKeyHolder.isUnlocked] == true).
     */
    fun encryptPassword(password: String): String {
        return try {
            val secretKey = VaultKeyHolder.requireKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val iv = cipher.iv
            val encryptedData = cipher.doFinal(password.toByteArray(Charsets.UTF_8))

            // Prepend IV to ciphertext, then Base64-encode the whole thing
            val combined = iv + encryptedData
            Base64.encodeToString(combined, Base64.DEFAULT)
        } catch (e: Exception) {
            throw SecurityException("Encryption failed", e)
        }
    }

    /**
     * Decrypt a password using AES-256-GCM.
     * Requires the vault to be unlocked.
     */
    fun decryptPassword(encryptedPassword: String): String {
        return try {
            val combined = Base64.decode(encryptedPassword, Base64.DEFAULT)
            val iv = combined.sliceArray(0 until IV_LENGTH)
            val encryptedData = combined.sliceArray(IV_LENGTH until combined.size)

            val secretKey = VaultKeyHolder.requireKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decryptedData = cipher.doFinal(encryptedData)
            String(decryptedData, Charsets.UTF_8)
        } catch (e: Exception) {
            throw SecurityException("Decryption failed", e)
        }
    }

    /**
     * Generate a cryptographically secure random password.
     * Does NOT require the vault to be unlocked.
     */
    fun generateSecurePassword(
        length: Int = DEFAULT_PASSWORD_LENGTH,
        includeUppercase: Boolean = true,
        includeLowercase: Boolean = true,
        includeNumbers: Boolean = true,
        includeSymbols: Boolean = true
    ): String {
        val chars = buildString {
            if (includeUppercase) append(UPPERCASE_CHARS)
            if (includeLowercase) append(LOWERCASE_CHARS)
            if (includeNumbers) append(NUMBER_CHARS)
            if (includeSymbols) append(SYMBOL_CHARS)
        }
        if (chars.isEmpty()) throw IllegalArgumentException("At least one character type must be selected")

        val random = java.security.SecureRandom()
        return (1..length).map { chars[random.nextInt(chars.length)] }.joinToString("")
    }

    /** Returns true if the vault is unlocked and encryption is available. */
    fun isEncryptionAvailable(): Boolean = VaultKeyHolder.isUnlocked

    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
        private const val DEFAULT_PASSWORD_LENGTH = 16

        private const val UPPERCASE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        private const val LOWERCASE_CHARS = "abcdefghijklmnopqrstuvwxyz"
        private const val NUMBER_CHARS = "0123456789"
        private const val SYMBOL_CHARS = "!@#\$%^&*()_+-=[]{}|;:,.<>?"
    }
}