#!/usr/bin/env bash

set -e

BASE_URL="http://localhost:8000"

echo "🔎 0. Health check"
curl -s $BASE_URL/health | python3 -m json.tool
echo "---------------------------------------"

echo "👤 1. Register alice"
curl -s -X POST $BASE_URL/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "alice",
    "password_hash": "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8",
    "public_key": "ALICE_PUBLIC_KEY"
  }' | python3 -m json.tool
echo "---------------------------------------"

echo "👤 2. Register bob"
curl -s -X POST $BASE_URL/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "bob",
    "password_hash": "6cf615d5bcaac778352a8f1f3360d23f02f34ec182e259897fd6ce485d7870d4",
    "public_key": "BOB_PUBLIC_KEY"
  }' | python3 -m json.tool
echo "---------------------------------------"

echo "🔐 3. Login alice"
TOKEN_ALICE=$(curl -s -X POST $BASE_URL/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "alice",
    "password_hash": "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8"
  }' | python3 -c "import sys, json; print(json.load(sys.stdin)['access_token'])")

echo "TOKEN_ALICE obtenido"
echo "---------------------------------------"

echo "🔐 4. Login bob"
TOKEN_BOB=$(curl -s -X POST $BASE_URL/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "bob",
    "password_hash": "6cf615d5bcaac778352a8f1f3360d23f02f34ec182e259897fd6ce485d7870d4"
  }' | python3 -c "import sys, json; print(json.load(sys.stdin)['access_token'])")

echo "TOKEN_BOB obtenido"
echo "---------------------------------------"

echo "🔑 5. Get bob public key (as alice)"
curl -s $BASE_URL/auth/public-key/bob \
  -H "Authorization: Bearer $TOKEN_ALICE" \
  | python3 -m json.tool
echo "---------------------------------------"

echo "👥 6. Create group (as alice)"
GROUP_ID=$(curl -s -X POST $BASE_URL/groups/ \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_ALICE" \
  -d '{
    "name": "Equipo HackUDC",
    "encrypted_sgk": "base64-sgk-cifrada-con-pubkey-alice-EXAMPLE=="
  }' | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])")

echo "GROUP_ID: $GROUP_ID"
echo "---------------------------------------"

echo "📋 7. List groups (alice)"
curl -s $BASE_URL/groups/ \
  -H "Authorization: Bearer $TOKEN_ALICE" \
  | python3 -m json.tool
echo "---------------------------------------"

echo "➕ 8. Add bob to group"
curl -s -X POST "$BASE_URL/groups/$GROUP_ID/members" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_ALICE" \
  -d '{
    "user_id": "bob",
    "encrypted_sgk": "base64-sgk-cifrada-con-pubkey-bob-EXAMPLE=="
  }' | python3 -m json.tool
echo "---------------------------------------"

echo "🔐 9. Get SGK (bob)"
curl -s "$BASE_URL/groups/$GROUP_ID/sgk" \
  -H "Authorization: Bearer $TOKEN_BOB" \
  | python3 -m json.tool
echo "---------------------------------------"

echo "📋 10. List groups (bob)"
curl -s $BASE_URL/groups/ \
  -H "Authorization: Bearer $TOKEN_BOB" \
  | python3 -m json.tool
echo "---------------------------------------"

echo "🔒 11. Share password (alice)"
PASSWORD_ID=$(curl -s -X POST "$BASE_URL/groups/$GROUP_ID/passwords" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_ALICE" \
  -d '{
    "label": "GitHub - cuenta del equipo",
    "encrypted_data": "aes-256-gcm-cifrado-con-sgk-EXAMPLE-data-base64=="
  }' | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])")

echo "PASSWORD_ID: $PASSWORD_ID"
echo "---------------------------------------"

echo "📋 12. List passwords (bob)"
curl -s "$BASE_URL/groups/$GROUP_ID/passwords" \
  -H "Authorization: Bearer $TOKEN_BOB" \
  | python3 -m json.tool
echo "---------------------------------------"

echo "✏️ 13. Update password (alice)"
curl -s -X PATCH "$BASE_URL/groups/$GROUP_ID/passwords/$PASSWORD_ID" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_ALICE" \
  -d '{
    "label": "GitHub - cuenta equipo (actualizada)",
    "encrypted_data": "aes-256-gcm-nuevo-cifrado-EXAMPLE-base64=="
  }' | python3 -m json.tool
echo "---------------------------------------"

echo "🗑️ 14. Delete password (alice)"
curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" \
  -X DELETE "$BASE_URL/groups/$GROUP_ID/passwords/$PASSWORD_ID" \
  -H "Authorization: Bearer $TOKEN_ALICE"
echo "---------------------------------------"

echo "🔎 15. Verify deletion (bob)"
curl -s "$BASE_URL/groups/$GROUP_ID/passwords" \
  -H "Authorization: Bearer $TOKEN_BOB" \
  | python3 -m json.tool

echo "---------------------------------------"
echo "✅ END-TO-END TEST COMPLETED"
