package com.locked.lockedin.repository

import com.locked.lockedin.network.ApiClient
import com.locked.lockedin.network.SessionManager
import com.locked.lockedin.network.VaultApiService
import com.locked.lockedin.network.model.*
import com.locked.lockedin.security.PasswordHasher
import com.locked.lockedin.security.RsaKeyManager
import com.locked.lockedin.security.SgkManager

/**
 * High-level repository that wraps the remote Vault API and the local
 * crypto primitives into simple, use-case-oriented methods.
 *
 * All public methods are `suspend` and safe to call from a coroutine scope.
 *
 * Typical flows:
 *
 * **Registration**
 * ```
 * register(userId, masterPassword)
 * ```
 * → hashes the password with SHA-256, generates an RSA key pair if needed,
 *   and POSTs to `/auth/register`.
 *
 * **Login**
 * ```
 * login(userId, masterPassword)
 * ```
 * → hashes, POSTs to `/auth/login`, stores the JWT.
 *
 * **Create a group & share passwords**
 * ```
 * createGroup("My Team")           // generates SGK, encrypts it with our RSA pubkey
 * addMember(groupId, "bob")        // fetches bob's pubkey, encrypts SGK for him
 * sharePassword(groupId, "GitHub", "user:pass:otp")
 * ```
 *
 * **Fetch & decrypt shared passwords**
 * ```
 * val sgkBytes = fetchAndDecryptSgk(groupId)
 * val passwords = listGroupPasswords(groupId)
 * passwords.forEach {
 *     val clearText = SgkManager.decryptWithSgk(it.encryptedData, sgkBytes)
 * }
 * ```
 */
class VaultRepository(
    private val sessionManager: SessionManager,
    private val rsaKeyManager: RsaKeyManager,
    private val api: VaultApiService = ApiClient.service
) {

    // ── Auth ────────────────────────────────────────────────────────────────

    /**
     * Register a new user.
     *
     * 1. Generates an RSA key pair (if not already done).
     * 2. Hashes [masterPassword] with SHA-256.
     * 3. POSTs to `/auth/register`.
     *
     * @return [RegisterResponse] on success.
     * @throws ApiException on failure.
     */
    suspend fun register(userId: String, masterPassword: String): RegisterResponse {
        // Ensure an RSA key pair exists
        rsaKeyManager.generateKeyPair()

        val passwordHash = PasswordHasher.sha256(masterPassword)
        val publicKeyPem = rsaKeyManager.getPublicKeyPem()

        val response = api.register(
            RegisterRequest(
                userId = userId,
                passwordHash = passwordHash,
                publicKey = publicKeyPem
            )
        )

        if (!response.isSuccessful) {
            throw ApiException(response.code(), response.errorBody()?.string())
        }

        return response.body()!!
    }

    /**
     * Log in and store the JWT.
     *
     * @return [LoginResponse] containing the access token.
     * @throws ApiException on failure.
     */
    suspend fun login(userId: String, masterPassword: String): LoginResponse {
        val passwordHash = PasswordHasher.sha256(masterPassword)

        val response = api.login(
            LoginRequest(userId = userId, passwordHash = passwordHash)
        )

        if (!response.isSuccessful) {
            throw ApiException(response.code(), response.errorBody()?.string())
        }

        val body = response.body()!!
        sessionManager.saveToken(body.accessToken)
        sessionManager.saveUserId(userId)
        return body
    }

    /**
     * Fetch another user's public key (requires authentication).
     */
    suspend fun getPublicKey(userId: String): String {
        val token = requireToken()
        val response = api.getPublicKey(userId, token)

        if (!response.isSuccessful) {
            throw ApiException(response.code(), response.errorBody()?.string())
        }

        return response.body()!!.publicKey
    }

    // ── Groups ──────────────────────────────────────────────────────────────

    /**
     * Create a new sharing group.
     *
     * 1. Generates a fresh AES-256 SGK.
     * 2. Encrypts it with our own RSA public key.
     * 3. POSTs to `/groups/`.
     *
     * @return [GroupResponse] (includes the new group ID).
     */
    suspend fun createGroup(name: String): GroupResponse {
        val token = requireToken()

        // Generate a fresh SGK
        val sgkBytes = SgkManager.generateSgk()

        // Encrypt it with our own public key so we can retrieve it later
        val encryptedSgk = rsaKeyManager.encryptWithPublicKey(
            sgkBytes,
            rsaKeyManager.getPublicKeyPem()
        )

        val response = api.createGroup(
            GroupCreateRequest(name = name, encryptedSgk = encryptedSgk),
            token
        )

        if (!response.isSuccessful) {
            throw ApiException(response.code(), response.errorBody()?.string())
        }

        return response.body()!!
    }

    /**
     * List all groups the current user belongs to.
     */
    suspend fun listMyGroups(): List<GroupListItem> {
        val token = requireToken()
        val response = api.listMyGroups(token)

        if (!response.isSuccessful) {
            throw ApiException(response.code(), response.errorBody()?.string())
        }

        return response.body()!!
    }

    suspend fun deleteGroup(groupId: String) {
        val token = sessionManager.bearerToken()
            ?: throw IllegalStateException("Not authenticated")
        val response = api.deleteGroup(groupId, token)
        if (!response.isSuccessful) {
            throw Exception("Failed to delete group: ${response.code()} ${response.message()}")
        }
    }

    /**
     * Add a member to a group.
     *
     * 1. Fetches the target user's public key.
     * 2. Decrypts our own SGK copy.
     * 3. Re-encrypts the SGK with the target user's public key.
     * 4. POSTs to `/groups/{id}/members`.
     */
    suspend fun addMember(groupId: String, targetUserId: String): MemberResponse {
        val token = requireToken()

        // 1. Get target user's public key
        val targetPubKeyPem = getPublicKey(targetUserId)

        // 2. Get our encrypted SGK and decrypt it
        val sgkBytes = fetchAndDecryptSgk(groupId)

        // 3. Re-encrypt with target user's public key
        val encryptedSgkForTarget = rsaKeyManager.encryptWithPublicKey(sgkBytes, targetPubKeyPem)

        val response = api.addMember(
            groupId,
            AddMemberRequest(userId = targetUserId, encryptedSgk = encryptedSgkForTarget),
            token
        )

        if (!response.isSuccessful) {
            throw ApiException(response.code(), response.errorBody()?.string())
        }

        return response.body()!!
    }

    /**
     * Retrieve the SGK for a group and decrypt it with our RSA private key.
     *
     * @return The raw SGK bytes (32 bytes, AES-256 key).
     */
    suspend fun fetchAndDecryptSgk(groupId: String): ByteArray {
        val token = requireToken()
        val response = api.getMySgk(groupId, token)

        if (!response.isSuccessful) {
            throw ApiException(response.code(), response.errorBody()?.string())
        }

        val encryptedSgk = response.body()!!.encryptedSgk
        return rsaKeyManager.decryptWithPrivateKey(encryptedSgk)
    }

    // ── Group passwords ─────────────────────────────────────────────────────

    /**
     * Share (create) a password in a group.
     *
     * Encrypts [plainData] with the group's SGK before uploading.
     *
     * @param groupId   The group UUID.
     * @param label     A human-readable label (e.g. "GitHub - team account").
     * @param plainData The password / secret to share, in cleartext.
     * @return [PasswordResponse] with the newly created entry.
     */
    suspend fun sharePassword(
        groupId: String,
        label: String,
        plainData: String
    ): PasswordResponse {
        val token = requireToken()
        val sgkBytes = fetchAndDecryptSgk(groupId)
        val encryptedData = SgkManager.encryptWithSgk(plainData, sgkBytes)

        val response = api.createPassword(
            groupId,
            PasswordCreateRequest(label = label, encryptedData = encryptedData),
            token
        )

        if (!response.isSuccessful) {
            throw ApiException(response.code(), response.errorBody()?.string())
        }

        return response.body()!!
    }

    /**
     * List all passwords in a group (still encrypted with the SGK).
     *
     * Decrypt each entry's `encryptedData` using [SgkManager.decryptWithSgk]
     * with the SGK obtained from [fetchAndDecryptSgk].
     */
    suspend fun listGroupPasswords(groupId: String): List<PasswordResponse> {
        val token = requireToken()
        val response = api.listPasswords(groupId, token)

        if (!response.isSuccessful) {
            throw ApiException(response.code(), response.errorBody()?.string())
        }

        return response.body()!!
    }

    /**
     * Update a password entry.
     *
     * If [newPlainData] is provided, it will be re-encrypted with the SGK.
     */
    suspend fun updateGroupPassword(
        groupId: String,
        passwordId: String,
        newLabel: String? = null,
        newPlainData: String? = null
    ): PasswordResponse {
        val token = requireToken()

        val encryptedData = if (newPlainData != null) {
            val sgkBytes = fetchAndDecryptSgk(groupId)
            SgkManager.encryptWithSgk(newPlainData, sgkBytes)
        } else null

        val response = api.updatePassword(
            groupId,
            passwordId,
            PasswordUpdateRequest(label = newLabel, encryptedData = encryptedData),
            token
        )

        if (!response.isSuccessful) {
            throw ApiException(response.code(), response.errorBody()?.string())
        }

        return response.body()!!
    }

    /**
     * Delete a password entry from a group.
     */
    suspend fun deleteGroupPassword(groupId: String, passwordId: String) {
        val token = requireToken()
        val response = api.deletePassword(groupId, passwordId, token)

        if (!response.isSuccessful) {
            throw ApiException(response.code(), response.errorBody()?.string())
        }
    }

    // ── Session helpers ─────────────────────────────────────────────────────

    /** Convenience: true if a JWT is stored. */
    val isLoggedIn: Boolean get() = sessionManager.isLoggedIn

    /** Convenience: stored user ID. */
    val currentUserId: String? get() = sessionManager.getUserId()

    /** Log out: clear the JWT and user data. */
    fun logout() = sessionManager.clear()

    // ── Internal helpers ────────────────────────────────────────────────────

    private fun requireToken(): String =
        sessionManager.bearerToken()
            ?: throw IllegalStateException("Not logged in — call login() first.")
}

// ── Exception ───────────────────────────────────────────────────────────────

/**
 * Represents an HTTP-level error from the Vault API.
 */
class ApiException(
    val httpCode: Int,
    val errorBody: String?
) : Exception("API error $httpCode: ${errorBody ?: "no body"}")
