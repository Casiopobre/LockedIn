package com.locked.lockedin.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.locked.lockedin.ui.theme.PasswordManagerTheme

/**
 * Screen for managing a specific group, displaying its members and passwords.
 */
@Composable
fun ManageGroupScreen(
    groupName: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Dummy data for members and passwords
    val members = listOf(
        MemberData(1, "Puri profe", "P"),
        MemberData(2, "Juan alumno", "J")
    )

    val passwords = listOf(
        PasswordEntry(id = 1, title = "Google", username = "you", website = "google.com", encryptedPassword = ""),
        PasswordEntry(id = 2, title = "GitHub", username = "you", website = "github.com", encryptedPassword = "")
    )

    ManageGroupScreenContent(
        groupName = groupName,
        members = members,
        passwords = passwords,
        onBackClick = onBackClick,
        onAddMemberClick = { /* TODO */ },
        onDelMemberClick = { /* TODO */ },
        onAddPasswordClick = { /* TODO */ },
        onDelPasswordClick = { /* TODO */ },
        onPasswordClick = { /* TODO */ },
        modifier = modifier
    )
}

data class MemberData(
    val id: Int,
    val name: String,
    val initial: String
)

@Composable
fun ManageGroupScreenContent(
    groupName: String,
    members: List<MemberData>,
    passwords: List<PasswordEntry>,
    onBackClick: () -> Unit,
    onAddMemberClick: () -> Unit,
    onDelMemberClick: () -> Unit,
    onAddPasswordClick: () -> Unit,
    onDelPasswordClick: () -> Unit,
    onPasswordClick: (PasswordEntry) -> Unit,
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
                // Header: Group Name, Member count
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
                            text = "${members.size} members",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Scrollable content
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

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
                        ActionSmallButton(
                            text = "Del. member",
                            icon = Icons.Default.PersonRemove,
                            backgroundColor = Color(0xFFF4C2C2),
                            onClick = onDelMemberClick,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    members.forEach { member ->
                        MemberRow(member)
                        Spacer(modifier = Modifier.height(12.dp))
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
                        ActionSmallButton(
                            text = "Del password",
                            icon = Icons.Default.Lock,
                            backgroundColor = Color(0xFFF4C2C2),
                            onClick = onDelPasswordClick,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    passwords.forEach { password ->
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
                                password = password,
                                onClick = { onPasswordClick(password) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Account for the floating button at the bottom
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }

            // Back button moved to bottom right
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
            .height(44.dp) // Fixed height to prevent abnormal stretching
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun MemberRow(member: MemberData) {
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
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = member.initial,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(16.dp))
        Text(
            text = member.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun ManageGroupScreenPreview() {
    PasswordManagerTheme {
        ManageGroupScreen(
            groupName = "Grupiño chulo",
            onBackClick = {}
        )
    }
}
