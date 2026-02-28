package com.yourname.passwordmanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Data class representing a password entry in the database
 */
@Entity(tableName = "password_entries")
data class PasswordEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val username: String,
    val encryptedPassword: String,
    val website: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)