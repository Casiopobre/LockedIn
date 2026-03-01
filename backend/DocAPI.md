# Vault Backend – Documentación completa de la API

> **Base URL:** `http://localhost:8000`
>
> Todas las conexiones en producción deben realizarse sobre **HTTPS**.

---

## Índice

- [Vault Backend – Documentación completa de la API](#vault-backend--documentación-completa-de-la-api)
  - [Índice](#índice)
  - [1. Visión general](#1-visión-general)
    - [Conceptos clave](#conceptos-clave)
  - [2. Modelo de seguridad](#2-modelo-de-seguridad)
    - [Identificación del usuario](#identificación-del-usuario)
    - [Contraseña maestra (zero-knowledge)](#contraseña-maestra-zero-knowledge)
    - [Secretos compartidos (E2E)](#secretos-compartidos-e2e)
    - [Tokens de sesión](#tokens-de-sesión)
  - [3. Autenticación (JWT)](#3-autenticación-jwt)
  - [4. Endpoints](#4-endpoints)
    - [4.1. Health check](#41-health-check)
    - [4.2. Auth](#42-auth)
      - [4.2.1. Register](#421-register)
      - [4.2.2. Login](#422-login)
      - [4.2.3. Public key lookup](#423-public-key-lookup)
    - [4.3. Groups](#43-groups)
      - [4.3.1. Create group](#431-create-group)
      - [4.3.2. List my groups](#432-list-my-groups)
      - [4.3.3. Add member](#433-add-member)
      - [4.3.4. Get my SGK](#434-get-my-sgk)
    - [4.4. Passwords](#44-passwords)
      - [4.4.1. Create password](#441-create-password)
      - [4.4.2. List passwords](#442-list-passwords)
      - [4.4.3. Update password](#443-update-password)
      - [4.4.4. Delete password](#444-delete-password)
  - [5. Códigos de error comunes](#5-códigos-de-error-comunes)
  - [6. Flujo típico de uso](#6-flujo-típico-de-uso)
    - [Resumen del flujo](#resumen-del-flujo)
  - [7. Configuración del servidor](#7-configuración-del-servidor)
    - [Arranque con Docker](#arranque-con-docker)

---

## 1. Visión general

Vault Backend es una API REST construida con **FastAPI** y **PostgreSQL** (async vía SQLAlchemy + asyncpg). Implementa un sistema de gestión de contraseñas compartidas entre grupos de usuarios con **cifrado extremo a extremo (E2E)**.

El servidor **nunca ve** las contraseñas en claro de los grupos ni la contraseña maestra del usuario. Toda la criptografía de los secretos compartidos ocurre en el cliente. El servidor solo almacena blobs cifrados y hashes.

### Conceptos clave

| Concepto | Descripción |
|---|---|
| **user_id** | Número de teléfono del usuario. Se usa como identificador externo. |
| **id_lookup** | `HMAC-SHA256(user_id, PEPPER)` — valor determinista indexado para búsquedas O(1). |
| **id_hash** | `Argon2id(user_id + PEPPER)` — verificación defence-in-depth tras el lookup. |
| **password_hash** | `Argon2id(SHA256(password) + PEPPER2)` — hash lento de la contraseña maestra. El cliente envía el SHA-256; el servidor nunca ve la contraseña real. |
| **SGK** | Shared Group Key — clave simétrica (ej. AES-256) generada por el cliente para cada grupo. Se cifra con la clave pública de cada miembro antes de subirse al servidor. |
| **public_key** | Clave pública EC (P-256) del usuario. Se usa para cifrar la SGK para ese usuario mediante ECIES. |

---

## 2. Modelo de seguridad

### Identificación del usuario

1. El cliente envía el **número de teléfono** como `user_id`.
2. El servidor calcula `HMAC-SHA256(user_id, PEPPER)` → columna `id_lookup` (indexada, búsqueda O(1)).
3. Como defensa en profundidad, el servidor almacena además `Argon2id(user_id + PEPPER)` → columna `id_hash`. Tras encontrar el usuario por HMAC, verifica con Argon2 para descartar colisiones.

### Contraseña maestra (zero-knowledge)

1. El cliente calcula `SHA-256(contraseña)` y envía el digest de 64 caracteres hex como `password_hash`.
2. El servidor aplica `Argon2id(password_hash + PEPPER2)` y almacena el resultado.
3. El servidor **nunca ve la contraseña real** — solo recibe el hash SHA-256.
4. `PEPPER2` es un secreto interno del servidor, **diferente** de `PEPPER`.

### Secretos compartidos (E2E)

- El servidor **no tiene acceso** a las claves de cifrado de grupo (SGK) ni a las contraseñas compartidas. Solo almacena blobs cifrados.
- Cada miembro de un grupo recibe la SGK cifrada con su propia clave pública.
- Las contraseñas del grupo se cifran con la SGK (ej. AES-256-GCM) en el cliente.

### Tokens de sesión

- **JWT** firmado con HS256.
- El `sub` (subject) del token es el UUID interno del usuario (nunca expuesto como dato de entrada).
- Expiración configurable (por defecto 60 minutos).

---

## 3. Autenticación (JWT)

Todos los endpoints (excepto `/health`, `/auth/register` y `/auth/login`) requieren un token JWT en la cabecera:

```
Authorization: Bearer <access_token>
```

El token se obtiene mediante el endpoint de login.

---

## 4. Endpoints

### 4.1. Health check

| | |
|---|---|
| **Método** | `GET` |
| **Ruta** | `/health` |
| **Auth** | No |

**Respuesta** `200 OK`:

```json
{
  "status": "ok"
}
```

---

### 4.2. Auth

#### 4.2.1. Register

Registra un nuevo usuario en el sistema.

| | |
|---|---|
| **Método** | `POST` |
| **Ruta** | `/auth/register` |
| **Auth** | No |
| **Content-Type** | `application/json` |

**Request body:**

| Campo | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `user_id` | `string` | Sí | Número de teléfono del usuario (min. 1 carácter). |
| `password_hash` | `string` | Sí | SHA-256 hex-digest de la contraseña maestra (64 caracteres, calculado en el cliente). |
| `public_key` | `string` | Sí | Clave pública del usuario en formato PEM o base64 (min. 1 carácter). |

**Ejemplo:**

```json
{
  "user_id": "666111222",
  "password_hash": "2dd9f649063164ed47425c17ce5bd705c2273f877f8b89d2952958506ea3cdd9",
  "public_key": "-----BEGIN PUBLIC KEY-----\nMIIBIjAN...wQIDAQAB\n-----END PUBLIC KEY-----"
}
```

**Respuesta** `201 Created`:

```json
{
  "message": "User registered successfully"
}
```

**Errores:**

| Código | Detalle | Causa |
|---|---|---|
| `409 Conflict` | `"User already exists"` | Ya existe un usuario con ese número de teléfono. |
| `422 Unprocessable Entity` | Validación | Campos faltantes o con formato inválido. |

---

#### 4.2.2. Login

Autentica un usuario y devuelve un token JWT.

| | |
|---|---|
| **Método** | `POST` |
| **Ruta** | `/auth/login` |
| **Auth** | No |
| **Content-Type** | `application/json` |

**Request body:**

| Campo | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `user_id` | `string` | Sí | Número de teléfono del usuario. |
| `password_hash` | `string` | Sí | SHA-256 hex-digest de la contraseña maestra (64 caracteres). |

**Ejemplo:**

```json
{
  "user_id": "666111222",
  "password_hash": "2dd9f649063164ed47425c17ce5bd705c2273f877f8b89d2952958506ea3cdd9"
}
```

**Respuesta** `200 OK`:

```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "bearer"
}
```

**Errores:**

| Código | Detalle | Causa |
|---|---|---|
| `401 Unauthorized` | `"Invalid credentials"` | Usuario no encontrado o contraseña incorrecta. |

---

#### 4.2.3. Public key lookup

Obtiene la clave pública de otro usuario para poder cifrar la SGK para él.

| | |
|---|---|
| **Método** | `GET` |
| **Ruta** | `/auth/public-key/{user_id}` |
| **Auth** | Sí (Bearer token) |

**Path parameters:**

| Parámetro | Tipo | Descripción |
|---|---|---|
| `user_id` | `string` | Número de teléfono del usuario cuya clave pública se desea obtener. |

**Respuesta** `200 OK`:

```json
{
  "public_key": "-----BEGIN PUBLIC KEY-----\nMIIBIjAN...wQIDAQAB\n-----END PUBLIC KEY-----"
}
```

**Errores:**

| Código | Detalle | Causa |
|---|---|---|
| `401 Unauthorized` | `"Invalid or expired token"` | Token JWT inválido o expirado. |
| `404 Not Found` | `"User not found"` | No existe un usuario con ese número de teléfono. |

---

### 4.3. Groups

#### 4.3.1. Create group

Crea un nuevo grupo de contraseñas compartidas. El creador se añade automáticamente como primer miembro.

| | |
|---|---|
| **Método** | `POST` |
| **Ruta** | `/groups/` |
| **Auth** | Sí (Bearer token) |
| **Content-Type** | `application/json` |

**Request body:**

| Campo | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `name` | `string` | Sí | Nombre del grupo (1–255 caracteres). |
| `encrypted_sgk` | `string` | Sí | SGK cifrada con la clave pública del creador (base64). |

**Ejemplo:**

```json
{
  "name": "Equipo HackUDC",
  "encrypted_sgk": "base64-sgk-cifrada-con-mi-pubkey=="
}
```

**Respuesta** `201 Created`:

```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "name": "Equipo HackUDC",
  "owner_id": "11111111-2222-3333-4444-555555555555",
  "created_at": "2026-02-28T12:00:00+00:00"
}
```

---

#### 4.3.2. List my groups

Lista todos los grupos a los que pertenece el usuario autenticado, incluyendo su copia cifrada de la SGK.

| | |
|---|---|
| **Método** | `GET` |
| **Ruta** | `/groups/` |
| **Auth** | Sí (Bearer token) |

**Respuesta** `200 OK`:

```json
[
  {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "name": "Equipo HackUDC",
    "owner_id": "11111111-2222-3333-4444-555555555555",
    "created_at": "2026-02-28T12:00:00+00:00",
    "encrypted_sgk": "base64-sgk-cifrada-con-mi-pubkey=="
  }
]
```

> Devuelve una lista vacía `[]` si el usuario no pertenece a ningún grupo.

---

#### 4.3.3. Add member

Añade un usuario a un grupo. El invocante debe ser miembro del grupo y proporcionar la SGK cifrada con la clave pública del nuevo miembro.

| | |
|---|---|
| **Método** | `POST` |
| **Ruta** | `/groups/{group_id}/members` |
| **Auth** | Sí (Bearer token) |
| **Content-Type** | `application/json` |

**Path parameters:**

| Parámetro | Tipo | Descripción |
|---|---|---|
| `group_id` | `UUID` | ID del grupo. |

**Request body:**

| Campo | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `user_id` | `string` | Sí | Número de teléfono del usuario a añadir. |
| `encrypted_sgk` | `string` | Sí | SGK cifrada con la clave pública del nuevo miembro (base64). |

**Ejemplo:**

```json
{
  "user_id": "666333444",
  "encrypted_sgk": "base64-sgk-cifrada-con-pubkey-del-nuevo-miembro=="
}
```

**Respuesta** `201 Created`:

```json
{
  "user_id": "22222222-3333-4444-5555-666666666666",
  "joined_at": "2026-02-28T12:05:00+00:00"
}
```

> Nota: el `user_id` en la respuesta es el UUID interno del usuario añadido, no su número de teléfono.

**Errores:**

| Código | Detalle | Causa |
|---|---|---|
| `403 Forbidden` | `"Not a member of this group"` | El solicitante no es miembro del grupo. |
| `404 Not Found` | `"Group not found"` | El grupo no existe. |
| `404 Not Found` | `"Target user not found"` | No se encontró un usuario con ese número de teléfono. |
| `409 Conflict` | `"User is already a member"` | El usuario ya es miembro del grupo. |

---

#### 4.3.4. Get my SGK

Recupera la SGK del grupo cifrada con la clave pública del usuario autenticado.

| | |
|---|---|
| **Método** | `GET` |
| **Ruta** | `/groups/{group_id}/sgk` |
| **Auth** | Sí (Bearer token) |

**Path parameters:**

| Parámetro | Tipo | Descripción |
|---|---|---|
| `group_id` | `UUID` | ID del grupo. |

**Respuesta** `200 OK`:

```json
{
  "encrypted_sgk": "base64-sgk-cifrada-con-mi-pubkey=="
}
```

**Errores:**

| Código | Detalle | Causa |
|---|---|---|
| `403 Forbidden` | `"Not a member of this group"` | El usuario no es miembro del grupo. |

---

### 4.4. Passwords

#### 4.4.1. Create password

Sube una entrada de contraseña cifrada con la SGK del grupo.

| | |
|---|---|
| **Método** | `POST` |
| **Ruta** | `/groups/{group_id}/passwords` |
| **Auth** | Sí (Bearer token) |
| **Content-Type** | `application/json` |

**Path parameters:**

| Parámetro | Tipo | Descripción |
|---|---|---|
| `group_id` | `UUID` | ID del grupo. |

**Request body:**

| Campo | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `label` | `string` | Sí | Nombre/etiqueta de la contraseña (1–512 caracteres). Puede ser texto plano o cifrado, a decisión del cliente. |
| `encrypted_data` | `string` | Sí | Datos de la contraseña cifrados con la SGK del grupo (base64). |

**Ejemplo:**

```json
{
  "label": "GitHub - cuenta del equipo",
  "encrypted_data": "aes-256-gcm-cifrado-con-sgk-base64=="
}
```

**Respuesta** `201 Created`:

```json
{
  "id": "aaaabbbb-cccc-dddd-eeee-ffffffffffff",
  "group_id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "created_by": "11111111-2222-3333-4444-555555555555",
  "label": "GitHub - cuenta del equipo",
  "encrypted_data": "aes-256-gcm-cifrado-con-sgk-base64==",
  "created_at": "2026-02-28T12:10:00+00:00",
  "updated_at": "2026-02-28T12:10:00+00:00"
}
```

**Errores:**

| Código | Detalle | Causa |
|---|---|---|
| `403 Forbidden` | `"Not a member of this group"` | El usuario no es miembro del grupo. |
| `404 Not Found` | `"Group not found"` | El grupo no existe. |

---

#### 4.4.2. List passwords

Lista todas las contraseñas de un grupo (cifradas). El cliente debe descifrarlas localmente con la SGK.

| | |
|---|---|
| **Método** | `GET` |
| **Ruta** | `/groups/{group_id}/passwords` |
| **Auth** | Sí (Bearer token) |

**Path parameters:**

| Parámetro | Tipo | Descripción |
|---|---|---|
| `group_id` | `UUID` | ID del grupo. |

**Respuesta** `200 OK`:

```json
[
  {
    "id": "aaaabbbb-cccc-dddd-eeee-ffffffffffff",
    "group_id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "created_by": "11111111-2222-3333-4444-555555555555",
    "label": "GitHub - cuenta del equipo",
    "encrypted_data": "aes-256-gcm-cifrado-con-sgk-base64==",
    "created_at": "2026-02-28T12:10:00+00:00",
    "updated_at": "2026-02-28T12:10:00+00:00"
  }
]
```

> Devuelve una lista vacía `[]` si no hay contraseñas en el grupo.

**Errores:**

| Código | Detalle | Causa |
|---|---|---|
| `403 Forbidden` | `"Not a member of this group"` | El usuario no es miembro del grupo. |
| `404 Not Found` | `"Group not found"` | El grupo no existe. |

---

#### 4.4.3. Update password

Actualiza la etiqueta y/o los datos cifrados de una contraseña existente.

| | |
|---|---|
| **Método** | `PATCH` |
| **Ruta** | `/groups/{group_id}/passwords/{password_id}` |
| **Auth** | Sí (Bearer token) |
| **Content-Type** | `application/json` |

**Path parameters:**

| Parámetro | Tipo | Descripción |
|---|---|---|
| `group_id` | `UUID` | ID del grupo. |
| `password_id` | `UUID` | ID de la contraseña. |

**Request body** (todos los campos son opcionales; se actualiza solo lo enviado):

| Campo | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `label` | `string` | No | Nueva etiqueta. |
| `encrypted_data` | `string` | No | Nuevos datos cifrados. |

**Ejemplo:**

```json
{
  "label": "GitHub - cuenta equipo (actualizada)",
  "encrypted_data": "aes-256-gcm-nuevo-cifrado-base64=="
}
```

**Respuesta** `200 OK`:

```json
{
  "id": "aaaabbbb-cccc-dddd-eeee-ffffffffffff",
  "group_id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "created_by": "11111111-2222-3333-4444-555555555555",
  "label": "GitHub - cuenta equipo (actualizada)",
  "encrypted_data": "aes-256-gcm-nuevo-cifrado-base64==",
  "created_at": "2026-02-28T12:10:00+00:00",
  "updated_at": "2026-02-28T12:15:00+00:00"
}
```

**Errores:**

| Código | Detalle | Causa |
|---|---|---|
| `403 Forbidden` | `"Not a member of this group"` | El usuario no es miembro del grupo. |
| `404 Not Found` | `"Group not found"` | El grupo no existe. |
| `404 Not Found` | `"Password entry not found"` | La contraseña no existe o no pertenece a ese grupo. |

---

#### 4.4.4. Delete password

Elimina una contraseña del grupo.

| | |
|---|---|
| **Método** | `DELETE` |
| **Ruta** | `/groups/{group_id}/passwords/{password_id}` |
| **Auth** | Sí (Bearer token) |

**Path parameters:**

| Parámetro | Tipo | Descripción |
|---|---|---|
| `group_id` | `UUID` | ID del grupo. |
| `password_id` | `UUID` | ID de la contraseña. |

**Respuesta** `204 No Content` (sin cuerpo).

**Errores:**

| Código | Detalle | Causa |
|---|---|---|
| `403 Forbidden` | `"Not a member of this group"` | El usuario no es miembro del grupo. |
| `404 Not Found` | `"Group not found"` | El grupo no existe. |
| `404 Not Found` | `"Password entry not found"` | La contraseña no existe o no pertenece a ese grupo. |

---

## 5. Códigos de error comunes

Todos los errores se devuelven con el siguiente formato JSON:

```json
{
  "detail": "Mensaje descriptivo del error"
}
```

| Código | Significado |
|---|---|
| `401 Unauthorized` | Token JWT ausente, inválido o expirado / Credenciales incorrectas. |
| `403 Forbidden` | El usuario no tiene permisos (no es miembro del grupo). |
| `404 Not Found` | El recurso solicitado no existe. |
| `409 Conflict` | Conflicto de unicidad (usuario ya existe, miembro ya añadido). |
| `422 Unprocessable Entity` | Error de validación en el body (campos faltantes, tipos incorrectos). |

---

## 6. Flujo típico de uso

```
┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│   Usuario A  │       │    Servidor  │       │   Usuario B  │
└──────┬───────┘       └──────┬───────┘       └──────┬───────┘
       │                      │                      │
       │ 1. POST /auth/register                      │
       │   { user_id: "teléfono_A",                  │
       │     password_hash: "SHA256(...)",              │
       │     public_key: "..." }                     │
       │─────────────────────►│                      │
       │◄─────────────────────│                      │
       │                      │                      │
       │                      │  2. POST /auth/register
       │                      │◄─────────────────────│
       │                      │─────────────────────►│
       │                      │                      │
       │ 3. POST /auth/login  │                      │
       │─────────────────────►│                      │
       │◄── { access_token }──│                      │
       │                      │                      │
       │ 4. POST /groups/     │                      │
       │   { name, encrypted_sgk }                   │
       │─────────────────────►│                      │
       │◄── { id: GROUP_ID }──│                      │
       │                      │                      │
       │ 5. GET /auth/public-key/teléfono_B          │
       │─────────────────────►│                      │
       │◄── { public_key_B }──│                      │
       │                      │                      │
       │ 6. [Cliente A cifra SGK con public_key_B]   │
       │                      │                      │
       │ 7. POST /groups/{id}/members                │
       │   { user_id: "teléfono_B",                  │
       │     encrypted_sgk: "..." }                  │
       │─────────────────────►│                      │
       │◄─── 201 Created ────│                      │
       │                      │                      │
       │ 8. POST /groups/{id}/passwords              │
       │   { label, encrypted_data }                 │
       │─────────────────────►│                      │
       │◄─── 201 Created ────│                      │
       │                      │                      │
       │                      │ 9. POST /auth/login  │
       │                      │◄─────────────────────│
       │                      │── { access_token } ─►│
       │                      │                      │
       │                      │ 10. GET /groups/{id}/sgk
       │                      │◄─────────────────────│
       │                      │── { encrypted_sgk } ►│
       │                      │                      │
       │                      │ 11. [B descifra SGK  │
       │                      │      con su privkey] │
       │                      │                      │
       │                      │ 12. GET /groups/{id}/passwords
       │                      │◄─────────────────────│
       │                      │── [{ encrypted_data }]►│
       │                      │                      │
       │                      │ 13. [B descifra cada │
       │                      │      password con SGK]│
```

### Resumen del flujo

1. **Registro:** cada usuario envía su teléfono, el SHA-256 de su contraseña y su clave pública. El servidor nunca ve la contraseña real.
2. **Login:** el usuario se autentica y recibe un JWT.
3. **Crear grupo:** el usuario genera una SGK (clave AES), la cifra con su propia clave pública y crea el grupo.
4. **Añadir miembro:** se obtiene la clave pública del nuevo miembro, se cifra la SGK con ella y se sube.
5. **Compartir contraseña:** se cifra la contraseña con la SGK y se sube al grupo.
6. **Leer contraseña:** el miembro obtiene su copia de la SGK, la descifra con su clave privada, y luego descifra las contraseñas del grupo.

---

## 7. Configuración del servidor

Variables de entorno (archivo `.env`):

| Variable | Descripción | Valor por defecto |
|---|---|---|
| `DATABASE_URL` | URL de conexión a PostgreSQL (asyncpg) | `postgresql+asyncpg://vault:vault_secret@localhost:5432/vault_db` |
| `SECRET_KEY` | Clave secreta para firmar tokens JWT | `change-me` |
| `PEPPER` | Secreto del servidor usado para HMAC del user ID y Argon2 del user ID | `change-me` |
| `PEPPER2` | Secreto del servidor usado para Argon2 de la contraseña. **Debe ser diferente de PEPPER.** | `change-me-too` |
| `JWT_ALGORITHM` | Algoritmo de firma JWT | `HS256` |
| `ACCESS_TOKEN_EXPIRE_MINUTES` | Tiempo de expiración del token JWT (minutos) | `60` |
| `RANDOM_ORG_API_KEY` | API key de random.org (opcional) | `""` |

> **Importante:** En producción, cambia `SECRET_KEY`, `PEPPER` y `PEPPER2` a valores aleatorios largos y seguros. `PEPPER` y `PEPPER2` **deben ser diferentes entre sí**.

### Arranque con Docker

```bash
docker compose down -v && docker compose up --build
```

Las tablas se crean automáticamente al iniciar la aplicación.
