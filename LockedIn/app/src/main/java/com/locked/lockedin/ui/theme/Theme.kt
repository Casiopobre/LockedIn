package com.locked.lockedin.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.locked.lockedin.ui.viewmodel.AppTheme

/**
 * Dark color scheme for the password manager
 */
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Grey900,
    surface = Grey800,
    onPrimary = Grey900,
    onSecondary = Grey900,
    onTertiary = Grey900,
    onBackground = Grey100,
    onSurface = Grey100,
)


/**
 * Light color scheme for the password manager
 */
private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Grey50,
    surface = Grey100,
    onPrimary = White,
    onSecondary = White,
    onTertiary = White,
    onBackground = Grey900,
    onSurface = Grey900,
)

// ui/theme/Theme.kt

@Composable
fun LockedInTheme(
    appTheme: AppTheme = AppTheme.SYSTEM, // Cambiamos Boolean por AppTheme
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    // Calculamos si debe usarse el modo oscuro según la selección
    val darkTheme = when (appTheme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Ajustamos el color de la barra de estado según el tema
            window.statusBarColor = colorScheme.surface.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            // Si el tema es oscuro, los iconos de la barra deben ser claros (false)
            controller.isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}