"""
Pydantic schemas for auth endpoints (register / login).
"""

from pydantic import BaseModel, Field


# Register
class RegisterRequest(BaseModel):
    user_id: str = Field(..., min_length=1, description="Phone number used as user identifier")
    password_hash: str = Field(
        ..., min_length=64, max_length=64,
        description="SHA-256 hex-digest of the master password (computed client-side)",
    )
    public_key: str = Field(..., min_length=1, description="PEM or base64-encoded public key")


class RegisterResponse(BaseModel):
    message: str = "User registered successfully"


# Login
class LoginRequest(BaseModel):
    user_id: str = Field(..., min_length=1, description="Phone number used as user identifier")
    password_hash: str = Field(
        ..., min_length=64, max_length=64,
        description="SHA-256 hex-digest of the master password (computed client-side)",
    )


class LoginResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"


# Public key lookup
class PublicKeyResponse(BaseModel):
    public_key: str
