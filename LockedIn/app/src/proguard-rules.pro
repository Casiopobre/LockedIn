# Keep Room entities and DAOs
-keep class com.yourname.passwordmanager.data.model.** { *; }
-keep class com.yourname.passwordmanager.data.dao.** { *; }

# Keep crypto classes
-keep class com.yourname.passwordmanager.security.** { *; }

# Keep androidx security
-keep class androidx.security.crypto.** { *; }