package com.yourname.passwordmanager.data.dao

import androidx.room.*
import com.yourname.passwordmanager.data.model.PasswordEntry
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for password operations
 */
@Dao
interface PasswordDao {
    
    /**
     * Get all passwords ordered by title
     */
    @Query("SELECT * FROM password_entries ORDER BY title ASC")
    fun getAllPasswords(): Flow<List<PasswordEntry>>
    
    /**
     * Get a specific password by ID
     */
    @Query("SELECT * FROM password_entries WHERE id = :id")
    suspend fun getPasswordById(id: Long): PasswordEntry?
    
    /**
     * Insert a new password entry
     */
    @Insert
    suspend fun insertPassword(password: PasswordEntry): Long
    
    /**
     * Update an existing password entry
     */
    @Update
    suspend fun updatePassword(password: PasswordEntry)
    
    /**
     * Delete a password entry
     */
    @Delete
    suspend fun deletePassword(password: PasswordEntry)
    
    /**
     * Search passwords by title or website
     */
    @Query("""
        SELECT * FROM password_entries 
        WHERE title LIKE :searchQuery 
        OR website LIKE :searchQuery 
        OR username LIKE :searchQuery
        ORDER BY title ASC
    """)
    fun searchPasswords(searchQuery: String): Flow<List<PasswordEntry>>
    
    /**
     * Get passwords count
     */
    @Query("SELECT COUNT(*) FROM password_entries")
    suspend fun getPasswordCount(): Int
}