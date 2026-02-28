package com.locked.lockedin.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.locked.lockedin.data.model.PasswordEntry
import java.text.SimpleDateFormat
import java.util.*

private val PwnedRed        = Color(0xFFD32F2F)
private val PwnedRedSurface = Color(0xFFFFF3F3)

@Composable
fun PasswordItem(
    password: PasswordEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPwned = password.isPwned

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPwned) 4.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPwned) PwnedRedSurface else MaterialTheme.colorScheme.surface
        ),
        border = if (isPwned) BorderStroke(1.5.dp, PwnedRed) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // ── Title row ─────────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text     = password.title,
                    style    = MaterialTheme.typography.titleMedium.copy(
                        color      = if (isPwned) PwnedRed else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isPwned) FontWeight.Bold else FontWeight.Normal
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Pwned badge
                if (isPwned) {
                    Spacer(modifier = Modifier.width(8.dp))
                    PwnedBadge(count = password.pwnedCount)
                }
            }

            // ── Breach warning strip ──────────────────────────────────────────
            if (isPwned) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector        = Icons.Default.Warning,
                        contentDescription = "Breached",
                        tint               = PwnedRed,
                        modifier           = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val times = if (password.pwnedCount > 0) "${password.pwnedCount} times" else "a known breach"
                    Text(
                        text  = "Found in $times — change this password!",
                        style = MaterialTheme.typography.labelSmall.copy(color = PwnedRed)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Username ──────────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Username",
                    tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text     = password.username,
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Website ───────────────────────────────────────────────────────
            if (password.website.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Language,
                        contentDescription = "Website",
                        tint     = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text     = password.website,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Timestamp ─────────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text  = "Updated ${formatDate(password.updatedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PwnedBadge(count: Int) {
    Surface(
        shape  = MaterialTheme.shapes.small,
        color  = PwnedRed,
        tonalElevation = 0.dp
    ) {
        Row(
            verticalAlignment    = Alignment.CenterVertically,
            modifier             = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Icon(
                imageVector        = Icons.Default.Warning,
                contentDescription = null,
                tint               = Color.White,
                modifier           = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text  = "PWNED",
                style = MaterialTheme.typography.labelSmall.copy(
                    color      = Color.White,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000       -> "just now"
        diff < 3_600_000    -> "${diff / 60_000}m ago"
        diff < 86_400_000   -> "${diff / 3_600_000}h ago"
        diff < 604_800_000  -> "${diff / 86_400_000}d ago"
        else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}