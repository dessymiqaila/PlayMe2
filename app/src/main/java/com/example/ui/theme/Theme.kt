package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PlayMeColorScheme = darkColorScheme(
    primary = CarrotOrange,
    onPrimary = Color.White,
    primaryContainer = CarrotOrangeDark,
    onPrimaryContainer = CarrotAmberLight,
    secondary = CarrotAmber,
    onSecondary = Color.Black,
    secondaryContainer = DarkSurfaceElevated,
    onSecondaryContainer = CarrotAmberLight,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    outlineVariant = DarkBorder.copy(alpha = 0.5f)
)

@Composable
fun PlayMeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PlayMeColorScheme,
        typography = Typography,
        content = content
    )
}
