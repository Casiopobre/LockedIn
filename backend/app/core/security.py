"""
Security helpers: Argon2 hashing with pepper, JWT creation / verification.
"""

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


# ── Argon2 helpers ──────────────────────────────────────────────────────────


def hash_id(user_id: str) -> str:
    """Hash the user-supplied ID with Argon2 + server pepper."""
    salted = f"{user_id}{settings.PEPPER}"
    return ph.hash(salted)


def verify_id(user_id: str, id_hash: str) -> bool:
    """Verify a plaintext user ID against its Argon2 hash."""
    try:
        salted = f"{user_id}{settings.PEPPER}"
        return ph.verify(id_hash, salted)
    except VerifyMismatchError:
        return False


# ── JWT helpers ─────────────────────────────────────────────────────────────


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


# ── FastAPI dependency ──────────────────────────────────────────────────────


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
