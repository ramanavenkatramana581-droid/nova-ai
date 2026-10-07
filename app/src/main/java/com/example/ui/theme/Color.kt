package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.data.preferences.NovaThemeAccent

// Deep Cosmic Space Backgrounds
val VoidBlack = Color(0xFF030712)
val SpaceDark = Color(0xFF070F1E)
val HudSurfaceDark = Color(0xFF0D1B2A)
val HudCardSurface = Color(0xFF132238)
val HudGlassOverlay = Color(0x3300E5FF)

// Text & Telemetry
val HologramWhite = Color(0xFFF0F6FC)
val HologramSubtext = Color(0xFF8B9BB4)
val HologramMuted = Color(0xFF4D627F)

// Theme Accents
fun getAccentColors(accent: NovaThemeAccent): Triple<Color, Color, Color> {
    return when (accent) {
        NovaThemeAccent.CYAN -> Triple(
            Color(0xFF00E5FF), // Primary
            Color(0xFF80F3FF), // Light
            Color(0xFF00838F)  // Glow
        )
        NovaThemeAccent.EMERALD -> Triple(
            Color(0xFF00FF88),
            Color(0xFF80FFC4),
            Color(0xFF009944)
        )
        NovaThemeAccent.AMBER -> Triple(
            Color(0xFFFF9100),
            Color(0xFFFFC04D),
            Color(0xFFC56000)
        )
        NovaThemeAccent.VIOLET -> Triple(
            Color(0xFFB388FF),
            Color(0xFFD1B8FF),
            Color(0xFF7C4DFF)
        )
        NovaThemeAccent.CRIMSON -> Triple(
            Color(0xFFFF1744),
            Color(0xFFFF616F),
            Color(0xFFB7001E)
        )
    }
}
