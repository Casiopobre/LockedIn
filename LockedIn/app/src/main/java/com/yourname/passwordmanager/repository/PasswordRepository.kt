package com.yourname.passwordmanager.repository

import com.yourname.passwordmanager.data.dao.PasswordDao
import com.yourname.passwordmanager.data.model.PasswordEntry
import com.yourname.passwordmanager.security.CryptoManager
import kotlinx.coroutines.flow.Flow

/**
 * Repository class that handles data operations
 * Acts as a single source of truth for password data
 */
class PasswordRepository(
    private val dao: PasswordDao,
    private val cryptoManager: CryptoManager
) {
    
    /**
     * Get all passwords as Flow
     */
    fun getAllPasswords(): Flow<List<PasswordEntry>> = dao.getAllPasswords()
    
    /**
     * Get a specific password by ID
     */
    suspend fun getPasswordById(id: Long): PasswordEntry? = dao.getPasswordById(id)
    
    /**
     * Insert a new password with encryption
     */
    suspend fun insertPassword(
        title: String,
        username: String,
        password: String,
        website: String = "",
        notes: String = ""
    ): Long {
        if (title.isBlank() || username.isBlank() || password.isBlank()) {
            throw IllegalArgumentException("Title, username, and password cannot be empty")
        }
        
        val encryptedPassword = cryptoManager.encryptPassword(password)
        val passwordEntry = PasswordEntry(
            title = title.trim(),
            username = username.trim(),
            encryptedPassword = encryptedPassword,
            website = website.trim(),
            notes = notes.trim()
        )
        return dao.insertPassword(passwordEntry)
    }
    
    /**
     * Update an existing password
     */
    suspend fun updatePassword(
        passwordEntry: PasswordEntry, 
        newPassword: String? = null
    ) {
        val updatedEntry = if (newPassword != null && newPassword.isNotBlank()) {
            passwordEntry.copy(
                encryptedPassword = cryptoManager.encryptPassword(newPassword),
                updatedAt = System.currentTimeMillis()
            )
        } else {
            passwordEntry.copy(updatedAt = System.currentTimeMillis())
        }
        dao.updatePassword(updatedEntry)
    }
    
    /**
     * Update password entry with new data
     */
    suspend fun updatePasswordEntry(
        id: Long,
        title: String,
        username: String,
        password: String,
        website: String = "",
        notes: String = ""
    ) {
        val existingEntry = getPasswordById(id) 
            ?: throw IllegalArgumentException("Password entry not found")
        
        if (title.isBlank() || username.isBlank() || password.isBlank()) {
            throw IllegalArgumentException("Title, username, and password cannot be empty")
        }
        
        val updatedEntry = existingEntry.copy(
            title = title.trim(),
            username = username.trim(),
            encryptedPassword = cryptoManager.encryptPassword(password),
            website = website.trim(),
            notes = notes.trim(),
            updatedAt = System.currentTimeMillis()
        )
        dao.updatePassword(updatedEntry)
    }
    
    /**
     * Delete a password entry
     */
    suspend fun deletePassword(passwordEntry: PasswordEntry) = dao.deletePassword(passwordEntry)
    
    /**
     * Search passwords by query
     */
    fun searchPasswords(query: String): Flow<List<PasswordEntry>> = 
        dao.searchPasswords("%${query.trim()}%")
    
    /**
     * Decrypt a password
     */
    fun decryptPassword(encryptedPassword: String): String =
        cryptoManager.decryptPassword(encryptedPassword)
    
    /**
     * Generate a secure password
     */
    fun generatePassword(
        length: Int = 16,
        includeUppercase: Boolean = true,
        includeLowercase: Boolean = true,
        includeNumbers: Boolean = true,
        includeSymbols: Boolean = true
    ): String = cryptoManager.generateSecurePassword(
        length, includeUppercase, includeLowercase, includeNumbers, includeSymbols
    )
    
    /**
     * Get total password count
     */
    suspend fun getPasswordCount(): Int = dao.getPasswordCount()
    
    /**
     * Check if encryption is working
     */
    fun isEncryptionAvailable(): Boolean = cryptoManager.isEncryptionAvailable()
}