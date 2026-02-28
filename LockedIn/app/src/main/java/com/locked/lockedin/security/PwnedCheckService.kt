package com.locked.lockedin.security

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.security.MessageDigest

/**
 * Checks passwords against the HaveIBeenPwned Passwords API using
 * the k-anonymity model: only the first 5 hex chars of the SHA-1 hash
 * are ever sent over the network — the full password never leaves the device.
 *
 * API docs: https://haveibeenpwned.com/API/v3#SearchingPwnedPasswords
 */
object PwnedCheckService {

    private const val TAG = "PwnedCheckService"
    private const val HIBP_RANGE_URL = "https://api.pwnedpasswords.com/range/"

    /**
     * Checks whether a plaintext password appears in any known breach.
     *
     * @param plainTextPassword The decrypted password to check.
     * @return The number of times it appeared in breaches (0 = not pwned).
     *         Returns -1 on network / IO error so callers can distinguish
     *         "clean" from "check failed".
     */
    suspend fun checkPassword(plainTextPassword: String): Int = withContext(Dispatchers.IO) {
        try {
            // 1. SHA-1 hash → uppercase hex
            val sha1 = sha1Hex(plainTextPassword)
            val prefix = sha1.substring(0, 5)   // sent to API
            val suffix = sha1.substring(5)       // checked locally

            // 2. Fetch all hashes sharing the prefix (response is ~800 lines)
            val responseBody = URL("$HIBP_RANGE_URL$prefix").readText()

            // 3. Search for our suffix in the response (case-insensitive to be safe)
            for (line in responseBody.lineSequence()) {
                // Each line: "SUFFIX:COUNT"
                val colonIdx = line.indexOf(':')
                if (colonIdx == -1) continue
                val hashSuffix = line.substring(0, colonIdx).trim()
                if (hashSuffix.equals(suffix, ignoreCase = true)) {
                    val count = line.substring(colonIdx + 1).trim().toIntOrNull() ?: 1
                    Log.w(TAG, "Password found in $count breach(es).")
                    return@withContext count
                }
            }
            0 // Not found in any breach
        } catch (e: Exception) {
            Log.e(TAG, "HIBP check failed: ${e.message}")
            -1 // Network / IO error
        }
    }

    /** Returns SHA-1 of [input] as an uppercase hex string. */
    private fun sha1Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-1")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02X".format(it) }
    }
}