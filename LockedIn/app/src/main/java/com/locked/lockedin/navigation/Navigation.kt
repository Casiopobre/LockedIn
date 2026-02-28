package com.locked.lockedin.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.locked.lockedin.security.MasterKeyManager
import com.locked.lockedin.security.VaultKeyHolder
import com.locked.lockedin.ui.screen.AddEditPasswordScreen
import com.locked.lockedin.ui.screen.GroupsScreen
import com.locked.lockedin.ui.screen.MainScreen
import com.locked.lockedin.ui.screen.SettingsScreen
import com.locked.lockedin.ui.screen.SetupScreen
import com.locked.lockedin.ui.screen.UnlockScreen
import com.locked.lockedin.ui.viewmodel.PasswordViewModel
import com.locked.lockedin.ui.viewmodel.SetupViewModel
import com.locked.lockedin.ui.viewmodel.UnlockViewModel

object NavigationRoutes {
    const val SETUP = "setup"
    const val UNLOCK = "unlock"
    const val MAIN = "my_passwords"
    const val GROUPS = "my_groups"
    const val SETTINGS = "settings"
    const val ADD_PASSWORD = "add_password"
    const val EDIT_PASSWORD = "edit_password/{passwordId}"

    fun editPassword(passwordId: Long) = "edit_password/$passwordId"
}

enum class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val contentDescription: String
) {
    GROUPS(NavigationRoutes.GROUPS, "My groups", Icons.Default.Person, "My groups"),
    PASSWORDS(NavigationRoutes.MAIN, "My passwords", Icons.Default.Lock, "My passwords"),
    SETTINGS(NavigationRoutes.SETTINGS, "Settings", Icons.Default.Settings, "Settings")
}

@Composable
fun PasswordManagerNavigation(
    navController: NavHostController,
    masterKeyManager: MasterKeyManager,
    passwordViewModel: PasswordViewModel,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val mainDestinations = listOf(
        NavigationRoutes.MAIN,
        NavigationRoutes.GROUPS,
        NavigationRoutes.SETTINGS
    )

    val showBottomBar = currentRoute in mainDestinations

    // Choose where to start
    val startDestination = when {
        !masterKeyManager.isMasterKeySet() -> NavigationRoutes.SETUP
        VaultKeyHolder.isUnlocked -> NavigationRoutes.MAIN
        else -> NavigationRoutes.UNLOCK
    }

    val onKeyDerived: (String) -> Unit = remember(masterKeyManager) {
        { masterKey ->
            val derivedKey = masterKeyManager.deriveEncryptionKey(masterKey)
            VaultKeyHolder.setKey(derivedKey)
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    Destination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                if (currentRoute != destination.route) {
                                    navController.navigate(destination.route) {
                                        // Pop up to the start destination of the graph to
                                        // avoid building up a large stack of destinations
                                        // on the back stack as users select items
                                        popUpTo(NavigationRoutes.MAIN) {
                                            saveState = true
                                        }
                                        // Avoid multiple copies of the same destination when
                                        // reselecting the same item
                                        launchSingleTop = true
                                        // Restore state when reselecting a previously selected item
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    destination.icon,
                                    contentDescription = destination.contentDescription
                                )
                            },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        }
    ) { contentPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = modifier.padding(contentPadding)
        ) {
            // Setup
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

            // Unlock
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

            // Passwords (Main)
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

            // Groups
            composable(NavigationRoutes.GROUPS) {
                GroupsScreen()
            }

            // Settings
            composable(NavigationRoutes.SETTINGS) {
                SettingsScreen()
            }

            // Add password
            composable(NavigationRoutes.ADD_PASSWORD) {
                AddEditPasswordScreen(
                    viewModel = passwordViewModel,
                    passwordEntry = null,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Edit password
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
}
