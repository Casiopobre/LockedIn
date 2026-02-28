package com.sabelaperez.lockedin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sabelaperez.lockedin.ui.lock.LockScreen
import com.sabelaperez.lockedin.ui.theme.KeyVaultTheme
import com.sabelaperez.lockedin.ui.vault.VaultScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            KeyVaultTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "lock") {
                    composable("lock") {
                        LockScreen(
                            activity = this@MainActivity,
                            onUnlocked = { navController.navigate("vault") {
                                popUpTo("lock") { inclusive = true }
                            }}
                        )
                    }
                    composable("vault") {
                        VaultScreen(
                            onLock = {
                                VaultSession.lock()
                                navController.navigate("lock") {
                                    popUpTo("vault") { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}