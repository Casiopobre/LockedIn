package com.locked.lockedin.repository

import com.locked.lockedin.data.dao.PasswordDao
import com.locked.lockedin.data.model.PasswordEntry
import com.locked.lockedin.security.CryptoManager
import com.locked.lockedin.security.PwnedCheckService
import kotlinx.coroutines.flow.Flow

class PasswordRepository(
    private val passwordDao: PasswordDao,
    private val cryptoManager: CryptoManager
) {

    fun getAllPasswords(): Flow<List<PasswordEntry>> =
        passwordDao.getAllPasswords()

    fun searchPasswords(query: String): Flow<List<PasswordEntry>> =
        passwordDao.searchPasswords(query)

    suspend fun getPasswordById(id: Long): PasswordEntry? =
        passwordDao.getPasswordById(id)

    suspend fun insertPassword(
        title: String,
        username: String,
        password: String,
        website: String = "",
        notes: String = ""
    ): Long {
        val encrypted = cryptoManager.encryptPassword(password)
        val entry = PasswordEntry(
            title = title,
            username = username,
            encryptedPassword = encrypted,
            website = website,
            notes = notes
        )
        return passwordDao.insertPassword(entry)
    }

    suspend fun updatePasswordEntry(
        id: Long,
        title: String,
        username: String,
        password: String,
        website: String = "",
        notes: String = ""
    ) {
        val existing = passwordDao.getPasswordById(id) ?: return
        val encrypted = cryptoManager.encryptPassword(password)
        passwordDao.updatePassword(
            existing.copy(
                title = title,
                username = username,
                encryptedPassword = encrypted,
                website = website,
                notes = notes,
                updatedAt = System.currentTimeMillis(),
                // Reset pwned flag when the password itself changes
                isPwned = false,
                pwnedCount = 0
            )
        )
    }

    suspend fun deletePassword(entry: PasswordEntry) =
        passwordDao.deletePassword(entry)

    fun decryptPassword(encryptedPassword: String): String =
        cryptoManager.decryptPassword(encryptedPassword)

    fun generatePassword(
        length: Int = 16,
        includeUppercase: Boolean = true,
        includeLowercase: Boolean = true,
        includeNumbers: Boolean = true,
        includeSymbols: Boolean = true
    ): String = cryptoManager.generateSecurePassword(
        length, includeUppercase, includeLowercase, includeNumbers, includeSymbols
    )

    fun isEncryptionAvailable(): Boolean = cryptoManager.isEncryptionAvailable()
    
    /**
     * Runs a k-anonymity HIBP check for every password currently in the vault.
     *
     * Requires the vault to be unlocked (passwords are decrypted in memory,
     * checked, then immediately discarded — never written to disk in plaintext).
     *
     * @param onProgress Called after each entry is checked: (checkedSoFar, total).
     * @return Number of entries found in at least one breach.
     */
    suspend fun runBreachCheck(
        onProgress: ((checked: Int, total: Int) -> Unit)? = null
    ): Int {
        val entries    = passwordDao.getAllPasswordsSnapshot()
        val total      = entries.size
        var pwnedFound = 0

        entries.forEachIndexed { index, entry ->
            try {
                val plainText   = cryptoManager.decryptPassword(entry.encryptedPassword)
                val breachCount = PwnedCheckService.checkPassword(plainText)
                val isPwned     = breachCount > 0

                if (isPwned) pwnedFound++

                // Only write if something actually changed (avoids unnecessary DB churn)
                if (isPwned != entry.isPwned || breachCount != entry.pwnedCount) {
                    passwordDao.updatePwnedStatus(entry.id, isPwned, breachCount)
                }
            } catch (_: Exception) {
                // Decryption or network failure — leave existing flag untouched
            }
            onProgress?.invoke(index + 1, total)
        }

        return pwnedFound
    }
}