package com.locked.lockedin.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.locked.lockedin.security.MasterKeyManager
import com.locked.lockedin.security.VaultKeyHolder
import com.locked.lockedin.ui.screen.AddEditPasswordScreen
import com.locked.lockedin.ui.screen.MainScreen
import com.locked.lockedin.ui.screen.SetupScreen
import com.locked.lockedin.ui.screen.UnlockScreen
import com.locked.lockedin.ui.viewmodel.PasswordViewModel
import com.locked.lockedin.ui.viewmodel.SetupViewModel
import com.locked.lockedin.ui.viewmodel.UnlockViewModel
import androidx.compose.runtime.LaunchedEffect

object NavigationRoutes {
    const val SETUP   = "setup"          // First-time master key creation
    const val UNLOCK  = "unlock"         // Unlock vault on subsequent launches
    const val MAIN    = "main"           // Password list
    const val ADD_PASSWORD = "add_password"
    const val EDIT_PASSWORD = "edit_password/{passwordId}"

    fun editPassword(passwordId: Long) = "edit_password/$passwordId"
}

/**
 * Root navigation graph.
 *
 * Start destination is decided at runtime:
 *  - No master key set  → SETUP
 *  - Master key set but vault locked → UNLOCK
 *  - Vault already unlocked (e.g. config change) → MAIN
 */
@Composable
fun PasswordManagerNavigation(
    navController: NavHostController,
    masterKeyManager: MasterKeyManager,
    passwordViewModel: PasswordViewModel,
    modifier: Modifier = Modifier
) {
    // Choose where to start
    val startDestination = when {
        !masterKeyManager.isMasterKeySet() -> NavigationRoutes.SETUP
        VaultKeyHolder.isUnlocked          -> NavigationRoutes.MAIN
        else                               -> NavigationRoutes.UNLOCK
    }

    // Shared callback: derive key → cache in VaultKeyHolder
    val onKeyDerived: (String) -> Unit = remember(masterKeyManager) { { masterKey ->
        val derivedKey = masterKeyManager.deriveEncryptionKey(masterKey)
        VaultKeyHolder.setKey(derivedKey)
    }}

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {

        // ── Setup (first launch) ──────────────────────────────────────────
        composable(NavigationRoutes.SETUP) {
            val setupViewModel = remember {
                SetupViewModel(
                    masterKeyManager = masterKeyManager,
                    onKeyDerived = onKeyDerived
                )
            }
            SetupScreen(
                viewModel = setupViewModel,
                onSetupComplete = {
                    navController.navigate(NavigationRoutes.MAIN) {
                        popUpTo(NavigationRoutes.SETUP) { inclusive = true }
                    }
                }
            )
        }

        // ── Unlock (subsequent launches) ─────────────────────────────────
        composable(NavigationRoutes.UNLOCK) {
            val unlockViewModel = remember {
                UnlockViewModel(
                    masterKeyManager = masterKeyManager,
                    onKeyDerived = onKeyDerived
                )
            }
            UnlockScreen(
                viewModel = unlockViewModel,
                onUnlockSuccess = {
                    navController.navigate(NavigationRoutes.MAIN) {
                        popUpTo(NavigationRoutes.UNLOCK) { inclusive = true }
                    }
                }
            )
        }

        // ── Main password list ────────────────────────────────────────────
        composable(NavigationRoutes.MAIN) {
            MainScreen(
                viewModel = passwordViewModel,
                onAddPasswordClick = {
                    navController.navigate(NavigationRoutes.ADD_PASSWORD)
                },
                onPasswordClick = { password ->
                    passwordViewModel.selectPassword(password)
                    navController.navigate(NavigationRoutes.editPassword(password.id))
                }
            )
        }

        // ── Add password ──────────────────────────────────────────────────
        composable(NavigationRoutes.ADD_PASSWORD) {
            AddEditPasswordScreen(
                viewModel = passwordViewModel,
                passwordEntry = null,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Edit password ─────────────────────────────────────────────────
        composable(
            route = NavigationRoutes.EDIT_PASSWORD,
            arguments = listOf(navArgument("passwordId") { type = NavType.LongType })
        ) { backStackEntry ->
            val passwordId = backStackEntry.arguments?.getLong("passwordId") ?: 0L
            val selectedPassword = passwordViewModel.selectedPassword.collectAsState().value

            if (selectedPassword != null && selectedPassword.id == passwordId) {
                AddEditPasswordScreen(
                    viewModel = passwordViewModel,
                    passwordEntry = selectedPassword,
                    onNavigateBack = {
                        passwordViewModel.clearSelectedPassword()
                        navController.popBackStack()
                    }
                )
            } else {
                LaunchedEffect(passwordId) {
                    passwordViewModel.getPasswordById(passwordId) { password ->
                        if (password != null) passwordViewModel.selectPassword(password)
                        else navController.popBackStack()
                    }
                }
                selectedPassword?.let { password ->
                    AddEditPasswordScreen(
                        viewModel = passwordViewModel,
                        passwordEntry = password,
                        onNavigateBack = {
                            passwordViewModel.clearSelectedPassword()
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}