"""
Auth router: register, login, public-key lookup.

All user lookups use the deterministic HMAC-SHA256 `id_lookup` column
for O(1) indexed queries instead of scanning the entire table.
"""

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.security import (
    compute_id_lookup,
    create_access_token,
    get_current_user,
    hash_id,
    hash_password,
    verify_id,
    verify_password,
)
from app.models.models import User
from app.schemas.auth import (
    LoginRequest,
    LoginResponse,
    PublicKeyResponse,
    RegisterRequest,
    RegisterResponse,
)

router = APIRouter(prefix="/auth", tags=["auth"])


# helpers
async def _get_user_by_plaintext_id(user_id: str, db: AsyncSession) -> User | None:
    """O(1) lookup via HMAC index + Argon2 verification."""
    lookup = compute_id_lookup(user_id)
    result = await db.execute(select(User).where(User.id_lookup == lookup))
    user = result.scalar_one_or_none()
    if user is None:
        return None
    # Defence-in-depth: verify Argon2 hash to guard against HMAC collisions
    if not verify_id(user_id, user.id_hash):
        return None
    return user


# Register
""" Veryfy that the user_id doesn't already exist (O(1) via HMAC index), then hash the user_id and password with Argon2 and store everything. """
@router.post(
    "/register",
    response_model=RegisterResponse,
    status_code=status.HTTP_201_CREATED,
)
async def register(body: RegisterRequest, db: AsyncSession = Depends(get_db)):
    # O(1) duplicate check via HMAC index
    if await _get_user_by_plaintext_id(body.user_id, db) is not None:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="User already exists",
        )

    user = User(
        id_lookup=compute_id_lookup(body.user_id),
        id_hash=hash_id(body.user_id),
        password_hash=hash_password(body.password_hash),
        public_key=body.public_key,
    )
    db.add(user)
    await db.commit()
    await db.refresh(user)
    return RegisterResponse()


# Login
@router.post("/login", response_model=LoginResponse)
async def login(body: LoginRequest, db: AsyncSession = Depends(get_db)):
    user = await _get_user_by_plaintext_id(body.user_id, db)

    if user is None or not verify_password(body.password_hash, user.password_hash):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid credentials",
        )

    token = create_access_token(subject=str(user.id))
    return LoginResponse(access_token=token)


# Public-key lookup
@router.get("/public-key/{user_id}", response_model=PublicKeyResponse)
async def get_public_key(
    user_id: str,
    db: AsyncSession = Depends(get_db),
    _current_user: User = Depends(get_current_user),
):
    """
    Authenticated endpoint: look up another user's public key by their
    plaintext user_id so you can encrypt the SGK for them.
    """
    user = await _get_user_by_plaintext_id(user_id, db)
    if user is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="User not found")

    return PublicKeyResponse(public_key=user.public_key)
