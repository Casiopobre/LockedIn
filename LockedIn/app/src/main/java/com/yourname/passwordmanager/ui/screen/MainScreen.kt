package com.yourname.passwordmanager.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yourname.passwordmanager.data.model.PasswordEntry
import com.yourname.passwordmanager.ui.component.PasswordItem
import com.yourname.passwordmanager.ui.viewmodel.PasswordViewModel

/**
 * Main screen displaying the list of passwords
 */
@OptIn(ExperimentalMaterial3Api::class)
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

    // Show encryption warning if needed
    LaunchedEffect(uiState.isEncryptionAvailable) {
        if (!uiState.isEncryptionAvailable) {
            // Handle encryption not available
        }
    }

    // Root container for the screen
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Search bar
            SearchBar(
                query = searchQuery,
                onQueryChange = viewModel::updateSearchQuery,
                onClearClick = viewModel::clearSearch,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Content based on state
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

        // Floating Action Button
        FloatingActionButton(
            onClick = onAddPasswordClick,
            modifier = Modifier
                .align(Alignment.BottomEnd) // This now works because it's inside a Box
                .padding(16.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add Password"
            )
        }
    }


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

// Helper composables moved outside MainScreen

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading passwords...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                text = if (hasSearchQuery) {
                    "No passwords found for \"$searchQuery\""
                } else {
                    "No passwords saved yet"
                },
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (hasSearchQuery) {
                    "Try adjusting your search terms"
                } else {
                    "Tap the + button to add your first password"
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 80.dp) // Account for FAB
    ) {
        items(
            items = passwords,
            key = { password -> password.id }
        ) { password ->
            PasswordItem(
                password = password,
                onClick = { onPasswordClick(password) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text("Search passwords") },
        placeholder = { Text("Search by title, username, or website") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search")
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = onClearClick) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                }
            }
        },
        modifier = modifier // Use the modifier passed into the function
    )
}