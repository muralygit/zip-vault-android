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

private val DarkColorScheme = darkColorScheme(
    primary = SlateDarkPrimary,
    secondary = SlateDarkSecondary,
    tertiary = SlateDarkTertiary,
    background = SlateDarkBackground,
    surface = SlateDarkSurface,
    surfaceVariant = SlateDarkSurfaceVariant,
    onPrimary = SlateDarkOnPrimary,
    onBackground = SlateDarkOnBackground,
    onSurface = SlateDarkOnSurface,
    outline = SlateDarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = SlateLightPrimary,
    secondary = SlateLightSecondary,
    tertiary = SlateLightTertiary,
    background = SlateLightBackground,
    surface = SlateLightSurface,
    surfaceVariant = SlateLightSurfaceVariant,
    onPrimary = SlateLightOnPrimary,
    onBackground = SlateLightOnBackground,
    onSurface = SlateLightOnSurface,
    outline = SlateLightOutline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep dynamic color disabled by default to show our custom Slate/Indigo palette
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> DarkColorScheme // Force dark theme for the premium "dark slate/indigo dashboard" experience, or allow darkTheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
