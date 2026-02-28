"""
Groups router: create group, add members, share passwords.
"""

from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.security import get_current_user, verify_id
from app.models.models import Group, GroupMember, GroupPassword, User
from app.schemas.groups import (
    AddMemberRequest,
    EncryptedSGKResponse,
    GroupCreateRequest,
    GroupListItem,
    GroupResponse,
    MemberResponse,
    PasswordCreateRequest,
    PasswordResponse,
    PasswordUpdateRequest,
)

router = APIRouter(prefix="/groups", tags=["groups"])


# ── Helpers ─────────────────────────────────────────────────────────────────


async def _get_group_or_404(group_id: UUID, db: AsyncSession) -> Group:
    result = await db.execute(select(Group).where(Group.id == group_id))
    group = result.scalar_one_or_none()
    if group is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Group not found")
    return group


async def _require_membership(group_id: UUID, user_id: UUID, db: AsyncSession) -> GroupMember:
    result = await db.execute(
        select(GroupMember).where(
            GroupMember.group_id == group_id,
            GroupMember.user_id == user_id,
        )
    )
    member = result.scalar_one_or_none()
    if member is None:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Not a member of this group")
    return member


# ── Create group ────────────────────────────────────────────────────────────


@router.post("/", response_model=GroupResponse, status_code=status.HTTP_201_CREATED)
async def create_group(
    body: GroupCreateRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    Create a new sharing group.
    The client generates the SGK, encrypts it with its own public key,
    and sends encrypted_sgk so it can later decrypt it.
    """
    group = Group(name=body.name, owner_id=current_user.id)
    db.add(group)
    await db.flush()  # get group.id

    # Creator is the first member
    member = GroupMember(
        group_id=group.id,
        user_id=current_user.id,
        encrypted_sgk=body.encrypted_sgk,
    )
    db.add(member)
    await db.commit()
    await db.refresh(group)

    return group


# ── List my groups ──────────────────────────────────────────────────────────


@router.get("/", response_model=list[GroupListItem])
async def list_my_groups(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Return all groups the current user belongs to, including their encrypted SGK."""
    result = await db.execute(
        select(Group, GroupMember.encrypted_sgk)
        .join(GroupMember, GroupMember.group_id == Group.id)
        .where(GroupMember.user_id == current_user.id)
    )
    rows = result.all()
    return [
        GroupListItem(
            id=group.id,
            name=group.name,
            owner_id=group.owner_id,
            created_at=group.created_at,
            encrypted_sgk=encrypted_sgk,
        )
        for group, encrypted_sgk in rows
    ]


# ── Add member ──────────────────────────────────────────────────────────────


@router.post("/{group_id}/members", response_model=MemberResponse, status_code=status.HTTP_201_CREATED)
async def add_member(
    group_id: UUID,
    body: AddMemberRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    Add a user to the group.
    The caller must already be a member and must supply the SGK encrypted
    with the new member's public key.
    """
    group = await _get_group_or_404(group_id, db)
    await _require_membership(group_id, current_user.id, db)

    # Resolve target user by plaintext ID
    result = await db.execute(select(User))
    all_users = result.scalars().all()
    target_user: User | None = None
    for u in all_users:
        if verify_id(body.user_id, u.id_hash):
            target_user = u
            break

    if target_user is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Target user not found")

    # Check if already a member
    existing = await db.execute(
        select(GroupMember).where(
            GroupMember.group_id == group_id,
            GroupMember.user_id == target_user.id,
        )
    )
    if existing.scalar_one_or_none() is not None:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="User is already a member")

    member = GroupMember(
        group_id=group_id,
        user_id=target_user.id,
        encrypted_sgk=body.encrypted_sgk,
    )
    db.add(member)
    await db.commit()
    await db.refresh(member)

    return MemberResponse(user_id=target_user.id, joined_at=member.joined_at)


# ── Get my encrypted SGK for a group ───────────────────────────────────────


@router.get("/{group_id}/sgk", response_model=EncryptedSGKResponse)
async def get_my_sgk(
    group_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Return the SGK encrypted with the current user's public key."""
    member = await _require_membership(group_id, current_user.id, db)
    return EncryptedSGKResponse(encrypted_sgk=member.encrypted_sgk)


# ── Share a password ────────────────────────────────────────────────────────


@router.post(
    "/{group_id}/passwords",
    response_model=PasswordResponse,
    status_code=status.HTTP_201_CREATED,
)
async def create_password(
    group_id: UUID,
    body: PasswordCreateRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Upload a password encrypted with the group's SGK."""
    await _get_group_or_404(group_id, db)
    await _require_membership(group_id, current_user.id, db)

    entry = GroupPassword(
        group_id=group_id,
        created_by=current_user.id,
        label=body.label,
        encrypted_data=body.encrypted_data,
    )
    db.add(entry)
    await db.commit()
    await db.refresh(entry)

    return entry


# ── List group passwords ───────────────────────────────────────────────────


@router.get("/{group_id}/passwords", response_model=list[PasswordResponse])
async def list_passwords(
    group_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    await _get_group_or_404(group_id, db)
    await _require_membership(group_id, current_user.id, db)

    result = await db.execute(
        select(GroupPassword).where(GroupPassword.group_id == group_id)
    )
    return result.scalars().all()


# ── Update a password ──────────────────────────────────────────────────────


@router.patch("/{group_id}/passwords/{password_id}", response_model=PasswordResponse)
async def update_password(
    group_id: UUID,
    password_id: UUID,
    body: PasswordUpdateRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    await _get_group_or_404(group_id, db)
    await _require_membership(group_id, current_user.id, db)

    result = await db.execute(
        select(GroupPassword).where(
            GroupPassword.id == password_id,
            GroupPassword.group_id == group_id,
        )
    )
    entry = result.scalar_one_or_none()
    if entry is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Password entry not found")

    if body.label is not None:
        entry.label = body.label
    if body.encrypted_data is not None:
        entry.encrypted_data = body.encrypted_data

    await db.commit()
    await db.refresh(entry)
    return entry


# ── Delete a password ──────────────────────────────────────────────────────


@router.delete("/{group_id}/passwords/{password_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_password(
    group_id: UUID,
    password_id: UUID,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    await _get_group_or_404(group_id, db)
    await _require_membership(group_id, current_user.id, db)

    result = await db.execute(
        select(GroupPassword).where(
            GroupPassword.id == password_id,
            GroupPassword.group_id == group_id,
        )
    )
    entry = result.scalar_one_or_none()
    if entry is None:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Password entry not found")

    await db.delete(entry)
    await db.commit()
