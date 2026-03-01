package com.locked.lockedin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.locked.lockedin.repository.ApiException
import com.locked.lockedin.repository.VaultRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── Constants ───────────────────────────────────────────────────────────────

/**
 * Hardcoded user ID (phone number).
 * TODO: replace with dynamic input from the user later.
 */
const val HARDCODED_USER_ID = "666666666"

// ── UI State ────────────────────────────────────────────────────────────────

data class AuthUiState(
    val masterPassword: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

// ── ViewModel ───────────────────────────────────────────────────────────────

class AuthViewModel(
    private val vaultRepository: VaultRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    /** The phone number used for auth. Hardcoded for now. */
    val userId: String = HARDCODED_USER_ID

    // ── Field handlers ──────────────────────────────────────────────────────

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(masterPassword = value, errorMessage = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update { it.copy(confirmPassword = value, errorMessage = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun toggleConfirmVisibility() {
        _uiState.update { it.copy(isConfirmVisible = !it.isConfirmVisible) }
    }

    fun clearState() {
        _uiState.value = AuthUiState()
    }

    // ── Register ────────────────────────────────────────────────────────────

    /**
     * Register a new user against the backend.
     *
     * Flow:
     * 1. Validates passwords match.
     * 2. `VaultRepository.register()` generates an RSA key pair (if needed),
     *    SHA-256-hashes the password, and POSTs to `/auth/register`.
     * 3. On success, automatically logs in.
     */
    fun register() {
        val state = _uiState.value

        // Validate
        if (state.masterPassword.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Password cannot be empty.") }
            return
        }
        if (state.masterPassword.length < 8) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 8 characters.") }
            return
        }
        if (state.masterPassword != state.confirmPassword) {
            _uiState.update { it.copy(errorMessage = "Passwords do not match.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // Register (generates RSA keys + SHA-256 hash internally)
                vaultRepository.register(userId, state.masterPassword)

                // Auto-login after successful registration
                vaultRepository.login(userId, state.masterPassword)

                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } catch (e: ApiException) {
                val msg = when (e.httpCode) {
                    409 -> "User already exists. Try logging in instead."
                    422 -> "Invalid data. Please check your input."
                    else -> "Registration failed: ${e.message}"
                }
                _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Network error: ${e.message}")
                }
            }
        }
    }

    // ── Login ───────────────────────────────────────────────────────────────

    /**
     * Log in against the backend.
     *
     * Flow:
     * 1. `VaultRepository.login()` SHA-256-hashes the password and POSTs
     *    to `/auth/login`.
     * 2. The JWT is stored in [SessionManager] automatically.
     */
    fun login() {
        val state = _uiState.value

        if (state.masterPassword.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Password cannot be empty.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                vaultRepository.login(userId, state.masterPassword)
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } catch (e: ApiException) {
                val msg = when (e.httpCode) {
                    401 -> "Invalid credentials. Please try again."
                    else -> "Login failed: ${e.message}"
                }
                _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Network error: ${e.message}")
                }
            }
        }
    }

    /** Whether the user already has a valid session. */
    val isLoggedIn: Boolean get() = vaultRepository.isLoggedIn
}

// ── Factory ─────────────────────────────────────────────────────────────────

class AuthViewModelFactory(
    private val vaultRepository: VaultRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(vaultRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
