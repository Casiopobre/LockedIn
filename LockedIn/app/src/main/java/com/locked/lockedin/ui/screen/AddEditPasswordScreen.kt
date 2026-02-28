package com.locked.lockedin.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.locked.lockedin.data.model.PasswordEntry
import com.locked.lockedin.ui.theme.PasswordManagerTheme
import com.locked.lockedin.ui.viewmodel.PasswordViewModel

/**
 * Screen for adding or editing a password entry, styled to match the refined
 * Material 3 aesthetic of PasswordItem, while keeping the title as plain text.
 */
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
    var isPasswordVisible by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    val isEditing = passwordEntry != null

    AddEditPasswordContent(
        title = title,
        username = username,
        password = password,
        isPasswordVisible = isPasswordVisible,
        isEditing = isEditing,
        onTitleChange = { title = it },
        onUsernameChange = { username = it },
        onPasswordChange = { password = it },
        onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
        onGoBack = onNavigateBack,
        onDeleteClick = { showDeleteDialog = true },
        onSaveClick = {
            viewModel.addPassword(title, username, password, "", "") { result ->
                if (result.isSuccess) onNavigateBack()
            }
        }
    )

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirm Delete") },
            text = { Text("Are you sure you want to delete this password?") },
            confirmButton = {
                TextButton(onClick = {
                    passwordEntry?.let { entry ->
                        viewModel.deletePassword(entry) { onNavigateBack() }
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun AddEditPasswordContent(
    title: String,
    username: String,
    password: String,
    isPasswordVisible: Boolean,
    isEditing: Boolean,
    onTitleChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onGoBack: () -> Unit,
    onDeleteClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .wrapContentSize()
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .wrapContentSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PASSWORD FOR",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    // Plain text input for title (no Surface/box)
                    BasicTextField(
                        value = title,
                        onValueChange = onTitleChange,
                        textStyle = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.padding(top = 8.dp),
                        decorationBox = { innerTextField ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box {
                                    if (title.isEmpty()) Text("site_name", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                    innerTextField()
                                }
                            }
                        }
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title.take(1).uppercase().ifEmpty { "?" },
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            FormSection(
                label = "Login info",
                value = username,
                subLabel = "Email, tel ...",
                icon = Icons.Default.Edit,
                onValueChange = onUsernameChange
            )

            Spacer(modifier = Modifier.height(24.dp))

            FormSection(
                label = "Password",
                value = if (isPasswordVisible) password else "••••••••",
                subLabel = "Tap to reveal",
                icon = Icons.Default.Edit,
                onValueChange = onPasswordChange,
                innerIcon = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                onInnerIconClick = onTogglePasswordVisibility
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Groups section
            Text(
                text = "Groups",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(15.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = borderStroke(),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "None", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))
                
                IconButton(
                    onClick = { },
                    modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "View group", tint = MaterialTheme.colorScheme.primary)
                }
                
                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f), CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete from group", tint = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(32.dp))

            // Bottom actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Go back
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FilledTonalIconButton(
                        onClick = onGoBack,
                        modifier = Modifier.size(56.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color(0xFFFFE599).copy(alpha = 0.8f) // Keeping the yellow but subtle
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = "Go back", tint = Color.Black)
                    }
                }

                if (isEditing) {
                    Button(
                        onClick = onDeleteClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .height(64.dp)
                            .fillMaxWidth(0.8f)
                            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Delete password",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onSaveClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(64.dp)
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                    ) {
                        Text("Save New Password", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun FormSection(
    label: String,
    value: String,
    subLabel: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onIconClick: () -> Unit = {},
    onValueChange: (String) -> Unit,
    innerIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onInnerIconClick: () -> Unit = {}
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(15.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = borderStroke(),
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp)) {
                        BasicTextField(
                            value = value,
                            onValueChange = onValueChange,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (innerIcon != null) {
                        IconButton(onClick = onInnerIconClick) {
                            Icon(innerIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            IconButton(
                onClick = onIconClick,
                modifier = Modifier
                    .size(48.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun borderStroke() = BorderStroke(
    width = 1.dp,
    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
)

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun AddEditPasswordScreenPreview() {
    PasswordManagerTheme {
        AddEditPasswordContent(
            title = "google.com",
            username = "user@gmail.com",
            password = "password123",
            isPasswordVisible = false,
            isEditing = true,
            onTitleChange = {},
            onUsernameChange = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onGoBack = {},
            onDeleteClick = {},
            onSaveClick = {}
        )
    }
}
