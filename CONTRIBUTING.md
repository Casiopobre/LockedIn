# Contributing to LockedIn

Thank you for your interest in contributing to **LockedIn**! This document provides guidelines and instructions to make the contribution process smooth for everyone.

---

## Table of Contents

- [Contributing to LockedIn](#contributing-to-lockedin)
	- [Table of Contents](#table-of-contents)
	- [Code of Conduct](#code-of-conduct)
	- [How Can I Contribute?](#how-can-i-contribute)
		- [Reporting Bugs](#reporting-bugs)
		- [Suggesting Features](#suggesting-features)
		- [Submitting Changes](#submitting-changes)
	- [Development Setup](#development-setup)
		- [Backend](#backend)
		- [Android App](#android-app)
	- [Coding Guidelines](#coding-guidelines)
		- [General](#general)
		- [Python (Backend)](#python-backend)
		- [Kotlin (Android)](#kotlin-android)
		- [Security](#security)
	- [Commit Messages](#commit-messages)
	- [Pull Request Process](#pull-request-process)

---

## Code of Conduct

This project adheres to a [Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code. Please report unacceptable behaviour by opening an issue.

---

## How Can I Contribute?

### Reporting Bugs

If you find a bug, please open an issue with the following information:

1. **Summary** — A clear and descriptive title.
2. **Steps to reproduce** — Detailed steps to reproduce the behaviour.
3. **Expected behaviour** — What you expected to happen.
4. **Actual behaviour** — What actually happened.
5. **Environment** — OS, Android version, device model, backend version, etc.
6. **Screenshots / Logs** — If applicable.

### Suggesting Features

Feature requests are welcome. Please open an issue and include:

- A clear description of the feature and the problem it solves.
- Any alternative solutions you have considered.
- Mockups or diagrams if relevant.

### Submitting Changes

1. Fork the repository.
2. Create a feature branch from `main`:
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. Make your changes (see [Coding Guidelines](#coding-guidelines)).
4. Commit your changes (see [Commit Messages](#commit-messages)).
5. Push to your fork and open a Pull Request.

---

## Development Setup

### Backend

**Requirements:** Python 3.11+, Docker & Docker Compose.

```bash
cd backend

# Option A: Run with Docker (recommended)
docker compose up -d

# Option B: Run locally
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload
```

Create a `.env` file with the required secrets before starting the server. See `backend/README.md` for the full list of environment variables.

### Android App

**Requirements:** Android Studio Giraffe (2022.3.1) or newer.

1. Open the `LockedIn/` directory in Android Studio.
2. Let Gradle sync the dependencies.
3. Run on an emulator or connected device (min SDK 24).

---

## Coding Guidelines

### General

- Write clear, self-documenting code. Add comments only where the *why* is not obvious.
- Keep functions and classes small and focused.
- Follow existing patterns and conventions already present in the codebase.

### Python (Backend)

- Follow [PEP 8](https://peps.python.org/pep-0008/) style.
- Use type hints for function signatures.
- Use `async`/`await` for all I/O-bound operations.
- Add docstrings to public functions and classes.

### Kotlin (Android)

- Follow the [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html).
- Use Jetpack Compose for all new UI work.
- Follow the existing MVVM architecture with repositories.

### Security

- **Never** log or print secrets, keys, or passwords.
- All cryptographic operations must happen on the client. The server must remain zero-knowledge.
- Use well-established libraries for cryptography — do not roll your own.

---

## Commit Messages

Use clear, descriptive commit messages. We recommend the [Conventional Commits](https://www.conventionalcommits.org/) format:

```
<type>(<scope>): <short summary>

<optional body>
```

**Types:** `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`, `ci`.

**Examples:**

```
feat(backend): add password update endpoint
fix(android): fix crash on empty vault screen
docs: update README with deployment instructions
```

---

## Pull Request Process

1. Ensure your branch is up to date with `main`.
2. Verify that the backend tests pass:
   ```bash
   cd backend
   bash test_vault.sh
   ```
3. Verify that the Android app builds successfully:
   ```bash
   cd LockedIn
   ./gradlew assembleDebug
   ```
4. Fill in the PR template with a description of your changes and any related issues.
5. Request a review from at least one maintainer.
6. Once approved and CI passes, a maintainer will merge your PR.

---

Thank you for helping make LockedIn better!
