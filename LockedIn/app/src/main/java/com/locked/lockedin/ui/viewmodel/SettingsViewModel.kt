package com.locked.lockedin.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ─── Enums / models (puedes moverlos a un archivo aparte si prefieres) ───────

enum class AppTheme { SYSTEM, LIGHT, DARK }

data class Language(val code: String, val displayName: String, val flag: String)

val supportedLanguages = listOf(
    Language("es", "Español",   "🇪🇸"),
    Language("en", "English",   "🇬🇧"),
)

// ─── State ────────────────────────────────────────────────────────────────────

data class SettingsUiState(
    val theme: AppTheme = AppTheme.SYSTEM,
    val language: Language = supportedLanguages[0],
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

private const val PREFS_NAME  = "settings_prefs"
private const val KEY_THEME   = "theme"
private const val KEY_LANG    = "language"

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(loadSavedState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // ── Load ──────────────────────────────────────────────────────────────────

    private fun loadSavedState(): SettingsUiState {
        val themeName = prefs.getString(KEY_THEME, AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name
        val langCode  = prefs.getString(KEY_LANG,  "es") ?: "es"
        return SettingsUiState(
            theme    = AppTheme.valueOf(themeName),
            language = supportedLanguages.firstOrNull { it.code == langCode }
                ?: supportedLanguages[0],
        )
    }

    // ── Theme ─────────────────────────────────────────────────────────────────

    fun setTheme(theme: AppTheme) {
        _uiState.update { it.copy(theme = theme) }
        prefs.edit().putString(KEY_THEME, theme.name).apply()
        applyTheme(theme)
    }

    /** Llama esto en MainActivity.onCreate() para restaurar el tema guardado. */
    fun applyCurrentTheme() = applyTheme(_uiState.value.theme)

    private fun applyTheme(theme: AppTheme) {
        val mode = when (theme) {
            AppTheme.LIGHT  -> AppCompatDelegate.MODE_NIGHT_NO
            AppTheme.DARK   -> AppCompatDelegate.MODE_NIGHT_YES
            AppTheme.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    // ── Language ──────────────────────────────────────────────────────────────

    fun setLanguage(language: Language) {
        _uiState.update { it.copy(language = language) }
        prefs.edit().putString(KEY_LANG, language.code).apply()
        applyLanguage(language)
    }

    /** Llama esto en MainActivity.onCreate() para restaurar el idioma guardado. */
    fun applyCurrentLanguage() = applyLanguage(_uiState.value.language)

    private fun applyLanguage(language: Language) {
        // Per-App Language Preferences API (requiere appcompat 1.6+)
        val localeList = LocaleListCompat.forLanguageTags(language.code)
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}