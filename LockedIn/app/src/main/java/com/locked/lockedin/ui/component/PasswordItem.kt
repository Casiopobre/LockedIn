package com.locked.lockedin.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.locked.lockedin.R
import com.locked.lockedin.data.model.PasswordEntry
import com.locked.lockedin.ui.theme.PasswordManagerTheme

/**
 * Composable for displaying a password entry item in the list.
 * Inspired by the provided sketch but refined with modern Material 3 aesthetics.
 */
private val PwnedRed        = Color(0xFFD32F2F)
private val PwnedRedSurface = Color(0xFFFFF3F3)

@Composable
fun PasswordItem(
    password: PasswordEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onCopyClick: () -> Unit = {}
) {
    val backgroundColor = if (password.isPwned) PwnedRedSurface else MaterialTheme.colorScheme.surface

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        color = backgroundColor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Identity Section (Pill)
            Surface(
                onClick = onClick,
                shape = RoundedCornerShape(15.dp),
                color = if (password.isPwned) PwnedRed.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (password.isPwned) PwnedRed.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                ),
                modifier = Modifier.wrapContentSize()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo Circle
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(if (password.isPwned) PwnedRed else MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = password.title.take(1).uppercase(),
                            color = if (password.isPwned) Color.White else MaterialTheme.colorScheme.onPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = password.website.ifBlank { password.title },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // User info and Pwned status
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = password.username,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (password.isPwned) {
                    Spacer(modifier = Modifier.height(4.dp))
                    PwnedBadge(count = password.pwnedCount)
                }
            }

            // Copy Action
            IconButton(
                onClick = onCopyClick,
                modifier = Modifier
                    .size(40.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy Password",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun PwnedBadge(count: Int, modifier: Modifier = Modifier) {
    Surface(
        shape  = MaterialTheme.shapes.extraSmall,
        color  = PwnedRed,
        modifier = modifier
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
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text  = "${stringResource(R.string.pwned_alert)} ($count)",
                style = MaterialTheme.typography.labelSmall.copy(
                    color      = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PasswordItemPreview() {
    PasswordManagerTheme {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PasswordItem(
                password = PasswordEntry(
                    title = "Google",
                    username = "you",
                    encryptedPassword = "encrypted",
                    website = "google.com"
                ),
                onClick = { }
            )

            PasswordItem(
                password = PasswordEntry(
                    title = "Adobe",
                    username = "creative_mind",
                    encryptedPassword = "encrypted",
                    website = "adobe.com",
                    isPwned = true,
                    pwnedCount = 3
                ),
                onClick = { }
            )
        }
    }
}
