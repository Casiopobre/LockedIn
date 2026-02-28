package com.locked.lockedin.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.locked.lockedin.data.model.PasswordEntry
import com.locked.lockedin.ui.component.PasswordItem
import com.locked.lockedin.ui.theme.PasswordManagerTheme
import com.locked.lockedin.ui.viewmodel.PasswordUiState
import com.locked.lockedin.ui.viewmodel.PasswordViewModel

/**
 * Main screen displaying the list of passwords
 */
@Composable
fun MainScreen(
    viewModel: PasswordViewModel,
    onAddPasswordClick: () -> Unit,
    onPasswordClick: (PasswordEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val passwords by viewModel.passwords.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    MainScreenContent(
        passwords = passwords,
        searchQuery = searchQuery,
        uiState = uiState,
        onSearchQueryChange = viewModel::updateSearchQuery,
        onSearchClearClick = viewModel::clearSearch,
        onAddPasswordClick = onAddPasswordClick,
        onPasswordClick = onPasswordClick,
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
                // Add password button (Refined style)
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

                // Del passwords button (Refined style)
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
                // Search box (Refined style like AddEditPasswordScreen fields)
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
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Header Checkbox placeholder (Matches layout in image)
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
                        searchQuery = searchQuery
                    )
                }
                else -> {
                    PasswordList(
                        passwords = passwords,
                        onPasswordClick = onPasswordClick
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
private fun EmptyContent(
    hasSearchQuery: Boolean,
    searchQuery: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
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
    onPasswordClick: (PasswordEntry) -> Unit
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
                // Checkbox from sketch (Maintains layout while keeping modern style)
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(3.dp))
                )
                
                Spacer(Modifier.width(12.dp))
                
                PasswordItem(
                    password = password,
                    onClick = { onPasswordClick(password) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun MainScreenPreview() {
    PasswordManagerTheme {
        MainScreenContent(
            passwords = listOf(
                PasswordEntry(id = 1, title = "Google", username = "you", website = "google.com", encryptedPassword = ""),
                PasswordEntry(id = 2, title = "GitHub", username = "you", website = "github.com", encryptedPassword = ""),
                PasswordEntry(id = 3, title = "Netflix", username = "you", website = "netflix.com", encryptedPassword = "")
            ),
            searchQuery = "",
            uiState = PasswordUiState(),
            onSearchQueryChange = {},
            onSearchClearClick = {},
            onAddPasswordClick = {},
            onPasswordClick = {}
        )
    }
}
