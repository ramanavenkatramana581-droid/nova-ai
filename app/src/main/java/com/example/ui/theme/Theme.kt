package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.data.preferences.NovaThemeAccent

@Composable
fun NovaTheme(
    accent: NovaThemeAccent = NovaThemeAccent.CYAN,
    content: @Composable () -> Unit
) {
    val (primaryColor, secondaryColor, tertiaryColor) = getAccentColors(accent)

    val colorScheme = darkColorScheme(
        primary = primaryColor,
        onPrimary = Color(0xFF030712),
        primaryContainer = primaryColor.copy(alpha = 0.2f),
        onPrimaryContainer = secondaryColor,
        secondary = secondaryColor,
        onSecondary = Color(0xFF030712),
        secondaryContainer = HudCardSurface,
        onSecondaryContainer = HologramWhite,
        tertiary = tertiaryColor,
        background = VoidBlack,
        onBackground = HologramWhite,
        surface = SpaceDark,
        onSurface = HologramWhite,
        surfaceVariant = HudSurfaceDark,
        onSurfaceVariant = HologramSubtext,
        outline = primaryColor.copy(alpha = 0.4f),
        outlineVariant = primaryColor.copy(alpha = 0.15f)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
