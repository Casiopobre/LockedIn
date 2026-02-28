"""
Security helpers
~~~~~~~~~~~~~~~~
- HMAC-SHA256  → deterministic `id_lookup` for O(1) indexed DB queries (uses PEPPER).
- Argon2       → slow hash for `id_hash` (defence-in-depth, uses PEPPER)
                 **and** `password_hash` (uses PEPPER2).
- JWT          → session tokens.

PEPPER  is used exclusively for user-ID operations (lookup + defence-in-depth hash).
PEPPER2 is used exclusively for password hashing.
"""

import hashlib
import hmac
from datetime import datetime, timedelta, timezone

from argon2 import PasswordHasher
from argon2.exceptions import VerifyMismatchError
from jose import JWTError, jwt
from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import get_settings
from app.core.database import get_db
from app.models.models import User

settings = get_settings()
ph = PasswordHasher()

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/auth/login")


# ID helpers
def compute_id_lookup(user_id: str) -> str:
    """Deterministic HMAC-SHA256(user_id, PEPPER) for indexed O(1) lookup."""
    return hmac.new(
        settings.PEPPER.encode(),
        user_id.encode(),
        hashlib.sha256,
    ).hexdigest()


def hash_id(user_id: str) -> str:
    """Argon2 hash of user_id+pepper (defence-in-depth, NOT used for lookup)."""
    salted = f"{user_id}{settings.PEPPER}"
    return ph.hash(salted)


def verify_id(user_id: str, id_hash: str) -> bool:
    """Verify a plaintext user ID against its Argon2 id_hash."""
    try:
        salted = f"{user_id}{settings.PEPPER}"
        return ph.verify(id_hash, salted)
    except VerifyMismatchError:
        return False


# Password helpers (server-side Argon2 over client-side SHA-256 + PEPPER2)
def hash_password(sha256_from_client: str) -> str:
    """Argon2id hash of SHA-256(password) + PEPPER2.

    The client computes SHA-256 of the master password and sends that digest.
    The server never sees the plaintext password (zero-knowledge).
    """
    peppered = f"{sha256_from_client}{settings.PEPPER2}"
    return ph.hash(peppered)


def verify_password(sha256_from_client: str, stored_hash: str) -> bool:
    """Verify a client-provided SHA-256 digest against the stored Argon2 hash."""
    try:
        peppered = f"{sha256_from_client}{settings.PEPPER2}"
        return ph.verify(stored_hash, peppered)
    except VerifyMismatchError:
        return False


# JWT helpers
def create_access_token(subject: str, expires_delta: timedelta | None = None) -> str:
    expire = datetime.now(timezone.utc) + (
        expires_delta or timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)
    )
    to_encode = {"sub": subject, "exp": expire}
    return jwt.encode(to_encode, settings.SECRET_KEY, algorithm=settings.JWT_ALGORITHM)


def decode_access_token(token: str) -> str | None:
    """Return the subject (user internal UUID) or None if invalid."""
    try:
        payload = jwt.decode(token, settings.SECRET_KEY, algorithms=[settings.JWT_ALGORITHM])
        return payload.get("sub")
    except JWTError:
        return None


# FastAPI dependency
async def get_current_user(
    token: str = Depends(oauth2_scheme),
    db: AsyncSession = Depends(get_db),
) -> User:
    """Dependency that extracts and validates the current user from the JWT."""
    credentials_exception = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Invalid or expired token",
        headers={"WWW-Authenticate": "Bearer"},
    )
    subject = decode_access_token(token)
    if subject is None:
        raise credentials_exception

    result = await db.execute(select(User).where(User.id == subject))
    user = result.scalar_one_or_none()
    if user is None:
        raise credentials_exception
    return user
