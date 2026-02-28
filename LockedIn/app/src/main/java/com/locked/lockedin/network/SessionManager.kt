package com.locked.lockedin.network

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * Manages session state: JWT token and current user_id.
 *
 * Tokens are stored in EncryptedSharedPreferences so they survive
 * process restarts while remaining encrypted at rest.
 */
class SessionManager(context: Context) {

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        "vault_session_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // ── Token ───────────────────────────────────────────────────────────────

    /** Save the JWT access token. */
    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    /** Retrieve the JWT access token, or null if not logged in. */
    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    /** Returns the token formatted for the Authorization header. */
    fun bearerToken(): String? = getToken()?.let { "Bearer $it" }

    // ── User ID ─────────────────────────────────────────────────────────────

    /** Save the plaintext user_id for convenience (e.g. display). */
    fun saveUserId(userId: String) {
        prefs.edit().putString(KEY_USER_ID, userId).apply()
    }

    /** Retrieve the stored user_id. */
    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)

    // ── Password hash (cached for deferred auth) ────────────────────────────

    /** Save the SHA-256 hash of the master password for deferred auth. */
    fun savePasswordHash(hash: String) {
        prefs.edit().putString(KEY_PASSWORD_HASH, hash).apply()
    }

    /** Retrieve the cached password hash, or null. */
    fun getPasswordHash(): String? = prefs.getString(KEY_PASSWORD_HASH, null)

    // ── Session control ─────────────────────────────────────────────────────

    /** Returns true if a valid session exists. */
    val isLoggedIn: Boolean
        get() = getToken() != null

    /** Clear all session data (logout). */
    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_TOKEN         = "jwt_token"
        private const val KEY_USER_ID       = "user_id"
        private const val KEY_PASSWORD_HASH = "password_hash"
    }
}
