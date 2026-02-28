package com.locked.lockedin.security

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher

/**
 * Manages an RSA-2048 key pair used for group-sharing encryption.
 *
 * The key pair is generated once and stored in [EncryptedSharedPreferences].
 * The **public key** is uploaded to the server during registration so other users
 * can encrypt the Shared Group Key (SGK) for us.
 * The **private key** stays on-device and is used to decrypt our copy of the SGK.
 */
class RsaKeyManager(context: Context) {

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val prefs = EncryptedSharedPreferences.create(
        "rsa_key_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // ── Public API ──────────────────────────────────────────────────────────

    /** Returns true if an RSA key pair has already been generated. */
    fun isKeyPairGenerated(): Boolean =
        prefs.getString(KEY_PUBLIC, null) != null

    /**
     * Generate a fresh RSA-2048 key pair and persist both halves.
     * Does nothing if the key pair already exists — call [clearKeyPair] first
     * if you want to regenerate.
     */
    fun generateKeyPair() {
        if (isKeyPairGenerated()) return

        val keyPairGen = KeyPairGenerator.getInstance("RSA")
        keyPairGen.initialize(RSA_KEY_SIZE)
        val keyPair = keyPairGen.generateKeyPair()

        val publicB64  = Base64.encodeToString(keyPair.public.encoded,  Base64.NO_WRAP)
        val privateB64 = Base64.encodeToString(keyPair.private.encoded, Base64.NO_WRAP)

        prefs.edit()
            .putString(KEY_PUBLIC, publicB64)
            .putString(KEY_PRIVATE, privateB64)
            .apply()
    }

    /**
     * Returns the public key in PEM format, ready to be sent to the server.
     * @throws IllegalStateException if the key pair hasn't been generated yet.
     */
    fun getPublicKeyPem(): String {
        val publicB64 = prefs.getString(KEY_PUBLIC, null)
            ?: error("RSA key pair not generated — call generateKeyPair() first.")

        // Wrap the raw Base64 in PEM armor (64-char lines)
        val lines = publicB64.chunked(64).joinToString("\n")
        return "-----BEGIN PUBLIC KEY-----\n$lines\n-----END PUBLIC KEY-----"
    }

    /**
     * Returns the raw [PublicKey] object.
     */
    fun getPublicKey(): PublicKey {
        val publicB64 = prefs.getString(KEY_PUBLIC, null)
            ?: error("RSA key pair not generated.")
        val keyBytes = Base64.decode(publicB64, Base64.NO_WRAP)
        val spec = X509EncodedKeySpec(keyBytes)
        return KeyFactory.getInstance("RSA").generatePublic(spec)
    }

    /**
     * Returns the raw [PrivateKey] object.
     */
    fun getPrivateKey(): PrivateKey {
        val privateB64 = prefs.getString(KEY_PRIVATE, null)
            ?: error("RSA key pair not generated.")
        val keyBytes = Base64.decode(privateB64, Base64.NO_WRAP)
        val spec = PKCS8EncodedKeySpec(keyBytes)
        return KeyFactory.getInstance("RSA").generatePrivate(spec)
    }

    /**
     * Encrypt data with a PEM-encoded public key (e.g. from the server).
     * Used to encrypt the SGK for another user.
     *
     * @param data      Plaintext bytes to encrypt.
     * @param publicPem PEM-encoded public key of the recipient.
     * @return Base64-encoded ciphertext.
     */
    fun encryptWithPublicKey(data: ByteArray, publicPem: String): String {
        val publicKey = pemToPublicKey(publicPem)
        val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        val encrypted = cipher.doFinal(data)
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    /**
     * Decrypt data with our private key.
     * Used to decrypt the SGK that was encrypted for us.
     *
     * @param encryptedB64 Base64-encoded ciphertext.
     * @return Decrypted plaintext bytes.
     */
    fun decryptWithPrivateKey(encryptedB64: String): ByteArray {
        val privateKey = getPrivateKey()
        val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        val encryptedBytes = Base64.decode(encryptedB64, Base64.NO_WRAP)
        return cipher.doFinal(encryptedBytes)
    }

    /** Delete the stored key pair (e.g. account reset). */
    fun clearKeyPair() {
        prefs.edit().clear().apply()
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    companion object {
        private const val KEY_PUBLIC  = "rsa_public_key"
        private const val KEY_PRIVATE = "rsa_private_key"

        private const val RSA_KEY_SIZE = 2048
        private const val RSA_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"

        /**
         * Parse a PEM-encoded public key string into a [PublicKey].
         */
        fun pemToPublicKey(pem: String): PublicKey {
            val base64 = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("\\s+".toRegex(), "")
            val keyBytes = Base64.decode(base64, Base64.NO_WRAP)
            val spec = X509EncodedKeySpec(keyBytes)
            return KeyFactory.getInstance("RSA").generatePublic(spec)
        }
    }
}
