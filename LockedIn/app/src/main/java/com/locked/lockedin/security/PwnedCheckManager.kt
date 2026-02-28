package com.locked.lockedin.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Manages breach-check scheduling.
 *
 * Because the vault must be unlocked to decrypt passwords, we cannot run
 * the check silently in the background. Instead we run it opportunistically:
 * whenever [shouldRunCheck] returns true the caller (ViewModel) initiates
 * the check and then calls [recordCheckCompleted] to update the timestamp.
 */
class PwnedCheckManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "pwned_check_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /** True if no check has been run yet, or the last check was > 24 h ago. */
    fun shouldRunCheck(): Boolean {
        val lastCheck = prefs.getLong(KEY_LAST_CHECK_MS, 0L)
        return System.currentTimeMillis() - lastCheck >= INTERVAL_MS
    }

    /** Call this after a successful check run to reset the 24-h timer. */
    fun recordCheckCompleted() {
        prefs.edit().putLong(KEY_LAST_CHECK_MS, System.currentTimeMillis()).apply()
    }

    /**
     * DEBUG: Clears the stored timestamp so [shouldRunCheck] returns true
     * immediately on the next call — useful for manual testing.
     */
    fun resetTimer() {
        prefs.edit().remove(KEY_LAST_CHECK_MS).apply()
    }

    /** Milliseconds remaining until the next check is due (0 if overdue). */
    fun millisUntilNextCheck(): Long {
        val lastCheck = prefs.getLong(KEY_LAST_CHECK_MS, 0L)
        val elapsed   = System.currentTimeMillis() - lastCheck
        return maxOf(0L, INTERVAL_MS - elapsed)
    }

    companion object {
        private const val KEY_LAST_CHECK_MS = "last_check_ms"
        //private const val INTERVAL_MS       = 24 * 60 * 60 * 1_000L // 24 hours
        private const val INTERVAL_MS       = 1 * 1 * 60 * 1_000L // 1 min
    }
}