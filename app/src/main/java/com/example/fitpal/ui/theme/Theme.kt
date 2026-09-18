package com.example.fitpal.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

var isDarkThemeGlobal: Boolean? by mutableStateOf(null)
var globalFontFamilyName: String by mutableStateOf("SansSerif")
var globalFontSizeScale: Float by mutableStateOf(1.0f)

val DarkSleekColorScheme = darkColorScheme(
    primary = VividElectricLime,                  // Electric Lime CTAs, selected tabs, progress curves
    onPrimary = Color(0xFF041804),                // High contrast on lime
    primaryContainer = DarkForestContainer,       // Layered olive/forest container
    onPrimaryContainer = VividElectricLime,
    secondary = MutedForestOlive,                 // Muted Forest Olive
    onSecondary = CrispWhite,
    secondaryContainer = DarkElevatedSurface,     // Layered surface
    onSecondaryContainer = CrispWhite,
    tertiary = CoralOrange,                       // Coral Orange for badge indicators & burn
    onTertiary = CrispWhite,
    background = DarkOledCharcoal,                // Deep OLED Charcoal (0xFF0C0F0C)
    surface = DarkMutedGreenBlack,                // Dark Muted Green-Black (0xFF141A14)
    surfaceVariant = DarkElevatedSurface,         // Layered Elevated Surface (0xFF1E251E)
    onBackground = CrispWhite,                    // High-contrast crisp white (0xFFF5F5F7)
    onSurface = CrispWhite,                       // High-contrast crisp white
    onSurfaceVariant = MutedGrey,                 // Neutral muted grey (0xFF9CA3AF)
    outline = MutedForestOlive,                   // Chip borders & dividers
    outlineVariant = DarkSurfaceStroke            // Subtle 1dp border: Color.White.copy(alpha = 0.08f)
)

@Composable
fun FitPalTheme(
    darkTheme: Boolean = true, // Defaulting to the dark, sleek, high-contrast design
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkSleekColorScheme,
        typography = FitPalTypography,
        shapes = FitPalShapes,
        content = content
    )
}

