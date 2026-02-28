package com.yourname.passwordmanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourname.passwordmanager.security.MasterKeyManager
import com.yourname.passwordmanager.ui.screen.PasswordStrength
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ============================================================
// SetupViewModel — first-time master key creation
// ============================================================

data class SetupUiState(
    val masterKey: String = "",
    val confirmKey: String = "",
    val isMasterKeyVisible: Boolean = false,
    val isConfirmKeyVisible: Boolean = false,
    val masterKeyError: String? = null,
    val confirmKeyError: String? = null,
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
    val isSetupComplete: Boolean = false,
    val passwordStrength: PasswordStrength = PasswordStrength.WEAK
)

class SetupViewModel(
    private val masterKeyManager: MasterKeyManager,
    private val onKeyDerived: (masterKey: String) -> Unit
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState = _uiState.asStateFlow()

    fun onMasterKeyChange(value: String) {
        _uiState.value = _uiState.value.copy(
            masterKey = value,
            masterKeyError = null,
            errorMessage = null,
            passwordStrength = evaluateStrength(value)
        )
    }

    fun onConfirmKeyChange(value: String) {
        _uiState.value = _uiState.value.copy(
            confirmKey = value,
            confirmKeyError = null
        )
    }

    fun toggleMasterKeyVisibility() {
        _uiState.value = _uiState.value.copy(
            isMasterKeyVisible = !_uiState.value.isMasterKeyVisible
        )
    }

    fun toggleConfirmKeyVisibility() {
        _uiState.value = _uiState.value.copy(
            isConfirmKeyVisible = !_uiState.value.isConfirmKeyVisible
        )
    }

    fun setupMasterKey() {
        val state = _uiState.value
        if (!validate(state)) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                withContext(Dispatchers.Default) {
                    // PBKDF2 is intentionally slow — run off the main thread
                    masterKeyManager.setupMasterKey(state.masterKey)
                }
                // Derive and cache the encryption key in memory
                onKeyDerived(state.masterKey)
                _uiState.value = _uiState.value.copy(isSetupComplete = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Setup failed: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    private fun validate(state: SetupUiState): Boolean {
        var valid = true

        val keyError = when {
            state.masterKey.isBlank() -> "Master key cannot be empty."
            state.masterKey.length < 8 -> "Master key must be at least 8 characters."
            else -> null
        }
        val confirmError = when {
            state.confirmKey.isBlank() -> "Please confirm your master key."
            state.masterKey != state.confirmKey -> "Keys do not match."
            else -> null
        }

        if (keyError != null || confirmError != null) {
            _uiState.value = _uiState.value.copy(
                masterKeyError = keyError,
                confirmKeyError = confirmError
            )
            valid = false
        }
        return valid
    }

    private fun evaluateStrength(password: String): PasswordStrength {
        if (password.length < 8) return PasswordStrength.WEAK
        var score = 0
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isLowerCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++
        if (password.length >= 16) score++

        return when {
            score >= 5 -> PasswordStrength.STRONG
            score >= 4 -> PasswordStrength.GOOD
            score >= 3 -> PasswordStrength.FAIR
            else -> PasswordStrength.WEAK
        }
    }
}

// ============================================================
// UnlockViewModel — unlock vault on subsequent launches
// ============================================================

data class UnlockUiState(
    val masterKey: String = "",
    val isKeyVisible: Boolean = false,
    val isError: Boolean = false,
    val isLoading: Boolean = false,
    val isUnlocked: Boolean = false,
    val failedAttempts: Int = 0
)

class UnlockViewModel(
    private val masterKeyManager: MasterKeyManager,
    private val onKeyDerived: (masterKey: String) -> Unit
) : ViewModel() {

    private val _uiState = MutableStateFlow(UnlockUiState())
    val uiState = _uiState.asStateFlow()

    fun onMasterKeyChange(value: String) {
        _uiState.value = _uiState.value.copy(masterKey = value, isError = false)
    }

    fun toggleKeyVisibility() {
        _uiState.value = _uiState.value.copy(isKeyVisible = !_uiState.value.isKeyVisible)
    }

    fun unlock() {
        val key = _uiState.value.masterKey
        if (key.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val isValid = withContext(Dispatchers.Default) {
                // PBKDF2 verification is intentionally slow — off main thread
                masterKeyManager.verifyMasterKey(key)
            }
            if (isValid) {
                // Derive and cache the encryption key in memory
                onKeyDerived(key)
                _uiState.value = _uiState.value.copy(isUnlocked = true)
            } else {
                _uiState.value = _uiState.value.copy(
                    isError = true,
                    isLoading = false,
                    masterKey = "",
                    failedAttempts = _uiState.value.failedAttempts + 1
                )
            }
        }
    }
}