package com.locked.lockedin.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.locked.lockedin.ui.theme.LockedInTheme
import com.locked.lockedin.ui.viewmodel.PasswordUiState
import com.locked.lockedin.ui.viewmodel.PasswordViewModel

/**
 * Main screen displaying the list of passwords
 */
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
        onCopyPassword      = { entry ->
            val plain = viewModel.decryptPassword(entry.encryptedPassword)
            if (plain != null) copyToClipboard(context, "Password", plain)
            else Toast.makeText(context, "Could not decrypt password", Toast.LENGTH_SHORT).show()
        },
        modifier = modifier
    )

    // Show messages
    uiState.errorMessage?.let { message ->
        LaunchedEffect(message) {
            // Show snackbar or toast
        }
    }

    uiState.successMessage?.let { message ->
        LaunchedEffect(message) {
            // Show snackbar or toast
        }
    }
}

/**
 * Stateless content of the MainScreen for easy previewing
 */
@Composable
fun MainScreenContent(
    passwords: List<PasswordEntry>,
    searchQuery: String,
    uiState: PasswordUiState,
    onSearchQueryChange: (String) -> Unit,
    onSearchClearClick: () -> Unit,
    onAddPasswordClick: () -> Unit,
    onPasswordClick: (PasswordEntry) -> Unit,
    onCopyPassword: (PasswordEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            // 1. Header: MY PASSWORDS
            Text(
                text = "MY PASSWORDS",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Action Buttons: Add and Del
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Add password button
                Button(
                    onClick = onAddPasswordClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFC1E1C1).copy(alpha = 0.9f),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add password", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                // Del passwords button
                Button(
                    onClick = { /* TODO: Implement delete logic */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF4C2C2).copy(alpha = 0.9f),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Delete passwords", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Search and Filter
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) Text("Search", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                innerTextField()
                            }
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                // Filter button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { /* TODO: Implement filter logic */ }
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filtrar", tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    Text("Filtrar", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. List Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(3.dp))
                )
                Spacer(Modifier.width(24.dp))
                Text("Site name", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                Text("Owner", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.wrapContentSize())
                Spacer(Modifier.width(12.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))

            // 5. Content based on state
            when {
                uiState.isLoading -> {
                    LoadingContent()
                }
                passwords.isEmpty() -> {
                    EmptyContent(
                        hasSearchQuery = searchQuery.isNotBlank(),
                        searchQuery    = searchQuery
                    )
                }
                else -> {
                    PasswordList(
                        passwords       = passwords,
                        onPasswordClick = onPasswordClick,
                        onCopyClick     = onCopyPassword
                    )
                }
            }
        }
    }
}

// Helper composables

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Loading passwords...", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun EmptyContent(hasSearchQuery: Boolean, searchQuery: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.padding(32.dp)
        ) {
            Text(
                text = if (hasSearchQuery) "No results found" else "No passwords saved",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PasswordList(
    passwords: List<PasswordEntry>,
    onPasswordClick: (PasswordEntry) -> Unit,
    onCopyClick: (PasswordEntry) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        items(
            items = passwords,
            key = { password -> password.id }
        ) { password ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(3.dp))
                )

                Spacer(Modifier.width(12.dp))

                PasswordItem(
                    password    = password,
                    onClick     = { onPasswordClick(password) },
                    onCopyClick = { onCopyClick(password) },
                    modifier    = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value         = query,
        onValueChange = onQueryChange,
        label         = { Text("Search passwords") },
        placeholder   = { Text("Search by title, username, or website") },
        leadingIcon   = { Icon(Icons.Default.Search, contentDescription = "Search") },
        trailingIcon  = {
            if (query.isNotBlank()) {
                IconButton(onClick = onClearClick) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                }
            }
        },
        modifier = modifier
    )
}

// Clipboard helper

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
}