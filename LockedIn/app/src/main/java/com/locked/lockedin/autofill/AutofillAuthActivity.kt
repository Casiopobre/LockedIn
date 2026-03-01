package com.locked.lockedin.autofill

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.service.autofill.FillResponse
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.locked.lockedin.security.MasterKeyManager
import com.locked.lockedin.security.VaultKeyHolder
import com.locked.lockedin.ui.theme.LockedInTheme

class AutofillAuthActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Recuperamos el FillResponse pendiente que creó el servicio
        val pendingResponse = intent.getParcelableExtra<FillResponse>(
            PasswordAutofillService.EXTRA_FILL_RESPONSE
        )

        setContent {
            LockedInTheme {
                AuthScreen(
                    onAuthenticated = {
                        // El vault ya está desbloqueado, devolvemos el FillResponse
                        val replyIntent = Intent().apply {
                            putExtra(
                                android.view.autofill.AutofillManager.EXTRA_AUTHENTICATION_RESULT,
                                pendingResponse
                            )
                        }
                        setResult(RESULT_OK, replyIntent)
                        finish()
                    },
                    onCancel = {
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    },
                    // Pasamos a función de desbloqueo aquí
                    onTryUnlock = { pin -> tryUnlock(pin) }
                )
            }
        }
    }

    private fun tryUnlock(pin: String): Boolean {
        val masterKeyManager = MasterKeyManager(this)
        return if (masterKeyManager.verifyMasterKey(pin)) {
            val key = masterKeyManager.deriveEncryptionKey(pin)
            VaultKeyHolder.setKey(key)
            true
        } else {
            false
        }
    }
}

@Composable
private fun AuthScreen(
    onAuthenticated: () -> Unit,
    onCancel: () -> Unit,
    onTryUnlock: (String) -> Boolean // Definimos a lambda
) {
    // Reutilizamos MasterKeyManager para verificar el PIN
    // Adapta esto según tu implementación en SetupUnlockViewModels.kt
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Desbloquea tu vault", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it; error = false },
            label = { Text("PIN / Contraseña maestra") },
            visualTransformation = PasswordVisualTransformation(),
            isError = error
        )

        if (error) {
            Text("PIN incorrecto", color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCancel) { Text("Cancelar") }
            Button(onClick = {
                // Chamamos á lambda que recibimos
                if (onTryUnlock(pin)) onAuthenticated() else error = true
            }) { Text("Desbloquear") }
        }
    }
}

// Adapta esta función a tu implementación de MasterKeyManager/VaultKeyHolder
private fun tryUnlock(pin: String): Boolean {
    return try {
        // Ejemplo: MasterKeyManager.verifyPin(pin) && VaultKeyHolder.unlock(...)
        // Implementa según tu lógica existente en UnlockScreen.kt
        false // reemplaza con tu lógica real
    } catch (e: Exception) {
        false
    }
}