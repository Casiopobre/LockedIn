package com.locked.lockedin

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.locked.lockedin.data.database.PasswordDatabase
import com.locked.lockedin.navigation.PasswordManagerNavigation
import com.locked.lockedin.network.SessionManager
import com.locked.lockedin.repository.PasswordRepository
import com.locked.lockedin.repository.VaultRepository
import com.locked.lockedin.security.BiometricKeyManager
import com.locked.lockedin.security.CryptoManager
import com.locked.lockedin.security.MasterKeyManager
import com.locked.lockedin.security.PwnedCheckManager
import com.locked.lockedin.ui.theme.LockedInTheme
import com.locked.lockedin.security.EcKeyManager
import com.locked.lockedin.ui.viewmodel.GroupViewModel
import com.locked.lockedin.ui.viewmodel.GroupViewModelFactory
import com.locked.lockedin.ui.viewmodel.PasswordViewModel
import com.locked.lockedin.ui.viewmodel.PasswordViewModelFactory
import com.locked.lockedin.ui.viewmodel.SettingsViewModel
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

class MainActivity : FragmentActivity() {

    // Instancia el ViewModel a nivel de clase, no dentro de onCreate
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Ahora sí puedes llamar los métodos de instancia
        settingsViewModel.applyCurrentTheme()
        settingsViewModel.applyCurrentLanguage()

        super.onCreate(savedInstanceState)

        setContent {
            val uiState by settingsViewModel.uiState.collectAsState()

            LockedInTheme(appTheme = uiState.theme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = MaterialTheme.colorScheme.background
                ) {
                    val context = applicationContext

                    val masterKeyManager    = remember { MasterKeyManager(context) }
                    val cryptoManager       = remember { CryptoManager() }
                    val biometricKeyManager = remember { BiometricKeyManager(context) }
                    val pwnedCheckManager   = remember { PwnedCheckManager(context) }
                    val ecKeyManager        = remember { EcKeyManager(context) }
                    val sessionManager      = remember { SessionManager(context) }

                    // Database & repositories
                    val database   = remember { PasswordDatabase.getDatabase(context) }
                    val repository = remember { PasswordRepository(database.passwordDao(), cryptoManager) }
                    val vaultRepository = remember { VaultRepository(sessionManager, ecKeyManager) }

                    val passwordViewModel: PasswordViewModel = viewModel(
                        factory = PasswordViewModelFactory(repository, pwnedCheckManager)
                    )
                    val groupViewModel: GroupViewModel = viewModel(
                        factory = GroupViewModelFactory(vaultRepository)
                    )

                    val navController = rememberNavController()

                    PasswordManagerNavigation(
                        navController       = navController,
                        masterKeyManager    = masterKeyManager,
                        biometricKeyManager = biometricKeyManager,
                        passwordViewModel   = passwordViewModel,
                        settingsViewModel    = settingsViewModel,
                        groupViewModel      = groupViewModel,
                        vaultRepository     = vaultRepository,
                        activity            = this@MainActivity
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Uncomment to lock vault when app backgrounds:
        // VaultKeyHolder.clearKey()
    }
}
