package com.locked.lockedin.security

import java.security.MessageDigest

/**
 * Utility for hashing the master password on the client side before sending
 * it to the server.
 *
 * The server expects a **SHA-256 hex-digest** (64-character lowercase string)
 * of the master password. This is NOT the final hash — the server applies
 * Argon2 on top of it.
 */
object PasswordHasher {

    /**
     * Compute the SHA-256 hex-digest of [password].
     *
     * ```
     * PasswordHasher.sha256("password")
     * // → "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8"
     * ```
     */
    fun sha256(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
