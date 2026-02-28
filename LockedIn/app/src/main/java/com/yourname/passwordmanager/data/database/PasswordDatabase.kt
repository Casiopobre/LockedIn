package com.yourname.passwordmanager.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.yourname.passwordmanager.data.dao.PasswordDao
import com.yourname.passwordmanager.data.model.PasswordEntry

/**
 * Room database for password storage
 */
@Database(
    entities = [PasswordEntry::class],
    version = 1,
    exportSchema = false
)
abstract class PasswordDatabase : RoomDatabase() {
    
    abstract fun passwordDao(): PasswordDao
    
    companion object {
        @Volatile
        private var INSTANCE: PasswordDatabase? = null
        
        /**
         * Get database instance (Singleton pattern)
         */
        fun getDatabase(context: Context): PasswordDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PasswordDatabase::class.java,
                    "password_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * Clear database instance (for testing)
         */
        fun clearInstance() {
            INSTANCE = null
        }
    }
}