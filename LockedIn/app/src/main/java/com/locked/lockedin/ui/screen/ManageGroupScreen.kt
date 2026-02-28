package com.locked.lockedin.ui.screen

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.locked.lockedin.ui.component.PasswordItem
import com.locked.lockedin.ui.theme.LockedInTheme
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.locked.lockedin.ui.viewmodel.GroupPasswordItem
import com.locked.lockedin.ui.viewmodel.GroupViewModel

/**
 * Screen for managing a specific group, displaying its members and passwords.
 */
@Composable
fun ManageGroupScreen(
    groupId: String,
    groupName: String,
    groupViewModel: GroupViewModel,
    onBackClick: () -> Unit,
    onAddPasswordClick: () -> Unit,         // ← navigates to AddGroupPasswordScreen
    modifier: Modifier = Modifier
) {
    val uiState by groupViewModel.uiState.collectAsState()
    val groupPasswords by groupViewModel.groupPasswords.collectAsState()

    var showAddMemberDialog by remember { mutableStateOf(false) }
    var showDeletePasswordDialog by remember { mutableStateOf<GroupPasswordItem?>(null) }

    // Load passwords on first composition
    LaunchedEffect(groupId) {
        groupViewModel.loadGroupPasswords(groupId)
    }

    ManageGroupScreenContent(
        groupName = groupName,
        passwords = groupPasswords,
        isLoading = uiState.isLoading,
        onBackClick = onBackClick,
        onAddMemberClick = { showAddMemberDialog = true },
        onAddPasswordClick = onAddPasswordClick,            // ← wired directly
        onDeletePasswordClick = { pw -> showDeletePasswordDialog = pw },
        modifier = modifier
    )

    // Error snackbar
    uiState.errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { groupViewModel.clearMessages() },
            confirmButton = { TextButton(onClick = { groupViewModel.clearMessages() }) { Text("OK") } },
            title = { Text("Error") },
            text = { Text(msg) }
        )
    }

    uiState.successMessage?.let { msg ->
        LaunchedEffect(msg) {
            groupViewModel.clearMessages()
        }
    }

    // ── Add member dialog ───────────────────────────────────────────────────
    if (showAddMemberDialog) {
        AddMemberDialog(
            onDismiss = { showAddMemberDialog = false },
            onAdd = { phoneNumber ->
                groupViewModel.addMember(groupId, phoneNumber) { result ->
                    if (result.isSuccess) {
                        showAddMemberDialog = false
                    }
                }
            }
        )
    }

    // ── Delete password confirmation ────────────────────────────────────────
    showDeletePasswordDialog?.let { pw ->
        AlertDialog(
            onDismissRequest = { showDeletePasswordDialog = null },
            title = { Text("Delete password") },
            text = { Text("Are you sure you want to delete \"${pw.label}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    groupViewModel.deleteGroupPassword(groupId, pw.id)
                    showDeletePasswordDialog = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePasswordDialog = null }) { Text("Cancel") }
            }
        )
    }
}

// ── Add Member Dialog ───────────────────────────────────────────────────────

@Composable
fun AddMemberDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .wrapContentSize()
                .padding(16.dp)
                .clip(RoundedCornerShape(12.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "ADD MEMBER",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Add by phone number",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                Surface(
                    shape = RoundedCornerShape(15.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            "Phone number",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        BasicTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (phoneNumber.isEmpty()) {
                                        Text(
                                            "e.g. 666111222",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FilledTonalIconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(56.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color(0xFFFFE599).copy(alpha = 0.8f)
                        )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Reply,
                            contentDescription = "Go back",
                            tint = Color.Black
                        )
                    }

                    Button(
                        onClick = { if (phoneNumber.isNotBlank()) onAdd(phoneNumber) },
                        enabled = phoneNumber.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .height(56.dp)
                            .weight(1f)
                            .padding(start = 12.dp)
                    ) {
                        Text("Add Member", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ── ManageGroupScreenContent (stateless) ────────────────────────────────────

@Composable
fun ManageGroupScreenContent(
    groupName: String,
    passwords: List<GroupPasswordItem>,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    onAddMemberClick: () -> Unit,
    onAddPasswordClick: () -> Unit,
    onDeletePasswordClick: (GroupPasswordItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                // Header: Group Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = groupName.uppercase(),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${passwords.size} shared passwords",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Scrollable content
                Column(modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .weight(1f)
                ) {

                    // --- MEMBERS SECTION ---
                    Text(
                        text = "MEMBERS",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ActionSmallButton(
                            text = "Add member",
                            icon = Icons.Default.PersonAdd,
                            backgroundColor = Color(0xFFC1E1C1),
                            onClick = onAddMemberClick,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // --- PASSWORDS SECTION ---
                    Text(
                        text = "PASSWORDS",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ActionSmallButton(
                            text = "Add password",
                            icon = Icons.Default.LockOpen,
                            backgroundColor = Color(0xFFC1E1C1),
                            onClick = onAddPasswordClick,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (passwords.isEmpty()) {
                        Text(
                            text = "No passwords shared yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        passwords.forEach { pw ->
                            GroupPasswordRow(
                                password = pw,
                                onDeleteClick = { onDeletePasswordClick(pw) }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    // Account for the floating button at the bottom
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }

            // Back button — bottom right
            FilledTonalIconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .size(56.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Color(0xFFFFE599).copy(alpha = 0.8f)
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = "Back", tint = Color.Black)
            }
        }
    }
}


@Composable
private fun GroupPasswordRow(
    password: GroupPasswordItem,
    onDeleteClick: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = password.label.take(1).uppercase(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(12.dp))

            // Label + password value
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = password.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                password.decryptedData?.let { data ->
                    Text(
                        text = if (isVisible) data
                        else "•".repeat(data.length.coerceIn(6, 20)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Show / hide button
            password.decryptedData?.let { data ->
                IconButton(onClick = { isVisible = !isVisible }) {
                    Icon(
                        imageVector = if (isVisible) Icons.Default.VisibilityOff
                        else Icons.Default.Visibility,
                        contentDescription = if (isVisible) "Hide password" else "Show password",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Copy button
                IconButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                            as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Password", data)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "${password.label} copied to clipboard", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy password",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Delete button
            IconButton(onClick = onDeleteClick) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// ── ActionSmallButton ───────────────────────────────────────────────────────

@Composable
private fun ActionSmallButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor.copy(alpha = 0.9f),
            contentColor = Color.Black
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .height(44.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

// ── Preview ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun ManageGroupScreenPreview() {
    LockedInTheme() {
        ManageGroupScreen(
            groupName = "Grupiño chulo",
            passwords = listOf(
                GroupPasswordItem("1", "g1", "u1", "GitHub", "s3cr3t", "2026-01-01", "2026-01-01"),
                GroupPasswordItem("2", "g1", "u1", "Google", "p4ssw0rd", "2026-01-01", "2026-01-01")
            ),
            isLoading = false,
            onBackClick = {},
            onAddMemberClick = {},
            onAddPasswordClick = {},
            onDeletePasswordClick = {}
        )
    }
}