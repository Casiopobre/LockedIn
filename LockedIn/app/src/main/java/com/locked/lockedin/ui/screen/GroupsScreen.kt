package com.locked.lockedin.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.locked.lockedin.ui.theme.PasswordManagerTheme
import com.locked.lockedin.ui.viewmodel.GroupViewModel
import kotlinx.coroutines.launch

/**
 * Groups screen displaying the list of password groups.
 *
 * On first composition it transparently registers/logs in to the backend.
 * If connection fails, a full-screen error message is shown.
 */
@Composable
fun GroupsScreen(
    groupViewModel: GroupViewModel,
    onAddGroupClick: () -> Unit,
    onGroupClick: (GroupItemData) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val groups  by groupViewModel.groups.collectAsState()
    val uiState by groupViewModel.uiState.collectAsState()
    val scope   = rememberCoroutineScope()

    // Authenticate + load groups on first composition
    LaunchedEffect(Unit) {
        groupViewModel.ensureAuthenticatedAndLoadGroups()
    }

    // ── Connection error state ──────────────────────────────────────────
    if (uiState.connectionError != null) {
        ConnectionErrorScreen(
            message = uiState.connectionError!!,
            onRetry = { groupViewModel.ensureAuthenticatedAndLoadGroups() },
            modifier = modifier
        )
        return
    }

    // ── Normal state (authenticated) ────────────────────────────────────
    // Map API model to UI model
    val groupItems = groups.map { g ->
        GroupItemData(
            id = g.id,
            name = g.name,
            ownerId = g.ownerId
        )
    }.let { list ->
        if (searchQuery.isBlank()) list
        else list.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    GroupsScreenContent(
        groups              = groupItems,
        searchQuery         = searchQuery,
        isLoading           = uiState.isLoading,
        onSearchQueryChange = { searchQuery = it },
        onAddGroupClick     = onAddGroupClick,
        onGroupClick        = onGroupClick,
        onDeleteGroups      = { ids ->
            // Delete sequentially; each deleteGroup call already refreshes the list
            scope.launch {
                ids.forEach { groupId -> groupViewModel.deleteGroup(groupId) }
            }
        },
        modifier = modifier
    )
}

// ── UI model ────────────────────────────────────────────────────────────────

data class GroupItemData(
    val id: String,
    val name: String,
    val ownerId: String
)

// ── Stateless content ────────────────────────────────────────────────────────

@Composable
fun GroupsScreenContent(
    groups: List<GroupItemData>,
    searchQuery: String,
    isLoading: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onAddGroupClick: () -> Unit,
    onGroupClick: (GroupItemData) -> Unit,
    onDeleteGroups: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectionMode     by remember { mutableStateOf(false) }
    var selectedIds       by remember { mutableStateOf(emptySet<String>()) }
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
                    text     = if (selectionMode) "${selectedIds.size} selected" else "MY GROUPS",
                    style    = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier.weight(1f)
                )
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
                AnimatedVisibility(
                    visible  = !selectionMode,
                    modifier = Modifier.weight(1f)
                ) {
                    Button(
                        onClick = onAddGroupClick,
                        colors  = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFC1E1C1).copy(alpha = 0.9f),
                            contentColor   = Color.Black
                        ),
                        shape    = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add group", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                Button(
                    onClick = {
                        if (!selectionMode) selectionMode = true
                        else if (selectedIds.isNotEmpty()) showDeleteConfirm = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectionMode && selectedIds.isNotEmpty())
                            Color(0xFFD32F2F).copy(alpha = 0.85f)
                        else
                            Color(0xFFF4C2C2).copy(alpha = 0.9f),
                        contentColor = if (selectionMode && selectedIds.isNotEmpty())
                            Color.White else Color.Black
                    ),
                    shape    = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = if (selectionMode && selectedIds.isNotEmpty())
                            Icons.Default.DeleteForever else Icons.Default.RemoveCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (selectionMode && selectedIds.isNotEmpty())
                            "Delete (${selectedIds.size})" else "Del group",
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
                    border   = androidx.compose.foundation.BorderStroke(
                        1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
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
                                if (searchQuery.isEmpty()) Text(
                                    "Buscar...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                innerTextField()
                            }
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.clickable { /* TODO */ }
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filtrar", tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    Text("Filtrar", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
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
                if (selectionMode) {
                    Checkbox(
                        checked         = selectedIds.size == groups.size && groups.isNotEmpty(),
                        onCheckedChange = { checked ->
                            selectedIds = if (checked) groups.map { it.id }.toSet() else emptySet()
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
                Spacer(Modifier.width(16.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    "Group name",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))

            // ── List / empty / loading ────────────────────────────────────────
            when {
                isLoading        -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                groups.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No groups found", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else             -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding      = PaddingValues(bottom = 24.dp)
                ) {
                    items(groups, key = { it.id }) { group ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier          = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked         = selectedIds.contains(group.id),
                                onCheckedChange = { checked ->
                                    if (selectionMode) {
                                        selectedIds = if (checked) selectedIds + group.id
                                        else selectedIds - group.id
                                    }
                                },
                                enabled  = selectionMode,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            GroupItem(
                                group    = group,
                                onClick  = {
                                    if (selectionMode) {
                                        selectedIds = if (selectedIds.contains(group.id))
                                            selectedIds - group.id
                                        else
                                            selectedIds + group.id
                                    } else {
                                        onGroupClick(group)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Delete confirmation dialog ────────────────────────────────────────────
    if (showDeleteConfirm) {
        val count = selectedIds.size
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title            = { Text("Delete $count group${if (count > 1) "s" else ""}?") },
            text             = { Text("This action cannot be undone.") },
            confirmButton    = {
                TextButton(onClick = {
                    onDeleteGroups(selectedIds.toList())
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

// ── GroupItem ────────────────────────────────────────────────────────────────

@Composable
fun GroupItem(
    group: GroupItemData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .border(1.dp, Color.Black, RoundedCornerShape(8.dp)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.Groups,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onPrimary,
                    modifier           = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Text(
                text     = group.name,
                style    = MaterialTheme.typography.titleMedium,
                color    = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Full-screen error shown when the backend is unreachable.
 */
@Composable
fun ConnectionErrorScreen(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.WifiOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Reintentar")
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun GroupsScreenPreview() {
    PasswordManagerTheme {
        GroupsScreenContent(
            groups              = listOf(
                GroupItemData("1", "Grupiño chulo", "owner-1"),
                GroupItemData("2", "Work team",     "owner-2"),
                GroupItemData("3", "Family",         "owner-3")
            ),
            searchQuery         = "",
            isLoading           = false,
            onSearchQueryChange = {},
            onAddGroupClick     = {},
            onGroupClick        = {},
            onDeleteGroups      = {}
        )
    }
}