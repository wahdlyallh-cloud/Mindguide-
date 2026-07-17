package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = SageGreen,
    onPrimary = Color.White,
    primaryContainer = SageLight,
    onPrimaryContainer = EarthDark,
    secondary = Terracotta,
    onSecondary = Color.White,
    secondaryContainer = WarmSand,
    onSecondaryContainer = EarthDark,
    background = CreamBackground,
    onBackground = EarthDark,
    surface = SurfaceIvory,
    onSurface = EarthDark,
    surfaceVariant = WarmSand,
    onSurfaceVariant = EarthDark,
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = SageGreenDark,
    onPrimary = EarthDark,
    primaryContainer = SageGreen,
    onPrimaryContainer = EarthLight,
    secondary = Terracotta,
    onSecondary = Color.White,
    secondaryContainer = DarkSurface,
    onSecondaryContainer = EarthLight,
    background = DarkBackground,
    onBackground = EarthLight,
    surface = DarkSurface,
    onSurface = EarthLight,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = EarthLight,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disable dynamic coloring to force our gorgeous handcrafted therapeutic palette
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
