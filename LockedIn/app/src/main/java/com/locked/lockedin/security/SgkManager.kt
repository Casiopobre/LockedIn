package com.locked.lockedin.security

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Shared Group Key (SGK) utilities.
 *
 * The SGK is a random AES-256 key shared among all members of a group.
 * Passwords stored in a group are encrypted with the SGK using AES-256-GCM.
 *
 * Workflow:
 * 1. Group creator calls [generateSgk] to create a fresh AES-256 key.
 * 2. The SGK is encrypted with each member's RSA public key ([RsaKeyManager.encryptWithPublicKey])
 *    and uploaded to the server.
 * 3. Members retrieve their encrypted SGK copy and decrypt it with their RSA private key
 *    ([RsaKeyManager.decryptWithPrivateKey]).
 * 4. Passwords are encrypted/decrypted with [encryptWithSgk] / [decryptWithSgk].
 */
object SgkManager {

    private const val AES_KEY_SIZE_BITS = 256
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    // ── Key generation ──────────────────────────────────────────────────────

    /**
     * Generate a new random AES-256 key to use as SGK.
     * @return Raw key bytes (32 bytes).
     */
    fun generateSgk(): ByteArray {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(AES_KEY_SIZE_BITS)
        return keyGen.generateKey().encoded
    }

    // ── Encrypt / Decrypt with SGK ──────────────────────────────────────────

    /**
     * Encrypt plaintext data using the given SGK bytes (AES-256-GCM).
     *
     * @param plaintext  The data to encrypt.
     * @param sgkBytes   The 32-byte SGK.
     * @return Base64-encoded string: `IV || ciphertext`.
     */
    fun encryptWithSgk(plaintext: String, sgkBytes: ByteArray): String {
        val key: SecretKey = SecretKeySpec(sgkBytes, "AES")
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val iv = cipher.iv                                       // 12 bytes
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = iv + ciphertext                           // IV || ciphertext+tag
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * Decrypt Base64-encoded `IV || ciphertext` produced by [encryptWithSgk].
     *
     * @param encryptedB64  The Base64-encoded ciphertext.
     * @param sgkBytes      The 32-byte SGK.
     * @return Decrypted plaintext string.
     */
    fun decryptWithSgk(encryptedB64: String, sgkBytes: ByteArray): String {
        val combined = Base64.decode(encryptedB64, Base64.NO_WRAP)
        val iv = combined.sliceArray(0 until IV_LENGTH)
        val ciphertext = combined.sliceArray(IV_LENGTH until combined.size)

        val key: SecretKey = SecretKeySpec(sgkBytes, "AES")
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))

        val decrypted = cipher.doFinal(ciphertext)
        return String(decrypted, Charsets.UTF_8)
    }

    // ── Serialisation helpers ───────────────────────────────────────────────

    /** Encode raw SGK bytes to Base64 (for storage / transmission). */
    fun sgkToBase64(sgkBytes: ByteArray): String =
        Base64.encodeToString(sgkBytes, Base64.NO_WRAP)

    /** Decode a Base64 SGK back to raw bytes. */
    fun sgkFromBase64(sgkB64: String): ByteArray =
        Base64.decode(sgkB64, Base64.NO_WRAP)
}
