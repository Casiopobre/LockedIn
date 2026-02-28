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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.runtime.LaunchedEffect
import com.locked.lockedin.security.BiometricKeyManager
import com.locked.lockedin.security.MasterKeyManager
import com.locked.lockedin.security.VaultKeyHolder
import com.locked.lockedin.ui.screen.*
import com.locked.lockedin.ui.theme.PasswordManagerTheme
import com.locked.lockedin.ui.viewmodel.PasswordViewModel
import com.locked.lockedin.ui.screen.UnlockScreen
import com.yourname.passwordmanager.ui.viewmodel.SetupViewModel
import com.yourname.passwordmanager.ui.viewmodel.UnlockViewModel

object NavigationRoutes {
    const val SETUP = "setup"
    const val UNLOCK = "unlock"
    // Anter MAIN = "main"
    const val MAIN = "my_passwords"
    const val GROUPS = "my_groups"
    const val ADD_GROUP = "add_group"
    const val MANAGE_GROUP = "manage_group/{groupName}"
    const val SETTINGS = "settings"
    const val ADD_PASSWORD = "add_password"
    const val EDIT_PASSWORD = "edit_password/{passwordId}"

    fun editPassword(passwordId: Long) = "edit_password/$passwordId"
    fun manageGroup(groupName: String) = "manage_group/$groupName"
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
    biometricKeyManager: BiometricKeyManager,
    passwordViewModel: PasswordViewModel,
    activity: FragmentActivity,
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
                                        popUpTo(NavigationRoutes.MAIN) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
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
                    },
                    biometricKeyManager = biometricKeyManager,
                    activity = activity   // ← use the parameter passed to PasswordManagerNavigation
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
                    biometricKeyManager = biometricKeyManager,
                    activity            = activity,
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
                GroupsScreen(
                    onAddGroupClick = {
                        navController.navigate(NavigationRoutes.ADD_GROUP)
                    },
                    onGroupClick = { group ->
                        navController.navigate(NavigationRoutes.manageGroup(group.name))
                    }
                )
            }

            // Manage Group
            composable(
                route = NavigationRoutes.MANAGE_GROUP,
                arguments = listOf(navArgument("groupName") { type = NavType.StringType })
            ) { backStackEntry ->
                val groupName = backStackEntry.arguments?.getString("groupName") ?: ""
                ManageGroupScreen(
                    groupName = groupName,
                    onBackClick = { navController.popBackStack() }
                )
            }

            // Settings
            composable(NavigationRoutes.SETTINGS) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Add password as a Dialog
            dialog(
                route = NavigationRoutes.ADD_PASSWORD,
                dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                AddEditPasswordScreen(
                    viewModel      = passwordViewModel,
                    passwordEntry  = null, // see that (before a selectedPassword were here)
                    onNavigateBack = {
                        passwordViewModel.clearSelectedPassword()
                        navController.popBackStack()
                    }
                )
            }

            // Edit password as a Dialog
            dialog(
                route = NavigationRoutes.EDIT_PASSWORD,
                arguments = listOf(navArgument("passwordId") { type = NavType.LongType }),
                dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
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

            // Add group as a Dialog
            dialog(
                route = NavigationRoutes.ADD_GROUP,
                dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                AddGroupScreen(
                    onDismiss = { navController.popBackStack() },
                    onSave = { _ ->
                        // TODO: Implement actual saving logic when GroupViewModel is ready
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NavigationPreview() {
    PasswordManagerTheme {
        val navController = rememberNavController()
        val context = LocalContext.current
        val masterKeyManager = remember { MasterKeyManager(context) }
        
        Scaffold(
            bottomBar = {
                NavigationBar {
                    Destination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = destination == Destination.PASSWORDS,
                            onClick = { },
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
        ) { padding ->
            Text("Preview Content", modifier = Modifier.padding(padding))
        }
    }
}
