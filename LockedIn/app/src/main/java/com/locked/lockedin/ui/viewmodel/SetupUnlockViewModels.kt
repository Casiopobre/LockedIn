package com.locked.lockedin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locked.lockedin.repository.VaultRepository
import com.locked.lockedin.security.BiometricKeyManager
import com.locked.lockedin.security.VaultKeyHolder
import com.locked.lockedin.security.MasterKeyManager
import com.locked.lockedin.ui.screen.PasswordStrength
import com.locked.lockedin.ui.viewmodel.HARDCODED_USER_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.crypto.Cipher

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
    private val vaultRepository: VaultRepository,
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
                // 1. Create the local vault
                withContext(Dispatchers.Default) {
                    masterKeyManager.setupMasterKey(state.masterKey)
                }

                // 2. Cache credentials for deferred auth (register/login on Groups entry)
                vaultRepository.cacheCredentials(HARDCODED_USER_ID, state.masterKey)

                // 3. Derive and cache the encryption key in memory
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

    fun enableBiometricAfterSetup(biometricKeyManager: BiometricKeyManager, cipher: Cipher) {
        val vaultKey = VaultKeyHolder.requireKey()
        biometricKeyManager.encryptAndSaveKey(cipher, vaultKey)  // use authenticated cipher
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
    private val vaultRepository: VaultRepository,
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
                masterKeyManager.verifyMasterKey(key)
            }
            if (isValid) {
                // Derive and cache the encryption key in memory
                onKeyDerived(key)

                // Cache credentials for deferred auth (register/login on Groups entry)
                vaultRepository.cacheCredentials(HARDCODED_USER_ID, key)

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

    /**
     * Call this AFTER unlock() completes (i.e. after isUnlocked == true)
     * when the user wants to enable biometric unlock for the first time
     * from a settings screen or similar.
     *
     * At this point onKeyDerived() has already run, so VaultKeyHolder holds the key.
     */
    fun enableBiometric(biometricKeyManager: BiometricKeyManager, cipher: Cipher) {
        val vaultKey = VaultKeyHolder.requireKey()
        biometricKeyManager.encryptAndSaveKey(cipher, vaultKey)
    }
}
