"""
SQLAlchemy ORM models for the password vault.

Tables
------
- users            : ID (Argon2+pepper hash), SHA-256 password, public key
- groups           : password-sharing groups
- group_members    : M2M with the encrypted SGK per member
- group_passwords  : passwords encrypted with SGK
"""

import uuid
from datetime import datetime, timezone

from sqlalchemy import (
    Column,
    DateTime,
    ForeignKey,
    String,
    Text,
    UniqueConstraint,
)
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import DeclarativeBase, relationship


class Base(DeclarativeBase):
    pass


# ── Users ───────────────────────────────────────────────────────────────────


class User(Base):
    __tablename__ = "users"

    # Internal surrogate PK (never exposed to client)
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)

    # User-supplied ID hashed with Argon2 + pepper (unique)
    id_hash = Column(String(512), unique=True, nullable=False, index=True)

    # Password already SHA-256-hashed on the client, stored as-is
    password_hash = Column(String(64), nullable=False)

    # RSA / EC public key in PEM or base64
    public_key = Column(Text, nullable=False)

    created_at = Column(
        DateTime(timezone=True), default=lambda: datetime.now(timezone.utc)
    )

    # relationships
    memberships = relationship("GroupMember", back_populates="user", cascade="all, delete-orphan")


# ── Groups ──────────────────────────────────────────────────────────────────


class Group(Base):
    __tablename__ = "groups"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    name = Column(String(255), nullable=False)

    # UUID of the user who created the group
    owner_id = Column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=False)

    created_at = Column(
        DateTime(timezone=True), default=lambda: datetime.now(timezone.utc)
    )

    # relationships
    members = relationship("GroupMember", back_populates="group", cascade="all, delete-orphan")
    passwords = relationship("GroupPassword", back_populates="group", cascade="all, delete-orphan")
    owner = relationship("User", foreign_keys=[owner_id])


# ── Group Members ──────────────────────────────────────────────────────────


class GroupMember(Base):
    """
    Each row stores the SGK encrypted with the member's public key,
    so only that member can decrypt it.
    """

    __tablename__ = "group_members"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)

    group_id = Column(UUID(as_uuid=True), ForeignKey("groups.id", ondelete="CASCADE"), nullable=False)
    user_id = Column(UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), nullable=False)

    # SGK encrypted with this user's public key (base64)
    encrypted_sgk = Column(Text, nullable=False)

    joined_at = Column(
        DateTime(timezone=True), default=lambda: datetime.now(timezone.utc)
    )

    __table_args__ = (
        UniqueConstraint("group_id", "user_id", name="uq_group_user"),
    )

    # relationships
    group = relationship("Group", back_populates="members")
    user = relationship("User", back_populates="memberships")


# ── Group Passwords ─────────────────────────────────────────────────────────


class GroupPassword(Base):
    """
    A password entry shared with the group, encrypted with the SGK.
    """

    __tablename__ = "group_passwords"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)

    group_id = Column(UUID(as_uuid=True), ForeignKey("groups.id", ondelete="CASCADE"), nullable=False)

    # Who uploaded the entry
    created_by = Column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=False)

    # Label / site name – can be plaintext or encrypted, up to client
    label = Column(String(512), nullable=False)

    # The actual encrypted blob (AES-encrypted with SGK, base64)
    encrypted_data = Column(Text, nullable=False)

    created_at = Column(
        DateTime(timezone=True), default=lambda: datetime.now(timezone.utc)
    )
    updated_at = Column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
        onupdate=lambda: datetime.now(timezone.utc),
    )

    # relationships
    group = relationship("Group", back_populates="passwords")
    creator = relationship("User", foreign_keys=[created_by])
