# Password Manager Android App 🔐

A secure, modern password manager built with Kotlin and Jetpack Compose for Android.

## 🚀 Features

- **🔒 AES-256-GCM Encryption** - Military-grade security for all passwords
- **📱 Modern UI** - Clean Material 3 design with Jetpack Compose
- **🔍 Search Functionality** - Quickly find passwords with real-time search
- **🎲 Password Generator** - Create strong, customizable passwords
- **📝 Complete CRUD** - Add, view, edit, and delete password entries
- **🏗️ Clean Architecture** - MVVM pattern with Repository design
- **💾 Local Storage** - Secure offline storage with Room database
- **🎨 Dark/Light Theme** - Automatic system theme support

## 📋 Requirements

- **Android Studio** Giraffe (2022.3.1) or newer
- **Minimum SDK** 24 (Android 7.0)
- **Target SDK** 34 (Android 14)
- **Kotlin** 1.9.0+

## 🛠️ Tech Stack

### Core Technologies
- **Kotlin** - Primary programming language
- **Jetpack Compose** - Modern Android UI toolkit
- **Room Database** - Local data persistence
- **Navigation Component** - In-app navigation
- **ViewModel & LiveData** - MVVM architecture

### Security
- **EncryptedSharedPreferences** - Secure key storage
- **AES-256-GCM** - Symmetric encryption
- **SecureRandom** - Cryptographically secure random generation

### Dependencies
```kotlin
// Core Android
androidx.core:core-ktx:1.12.0
androidx.lifecycle:lifecycle-runtime-ktx:2.7.0
androidx.activity:activity-compose:1.8.2

// Jetpack Compose
androidx.compose.ui:ui:1.5.8
androidx.compose.material3:material3:1.1.2
androidx.navigation:navigation-compose:2.7.6

// Room Database
androidx.room:room-runtime:2.6.1
androidx.room:room-ktx:2.6.1

// Security
androidx.security:security-crypto:1.1.0-alpha06
androidx.biometric:biometric:1.1.0
```

## 📁 Project Structure

```
app/src/main/java/com/yourname/passwordmanager/
├── data/
│   ├── dao/
│   │   └── PasswordDao.kt           # Database access object
│   ├── database/
│   │   └── PasswordDatabase.kt      # Room database setup
│   └── model/
│       └── PasswordEntry.kt         # Data model
├── navigation/
│   └── Navigation.kt                # App navigation
├── repository/
│   └── PasswordRepository.kt        # Data repository
├── security/
│   └── CryptoManager.kt            # Encryption/decryption
├── ui/
│   ├── component/
│   │   └── PasswordItem.kt         # Reusable UI components
│   ├── screen/
│   │   ├── AddEditPasswordScreen.kt # Add/edit password screen
│   │   └── MainScreen.kt           # Main password list screen
│   ├── theme/
│   │   ├── Color.kt                # App colors
│   │   ├── Theme.kt                # Material theme
│   │   └── Type.kt                 # Typography
│   └── viewmodel/
│       └── PasswordViewModel.kt    # UI state management
└── MainActivity.kt                  # Entry point
```

## 💡 How to Use

### Adding a Password
1. Tap the "+" floating action button
2. Fill in the required fields (Title, Username, Password)
3. Optionally add website and notes
4. Tap "Save"

### Generating Secure Passwords
1. In the add/edit screen, tap the refresh icon next to password field
2. Customize length and character types
3. Tap "Generate" to create a secure password

### Searching Passwords
1. Use the search bar on the main screen
2. Search by title, username, or website
3. Results update in real-time

### Editing/Deleting
1. Tap on any password entry
2. Edit the information as needed
3. Tap "Update" to save changes
4. Use "Delete" button to remove entry

## 🔒 Security Features

### Encryption
- **AES-256-GCM encryption** for all passwords
- **Unique encryption keys** per app installation
- **Encrypted SharedPreferences** for key storage
- **No plaintext storage** anywhere in the app

### Best Practices
- Passwords are encrypted before database storage
- Encryption keys are generated using Android Keystore
- Secure random password generation
- Memory is cleared after use

## 🧪 Testing

### Running Tests
```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest
```

### Test Coverage
The project includes:
- Unit tests for ViewModels
- Repository tests with mock data
- Encryption/decryption tests
- UI tests for critical user flows

## 🚀 Deployment

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build
```bash
./gradlew assembleRelease
```

### Signing Configuration
For release builds, add signing configuration to `build.gradle.kts`:
```kotlin
signingConfigs {
    create("release") {
        keyAlias = "your-key-alias"
        keyPassword = "your-key-password"
        storeFile = file("path/to/your/keystore.jks")
        storePassword = "your-store-password"
    }
}
```

## 🔧 Troubleshooting

### Common Issues

**Gradle Sync Failed**
- Check internet connection
- Try "File" → "Sync Project with Gradle Files"
- Clear cache: "File" → "Invalidate Caches and Restart"

**Encryption Errors**
- Ensure target device supports the required encryption
- Check Android version compatibility (minimum API 24)

**Room Database Issues**
- Clear app data if schema changes
- Check entity annotations are correct

### Getting Help
1. Check Android Studio's error messages
2. Review logcat for runtime issues
3. Verify all dependencies are properly imported
4. Ensure proper package names throughout the project

## 📚 Learning Resources

- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Room Database Guide](https://developer.android.com/training/data-storage/room)
- [Android Security Best Practices](https://developer.android.com/topic/security/best-practices)
- [Material Design 3](https://m3.material.io/)

## 🎯 Project Objectives Met

✅ **Modular Design** - Clean separation of concerns  
✅ **Modern Architecture** - MVVM with Repository pattern  
✅ **Security Implementation** - Industry-standard encryption  
✅ **User Experience** - Intuitive Material 3 design  
✅ **Data Persistence** - Robust local database storage  
✅ **Error Handling** - Comprehensive exception management  
✅ **Code Quality** - Well-documented, maintainable code  


**Happy Coding! 🚀**