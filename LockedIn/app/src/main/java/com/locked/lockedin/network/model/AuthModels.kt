package com.locked.lockedin.network.model

import com.google.gson.annotations.SerializedName

// ── Register ────────────────────────────────────────────────────────────────

data class RegisterRequest(
    @SerializedName("user_id")      val userId: String,
    @SerializedName("password_hash") val passwordHash: String,
    @SerializedName("public_key")    val publicKey: String
)

data class RegisterResponse(
    val message: String
)

// ── Login ───────────────────────────────────────────────────────────────────

data class LoginRequest(
    @SerializedName("user_id")      val userId: String,
    @SerializedName("password_hash") val passwordHash: String
)

data class LoginResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type")   val tokenType: String
)

// ── Public-key lookup ───────────────────────────────────────────────────────

data class PublicKeyResponse(
    @SerializedName("public_key") val publicKey: String
)
