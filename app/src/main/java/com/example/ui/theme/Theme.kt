package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.domain.model.AppThemeMode

private val TrDarkColorScheme = darkColorScheme(
    primary = TrDarkPrimary,
    onPrimary = TrDarkBackground,
    primaryContainer = TrDarkSurfaceVariant,
    onPrimaryContainer = TrDarkPrimary,
    secondary = TrDarkSecondary,
    onSecondary = TrDarkBackground,
    tertiary = TrDarkTertiary,
    background = TrDarkBackground,
    onBackground = TrDarkOnSurface,
    surface = TrDarkSurface,
    onSurface = TrDarkOnSurface,
    surfaceVariant = TrDarkSurfaceVariant,
    onSurfaceVariant = TrDarkOnSurfaceVariant,
    outline = TrDarkGlassBorder,
    error = TrDarkError
)

private val TrLightColorScheme = lightColorScheme(
    primary = TrLightPrimary,
    onPrimary = TrLightSurface,
    primaryContainer = TrLightSurfaceVariant,
    onPrimaryContainer = TrLightPrimaryVariant,
    secondary = TrLightSecondary,
    onSecondary = TrLightSurface,
    tertiary = TrLightTertiary,
    background = TrLightBackground,
    onBackground = TrLightOnSurface,
    surface = TrLightSurface,
    onSurface = TrLightOnSurface,
    surfaceVariant = TrLightSurfaceVariant,
    onSurfaceVariant = TrLightOnSurfaceVariant,
    outline = TrLightGlassBorder,
    error = TrLightError
)

@Composable
fun TrVpnTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> systemInDark
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
    }

    val colorScheme = if (isDark) TrDarkColorScheme else TrLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
