#!/usr/bin/env bash

set -e

BASE_URL="http://localhost:8000"

echo "🔎 0. Health check"
curl -s $BASE_URL/health | python3 -m json.tool
echo "---------------------------------------"

echo "👤 1. Register user 1 (666111222)"
curl -s -X POST $BASE_URL/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "666111222",
    "password_hash": "2dd9f649063164ed47425c17ce5bd705c2273f877f8b89d2952958506ea3cdd9",
    "public_key": "ALICE_PUBLIC_KEY"
  }' | python3 -m json.tool
echo "---------------------------------------"

echo "👤 2. Register user 2 (666333444)"
curl -s -X POST $BASE_URL/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "666333444",
    "password_hash": "b9f6996bdcad773042e43796c497a6af674657412bdca82f3a0f15b098d71dc7",
    "public_key": "BOB_PUBLIC_KEY"
  }' | python3 -m json.tool
echo "---------------------------------------"

echo "🔐 3. Login user 1"
TOKEN_ALICE=$(curl -s -X POST $BASE_URL/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "666111222",
    "password_hash": "2dd9f649063164ed47425c17ce5bd705c2273f877f8b89d2952958506ea3cdd9"
  }' | python3 -c "import sys, json; print(json.load(sys.stdin)['access_token'])")

echo "TOKEN_ALICE obtenido"
echo "---------------------------------------"

echo "🔐 4. Login user 2"
TOKEN_BOB=$(curl -s -X POST $BASE_URL/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "666333444",
    "password_hash": "b9f6996bdcad773042e43796c497a6af674657412bdca82f3a0f15b098d71dc7"
  }' | python3 -c "import sys, json; print(json.load(sys.stdin)['access_token'])")
echo "TOKEN_BOB obtenido"
echo "---------------------------------------"

echo "🔑 5. Get user 2 public key (as user 1)"
curl -s $BASE_URL/auth/public-key/666333444 \
  -H "Authorization: Bearer $TOKEN_ALICE" \
  | python3 -m json.tool
echo "---------------------------------------"

echo "👥 6. Create group (as user 1)"
GROUP_ID=$(curl -s -X POST $BASE_URL/groups/ \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_ALICE" \
  -d '{
    "name": "Equipo HackUDC",
    "encrypted_sgk": "base64-sgk-cifrada-con-pubkey-alice-EXAMPLE=="
  }' | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])")

echo "GROUP_ID: $GROUP_ID"
echo "---------------------------------------"

echo "📋 7. List groups (user 1)"
curl -s $BASE_URL/groups/ \
  -H "Authorization: Bearer $TOKEN_ALICE" \
  | python3 -m json.tool
echo "---------------------------------------"

echo "➕ 8. Add user 2 to group"
curl -s -X POST "$BASE_URL/groups/$GROUP_ID/members" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_ALICE" \
  -d '{
    "user_id": "666333444",
    "encrypted_sgk": "base64-sgk-cifrada-con-pubkey-bob-EXAMPLE=="
  }' | python3 -m json.tool
echo "---------------------------------------"

echo "🔐 9. Get SGK (user 2)"
curl -s "$BASE_URL/groups/$GROUP_ID/sgk" \
  -H "Authorization: Bearer $TOKEN_BOB" \
  | python3 -m json.tool
echo "---------------------------------------"

echo "📋 10. List groups (user 2)"
curl -s $BASE_URL/groups/ \
  -H "Authorization: Bearer $TOKEN_BOB" \
  | python3 -m json.tool
echo "---------------------------------------"

echo "🔒 11. Share password (user 1)"
PASSWORD_ID=$(curl -s -X POST "$BASE_URL/groups/$GROUP_ID/passwords" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_ALICE" \
  -d '{
    "label": "GitHub - cuenta del equipo",
    "encrypted_data": "aes-256-gcm-cifrado-con-sgk-EXAMPLE-data-base64=="
  }' | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])")

echo "PASSWORD_ID: $PASSWORD_ID"
echo "---------------------------------------"

echo "📋 12. List passwords (user 2)"
curl -s "$BASE_URL/groups/$GROUP_ID/passwords" \
  -H "Authorization: Bearer $TOKEN_BOB" \
  | python3 -m json.tool
echo "---------------------------------------"

echo "✏️ 13. Update password (user 1)"
curl -s -X PATCH "$BASE_URL/groups/$GROUP_ID/passwords/$PASSWORD_ID" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_ALICE" \
  -d '{
    "label": "GitHub - cuenta equipo (actualizada)",
    "encrypted_data": "aes-256-gcm-nuevo-cifrado-EXAMPLE-base64=="
  }' | python3 -m json.tool
echo "---------------------------------------"

echo "🗑️ 14. Delete password (user 1)"
curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" \
  -X DELETE "$BASE_URL/groups/$GROUP_ID/passwords/$PASSWORD_ID" \
  -H "Authorization: Bearer $TOKEN_ALICE"
echo "---------------------------------------"

echo "🔎 15. Verify deletion (user 2)"
curl -s "$BASE_URL/groups/$GROUP_ID/passwords" \
  -H "Authorization: Bearer $TOKEN_BOB" \
  | python3 -m json.tool

echo "---------------------------------------"
echo "✅ END-TO-END TEST COMPLETED"
