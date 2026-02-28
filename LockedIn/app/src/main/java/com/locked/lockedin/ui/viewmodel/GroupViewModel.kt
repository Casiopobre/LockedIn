package com.locked.lockedin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.locked.lockedin.network.model.GroupListItem
import com.locked.lockedin.network.model.PasswordResponse
import com.locked.lockedin.repository.ApiException
import com.locked.lockedin.repository.VaultRepository
import com.locked.lockedin.security.SgkManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── UI state data classes ───────────────────────────────────────────────────

data class GroupUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val connectionError: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

data class GroupPasswordItem(
    val id: String,
    val groupId: String,
    val createdBy: String,
    val label: String,
    val decryptedData: String?,
    val createdAt: String,
    val updatedAt: String
)

// ── ViewModel ───────────────────────────────────────────────────────────────

class GroupViewModel(
    private val vaultRepository: VaultRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupUiState())
    val uiState = _uiState.asStateFlow()

    private val _groups = MutableStateFlow<List<GroupListItem>>(emptyList())
    val groups = _groups.asStateFlow()

    /** Passwords for the currently selected group (already decrypted). */
    private val _groupPasswords = MutableStateFlow<List<GroupPasswordItem>>(emptyList())
    val groupPasswords = _groupPasswords.asStateFlow()

    /** Members of the currently selected group (phone numbers). */
    private val _groupMembers = MutableStateFlow<List<String>>(emptyList())
    val groupMembers = _groupMembers.asStateFlow()

    // ── Auth + Groups entry point ─────────────────────────────────────────

    /**
     * Called when the user navigates to the Groups screen.
     * Transparently registers or logs in, then loads groups.
     *
     * - Not registered → generates RSA key pair, registers via API, logs in.
     * - Already registered → logs in.
     * - Connection failure → shows connection error.
     * - Stale JWT → clears session and retries once.
     */
    fun ensureAuthenticatedAndLoadGroups() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, connectionError = null, errorMessage = null)
            }
            try {
                // Authenticate (login → register if needed → login)
                vaultRepository.ensureAuthenticated()
                _uiState.update { it.copy(isAuthenticated = true) }

                // Load groups — with one 401-retry (handles expired JWT)
                val result = try {
                    vaultRepository.listMyGroups()
                } catch (e: ApiException) {
                    if (e.httpCode == 401) {
                        // JWT was stale; force fresh auth and retry
                        vaultRepository.clearSession()
                        vaultRepository.ensureAuthenticated()
                        vaultRepository.listMyGroups()
                    } else {
                        throw e
                    }
                }
                _groups.value = result
            } catch (e: ApiException) {
                // HTTP-level error from the API (not a network issue)
                _uiState.update {
                    it.copy(
                        isAuthenticated = false,
                        connectionError = when (e.httpCode) {
                            409 -> "Error de autenticación. El usuario existe en el servidor pero las credenciales no coinciden."
                            401 -> "Error de autenticación. No se pudo iniciar sesión."
                            else -> "Error del servidor (${e.httpCode}). Inténtalo de nuevo más tarde."
                        }
                    )
                }
            } catch (_: java.net.UnknownHostException) {
                _uiState.update {
                    it.copy(
                        isAuthenticated = false,
                        connectionError = "Error de conexión. Debes estar conectado a internet para usar esta función."
                    )
                }
            } catch (_: java.net.ConnectException) {
                _uiState.update {
                    it.copy(
                        isAuthenticated = false,
                        connectionError = "Error de conexión. Debes estar conectado a internet para usar esta función."
                    )
                }
            } catch (_: java.io.IOException) {
                _uiState.update {
                    it.copy(
                        isAuthenticated = false,
                        connectionError = "Error de conexión. Debes estar conectado a internet para usar esta función."
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAuthenticated = false,
                        connectionError = "Error inesperado: ${e.message}"
                    )
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // ── Groups CRUD ─────────────────────────────────────────────────────────

    fun loadGroups() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val result = vaultRepository.listMyGroups()
                _groups.value = result
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to load groups: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun createGroup(name: String, onResult: (Result<String>) -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val response = vaultRepository.createGroup(name)
                _uiState.update { it.copy(successMessage = "Group created successfully") }
                loadGroups() // refresh
                onResult(Result.success(response.id))
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to create group: ${e.message}") }
                onResult(Result.failure(e))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // ── Members ─────────────────────────────────────────────────────────────

    fun addMember(groupId: String, targetUserId: String, onResult: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                vaultRepository.addMember(groupId, targetUserId)
                _uiState.update { it.copy(successMessage = "Member added successfully") }
                onResult(Result.success(Unit))
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to add member: ${e.message}") }
                onResult(Result.failure(e))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // ── Group Passwords ─────────────────────────────────────────────────────

    fun loadGroupPasswords(groupId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val sgkBytes = vaultRepository.fetchAndDecryptSgk(groupId)
                val encrypted = vaultRepository.listGroupPasswords(groupId)
                _groupPasswords.value = encrypted.map { pw ->
                    val decrypted = try {
                        SgkManager.decryptWithSgk(pw.encryptedData, sgkBytes)
                    } catch (_: Exception) {
                        null
                    }
                    GroupPasswordItem(
                        id = pw.id,
                        groupId = pw.groupId,
                        createdBy = pw.createdBy,
                        label = pw.label,
                        decryptedData = decrypted,
                        createdAt = pw.createdAt,
                        updatedAt = pw.updatedAt
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to load passwords: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun sharePassword(
        groupId: String,
        label: String,
        plainData: String,
        onResult: (Result<Unit>) -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                vaultRepository.sharePassword(groupId, label, plainData)
                _uiState.update { it.copy(successMessage = "Password shared successfully") }
                loadGroupPasswords(groupId) // refresh
                onResult(Result.success(Unit))
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to share password: ${e.message}") }
                onResult(Result.failure(e))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun updateGroupPassword(
        groupId: String,
        passwordId: String,
        newLabel: String? = null,
        newPlainData: String? = null,
        onResult: (Result<Unit>) -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                vaultRepository.updateGroupPassword(groupId, passwordId, newLabel, newPlainData)
                _uiState.update { it.copy(successMessage = "Password updated") }
                loadGroupPasswords(groupId)
                onResult(Result.success(Unit))
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to update password: ${e.message}") }
                onResult(Result.failure(e))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun deleteGroupPassword(
        groupId: String,
        passwordId: String,
        onResult: (Result<Unit>) -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                vaultRepository.deleteGroupPassword(groupId, passwordId)
                _uiState.update { it.copy(successMessage = "Password deleted") }
                loadGroupPasswords(groupId)
                onResult(Result.success(Unit))
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to delete password: ${e.message}") }
                onResult(Result.failure(e))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null, connectionError = null) }
    }
}

// ── Factory ─────────────────────────────────────────────────────────────────

class GroupViewModelFactory(
    private val vaultRepository: VaultRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroupViewModel::class.java)) {
            return GroupViewModel(vaultRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
