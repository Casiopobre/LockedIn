package com.yourname.passwordmanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.passwordmanager.data.model.PasswordEntry
import com.yourname.passwordmanager.repository.PasswordRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for managing password-related UI state and operations
 */
class PasswordViewModel(
    private val repository: PasswordRepository
) : ViewModel() {
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    
    private val _selectedPassword = MutableStateFlow<PasswordEntry?>(null)
    val selectedPassword = _selectedPassword.asStateFlow()
    
    private val _uiState = MutableStateFlow(PasswordUiState())
    val uiState = _uiState.asStateFlow()
    
    /**
     * Passwords flow that updates based on search query
     */
    val passwords = searchQuery
        .debounce(300) // Debounce search input
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.getAllPasswords()
            } else {
                repository.searchPasswords(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    init {
        // Check if encryption is available on startup
        viewModelScope.launch {
            val isEncryptionWorking = repository.isEncryptionAvailable()
            _uiState.value = _uiState.value.copy(
                isEncryptionAvailable = isEncryptionWorking
            )
        }
    }
    
    /**
     * Update search query
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    /**
     * Clear search query
     */
    fun clearSearch() {
        _searchQuery.value = ""
    }
    
    /**
     * Select a password for editing
     */
    fun selectPassword(password: PasswordEntry) {
        _selectedPassword.value = password
    }
    
    /**
     * Clear selected password
     */
    fun clearSelectedPassword() {
        _selectedPassword.value = null
    }
    
    /**
     * Add a new password
     */
    fun addPassword(
        title: String,
        username: String,
        password: String,
        website: String = "",
        notes: String = "",
        onResult: (Result<Long>) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                setLoading(true)
                val id = repository.insertPassword(title, username, password, website, notes)
                onResult(Result.success(id))
                setMessage("Password added successfully")
            } catch (e: Exception) {
                onResult(Result.failure(e))
                setError("Failed to add password: ${e.message}")
            } finally {
                setLoading(false)
            }
        }
    }
    
    /**
     * Update an existing password
     */
    fun updatePassword(
        passwordEntry: PasswordEntry,
        title: String,
        username: String,
        password: String,
        website: String = "",
        notes: String = "",
        onResult: (Result<Unit>) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                setLoading(true)
                repository.updatePasswordEntry(
                    passwordEntry.id, title, username, password, website, notes
                )
                onResult(Result.success(Unit))
                setMessage("Password updated successfully")
            } catch (e: Exception) {
                onResult(Result.failure(e))
                setError("Failed to update password: ${e.message}")
            } finally {
                setLoading(false)
            }
        }
    }
    
    /**
     * Delete a password
     */
    fun deletePassword(
        passwordEntry: PasswordEntry,
        onResult: (Result<Unit>) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                setLoading(true)
                repository.deletePassword(passwordEntry)
                onResult(Result.success(Unit))
                setMessage("Password deleted successfully")
            } catch (e: Exception) {
                onResult(Result.failure(e))
                setError("Failed to delete password: ${e.message}")
            } finally {
                setLoading(false)
            }
        }
    }
    
    /**
     * Decrypt a password safely
     */
    fun decryptPassword(encryptedPassword: String): String? {
        return try {
            repository.decryptPassword(encryptedPassword)
        } catch (e: Exception) {
            setError("Failed to decrypt password")
            null
        }
    }
    
    /**
     * Generate a secure password
     */
    fun generatePassword(
        length: Int = 16,
        includeUppercase: Boolean = true,
        includeLowercase: Boolean = true,
        includeNumbers: Boolean = true,
        includeSymbols: Boolean = true
    ): String {
        return try {
            repository.generatePassword(length, includeUppercase, includeLowercase, includeNumbers, includeSymbols)
        } catch (e: Exception) {
            setError("Failed to generate password")
            "DefaultPassword123!" // Fallback password
        }
    }
    
    /**
     * Get password by ID
     */
    fun getPasswordById(id: Long, onResult: (PasswordEntry?) -> Unit) {
        viewModelScope.launch {
            try {
                val password = repository.getPasswordById(id)
                onResult(password)
            } catch (e: Exception) {
                setError("Failed to load password")
                onResult(null)
            }
        }
    }
    
    /**
     * Set loading state
     */
    private fun setLoading(isLoading: Boolean) {
        _uiState.value = _uiState.value.copy(isLoading = isLoading)
    }
    
    /**
     * Set error message
     */
    private fun setError(message: String) {
        _uiState.value = _uiState.value.copy(
            errorMessage = message,
            successMessage = null
        )
    }
    
    /**
     * Set success message
     */
    private fun setMessage(message: String) {
        _uiState.value = _uiState.value.copy(
            successMessage = message,
            errorMessage = null
        )
    }
    
    /**
     * Clear messages
     */
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}

/**
 * UI state data class
 */
data class PasswordUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isEncryptionAvailable: Boolean = true
)