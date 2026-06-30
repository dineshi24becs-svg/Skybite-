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

private val SkyDarkColorScheme = darkColorScheme(
    primary = SkyDarkAccent,
    secondary = SkyDarkOrange,
    background = SkyDarkBackground,
    surface = SkyDarkSurface,
    surfaceVariant = SkyDarkSurfaceVariant,
    onPrimary = SkyDarkBackground,
    onSecondary = SkyDarkBackground,
    onBackground = SkyDarkTextPrimary,
    onSurface = SkyDarkTextPrimary,
    onSurfaceVariant = SkyDarkTextSecondary
)

private val SkyLightColorScheme = lightColorScheme(
    primary = SkyLightAccent,
    secondary = SkyLightOrange,
    background = SkyLightBackground,
    surface = SkyLightSurface,
    surfaceVariant = SkyLightSurfaceVariant,
    onPrimary = SkyLightBackground,
    onSecondary = SkyLightBackground,
    onBackground = SkyLightTextPrimary,
    onSurface = SkyLightTextPrimary,
    onSurfaceVariant = SkyLightTextSecondary
)

@Composable
fun SkyBiteTheme(
    darkTheme: Boolean = true, // Default to dark mode as requested
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) SkyDarkColorScheme else SkyLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Keep old theme wrapper for compatibility with tests if any, but mapping to SkyBiteTheme
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    SkyBiteTheme(darkTheme = darkTheme, content = content)
}
