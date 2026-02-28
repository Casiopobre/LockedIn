package com.locked.lockedin.ui.screen

import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.locked.lockedin.security.BiometricHelper
import com.locked.lockedin.security.BiometricKeyManager
import com.locked.lockedin.security.VaultKeyHolder
import com.locked.lockedin.ui.viewmodel.UnlockViewModel

@Composable
fun UnlockScreen(
    viewModel: UnlockViewModel,
    biometricKeyManager: BiometricKeyManager,
    activity: FragmentActivity,
    onUnlockSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    val isBiometricAvailable = remember {
        BiometricHelper.isAvailable(activity) && biometricKeyManager.isBiometricKeyStored()
    }

    // Dialog to offer biometric enrollment after master key unlock
    var showEnrollBiometricDialog by remember { mutableStateOf(false) }

    // Auto-trigger biometric prompt when screen opens (if enrolled)
    LaunchedEffect(Unit) {
        if (isBiometricAvailable) {
            launchBiometricPrompt(
                activity            = activity,
                biometricKeyManager = biometricKeyManager,
                onSuccess           = {
                    VaultKeyHolder.setKey(it)
                    onUnlockSuccess()
                }
            )
        }
    }

    // After master key unlock succeeds, offer biometric enrollment if not yet set up
    LaunchedEffect(uiState.isUnlocked) {
        if (uiState.isUnlocked) {
            val canOfferBiometric = BiometricHelper.isAvailable(activity) &&
                    !biometricKeyManager.isBiometricKeyStored()
            if (canOfferBiometric) {
                showEnrollBiometricDialog = true
            } else {
                onUnlockSuccess()
            }
        }
    }

    // Offer biometric enrollment dialog
    if (showEnrollBiometricDialog) {
        AlertDialog(
            onDismissRequest = {
                showEnrollBiometricDialog = false
                onUnlockSuccess()
            },
            icon    = { Icon(Icons.Default.Fingerprint, contentDescription = null) },
            title   = { Text("Enable Biometric Unlock?") },
            text    = {
                Text(
                    "Next time, skip typing your master key and unlock " +
                            "with your fingerprint or face instead.",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(onClick = {
                    // Step 1: get an encryption cipher
                    val cipher = biometricKeyManager.getCipherForEncryption()

                    // Step 2: show BiometricPrompt with that cipher
                    BiometricHelper.showPrompt(
                        activity = activity,
                        title = "Enable Biometric Unlock",
                        subtitle = "Authenticate to save your vault key",
                        cryptoObject = BiometricPrompt.CryptoObject(cipher),
                        onSuccess = { authenticatedCipher ->
                            // Step 3: encrypt & save with the authorized cipher
                            viewModel.enableBiometric(biometricKeyManager, authenticatedCipher)
                            showEnrollBiometricDialog = false
                            onUnlockSuccess()
                        },
                        onFailure = {
                            showEnrollBiometricDialog = false
                            onUnlockSuccess()
                        }
                    )
                }) { Text("Enable") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showEnrollBiometricDialog = false
                    onUnlockSuccess()
                }) { Text("Not now") }
            }
        )
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color    = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector     = Icons.Default.Lock,
                contentDescription = null,
                modifier        = Modifier.size(72.dp),
                tint            = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text       = "Welcome Back",
                style      = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text  = "Enter your master key to unlock your vault.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Master key input
            OutlinedTextField(
                value               = uiState.masterKey,
                onValueChange       = viewModel::onMasterKeyChange,
                label               = { Text("Master Key") },
                placeholder         = { Text("Enter your master key") },
                visualTransformation = if (uiState.isKeyVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
                trailingIcon        = {
                    IconButton(onClick = viewModel::toggleKeyVisibility) {
                        Icon(
                            imageVector     = if (uiState.isKeyVisible) Icons.Default.VisibilityOff
                            else Icons.Default.Visibility,
                            contentDescription = if (uiState.isKeyVisible) "Hide" else "Show"
                        )
                    }
                },
                isError             = uiState.isError,
                supportingText      = if (uiState.isError) {
                    {
                        Text(
                            if (uiState.failedAttempts >= 3)
                                "Wrong master key (${uiState.failedAttempts} failed attempts)"
                            else
                                "Incorrect master key. Please try again."
                        )
                    }
                } else null,
                keyboardOptions     = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine          = true,
                modifier            = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Unlock button
            Button(
                onClick  = viewModel::unlock,
                enabled  = !uiState.isLoading && uiState.masterKey.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        color       = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Unlock", style = MaterialTheme.typography.labelLarge)
                }
            }

            // Biometric button — only shown if biometric is enrolled
            AnimatedVisibility(visible = isBiometricAvailable) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick  = {
                            launchBiometricPrompt(
                                activity            = activity,
                                biometricKeyManager = biometricKeyManager,
                                onSuccess           = {
                                    VaultKeyHolder.setKey(it)
                                    onUnlockSuccess()
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(
                            imageVector     = Icons.Default.Fingerprint,
                            contentDescription = null,
                            modifier        = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Use Biometric", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

// ─── Helper ──────────────────────────────────────────────────────────────────

private fun launchBiometricPrompt(
    activity: FragmentActivity,
    biometricKeyManager: BiometricKeyManager,
    onSuccess: (javax.crypto.spec.SecretKeySpec) -> Unit
) {
    val cipher = biometricKeyManager.getCipherForDecryption() ?: return
    BiometricHelper.showPrompt(
        activity      = activity,
        cryptoObject  = BiometricPrompt.CryptoObject(cipher),
        onSuccess     = { authenticatedCipher ->
            val vaultKey = biometricKeyManager.decryptVaultKey(authenticatedCipher)
            onSuccess(vaultKey)
        },
        onFailure     = { /* user cancelled or error — master key field still available */ }
    )
}