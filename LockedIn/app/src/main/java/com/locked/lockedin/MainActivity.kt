package com.locked.lockedin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.locked.lockedin.data.database.PasswordDatabase
import com.locked.lockedin.navigation.PasswordManagerNavigation
import com.locked.lockedin.repository.PasswordRepository
import com.locked.lockedin.security.CryptoManager
import com.locked.lockedin.security.MasterKeyManager
import com.locked.lockedin.ui.theme.PasswordManagerTheme
import com.locked.lockedin.ui.viewmodel.PasswordViewModel
import com.locked.lockedin.ui.viewmodel.PasswordViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PasswordManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = applicationContext

                    // Security
                    val masterKeyManager = remember { MasterKeyManager(context) }
                    val cryptoManager    = remember { CryptoManager() }

                    // Database & repository
                    val database   = remember { PasswordDatabase.getDatabase(context) }
                    val repository = remember { PasswordRepository(database.passwordDao(), cryptoManager) }

                    // ViewModel
                    val passwordViewModel: PasswordViewModel = viewModel(
                        factory = PasswordViewModelFactory(repository)
                    )

                    val navController = rememberNavController()

                    PasswordManagerNavigation(
                        navController    = navController,
                        masterKeyManager = masterKeyManager,
                        passwordViewModel = passwordViewModel
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Optional: lock the vault when the app goes to the background.
        // Remove this if you want the vault to stay unlocked while the app is in the recents.
        // VaultKeyHolder.clearKey()
    }
}