package com.locked.lockedin.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.locked.lockedin.ui.viewmodel.PasswordViewModel

/**
 * DEBUG ONLY — remove before release.
 *
 * A collapsible panel rendered at the top of MainScreen that lets you:
 *  - Force a breach check right now (bypasses the 24 h throttle)
 *  - Reset the 24 h timer so the next app open triggers automatically
 *  - Add a known-bad test password ("password") to verify red highlighting
 */
@Composable
fun DebugBreachPanel(
    viewModel: PasswordViewModel,
    onForceCheck: () -> Unit,
    onResetTimer: () -> Unit,
    onAddTestPassword: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()

    Surface(
        color         = Color(0xFF1A237E).copy(alpha = 0.08f),
        shape         = MaterialTheme.shapes.medium,
        tonalElevation = 0.dp,
        modifier      = modifier.fillMaxWidth()
    ) {
        Column {
            // ── Header row ────────────────────────────────────────────────
            Row(
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector        = Icons.Default.BugReport,
                        contentDescription = null,
                        tint               = Color(0xFF1A237E),
                        modifier           = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text  = "DEBUG — Breach Check",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color      = Color(0xFF1A237E),
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                IconButton(
                    onClick  = { expanded = !expanded },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector        = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint               = Color(0xFF1A237E)
                    )
                }
            }

            // ── Expandable controls ───────────────────────────────────────
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(12.dp, 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Status line
                    val statusText = when {
                        uiState.isBreachCheckRunning -> {
                            // Wrap the expression in parentheses so the destructuring works correctly
                            val (c, t) = (uiState.breachCheckProgress ?: (0 to 0))
                            "Running… $c / $t"
                        }
                        uiState.lastBreachCheckPwnedCount > 0 ->
                            "Last result: ${uiState.lastBreachCheckPwnedCount} pwned"
                        else -> "No check running"
                    }
                    Text(
                        text  = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF1A237E)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Force check now
                        OutlinedButton(
                            onClick  = onForceCheck,
                            enabled  = !uiState.isBreachCheckRunning,
                            modifier = Modifier.weight(1f),
                            colors   = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF1A237E)
                            )
                        ) {
                            Text("Force check", style = MaterialTheme.typography.labelSmall)
                        }

                        // Reset 24 h timer
                        OutlinedButton(
                            onClick  = onResetTimer,
                            modifier = Modifier.weight(1f),
                            colors   = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF6A1B9A)
                            )
                        ) {
                            Text("Reset timer", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    // Add a definitely-pwned password for visual testing
                    OutlinedButton(
                        onClick  = onAddTestPassword,
                        modifier = Modifier.fillMaxWidth(),
                        colors   = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFB71C1C)
                        )
                    ) {
                        Text(
                            "Add test entry (\"password\" — will be flagged)",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}