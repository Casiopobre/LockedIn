package com.locked.lockedin.ui.screen

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Launch
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.locked.lockedin.ui.viewmodel.AppTheme
import com.locked.lockedin.ui.viewmodel.Language
import com.locked.lockedin.ui.viewmodel.SettingsViewModel
import com.locked.lockedin.ui.viewmodel.supportedLanguages
import androidx.core.net.toUri

// ---------------------------------------------------------------------------
// SettingsScreen — entry point
// ---------------------------------------------------------------------------

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()

    SettingsContent(
        currentTheme       = uiState.theme,
        currentLanguage    = uiState.language,
        onThemeSelected    = viewModel::setTheme,
        onLanguageSelected = viewModel::setLanguage,
        onNavigateBack     = onNavigateBack,
    )
}

// ---------------------------------------------------------------------------
// Pure-UI composable (easy to preview / test)
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    currentTheme: AppTheme,
    currentLanguage: Language,
    onThemeSelected: (AppTheme) -> Unit,
    onLanguageSelected: (Language) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingsSection(title = "Apariencia") {
                ThemeSelector(currentTheme = currentTheme, onThemeSelected = onThemeSelected)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                LanguageSelector(
                    currentLanguage    = currentLanguage,
                    languages          = supportedLanguages,
                    onLanguageSelected = onLanguageSelected,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            SettingsSection(title = "Autocompletado de contraseñas") {
                AutofillSection(
                    onOpenAndroidAutofillSettings = {
                        // ✅ REQUEST_SET_AUTOFILL_SERVICE necesita la URI del paquete
                        val intent = Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).apply {
                            data = "package:${context.packageName}".toUri()
                        }
                        // Fallback: si el dispositivo no soporta el intent directo,
                        // abrimos los ajustes generales de autocompletado
                        val fallback = Intent(Settings.ACTION_SETTINGS)

                        try {
                            context.startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            try {
                                context.startActivity(fallback)
                            } catch (e2: ActivityNotFoundException) {
                                // Último recurso: no hacer nada o mostrar un Toast
                            }
                        }
                    },
                    onOpenChromeAutofillSettings = {
                        val chooser = Intent.createChooser(
                            Intent(Intent.ACTION_APPLICATION_PREFERENCES).apply {
                                addCategory(Intent.CATEGORY_DEFAULT)
                                addCategory(Intent.CATEGORY_APP_BROWSER)
                                addCategory(Intent.CATEGORY_PREFERENCE)
                            },
                            "Selecciona el canal de Chrome",
                        )
                        context.startActivity(chooser)
                    },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// ThemeSelector
// ---------------------------------------------------------------------------

@Composable
private fun ThemeSelector(currentTheme: AppTheme, onThemeSelected: (AppTheme) -> Unit) {
    val themeOptions = listOf(
        Triple(AppTheme.SYSTEM, "Sistema", Icons.Outlined.SettingsSuggest),
        Triple(AppTheme.LIGHT,  "Claro",   Icons.Outlined.LightMode),
        Triple(AppTheme.DARK,   "Oscuro",  Icons.Outlined.DarkMode),
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsItemHeader(icon = Icons.Outlined.Palette, title = "Tema")
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            themeOptions.forEach { (theme, label, icon) ->
                ThemeChip(
                    modifier = Modifier.weight(1f),
                    label    = label,
                    icon     = icon,
                    selected = currentTheme == theme,
                    onClick  = { onThemeSelected(theme) },
                )
            }
        }
    }
}

@Composable
private fun ThemeChip(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier       = modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        shape          = RoundedCornerShape(12.dp),
        color          = if (selected) colors.primaryContainer else colors.surfaceVariant,
        tonalElevation = if (selected) 4.dp else 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
                modifier           = Modifier.size(22.dp),
            )
            Text(
                text       = label,
                style      = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color      = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// LanguageSelector
// ---------------------------------------------------------------------------

@Composable
private fun LanguageSelector(
    currentLanguage: Language,
    languages: List<Language>,
    onLanguageSelected: (Language) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "arrow")

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Outlined.Language, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Idioma", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    "${currentLanguage.flag}  ${currentLanguage.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Default.ExpandMore,
                if (expanded) "Colapsar" else "Expandir",
                modifier = Modifier.rotate(arrowRotation),
                tint     = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                languages.forEach { lang ->
                    val isSelected = lang.code == currentLanguage.code
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            )
                            .clickable { onLanguageSelected(lang); expanded = false }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment    = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(lang.flag, fontSize = 20.sp)
                        Text(
                            lang.displayName,
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color      = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface,
                            modifier   = Modifier.weight(1f),
                        )
                        if (isSelected) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// AutofillSection
// ---------------------------------------------------------------------------

@Composable
private fun AutofillSection(
    onOpenAndroidAutofillSettings: () -> Unit,
    onOpenChromeAutofillSettings: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp).padding(top = 2.dp))
                Text(
                    "Para que LockedIn pueda autocompletar tus credenciales, habilítalo " +
                            "en los ajustes del sistema y, si usas Chrome, también en el navegador.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }

        AutofillStep(
            stepNumber  = 1,
            title       = "Habilitar en Android",
            description = "Ve a Ajustes del sistema → Autocompletar y selecciona LockedIn como servicio.",
            actionLabel = "Abrir ajustes de autocompletado",
            actionIcon  = Icons.Outlined.PhoneAndroid,
            onAction    = onOpenAndroidAutofillSettings,
        )

        HorizontalDivider()

        AutofillStep(
            stepNumber  = 2,
            title       = "Habilitar en Chrome",
            description = "Abre los ajustes de Chrome → Contraseñas → activa LockedIn como proveedor de terceros.",
            actionLabel = "Abrir ajustes de Chrome",
            actionIcon  = Icons.Outlined.OpenInBrowser,
            onAction    = onOpenChromeAutofillSettings,
        )
    }
}

@Composable
private fun AutofillStep(
    stepNumber: Int,
    title: String,
    description: String,
    actionLabel: String,
    actionIcon: ImageVector,
    onAction: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stepNumber.toString(), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        }
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 36.dp))
        Spacer(Modifier.height(2.dp))
        Button(onClick = onAction, modifier = Modifier.fillMaxWidth().padding(start = 36.dp), shape = RoundedCornerShape(10.dp)) {
            Icon(actionIcon, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(actionLabel, style = MaterialTheme.typography.labelMedium)
        }
    }
}

// ---------------------------------------------------------------------------
// Shared helpers
// ---------------------------------------------------------------------------

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            title.uppercase(),
            style      = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.2.sp,
            modifier   = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
        )
        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsItemHeader(icon: ImageVector, title: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment    = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

// ---------------------------------------------------------------------------
// Preview
// ---------------------------------------------------------------------------

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SettingsScreenPreview() {
    MaterialTheme {
        SettingsContent(
            currentTheme       = AppTheme.SYSTEM,
            currentLanguage    = supportedLanguages[0],
            onThemeSelected    = {},
            onLanguageSelected = {},
            onNavigateBack     = {},
        )
    }
}