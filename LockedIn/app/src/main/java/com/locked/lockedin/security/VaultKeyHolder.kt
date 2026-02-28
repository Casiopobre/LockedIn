package com.locked.lockedin.security

import javax.crypto.spec.SecretKeySpec

/**
 * Holds the session-scoped derived AES key in memory.
 *
 * The key is set once the user successfully sets up or unlocks the vault,
 * and is cleared when the app process dies — it is NEVER written to disk directly.
 *
 * Access this object from [CryptoManager] instead of generating a random key.
 */
object VaultKeyHolder {

    @Volatile
    private var _key: SecretKeySpec? = null

    /** Returns true if the vault is currently unlocked (key is in memory). */
    val isUnlocked: Boolean
        get() = _key != null

    /**
     * Store the derived key. Call this after successful setup or unlock.
     */
    fun setKey(key: SecretKeySpec) {
        _key = key
    }

    /**
     * Retrieve the current session key.
     * @throws IllegalStateException if the vault has not been unlocked.
     */
    fun requireKey(): SecretKeySpec =
        _key ?: error("Vault is locked. Please unlock before accessing passwords.")

    /**
     * Clear the key from memory (lock the vault).
     * Call this when the app moves to the background if desired.
     */
    fun clearKey() {
        _key = null
    }
}