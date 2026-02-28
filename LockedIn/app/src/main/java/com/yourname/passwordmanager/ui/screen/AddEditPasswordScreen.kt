package com.yourname.passwordmanager.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.yourname.passwordmanager.data.model.PasswordEntry
import com.yourname.passwordmanager.ui.viewmodel.PasswordViewModel
import androidx.compose.runtime.mutableIntStateOf

/**
 * Screen for adding or editing a password entry
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPasswordScreen(
    viewModel: PasswordViewModel,
    passwordEntry: PasswordEntry? = null,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf(passwordEntry?.title ?: "") }
    var username by remember { mutableStateOf(passwordEntry?.username ?: "") }
    var password by remember { 
        mutableStateOf(
            passwordEntry?.let { 
                viewModel.decryptPassword(it.encryptedPassword) ?: ""
            } ?: ""
        ) 
    }
    var website by remember { mutableStateOf(passwordEntry?.website ?: "") }
    var notes by remember { mutableStateOf(passwordEntry?.notes ?: "") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showGenerateDialog by remember { mutableStateOf(false) }
    
    val isEditing = passwordEntry != null
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    
    // Form validation
    val isFormValid = title.isNotBlank() && 
                     username.isNotBlank() && 
                     password.isNotBlank()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Top App Bar
        TopAppBar(
            title = title,
            isEditing = isEditing,
            isFormValid = isFormValid,
            isLoading = uiState.isLoading,
            onNavigateBack = onNavigateBack,
            onSave = {
                if (isEditing) {
                    passwordEntry?.let { entry ->
                        viewModel.updatePassword(
                            entry, title, username, password, website, notes
                        ) { result ->
                            if (result.isSuccess) onNavigateBack()
                        }
                    }
                } else {
                    viewModel.addPassword(
                        title, username, password, website, notes
                    ) { result ->
                        if (result.isSuccess) onNavigateBack()
                    }
                }
            }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Form Fields
        PasswordForm(
            title = title,
            username = username,
            password = password,
            website = website,
            notes = notes,
            isPasswordVisible = isPasswordVisible,
            onTitleChange = { title = it },
            onUsernameChange = { username = it },
            onPasswordChange = { password = it },
            onWebsiteChange = { website = it },
            onNotesChange = { notes = it },
            onPasswordVisibilityToggle = { isPasswordVisible = !isPasswordVisible },
            onGeneratePassword = { showGenerateDialog = true }
        )
        
        // Delete button for editing mode
        if (isEditing) {
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { showDeleteDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete Password")
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Password") },
            text = { Text("Are you sure you want to delete this password? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        passwordEntry?.let { entry ->
                            viewModel.deletePassword(entry) { result ->
                                if (result.isSuccess) {
                                    showDeleteDialog = false
                                    onNavigateBack()
                                }
                            }
                        }
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Generate password dialog
    if (showGenerateDialog) {
        GeneratePasswordDialog(
            viewModel = viewModel,
            onDismiss = { showGenerateDialog = false },
            onPasswordGenerated = { generatedPassword ->
                password = generatedPassword
                showGenerateDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopAppBar(
    title: String,
    isEditing: Boolean,
    isFormValid: Boolean,
    isLoading: Boolean,
    onNavigateBack: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
        }
        
        Text(
            text = if (isEditing) "Edit Password" else "Add Password",
            style = MaterialTheme.typography.headlineSmall
        )
        
        Button(
            onClick = onSave,
            enabled = isFormValid && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(if (isEditing) "Update" else "Save")
            }
        }
    }
}

@Composable
private fun PasswordForm(
    title: String,
    username: String,
    password: String,
    website: String,
    notes: String,
    isPasswordVisible: Boolean,
    onTitleChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onWebsiteChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onGeneratePassword: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Title field
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("Title *") },
            placeholder = { Text("e.g., Gmail, Facebook") },
            leadingIcon = { Icon(Icons.Default.Label, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        // Username field
        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text("Username/Email *") },
            placeholder = { Text("e.g., john@example.com") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password *") },
            placeholder = { Text("Enter a strong password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            visualTransformation = if (isPasswordVisible) 
                VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                Row {
                    IconButton(onClick = onPasswordVisibilityToggle) {
                        Icon(
                            if (isPasswordVisible) Icons.Default.VisibilityOff 
                            else Icons.Default.Visibility,
                            contentDescription = if (isPasswordVisible) 
                                "Hide password" else "Show password"
                        )
                    }
                    IconButton(onClick = onGeneratePassword) {
                        Icon(
                            Icons.Default.Refresh, 
                            contentDescription = "Generate password"
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        // Website field
        OutlinedTextField(
            value = website,
            onValueChange = onWebsiteChange,
            label = { Text("Website") },
            placeholder = { Text("e.g., https://example.com") },
            leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        // Notes field
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text("Notes") },
            placeholder = { Text("Additional information (optional)") },
            leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5
        )
    }
}

@Composable
private fun GeneratePasswordDialog(
    viewModel: PasswordViewModel,
    onDismiss: () -> Unit,
    onPasswordGenerated: (String) -> Unit
) {
    var length by remember { mutableIntStateOf(16) }
    var includeUppercase by remember { mutableStateOf(true) }
    var includeLowercase by remember { mutableStateOf(true) }
    var includeNumbers by remember { mutableStateOf(true) }
    var includeSymbols by remember { mutableStateOf(true) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generate Password") },
        text = {
            Column {
                Text("Password Length: $length")
                Slider(
                    value = length.toFloat(),
                    onValueChange = { length = it.toInt() },
                    valueRange = 8f..32f,
                    steps = 23
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = includeUppercase,
                        onCheckedChange = { includeUppercase = it }
                    )
                    Text("Include Uppercase (A-Z)")
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = includeLowercase,
                        onCheckedChange = { includeLowercase = it }
                    )
                    Text("Include Lowercase (a-z)")
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = includeNumbers,
                        onCheckedChange = { includeNumbers = it }
                    )
                    Text("Include Numbers (0-9)")
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = includeSymbols,
                        onCheckedChange = { includeSymbols = it }
                    )
                    Text("Include Symbols (!@#$%)")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val generatedPassword = viewModel.generatePassword(
                        length, includeUppercase, includeLowercase, 
                        includeNumbers, includeSymbols
                    )
                    onPasswordGenerated(generatedPassword)
                }
            ) {
                Text("Generate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}