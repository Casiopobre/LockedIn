package com.locked.lockedin.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a single password vault entry.
 *
 * [encryptedPassword] is stored as AES-256-GCM ciphertext (Base64).
 * [isPwned] and [pwnedCount] are updated by the HIBP breach check and
 * are used purely for UI display — they don't affect encryption.
 */
@Entity(tableName = "passwords")
data class PasswordEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,
    val username: String,
    val encryptedPassword: String,
    val website: String = "",
    val notes: String = "",

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    val isPwned: Boolean = false,

    /**
     * Number of times the password appeared in breach databases.
     * 0  = clean, -1 = last check errored, >0 = breach count.
     */
    val pwnedCount: Int = 0
)