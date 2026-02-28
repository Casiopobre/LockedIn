# Pruebas de endpoints – Vault Backend

> **Base URL:** `http://localhost:8000`
>
> Ejecutar los curls en orden. Los valores entre `<...>` deben sustituirse
> con los datos devueltos por peticiones anteriores.
>
> **⚠️ Si vienes de una versión anterior**, el schema de la tabla `users` ha
> cambiado (nueva columna `id_lookup`, `password_hash` ahora almacena Argon2
> sobre el SHA-256 del cliente + PEPPER2).
> Recrea la DB:
> ```bash
> docker compose down -v && docker compose up --build
> ```

---

## 0. Health check

```bash
curl -s http://localhost:8000/health | python3 -m json.tool
```

---

## 1. Registro de usuario 1

El `user_id` es el número de teléfono del usuario. El `password_hash` es el
SHA-256 de la contraseña maestra, calculado en el cliente. El servidor nunca
ve la contraseña en claro (zero-knowledge). El backend aplica Argon2id + PEPPER2
sobre este hash.

```bash
curl -s -X POST http://localhost:8000/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "666111222",
    "password_hash": "2dd9f649063164ed47425c17ce5bd705c2273f877f8b89d2952958506ea3cdd9",
    "public_key": "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0Z3VS5JJcds3xfn/ygWe\nFGHKZOSauLkjJDBMnmG4qiiAnKmGT1FzNJt8ycWj4eCOoeq6GWhVU/0F9bEMPQym\nR50jwJKxNHyqIwwIvoBSn5RtQbEBIaFjR5d3xgfO1sIu7OqYfzCRiMOFQZ7N01A0\nshjKEeLqZIhw4+lv4Ly3NL4RLRfOP53ISJv9Z2BbZsFv4FZME40zSlbRvHKuzFb/\nO+Yvz7BOmTMGJGVqY+ItOMjx/MpVUVulMb0tN0w3GNDJtQ3MhZ3m2fXP+flFKIjM\nkz+7/R2CqzFk5WFEOHJCC6yS8eSEM+kHl94xdH2dTlRTdezj1YbGIJwfvQ1EXAMPLE\nwQIDAQAB\n-----END PUBLIC KEY-----"
  }' | python3 -m json.tool
```

---

## 2. Registro de usuario 2

```bash
curl -s -X POST http://localhost:8000/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "666333444",
    "password_hash": "b9f6996bdcad773042e43796c497a6af674657412bdca82f3a0f15b098d71dc7",
    "public_key": "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEBxyz1234567890abcdef\nBOBKEYEXAMPLEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\nAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\nAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\nwQIDAQAB\n-----END PUBLIC KEY-----"
  }' | python3 -m json.tool
```

---

## 3. Login de usuario 1

```bash
curl -s -X POST http://localhost:8000/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "666111222",
    "password_hash": "2dd9f649063164ed47425c17ce5bd705c2273f877f8b89d2952958506ea3cdd9"
  }' | python3 -m json.tool
```

> **Guardar el token devuelto:**
> ```
> export TOKEN_ALICE="<access_token>"
> ```

---

## 4. Login de usuario 2

```bash
curl -s -X POST http://localhost:8000/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "666333444",
    "password_hash": "b9f6996bdcad773042e43796c497a6af674657412bdca82f3a0f15b098d71dc7"
  }' | python3 -m json.tool
```

> **Guardar el token devuelto:**
> ```
> export TOKEN_BOB="<access_token>"
> ```

---

## 5. Obtener clave pública de usuario 2 (como usuario 1)

```bash
curl -s http://localhost:8000/auth/public-key/666333444 \
  -H "Authorization: Bearer $TOKEN_ALICE" \
  | python3 -m json.tool
```

---

## 6. Crear un grupo (como usuario 1)

Usuario 1 crea un grupo y sube su SGK cifrada con su propia clave pública.

```bash
curl -s -X POST http://localhost:8000/groups/ \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_ALICE" \
  -d '{
    "name": "Equipo HackUDC",
    "encrypted_sgk": "base64-sgk-cifrada-con-pubkey-alice-EXAMPLE=="
  }' | python3 -m json.tool
```

> **Guardar el ID del grupo:**
> ```
> export GROUP_ID="<id>"
> ```

---

## 7. Listar mis grupos (como usuario 1)

```bash
curl -s http://localhost:8000/groups/ \
  -H "Authorization: Bearer $TOKEN_ALICE" \
  | python3 -m json.tool
```

---

## 8. Añadir a usuario 2 al grupo (como usuario 1)

Usuario 1 cifra la SGK con la clave pública de usuario 2 y la sube.

```bash
curl -s -X POST "http://localhost:8000/groups/$GROUP_ID/members" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_ALICE" \
  -d '{
    "user_id": "666333444",
    "encrypted_sgk": "base64-sgk-cifrada-con-pubkey-bob-EXAMPLE=="
  }' | python3 -m json.tool
```

---

## 9. Obtener mi SGK cifrada (como usuario 2)

Usuario 2 recupera su copia de la SGK (cifrada con su pubkey).

```bash
curl -s "http://localhost:8000/groups/$GROUP_ID/sgk" \
  -H "Authorization: Bearer $TOKEN_BOB" \
  | python3 -m json.tool
```

---

## 10. Listar mis grupos (como usuario 2)

```bash
curl -s http://localhost:8000/groups/ \
  -H "Authorization: Bearer $TOKEN_BOB" \
  | python3 -m json.tool
```

---

## 11. Compartir una contraseña en el grupo (como usuario 1)

Usuario 1 sube una contraseña cifrada con la SGK del grupo.

```bash
curl -s -X POST "http://localhost:8000/groups/$GROUP_ID/passwords" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_ALICE" \
  -d '{
    "label": "GitHub - cuenta del equipo",
    "encrypted_data": "aes-256-gcm-cifrado-con-sgk-EXAMPLE-data-base64=="
  }' | python3 -m json.tool
```

> **Guardar el ID de la contraseña:**
> ```
> export PASSWORD_ID="<id>"
> ```

---

## 12. Listar contraseñas del grupo (como usuario 2)

Usuario 2 puede ver las contraseñas cifradas (las descifra localmente con la SGK).

```bash
curl -s "http://localhost:8000/groups/$GROUP_ID/passwords" \
  -H "Authorization: Bearer $TOKEN_BOB" \
  | python3 -m json.tool
```

---

## 13. Actualizar una contraseña (como usuario 1)

```bash
curl -s -X PATCH "http://localhost:8000/groups/$GROUP_ID/passwords/$PASSWORD_ID" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN_ALICE" \
  -d '{
    "label": "GitHub - cuenta equipo (actualizada)",
    "encrypted_data": "aes-256-gcm-nuevo-cifrado-EXAMPLE-base64=="
  }' | python3 -m json.tool
```

---

## 14. Borrar una contraseña (como usuario 1)

```bash
curl -s -X DELETE "http://localhost:8000/groups/$GROUP_ID/passwords/$PASSWORD_ID" \
  -H "Authorization: Bearer $TOKEN_ALICE" \
  -w "\nHTTP Status: %{http_code}\n"
```

> Debe devolver **HTTP Status: 204** (sin cuerpo).

---

## 15. Verificar que la contraseña fue borrada (como usuario 2)

```bash
curl -s "http://localhost:8000/groups/$GROUP_ID/passwords" \
  -H "Authorization: Bearer $TOKEN_BOB" \
  | python3 -m json.tool
```

> Debe devolver una lista vacía `[]`.

---

## Resumen de endpoints cubiertos

| #  | Método   | Endpoint                                      | Descripción                       |
|----|----------|-----------------------------------------------|-----------------------------------|
| 0  | GET      | `/health`                                     | Health check                      |
| 1  | POST     | `/auth/register`                              | Registro usuario 1                |
| 2  | POST     | `/auth/register`                              | Registro usuario 2                |
| 3  | POST     | `/auth/login`                                 | Login usuario 1                   |
| 4  | POST     | `/auth/login`                                 | Login usuario 2                   |
| 5  | GET      | `/auth/public-key/{user_id}`                  | Obtener clave pública             |
| 6  | POST     | `/groups/`                                    | Crear grupo                       |
| 7  | GET      | `/groups/`                                    | Listar mis grupos                 |
| 8  | POST     | `/groups/{id}/members`                        | Añadir miembro al grupo           |
| 9  | GET      | `/groups/{id}/sgk`                            | Obtener SGK cifrada               |
| 10 | GET      | `/groups/`                                    | Listar grupos (otro usuario)      |
| 11 | POST     | `/groups/{id}/passwords`                      | Compartir contraseña              |
| 12 | GET      | `/groups/{id}/passwords`                      | Listar contraseñas del grupo      |
| 13 | PATCH    | `/groups/{id}/passwords/{pid}`                | Actualizar contraseña             |
| 14 | DELETE   | `/groups/{id}/passwords/{pid}`                | Borrar contraseña                 |
| 15 | GET      | `/groups/{id}/passwords`                      | Verificar borrado                 |
