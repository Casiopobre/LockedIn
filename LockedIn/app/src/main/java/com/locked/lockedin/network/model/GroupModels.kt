package com.locked.lockedin.network.model

import com.google.gson.annotations.SerializedName

// ── Create group ────────────────────────────────────────────────────────────

data class GroupCreateRequest(
    val name: String,
    @SerializedName("encrypted_sgk") val encryptedSgk: String
)

data class GroupResponse(
    val id: String,
    val name: String,
    @SerializedName("owner_id")   val ownerId: String,
    @SerializedName("created_at") val createdAt: String
)

// ── List my groups ──────────────────────────────────────────────────────────

data class GroupListItem(
    val id: String,
    val name: String,
    @SerializedName("owner_id")      val ownerId: String,
    @SerializedName("created_at")    val createdAt: String,
    @SerializedName("encrypted_sgk") val encryptedSgk: String
)

// ── Add member ──────────────────────────────────────────────────────────────

data class AddMemberRequest(
    @SerializedName("user_id")       val userId: String,
    @SerializedName("encrypted_sgk") val encryptedSgk: String
)

data class MemberResponse(
    @SerializedName("user_id")   val userId: String,
    @SerializedName("joined_at") val joinedAt: String
)

// ── SGK retrieval ───────────────────────────────────────────────────────────

data class EncryptedSGKResponse(
    @SerializedName("encrypted_sgk") val encryptedSgk: String
)

// ── Passwords ───────────────────────────────────────────────────────────────

data class PasswordCreateRequest(
    val label: String,
    @SerializedName("encrypted_data") val encryptedData: String
)

data class PasswordResponse(
    val id: String,
    @SerializedName("group_id")     val groupId: String,
    @SerializedName("created_by")   val createdBy: String,
    val label: String,
    @SerializedName("encrypted_data") val encryptedData: String,
    @SerializedName("created_at")   val createdAt: String,
    @SerializedName("updated_at")   val updatedAt: String
)

data class PasswordUpdateRequest(
    val label: String? = null,
    @SerializedName("encrypted_data") val encryptedData: String? = null
)
