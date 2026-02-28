package com.locked.lockedin.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.locked.lockedin.security.CryptoManager
import com.locked.lockedin.ui.viewmodel.GroupViewModel

@Composable
fun AddGroupPasswordScreen(
    groupViewModel: GroupViewModel,
    groupId: String,
    onNavigateBack: () -> Unit
) {
    var label            by remember { mutableStateOf("") }
    var password         by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var showGeneratorOptions by remember { mutableStateOf(false) }
    var genLength        by remember { mutableStateOf(16f) }
    var genUppercase     by remember { mutableStateOf(true) }
    var genLowercase     by remember { mutableStateOf(true) }
    var genNumbers       by remember { mutableStateOf(true) }
    var genSymbols       by remember { mutableStateOf(true) }

    val context = LocalContext.current
    val cryptoManager = remember { CryptoManager() }

    fun generateAndFill() {
        val generated = cryptoManager.generateSecurePassword(
            length           = genLength.toInt(),
            includeUppercase = genUppercase,
            includeLowercase = genLowercase,
            includeNumbers   = genNumbers,
            includeSymbols   = genSymbols
        )
        password = generated
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Generated password", generated))
        Toast.makeText(context, "Generated password copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    Surface(
        modifier = Modifier
            .wrapContentSize()
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .wrapContentSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Header ───────────────────────────────────────────────────────
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text  = "SHARE TO GROUP",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    BasicTextField(
                        value         = label,
                        onValueChange = { label = it },
                        textStyle     = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier      = Modifier.padding(top = 8.dp),
                        decorationBox = { innerTextField ->
                            Box {
                                if (label.isEmpty()) Text(
                                    "label (e.g. GitHub)",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                innerTextField()
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Box(
                    modifier        = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = label.take(1).uppercase().ifEmpty { "?" },
                        color      = MaterialTheme.colorScheme.onPrimary,
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Password field ────────────────────────────────────────────────
            Text(
                text       = "Password",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape    = RoundedCornerShape(15.dp),
                color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.padding(start = 12.dp)
                ) {
                    BasicTextField(
                        value               = password,
                        onValueChange       = { password = it },
                        textStyle           = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        visualTransformation = if (!isPasswordVisible)
                            PasswordVisualTransformation() else VisualTransformation.None,
                        modifier            = Modifier
                            .weight(1f)
                            .padding(vertical = 12.dp),
                        decorationBox       = { innerTextField ->
                            Box {
                                if (password.isEmpty()) Text(
                                    "Tap to enter password",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                innerTextField()
                            }
                        }
                    )
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector        = if (isPasswordVisible) Icons.Default.VisibilityOff
                            else Icons.Default.Visibility,
                            contentDescription = if (isPasswordVisible) "Hide" else "Show",
                            tint               = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = {
                        if (password.isNotBlank()) {
                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cb.setPrimaryClip(ClipData.newPlainText("Password", password))
                            Toast.makeText(context, "Password copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Generator buttons ─────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick  = { generateAndFill() },
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD1ECF1).copy(alpha = 0.9f),
                        contentColor   = Color(0xFF0C5460)
                    ),
                    shape    = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .border(1.dp, Color(0xFF0C5460).copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Generate", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick  = { showGeneratorOptions = !showGeneratorOptions },
                    shape    = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(44.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        if (showGeneratorOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Options", fontSize = 13.sp)
                }
            }

            // ── Generator options panel ───────────────────────────────────────
            AnimatedVisibility(visible = showGeneratorOptions, enter = expandVertically(), exit = shrinkVertically()) {
                Surface(
                    shape    = RoundedCornerShape(12.dp),
                    color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Length", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text("${genLength.toInt()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(value = genLength, onValueChange = { genLength = it }, valueRange = 8f..64f, steps = 55, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        Spacer(Modifier.height(8.dp))
                        Text("Include", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            GenCheckbox("A–Z Uppercase", genUppercase, { genUppercase = it }, Modifier.weight(1f))
                            GenCheckbox("a–z Lowercase", genLowercase, { genLowercase = it }, Modifier.weight(1f))
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            GenCheckbox("0–9 Numbers",  genNumbers,  { genNumbers = it },  Modifier.weight(1f))
                            GenCheckbox("!@# Symbols",  genSymbols,  { genSymbols = it },  Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick  = { generateAndFill() },
                            enabled  = genUppercase || genLowercase || genNumbers || genSymbols,
                            modifier = Modifier.fillMaxWidth(),
                            shape    = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Generate & fill", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Bottom actions ────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick  = onNavigateBack,
                    modifier = Modifier.size(56.dp),
                    colors   = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color(0xFFFFE599).copy(alpha = 0.8f)
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = "Go back", tint = Color.Black)
                }

                Button(
                    onClick  = {
                        groupViewModel.sharePassword(groupId, label, password) { result ->
                            if (result.isSuccess) onNavigateBack()
                        }
                    },
                    enabled  = label.isNotBlank() && password.isNotBlank(),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor   = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape    = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(64.dp)
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                ) {
                    Text("Share to Group", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun GenCheckbox(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.padding(vertical = 2.dp)) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}