package com.locked.lockedin.data.dao

import androidx.room.*
import com.locked.lockedin.data.model.PasswordEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface PasswordDao {

    @Query("SELECT * FROM passwords ORDER BY updatedAt DESC")
    fun getAllPasswords(): Flow<List<PasswordEntry>>

    @Query("SELECT * FROM passwords WHERE title LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%' OR website LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchPasswords(query: String): Flow<List<PasswordEntry>>

    @Query("SELECT * FROM passwords WHERE id = :id")
    suspend fun getPasswordById(id: Long): PasswordEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPassword(password: PasswordEntry): Long

    @Update
    suspend fun updatePassword(password: PasswordEntry)

    @Delete
    suspend fun deletePassword(password: PasswordEntry)

    /**
     * Persist the result of a HIBP breach check for a single entry.
     * Only touches the two pwned columns — leaves everything else untouched.
     */
    @Query("UPDATE passwords SET isPwned = :isPwned, pwnedCount = :pwnedCount WHERE id = :id")
    suspend fun updatePwnedStatus(id: Long, isPwned: Boolean, pwnedCount: Int)

    /**
     * One-shot suspend query (no Flow) used by the breach-check loop so we
     * don't need to cancel a Flow mid-collection.
     */
    @Query("SELECT * FROM passwords ORDER BY updatedAt DESC")
    suspend fun getAllPasswordsSnapshot(): List<PasswordEntry>

    /** Returns all entries that are currently flagged as pwned. */
    @Query("SELECT * FROM passwords WHERE isPwned = 1 ORDER BY title ASC")
    fun getPwnedPasswords(): Flow<List<PasswordEntry>>
}