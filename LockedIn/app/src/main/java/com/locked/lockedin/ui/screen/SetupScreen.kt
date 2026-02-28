package com.locked.lockedin.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.locked.lockedin.ui.viewmodel.SetupUiState
import com.locked.lockedin.ui.viewmodel.SetupViewModel

/**
 * First-launch screen where the user creates their master key.
 */
@Composable
fun SetupScreen(
    viewModel: SetupViewModel,
    onSetupComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Navigate away once setup succeeds
    LaunchedEffect(uiState.isSetupComplete) {
        if (uiState.isSetupComplete) onSetupComplete()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text(
                text = "Create Your Master Key",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "Your master key encrypts all your passwords. " +
                        "Choose something strong — it cannot be recovered if lost.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Master key field
            MasterKeyField(
                value = uiState.masterKey,
                onValueChange = viewModel::onMasterKeyChange,
                label = "Master Key",
                placeholder = "Enter your master key",
                isVisible = uiState.isMasterKeyVisible,
                onToggleVisibility = viewModel::toggleMasterKeyVisibility,
                isError = uiState.masterKeyError != null,
                supportingText = uiState.masterKeyError
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Strength indicator
            AnimatedVisibility(visible = uiState.masterKey.isNotEmpty()) {
                PasswordStrengthIndicator(strength = uiState.passwordStrength)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Confirm field
            MasterKeyField(
                value = uiState.confirmKey,
                onValueChange = viewModel::onConfirmKeyChange,
                label = "Confirm Master Key",
                placeholder = "Re-enter your master key",
                isVisible = uiState.isConfirmKeyVisible,
                onToggleVisibility = viewModel::toggleConfirmKeyVisibility,
                isError = uiState.confirmKeyError != null,
                supportingText = uiState.confirmKeyError
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Error banner
            AnimatedVisibility(visible = uiState.errorMessage != null) {
                uiState.errorMessage?.let { message ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Create button
            Button(
                onClick = viewModel::setupMasterKey,
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Create Vault", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Warning
            Text(
                text = "⚠️ There is no way to recover your master key. " +
                        "If you forget it, all saved passwords will be lost.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun MasterKeyField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    isVisible: Boolean,
    onToggleVisibility: () -> Unit,
    isError: Boolean,
    supportingText: String?,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        visualTransformation = if (isVisible) VisualTransformation.None
        else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    imageVector = if (isVisible) Icons.Default.VisibilityOff
                    else Icons.Default.Visibility,
                    contentDescription = if (isVisible) "Hide" else "Show"
                )
            }
        },
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun PasswordStrengthIndicator(strength: PasswordStrength) {
    val (label, color, progress) = when (strength) {
        PasswordStrength.WEAK -> Triple(
            "Weak", MaterialTheme.colorScheme.error, 0.25f
        )
        PasswordStrength.FAIR -> Triple(
            "Fair", MaterialTheme.colorScheme.tertiary, 0.5f
        )
        PasswordStrength.GOOD -> Triple(
            "Good", MaterialTheme.colorScheme.secondary, 0.75f
        )
        PasswordStrength.STRONG -> Triple(
            "Strong", MaterialTheme.colorScheme.primary, 1f
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Strength",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            color = color
        )
    }
}

enum class PasswordStrength { WEAK, FAIR, GOOD, STRONG }