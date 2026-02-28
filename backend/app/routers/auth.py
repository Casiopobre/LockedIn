"""
Auth router: register, login, public-key lookup.
"""

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.security import hash_id, verify_id, create_access_token, get_current_user
from app.models.models import User
from app.schemas.auth import (
    LoginRequest,
    LoginResponse,
    PublicKeyResponse,
    RegisterRequest,
    RegisterResponse,
)

router = APIRouter(prefix="/auth", tags=["auth"])


# ── Register ────────────────────────────────────────────────────────────────


@router.post(
    "/register",
    response_model=RegisterResponse,
    status_code=status.HTTP_201_CREATED,
)
async def register(body: RegisterRequest, db: AsyncSession = Depends(get_db)):
    # Check for duplicate (we have to scan because Argon2 hashes are salted)
    # For scalability this could be optimised with a deterministic hash index
    result = await db.execute(select(User))
    existing_users = result.scalars().all()
    for u in existing_users:
        if verify_id(body.user_id, u.id_hash):
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail="User already exists",
            )

    id_hash = hash_id(body.user_id)

    user = User(
        id_hash=id_hash,
        password_hash=body.password_hash,
        public_key=body.public_key,
    )
    db.add(user)
    await db.commit()
    await db.refresh(user)
    return RegisterResponse()


# ── Login ───────────────────────────────────────────────────────────────────


@router.post("/login", response_model=LoginResponse)
async def login(body: LoginRequest, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(User))
    users = result.scalars().all()

    target_user: User | None = None
    for u in users:
        if verify_id(body.user_id, u.id_hash):
            target_user = u
            break

    if target_user is None or target_user.password_hash != body.password_hash:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid credentials",
        )

    token = create_access_token(subject=str(target_user.id))
    return LoginResponse(access_token=token)


# ── Public-key lookup ──────────────────────────────────────────────────────


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
    result = await db.execute(select(User))
    users = result.scalars().all()

    for u in users:
        if verify_id(user_id, u.id_hash):
            return PublicKeyResponse(public_key=u.public_key)

    raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="User not found")
