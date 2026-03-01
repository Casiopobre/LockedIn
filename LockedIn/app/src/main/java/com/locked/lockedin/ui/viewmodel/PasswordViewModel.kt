package com.locked.lockedin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locked.lockedin.data.model.PasswordEntry
import com.locked.lockedin.repository.PasswordRepository
import com.locked.lockedin.security.PwnedCheckManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PasswordViewModel(
    private val repository: PasswordRepository,
    private val pwnedCheckManager: PwnedCheckManager
) : ViewModel() {

    private val _searchQuery      = MutableStateFlow("")
    val searchQuery                = _searchQuery.asStateFlow()

    private val _selectedPassword = MutableStateFlow<PasswordEntry?>(null)
    val selectedPassword           = _selectedPassword.asStateFlow()

    private val _uiState = MutableStateFlow(PasswordUiState())
    val uiState           = _uiState.asStateFlow()

    val passwords = searchQuery
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) repository.getAllPasswords()
            else repository.searchPasswords(query)
        }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(isEncryptionAvailable = repository.isEncryptionAvailable()) }
        }
    }

    // ── Breach check ─────────────────────────────────────────────────────────

    /** Called on MainScreen entry. Skips if < 24 h since last check. */
    fun runBreachCheckIfDue() {
        if (!pwnedCheckManager.shouldRunCheck()) return
        launchBreachCheck()
    }

    /** DEBUG: runs the check unconditionally, ignoring the 24 h throttle. */
    fun forceBreachCheck() = launchBreachCheck()

    /** DEBUG: clears the stored timestamp so the next [runBreachCheckIfDue] will fire. */
    fun resetBreachTimer() = pwnedCheckManager.resetTimer()

    private fun launchBreachCheck() {
        if (_uiState.value.isBreachCheckRunning) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBreachCheckRunning = true) }
            try {
                val pwnedCount = repository.runBreachCheck { checked, total ->
                    _uiState.update { it.copy(breachCheckProgress = checked to total) }
                }
                pwnedCheckManager.recordCheckCompleted()
                _uiState.update {
                    it.copy(
                        isBreachCheckRunning      = false,
                        breachCheckProgress       = null,
                        lastBreachCheckPwnedCount = pwnedCount
                    )
                }
                if (pwnedCount > 0) setError("⚠️ $pwnedCount password(s) found in data breaches!")
            } catch (e: Exception) {
                _uiState.update { it.copy(isBreachCheckRunning = false, breachCheckProgress = null) }
                setError("Breach check failed: ${e.message}")
            }
        }
    }

    fun dismissBreachBanner() {
        _uiState.update { it.copy(lastBreachCheckPwnedCount = 0) }
        clearMessages()
    }

    // ── Search ────────────────────────────────────────────────────────────────

    fun updateSearchQuery(query: String) { _searchQuery.value = query }
    fun clearSearch()                    { _searchQuery.value = "" }

    // ── Selection ─────────────────────────────────────────────────────────────

    fun selectPassword(password: PasswordEntry) { _selectedPassword.value = password }
    fun clearSelectedPassword()                 { _selectedPassword.value = null }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    fun addPassword(
        title: String,
        username: String,
        password: String,
        website: String = "",
        notes: String   = "",
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
            } finally { setLoading(false) }
        }
    }

    fun updatePassword(
        passwordEntry: PasswordEntry,
        title: String,
        username: String,
        password: String,
        website: String = "",
        notes: String   = "",
        onResult: (Result<Unit>) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                setLoading(true)
                repository.updatePasswordEntry(passwordEntry.id, title, username, password, website, notes)
                onResult(Result.success(Unit))
                setMessage("Password updated successfully")
            } catch (e: Exception) {
                onResult(Result.failure(e))
                setError("Failed to update password: ${e.message}")
            } finally { setLoading(false) }
        }
    }

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
            } finally { setLoading(false) }
        }
    }

    fun decryptPassword(encryptedPassword: String): String? = try {
        repository.decryptPassword(encryptedPassword)
    } catch (e: Exception) {
        setError("Failed to decrypt password"); null
    }

    fun generatePassword(
        length: Int               = 16,
        includeUppercase: Boolean = true,
        includeLowercase: Boolean = true,
        includeNumbers: Boolean   = true,
        includeSymbols: Boolean   = true
    ): String = try {
        repository.generatePassword(length, includeUppercase, includeLowercase, includeNumbers, includeSymbols)
    } catch (e: Exception) {
        setError("Failed to generate password"); "DefaultPassword123!"
    }

    fun getPasswordById(id: Long, onResult: (PasswordEntry?) -> Unit) {
        viewModelScope.launch {
            try { onResult(repository.getPasswordById(id)) }
            catch (e: Exception) { setError("Failed to load password"); onResult(null) }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun setLoading(v: Boolean)  { _uiState.update { it.copy(isLoading = v) } }
    private fun setError(msg: String)   { _uiState.update { it.copy(errorMessage = msg,   successMessage = null) } }
    private fun setMessage(msg: String) { _uiState.update { it.copy(successMessage = msg, errorMessage   = null) } }
    fun clearMessages()                 { _uiState.update { it.copy(errorMessage = null,  successMessage = null) } }
}

data class PasswordUiState(
    val isLoading: Boolean                   = false,
    val errorMessage: String?                = null,
    val successMessage: String?              = null,
    val isEncryptionAvailable: Boolean       = true,
    val isBreachCheckRunning: Boolean        = false,
    val breachCheckProgress: Pair<Int, Int>? = null,
    val lastBreachCheckPwnedCount: Int       = 0
)