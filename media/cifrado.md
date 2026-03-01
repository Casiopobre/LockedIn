## System Architecture: Secure Password Sharing

This application allows users to share sensitive credentials (like a Netflix account) using a combination of **Asymmetric Encryption** for key exchange and **Symmetric Encryption** for data storage.

### 1. User Registration & Authentication

When a user signs up, the process is designed to be transparent. While the user only provides their phone number, several background operations occur:

* **Key Generation:** The app generates an ECDH **Public-Private key pair**.
* The **Private Key** is stored exclusively in the device's secure enclave.
* The **Public Key** is sent to the server.


* **Identity Indexing:** The phone number is sent to the server, which hashes it using **HMAC-SHA-256** with a dedicated **pepper**. This allows for $O(1)$ user lookups without storing the phone number in plain text.
* **Password Hashing:** 1.  **Client-side:** The user's password is hashed via **SHA-256** before transmission.
2.  **Server-side:** The server receives the hash and applies **Argon2id** (with a unique salt and a secondary pepper). This ensures that even if the database is breached, the passwords remain computationally expensive to crack.

---

### 2. Local Storage (Offline Mode)

The app doubles as an offline password manager. Security is handled locally to ensure the user can access their data without an internet connection.

* **Master Key:** A unique key is generated using `SecureRandom` and stored via `EncryptedSharedPreferences`.
* **Encryption Standard:** Local data is encrypted using **AES-256-GCM**, providing both confidentiality and data integrity.
* **Zero-Knowledge:** The Master Key never leaves the device.

---

### 3. Group Sharing & The SGK (Online Mode)

Sharing a password involves a **Symmetric Group Key (SGK)**. This allows multiple users to access the same encrypted data.

**The Workflow:**

1. **Creation:** The group creator generates a random **SGK** locally.
2. **Redundancy:** The creator encrypts the SGK with their own public key and uploads it (as you noted, this is a bit redundant but acts as a backup for the creator's other devices).
3. **Invitation:** To add a new member:
* The app fetches the new member's **Public Key** from the server.
* The SGK is encrypted with that Public Key.
* The encrypted SGK is uploaded to the server.


4. **Access:** The new member downloads the encrypted SGK and decrypts it using their **Private Key**.
5. **Synchronization:** Once all members have the SGK in plain text (locally), all group passwords are encrypted/decrypted using **AES** with that shared SGK.

---

### 4. Seamless Transition

The app handles the switch from **Offline Vault** to **Online Groups** without bothering the user. Since the "opaque" password (the SHA-256 hash) is stored securely or managed via a session token, the user doesn't have to re-login to access shared features.
