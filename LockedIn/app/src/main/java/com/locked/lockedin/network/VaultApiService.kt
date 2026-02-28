package com.locked.lockedin.network

import com.locked.lockedin.network.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit service interface for the Vault Backend API.
 *
 * Base URL should be set in [ApiClient].
 */
interface VaultApiService {

    // ── Health ──────────────────────────────────────────────────────────────

    @GET("health")
    suspend fun healthCheck(): Response<Map<String, String>>

    // ── Auth ────────────────────────────────────────────────────────────────

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<RegisterResponse>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @GET("auth/public-key/{user_id}")
    suspend fun getPublicKey(
        @Path("user_id") userId: String,
        @Header("Authorization") bearerToken: String
    ): Response<PublicKeyResponse>

    // ── Groups ──────────────────────────────────────────────────────────────

    @POST("groups/")
    suspend fun createGroup(
        @Body body: GroupCreateRequest,
        @Header("Authorization") bearerToken: String
    ): Response<GroupResponse>

    @GET("groups/")
    suspend fun listMyGroups(
        @Header("Authorization") bearerToken: String
    ): Response<List<GroupListItem>>

    @POST("groups/{group_id}/members")
    suspend fun addMember(
        @Path("group_id") groupId: String,
        @Body body: AddMemberRequest,
        @Header("Authorization") bearerToken: String
    ): Response<MemberResponse>

    @GET("groups/{group_id}/sgk")
    suspend fun getMySgk(
        @Path("group_id") groupId: String,
        @Header("Authorization") bearerToken: String
    ): Response<EncryptedSGKResponse>

    // ── Group passwords ─────────────────────────────────────────────────────

    @POST("groups/{group_id}/passwords")
    suspend fun createPassword(
        @Path("group_id") groupId: String,
        @Body body: PasswordCreateRequest,
        @Header("Authorization") bearerToken: String
    ): Response<PasswordResponse>

    @GET("groups/{group_id}/passwords")
    suspend fun listPasswords(
        @Path("group_id") groupId: String,
        @Header("Authorization") bearerToken: String
    ): Response<List<PasswordResponse>>

    @PATCH("groups/{group_id}/passwords/{password_id}")
    suspend fun updatePassword(
        @Path("group_id") groupId: String,
        @Path("password_id") passwordId: String,
        @Body body: PasswordUpdateRequest,
        @Header("Authorization") bearerToken: String
    ): Response<PasswordResponse>

    @DELETE("groups/{group_id}/passwords/{password_id}")
    suspend fun deletePassword(
        @Path("group_id") groupId: String,
        @Path("password_id") passwordId: String,
        @Header("Authorization") bearerToken: String
    ): Response<Unit>
}
