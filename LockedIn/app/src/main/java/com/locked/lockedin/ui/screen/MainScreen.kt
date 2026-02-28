package com.locked.lockedin.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.locked.lockedin.data.model.PasswordEntry
import com.locked.lockedin.ui.component.DebugBreachPanel
import com.locked.lockedin.ui.component.PasswordItem
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

    // Trigger breach check when the screen first becomes visible
    // (vault is guaranteed open at this point)
    LaunchedEffect(Unit) {
        viewModel.runBreachCheckIfDue()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── DEBUG panel (debug builds only) ───────────────────────────
            val isDebug = (0 != (LocalContext.current.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE))
            if (!isDebug) {
            // if (isDebug) { // ! is debug para que no salga
                DebugBreachPanel(
                    viewModel         = viewModel,
                    onForceCheck      = viewModel::forceBreachCheck,
                    onResetTimer      = viewModel::resetBreachTimer,
                    onAddTestPassword = {
                        // Adds a well-known pwned password so you can see the red UI
                        viewModel.addPassword(
                            title    = "⚠️ TEST — pwned entry",
                            username = "test@example.com",
                            password = "password",         // #1 most breached password
                            website  = "test.example.com",
                            notes    = "Added by debug panel — safe to delete"
                        )
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            // ── Breach alert banner ───────────────────────────────────────
            AnimatedVisibility(
                visible = uiState.lastBreachCheckPwnedCount > 0,
                enter   = expandVertically() + fadeIn(),
                exit    = shrinkVertically() + fadeOut()
            ) {
                BreachAlertBanner(
                    pwnedCount = uiState.lastBreachCheckPwnedCount,
                    onDismiss  = viewModel::dismissBreachBanner
                )
            }

            // ── Breach check progress bar ─────────────────────────────────
            if (uiState.isBreachCheckRunning) {
                BreachCheckProgress(progress = uiState.breachCheckProgress)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                SearchBar(
                    query         = searchQuery,
                    onQueryChange = viewModel::updateSearchQuery,
                    onClearClick  = viewModel::clearSearch,
                    modifier      = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                when {
                    uiState.isLoading  -> LoadingContent()
                    passwords.isEmpty() -> EmptyContent(
                        hasSearchQuery = searchQuery.isNotBlank(),
                        searchQuery    = searchQuery
                    )
                    else -> PasswordList(
                        passwords       = passwords,
                        onPasswordClick = onPasswordClick
                    )
                }
            }
        }

        FloatingActionButton(
            onClick  = onAddPasswordClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Password")
        }
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun BreachAlertBanner(pwnedCount: Int, onDismiss: () -> Unit) {
    Surface(color = PwnedRedLight, tonalElevation = 0.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Icon(
                imageVector        = Icons.Default.Warning,
                contentDescription = "Breach warning",
                tint               = PwnedRed,
                modifier           = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = "Data Breach Detected",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color      = PwnedRed,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text  = "$pwnedCount password(s) appeared in known breaches — scroll down to see which ones.",
                    style = MaterialTheme.typography.bodySmall.copy(color = PwnedRed)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Clear, contentDescription = "Dismiss", tint = PwnedRed)
            }
        }
    }
}

@Composable
private fun BreachCheckProgress(progress: Pair<Int, Int>?) {
    val (checked, total) = progress ?: (0 to 0)
    val fraction         = if (total > 0) checked.toFloat() / total else 0f
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text  = "Checking for breaches…",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (total > 0) Text(
                text  = "$checked / $total",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        if (total > 0) {
            LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text  = "Loading passwords…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                text      = if (hasSearchQuery) "No passwords found for \"$searchQuery\"" else "No passwords saved yet",
                style     = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color     = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text      = if (hasSearchQuery) "Try adjusting your search terms" else "Tap the + button to add your first password",
                style     = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color     = MaterialTheme.colorScheme.onSurfaceVariant
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
        contentPadding      = PaddingValues(bottom = 80.dp)
    ) {
        items(items = passwords, key = { it.id }) { password ->
            PasswordItem(password = password, onClick = { onPasswordClick(password) })
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