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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.locked.lockedin.R
import com.locked.lockedin.data.model.PasswordEntry
import com.locked.lockedin.ui.theme.LockedInTheme
import com.locked.lockedin.security.CryptoManager
import com.locked.lockedin.ui.viewmodel.PasswordViewModel

/**
 * Screen for adding or editing a password entry.
 */
@Composable
fun AddEditPasswordScreen(
    viewModel: PasswordViewModel,
    passwordEntry: PasswordEntry? = null,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var title    by remember { mutableStateOf(passwordEntry?.title ?: "") }
    var username by remember { mutableStateOf(passwordEntry?.username ?: "") }
    var password by remember {
        mutableStateOf(
            passwordEntry?.let {
                viewModel.decryptPassword(it.encryptedPassword) ?: ""
            } ?: ""
        )
    }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var showDeleteDialog  by remember { mutableStateOf(false) }

    val isEditing = passwordEntry != null

    AddEditPasswordContent(
        title              = title,
        username           = username,
        password           = password,
        isPasswordVisible  = isPasswordVisible,
        isEditing          = isEditing,
        onTitleChange      = { title = it },
        onUsernameChange   = { username = it },
        onPasswordChange   = { password = it },
        onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
        onGoBack           = onNavigateBack,
        onDeleteClick      = { showDeleteDialog = true },
        onSaveClick        = {
            if (isEditing && passwordEntry != null) {
                viewModel.updatePasswordEntry(
                    id       = passwordEntry.id,
                    title    = title,
                    username = username,
                    password = password,
                    website  = passwordEntry.website,
                    notes    = passwordEntry.notes
                ) { result ->
                    if (result.isSuccess) onNavigateBack()
                }
            } else {
                viewModel.addPassword(title, username, password, "", "") { result ->
                    if (result.isSuccess) onNavigateBack()
                }
            }
        }
    )

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title            = { Text("Confirm Delete") },
            text             = { Text("Are you sure you want to delete this password?") },
            confirmButton    = {
                TextButton(onClick = {
                    passwordEntry?.let { entry ->
                        viewModel.deletePassword(entry) { onNavigateBack() }
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton    = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Stateless content (also used by the @Preview)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AddEditPasswordContent(
    title: String,
    username: String,
    password: String,
    isPasswordVisible: Boolean,
    isEditing: Boolean,
    onTitleChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onGoBack: () -> Unit,
    onDeleteClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    val context = LocalContext.current

    // ── Password generator state ─────────────────────────────────────────────
    var showGeneratorOptions by remember { mutableStateOf(false) }
    var genLength            by remember { mutableStateOf(16f) }
    var genUppercase         by remember { mutableStateOf(true) }
    var genLowercase         by remember { mutableStateOf(true) }
    var genNumbers           by remember { mutableStateOf(true) }
    var genSymbols           by remember { mutableStateOf(true) }

    val cryptoManager = remember { CryptoManager() }

    fun generateAndFill() {
        val generated = cryptoManager.generateSecurePassword(
            length           = genLength.toInt(),
            includeUppercase = genUppercase,
            includeLowercase = genLowercase,
            includeNumbers   = genNumbers,
            includeSymbols   = genSymbols
        )
        onPasswordChange(generated)
        copyToClipboard(context, "Generated password", generated)
    }

    Surface(
        modifier = Modifier
            .wrapContentSize()
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .wrapContentSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Header ───────────────────────────────────────────────────────
            Row(
                modifier           = Modifier.fillMaxWidth(),
                verticalAlignment  = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text  = "PASSWORD FOR",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    BasicTextField(
                        value          = title,
                        onValueChange  = onTitleChange,
                        textStyle      = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier       = Modifier.padding(top = 8.dp),
                        decorationBox  = { innerTextField ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box {
                                    if (title.isEmpty()) Text(
                                        "site_name",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                    innerTextField()
                                }
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
                        text       = title.take(1).uppercase().ifEmpty { "?" },
                        color      = MaterialTheme.colorScheme.onPrimary,
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Username field ────────────────────────────────────────────────
            FormSection(
                label         = "Login info",
                value         = username,
                subLabel      = "Email, tel ...",
                isPassword    = false,
                isVisible     = true,
                onValueChange = onUsernameChange,
                onToggleVisibility = {},
                onCopy        = { copyToClipboard(context, "Username", username) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Password field ────────────────────────────────────────────────
            FormSection(
                label              = "Password",
                value              = password,
                subLabel           = "Tap to reveal",
                isPassword         = true,
                isVisible          = isPasswordVisible,
                onValueChange      = onPasswordChange,
                onToggleVisibility = onTogglePasswordVisibility,
                onCopy             = { copyToClipboard(context, "Password", password) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Generate password button ──────────────────────────────────────
            Row(
                modifier          = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Main generate button
                Button(
                    onClick = { generateAndFill() },
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD1ECF1).copy(alpha = 0.9f),
                        contentColor   = Color(0xFF0C5460)
                    ),
                    shape   = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .border(1.dp, Color(0xFF0C5460).copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.shield_locked_24),
                        contentDescription = "Locked Shield",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Generate", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                // Toggle options button
                OutlinedButton(
                    onClick = { showGeneratorOptions = !showGeneratorOptions },
                    shape   = RoundedCornerShape(12.dp),
                    colors  = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.height(44.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        if (showGeneratorOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle generator options",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Options", fontSize = 13.sp)
                }
            }

            // ── Generator options panel (collapsible) ─────────────────────────
            AnimatedVisibility(
                visible = showGeneratorOptions,
                enter   = expandVertically(),
                exit    = shrinkVertically()
            ) {
                Surface(
                    shape    = RoundedCornerShape(12.dp),
                    color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Length slider
                        Row(
                            modifier          = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Length",
                                style      = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier   = Modifier.weight(1f)
                            )
                            Text(
                                "${genLength.toInt()}",
                                style      = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color      = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value         = genLength,
                            onValueChange = { genLength = it },
                            valueRange    = 8f..64f,
                            steps         = 55,
                            modifier      = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        Spacer(Modifier.height(8.dp))

                        // Character type checkboxes — 2x2 grid
                        Text(
                            "Include",
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            OptionCheckbox("A–Z Uppercase", genUppercase, { genUppercase = it }, Modifier.weight(1f))
                            OptionCheckbox("a–z Lowercase", genLowercase, { genLowercase = it }, Modifier.weight(1f))
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            OptionCheckbox("0–9 Numbers", genNumbers, { genNumbers = it }, Modifier.weight(1f))
                            OptionCheckbox("!@# Symbols",  genSymbols,  { genSymbols = it },  Modifier.weight(1f))
                        }

                        Spacer(Modifier.height(12.dp))

                        // Generate & fill button inside the panel
                        Button(
                            onClick  = { generateAndFill() },
                            enabled  = genUppercase || genLowercase || genNumbers || genSymbols,
                            modifier = Modifier.fillMaxWidth(),
                            shape    = RoundedCornerShape(10.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Generate & fill", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Bottom actions ────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick  = onGoBack,
                    modifier = Modifier.size(56.dp),
                    colors   = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color(0xFFFFE599).copy(alpha = 0.8f)
                    )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Reply,
                        contentDescription = "Go back",
                        tint               = Color.Black
                    )
                }

                if (isEditing) {
                    Button(
                        onClick  = onDeleteClick,
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor   = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        shape    = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .height(64.dp)
                            .fillMaxWidth(0.8f)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                                RoundedCornerShape(24.dp)
                            )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text       = "Delete password",
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Button(
                        onClick  = onSaveClick,
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor   = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(64.dp)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                RoundedCornerShape(24.dp)
                            )
                    ) {
                        Text(
                            "Save New Password",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (isEditing) {
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalIconButton(
                        onClick  = onSaveClick,
                        modifier = Modifier.size(56.dp),
                        colors   = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Save changes",
                            tint               = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Small checkbox row used inside the generator options panel
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OptionCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier          = modifier.padding(vertical = 2.dp)
    ) {
        Checkbox(
            checked         = checked,
            onCheckedChange = onCheckedChange,
            modifier        = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable form field with copy & optional password masking
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FormSection(
    label: String,
    value: String,
    subLabel: String,
    isPassword: Boolean,
    isVisible: Boolean,
    onValueChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    onCopy: () -> Unit
) {
    Column {
        Text(
            text       = label,
            style      = MaterialTheme.typography.titleMedium,
            color      = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape    = RoundedCornerShape(15.dp),
                color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border   = borderStroke(),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.padding(start = 12.dp)
                ) {
                    BasicTextField(
                        value               = value,
                        onValueChange       = onValueChange,
                        textStyle           = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        visualTransformation = if (isPassword && !isVisible)
                            PasswordVisualTransformation()
                        else
                            VisualTransformation.None,
                        modifier            = Modifier
                            .weight(1f)
                            .padding(vertical = 12.dp),
                        decorationBox       = { innerTextField ->
                            Box {
                                if (value.isEmpty()) Text(
                                    subLabel,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                innerTextField()
                            }
                        }
                    )

                    if (isPassword) {
                        IconButton(onClick = onToggleVisibility) {
                            Icon(
                                imageVector        = if (isVisible) Icons.Default.VisibilityOff
                                else Icons.Default.Visibility,
                                contentDescription = if (isVisible) "Hide password" else "Show password",
                                tint               = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(onClick = onCopy) {
                        Icon(
                            imageVector        = Icons.Default.ContentCopy,
                            contentDescription = "Copy $label",
                            tint               = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun copyToClipboard(context: Context, label: String, text: String) {
    if (text.isBlank()) return
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip      = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
}

@Composable
private fun borderStroke() = BorderStroke(
    width = 1.dp,
    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
)

// ─────────────────────────────────────────────────────────────────────────────
// Preview
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun AddEditPasswordScreenPreview() {
    LockedInTheme() {
        AddEditPasswordContent(
            title                    = "google.com",
            username                 = "user@gmail.com",
            password                 = "password123",
            isPasswordVisible        = false,
            isEditing                = false,
            onTitleChange            = {},
            onUsernameChange         = {},
            onPasswordChange         = {},
            onTogglePasswordVisibility = {},
            onGoBack                 = {},
            onDeleteClick            = {},
            onSaveClick              = {}
        )
    }
}