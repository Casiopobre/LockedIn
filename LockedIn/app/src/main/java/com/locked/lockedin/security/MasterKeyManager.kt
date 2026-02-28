package com.locked.lockedin.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Manages the master key: creation, PBKDF2 hashing, verification,
 * and derivation of the vault encryption key.
 */
class MasterKeyManager(context: Context) {

    private val mainKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    // EncryptedSharedPreferences stores the salt + hash securely on-device
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "master_key_prefs",
        mainKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /** Returns true if a master key has already been set up. */
    fun isMasterKeySet(): Boolean =
        prefs.getString(KEY_HASH, null) != null

    /**
     * Creates the master key for the first time.
     * Generates a random salt, derives a PBKDF2 hash, and persists both.
     * @throws IllegalStateException if a master key is already set.
     */
    fun setupMasterKey(masterKey: String) {
        check(!isMasterKeySet()) { "Master key already configured." }
        val salt = generateSalt()
        val hash = deriveHash(masterKey, salt)
        prefs.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.DEFAULT))
            .putString(KEY_HASH, Base64.encodeToString(hash, Base64.DEFAULT))
            .apply()
    }

    /**
     * Verifies a candidate master key against the stored hash.
     * Returns true if correct, false otherwise.
     */
    fun verifyMasterKey(masterKey: String): Boolean {
        val saltB64 = prefs.getString(KEY_SALT, null) ?: return false
        val storedHashB64 = prefs.getString(KEY_HASH, null) ?: return false

        val salt = Base64.decode(saltB64, Base64.DEFAULT)
        val storedHash = Base64.decode(storedHashB64, Base64.DEFAULT)
        val candidateHash = deriveHash(masterKey, salt)

        return constantTimeEquals(candidateHash, storedHash)
    }

    /**
     * Derives a 256-bit AES secret key from the master key + stored salt.
     * This key is used by [CryptoManager] to encrypt/decrypt vault entries.
     * Call only after [verifyMasterKey] returns true.
     */
    fun deriveEncryptionKey(masterKey: String): SecretKeySpec {
        val saltB64 = prefs.getString(KEY_SALT, null)
            ?: error("Master key not set up — cannot derive encryption key.")
        val salt = Base64.decode(saltB64, Base64.DEFAULT)
        val keyBytes = deriveKeyBytes(masterKey, salt)
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Clears all master key data (use for reset / wipe functionality).
     * WARNING: after calling this all encrypted vault data becomes unrecoverable.
     */
    fun clearMasterKey() {
        prefs.edit().clear().apply()
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /** PBKDF2-HMAC-SHA256 hash suitable for key-stretching a password. */
    private fun deriveHash(password: String, salt: ByteArray): ByteArray =
        deriveBytes(password, salt, HASH_KEY_LENGTH_BITS)

    /** Derive 256-bit key bytes for AES. */
    private fun deriveKeyBytes(password: String, salt: ByteArray): ByteArray =
        deriveBytes(password, salt, AES_KEY_LENGTH_BITS)

    private fun deriveBytes(password: String, salt: ByteArray, lengthBits: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, lengthBits)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        return factory.generateSecret(spec).encoded
    }

    private fun generateSalt(): ByteArray =
        ByteArray(SALT_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }

    /** Timing-safe byte array comparison to prevent timing attacks. */
    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) result = result or (a[i].toInt() xor b[i].toInt())
        return result == 0
    }

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    companion object {
        private const val KEY_SALT = "master_salt"
        private const val KEY_HASH = "master_hash"

        private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val PBKDF2_ITERATIONS = 310_000   // OWASP 2023 recommendation
        private const val SALT_LENGTH_BYTES = 32
        private const val HASH_KEY_LENGTH_BITS = 256
        private const val AES_KEY_LENGTH_BITS = 256
    }
}