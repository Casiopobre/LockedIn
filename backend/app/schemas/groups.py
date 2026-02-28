"""
Pydantic schemas for group & password-sharing endpoints.
"""

from datetime import datetime
from uuid import UUID

from pydantic import BaseModel, Field


# Group
class GroupCreateRequest(BaseModel):
    name: str = Field(..., min_length=1, max_length=255)
    encrypted_sgk: str = Field(
        ...,
        description="SGK encrypted with the creator's own public key (base64)",
    )


class GroupResponse(BaseModel):
    id: UUID
    name: str
    owner_id: UUID
    created_at: datetime

    model_config = {"from_attributes": True}


# Add member
class AddMemberRequest(BaseModel):
    user_id: str = Field(..., description="Plaintext user ID of the member to add")
    encrypted_sgk: str = Field(
        ...,
        description="SGK encrypted with the new member's public key (base64)",
    )


class MemberResponse(BaseModel):
    user_id: UUID
    joined_at: datetime

    model_config = {"from_attributes": True}


# Encrypted SGK retrieval
class EncryptedSGKResponse(BaseModel):
    encrypted_sgk: str


# Passwords
class PasswordCreateRequest(BaseModel):
    label: str = Field(..., min_length=1, max_length=512)
    encrypted_data: str = Field(
        ...,
        description="Password data encrypted with the group's SGK (base64)",
    )


class PasswordResponse(BaseModel):
    id: UUID
    group_id: UUID
    created_by: UUID
    label: str
    encrypted_data: str
    created_at: datetime
    updated_at: datetime

    model_config = {"from_attributes": True}


class PasswordUpdateRequest(BaseModel):
    label: str | None = None
    encrypted_data: str | None = None


# My groups
class GroupListItem(BaseModel):
    id: UUID
    name: str
    owner_id: UUID
    created_at: datetime
    encrypted_sgk: str

    model_config = {"from_attributes": True}
