package com.locked.lockedin.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.locked.lockedin.data.model.PasswordEntry
import com.locked.lockedin.ui.component.PasswordItem
import com.locked.lockedin.ui.theme.PasswordManagerTheme
import com.locked.lockedin.ui.viewmodel.PasswordUiState
import com.locked.lockedin.ui.viewmodel.PasswordViewModel

private val PwnedRed      = Color(0xFFD32F2F)
private val PwnedRedLight = Color(0xFFFFEBEE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: PasswordViewModel,
    onAddPasswordClick: () -> Unit,
    onPasswordClick: (PasswordEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val passwords   by viewModel.passwords.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val uiState     by viewModel.uiState.collectAsState()
    val context     = LocalContext.current

    MainScreenContent(
        passwords           = passwords,
        searchQuery         = searchQuery,
        uiState             = uiState,
        onSearchQueryChange = viewModel::updateSearchQuery,
        onSearchClearClick  = viewModel::clearSearch,
        onAddPasswordClick  = onAddPasswordClick,
        onPasswordClick     = onPasswordClick,
        onDeletePasswords   = { entries ->
            entries.forEach { entry ->
                viewModel.deletePassword(entry)
            }
        },
        onCopyPassword      = { entry ->
            val plain = viewModel.decryptPassword(entry.encryptedPassword)
            if (plain != null) copyToClipboard(context, "Password", plain)
            else Toast.makeText(context, "Could not decrypt password", Toast.LENGTH_SHORT).show()
        },
        modifier = modifier
    )

    uiState.errorMessage?.let { message ->
        LaunchedEffect(message) { /* optionally show snackbar */ }
    }
    uiState.successMessage?.let { message ->
        LaunchedEffect(message) { /* optionally show snackbar */ }
    }
}

@Composable
fun MainScreenContent(
    passwords: List<PasswordEntry>,
    searchQuery: String,
    uiState: PasswordUiState,
    onSearchQueryChange: (String) -> Unit,
    onSearchClearClick: () -> Unit,
    onAddPasswordClick: () -> Unit,
    onPasswordClick: (PasswordEntry) -> Unit,
    onDeletePasswords: (List<PasswordEntry>) -> Unit,
    onCopyPassword: (PasswordEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    // ── Selection state (lives here, not in the ViewModel) ───────────────────
    var selectionMode     by remember { mutableStateOf(false) }
    var selectedIds       by remember { mutableStateOf(emptySet<Long>()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    fun exitSelectionMode() {
        selectionMode = false
        selectedIds   = emptySet()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color    = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text     = if (selectionMode) "${selectedIds.size} selected" else "MY PASSWORDS",
                    style    = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier.weight(1f)
                )
                // X button to cancel selection mode
                AnimatedVisibility(visible = selectionMode) {
                    IconButton(onClick = { exitSelectionMode() }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel selection")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Action buttons ────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Add password (hidden while in selection mode)
                AnimatedVisibility(
                    visible = !selectionMode,
                    modifier = Modifier.weight(1f)
                ) {
                    Button(
                        onClick = onAddPasswordClick,
                        colors  = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFC1E1C1).copy(alpha = 0.9f),
                            contentColor   = Color.Black
                        ),
                        shape   = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add password", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                // Delete button — enters selection mode or confirms deletion
                Button(
                    onClick = {
                        if (!selectionMode) {
                            // Enter selection mode
                            selectionMode = true
                        } else {
                            // Already selecting — confirm delete if anything selected
                            if (selectedIds.isNotEmpty()) showDeleteConfirm = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectionMode && selectedIds.isNotEmpty())
                            Color(0xFFD32F2F).copy(alpha = 0.85f)
                        else
                            Color(0xFFF4C2C2).copy(alpha = 0.9f),
                        contentColor = if (selectionMode && selectedIds.isNotEmpty())
                            Color.White
                        else
                            Color.Black
                    ),
                    shape    = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        if (selectionMode && selectedIds.isNotEmpty()) Icons.Default.DeleteForever
                        else Icons.Default.RemoveCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (selectionMode && selectedIds.isNotEmpty())
                            "Delete (${selectedIds.size})"
                        else
                            "Delete passwords",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Search + Filter ───────────────────────────────────────────────
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape    = RoundedCornerShape(8.dp),
                    color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier          = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        BasicTextField(
                            value         = searchQuery,
                            onValueChange = onSearchQueryChange,
                            singleLine    = true,
                            textStyle     = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            modifier      = Modifier.fillMaxWidth(),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) Text("Search", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                innerTextField()
                            }
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.clickable { }
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    Text("Filter", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── List header ───────────────────────────────────────────────────
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "Select all" checkbox when in selection mode
                if (selectionMode) {
                    Checkbox(
                        checked         = selectedIds.size == passwords.size && passwords.isNotEmpty(),
                        onCheckedChange = { checked ->
                            selectedIds = if (checked) passwords.map { it.id }.toSet()
                            else emptySet()
                        },
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(3.dp))
                    )
                }
                Spacer(Modifier.width(24.dp))
                Text("Site name", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                Text("Owner",     style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.wrapContentSize())
                Spacer(Modifier.width(12.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))

            // ── List / empty / loading ────────────────────────────────────────
            when {
                uiState.isLoading  -> LoadingContent()
                passwords.isEmpty() -> EmptyContent(
                    hasSearchQuery = searchQuery.isNotBlank(),
                    searchQuery    = searchQuery
                )
                else -> PasswordList(
                    passwords      = passwords,
                    selectionMode  = selectionMode,
                    selectedIds    = selectedIds,
                    onPasswordClick = { entry ->
                        if (selectionMode) {
                            selectedIds = if (selectedIds.contains(entry.id))
                                selectedIds - entry.id
                            else
                                selectedIds + entry.id
                        } else {
                            onPasswordClick(entry)
                        }
                    },
                    onCheckChange  = { entry, checked ->
                        selectedIds = if (checked) selectedIds + entry.id
                        else selectedIds - entry.id
                    },
                    onCopyClick    = { entry -> onCopyPassword(entry) }
                )
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        val count = selectedIds.size
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title            = { Text("Delete $count password${if (count > 1) "s" else ""}?") },
            text             = { Text("This action cannot be undone.") },
            confirmButton    = {
                TextButton(onClick = {
                    val toDelete = passwords.filter { it.id in selectedIds }
                    onDeletePasswords(toDelete)
                    showDeleteConfirm = false
                    exitSelectionMode()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton    = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

// Sub-composables

@Composable
private fun PasswordList(
    passwords: List<PasswordEntry>,
    selectionMode: Boolean,
    selectedIds: Set<Long>,
    onPasswordClick: (PasswordEntry) -> Unit,
    onCheckChange: (PasswordEntry, Boolean) -> Unit,
    onCopyClick: (PasswordEntry) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding      = PaddingValues(bottom = 24.dp)
    ) {
        items(items = passwords, key = { it.id }) { entry ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.fillMaxWidth()
            ) {
                // Functional checkbox (always visible; interactive only in selection mode)
                Checkbox(
                    checked         = selectedIds.contains(entry.id),
                    onCheckedChange = { checked ->
                        if (selectionMode) onCheckChange(entry, checked)
                    },
                    enabled  = selectionMode,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(Modifier.width(12.dp))

                PasswordItem(
                    password    = entry,
                    onClick     = { onPasswordClick(entry) },
                    // Copy icon copies the password directly from the list
                    onCopyClick = { onCopyClick(entry) },
                    modifier    = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Loading passwords...", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun EmptyContent(hasSearchQuery: Boolean, searchQuery: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text  = if (hasSearchQuery) "No results found" else "No passwords saved",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Clipboard helper

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
}


// Preview

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun MainScreenPreview() {
    PasswordManagerTheme {
        MainScreenContent(
            passwords = listOf(
                PasswordEntry(id = 1, title = "Google",  username = "you", website = "google.com",  encryptedPassword = ""),
                PasswordEntry(id = 2, title = "GitHub",  username = "you", website = "github.com",  encryptedPassword = ""),
                PasswordEntry(id = 3, title = "Netflix", username = "you", website = "netflix.com", encryptedPassword = "")
            ),
            searchQuery         = "",
            uiState             = PasswordUiState(),
            onSearchQueryChange = {},
            onSearchClearClick  = {},
            onAddPasswordClick  = {},
            onPasswordClick     = {},
            onDeletePasswords   = {},
            onCopyPassword      = {}
        )
    }
}