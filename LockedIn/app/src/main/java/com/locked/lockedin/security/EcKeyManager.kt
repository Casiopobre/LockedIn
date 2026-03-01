package com.locked.lockedin.security

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.security.*
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Manages an EC (Elliptic Curve) P-256 key pair used for group-sharing encryption.
 *
 * Uses an ECIES-like hybrid encryption scheme:
 * - **Key agreement**: ECDH with ephemeral key pairs
 * - **Key derivation**: SHA-256 of the shared secret
 * - **Symmetric encryption**: AES-256-GCM
 *
 * The key pair is generated once and stored in [EncryptedSharedPreferences].
 * The **public key** is uploaded to the server during registration so other users
 * can encrypt the Shared Group Key (SGK) for us.
 * The **private key** stays on-device and is used to decrypt our copy of the SGK.
 */
class EcKeyManager(context: Context) {

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val prefs = EncryptedSharedPreferences.create(
        "ec_key_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // ── Public API ──────────────────────────────────────────────────────────

    /** Returns true if an EC key pair has already been generated. */
    fun isKeyPairGenerated(): Boolean =
        prefs.getString(KEY_PUBLIC, null) != null

    /**
     * Generate a fresh EC P-256 key pair and persist both halves.
     * Does nothing if the key pair already exists — call [clearKeyPair] first
     * if you want to regenerate.
     */
    fun generateKeyPair() {
        if (isKeyPairGenerated()) return

        val keyPairGen = KeyPairGenerator.getInstance("EC")
        keyPairGen.initialize(ECGenParameterSpec(EC_CURVE))
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
            ?: error("EC key pair not generated — call generateKeyPair() first.")

        // Wrap the raw Base64 in PEM armor (64-char lines)
        val lines = publicB64.chunked(64).joinToString("\n")
        return "-----BEGIN PUBLIC KEY-----\n$lines\n-----END PUBLIC KEY-----"
    }

    /**
     * Returns the raw [PublicKey] object.
     */
    fun getPublicKey(): PublicKey {
        val publicB64 = prefs.getString(KEY_PUBLIC, null)
            ?: error("EC key pair not generated.")
        val keyBytes = Base64.decode(publicB64, Base64.NO_WRAP)
        val spec = X509EncodedKeySpec(keyBytes)
        return KeyFactory.getInstance("EC").generatePublic(spec)
    }

    /**
     * Returns the raw [PrivateKey] object.
     */
    fun getPrivateKey(): PrivateKey {
        val privateB64 = prefs.getString(KEY_PRIVATE, null)
            ?: error("EC key pair not generated.")
        val keyBytes = Base64.decode(privateB64, Base64.NO_WRAP)
        val spec = PKCS8EncodedKeySpec(keyBytes)
        return KeyFactory.getInstance("EC").generatePrivate(spec)
    }

    /**
     * Encrypt data using ECIES (ECDH + AES-256-GCM).
     *
     * 1. Generate an ephemeral EC key pair.
     * 2. Perform ECDH between the ephemeral private key and the recipient's public key.
     * 3. Derive an AES-256 key from the shared secret using SHA-256.
     * 4. Encrypt the data with AES-256-GCM.
     *
     * Output format (Base64-encoded):
     * `[2-byte ephemeral key length][ephemeral public key (X.509)][12-byte IV][ciphertext + GCM tag]`
     *
     * @param data      Plaintext bytes to encrypt.
     * @param publicPem PEM-encoded public key of the recipient.
     * @return Base64-encoded ciphertext.
     */
    fun encryptWithPublicKey(data: ByteArray, publicPem: String): String {
        val recipientPublicKey = pemToPublicKey(publicPem)

        // 1. Generate ephemeral EC key pair
        val ephemeralKpg = KeyPairGenerator.getInstance("EC")
        ephemeralKpg.initialize(ECGenParameterSpec(EC_CURVE))
        val ephemeralKeyPair = ephemeralKpg.generateKeyPair()

        // 2. ECDH key agreement
        val keyAgreement = KeyAgreement.getInstance("ECDH")
        keyAgreement.init(ephemeralKeyPair.private)
        keyAgreement.doPhase(recipientPublicKey, true)
        val sharedSecret = keyAgreement.generateSecret()

        // 3. Derive AES-256 key from shared secret
        val aesKey = deriveAesKey(sharedSecret)

        // 4. AES-256-GCM encrypt
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, aesKey)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(data)

        // 5. Pack: [2-byte ephem key len][ephem pubkey][IV][ciphertext+tag]
        val ephemPubBytes = ephemeralKeyPair.public.encoded
        val ephemLen = ephemPubBytes.size
        val output = ByteArray(2 + ephemLen + IV_LENGTH + ciphertext.size)
        output[0] = (ephemLen shr 8).toByte()
        output[1] = (ephemLen and 0xFF).toByte()
        System.arraycopy(ephemPubBytes, 0, output, 2, ephemLen)
        System.arraycopy(iv, 0, output, 2 + ephemLen, IV_LENGTH)
        System.arraycopy(ciphertext, 0, output, 2 + ephemLen + IV_LENGTH, ciphertext.size)

        return Base64.encodeToString(output, Base64.NO_WRAP)
    }

    /**
     * Decrypt data using ECIES (ECDH + AES-256-GCM) with our private key.
     *
     * @param encryptedB64 Base64-encoded ciphertext (produced by [encryptWithPublicKey]).
     * @return Decrypted plaintext bytes.
     */
    fun decryptWithPrivateKey(encryptedB64: String): ByteArray {
        val raw = Base64.decode(encryptedB64, Base64.NO_WRAP)

        // 1. Extract ephemeral public key
        val ephemLen = ((raw[0].toInt() and 0xFF) shl 8) or (raw[1].toInt() and 0xFF)
        val ephemPubBytes = raw.sliceArray(2 until 2 + ephemLen)
        val ephemPubKey = KeyFactory.getInstance("EC")
            .generatePublic(X509EncodedKeySpec(ephemPubBytes))

        // 2. ECDH key agreement with our private key
        val keyAgreement = KeyAgreement.getInstance("ECDH")
        keyAgreement.init(getPrivateKey())
        keyAgreement.doPhase(ephemPubKey, true)
        val sharedSecret = keyAgreement.generateSecret()

        // 3. Derive AES-256 key
        val aesKey = deriveAesKey(sharedSecret)

        // 4. AES-256-GCM decrypt
        val iv = raw.sliceArray(2 + ephemLen until 2 + ephemLen + IV_LENGTH)
        val ciphertext = raw.sliceArray(2 + ephemLen + IV_LENGTH until raw.size)
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, aesKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return cipher.doFinal(ciphertext)
    }

    /** Delete the stored key pair (e.g. account reset). */
    fun clearKeyPair() {
        prefs.edit().clear().apply()
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    companion object {
        private const val KEY_PUBLIC  = "ec_public_key"
        private const val KEY_PRIVATE = "ec_private_key"

        private const val EC_CURVE = "secp256r1"
        private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128

        /**
         * Derive an AES-256 key from the ECDH shared secret using SHA-256.
         */
        private fun deriveAesKey(sharedSecret: ByteArray): SecretKeySpec {
            val digest = MessageDigest.getInstance("SHA-256")
            val keyBytes = digest.digest(sharedSecret)
            return SecretKeySpec(keyBytes, "AES")
        }

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
            return KeyFactory.getInstance("EC").generatePublic(spec)
        }
    }
}
