package com.example.fitpal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

var isDarkThemeGlobal: Boolean? by mutableStateOf(null)
var globalFontFamilyName: String by mutableStateOf("SansSerif")
var globalFontSizeScale: Float by mutableStateOf(1.0f)

val DarkSleekColorScheme = darkColorScheme(
    primary = DarkPrimaryAccent,                  // Vibrant Electric Mint
    onPrimary = Color(0xFF041804),                // High contrast on mint
    primaryContainer = DarkPrimaryContainer,       // Layered athletic container
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondaryAccent,             // Electric Azure
    onSecondary = Color.White,
    secondaryContainer = DarkCardSurfaceVariant, // Layered surface
    onSecondaryContainer = DarkTextPrimary,
    tertiary = DarkTertiaryFlame,                 // Athletic Flame Coral
    onTertiary = Color.White,
    background = DarkCanvasBg,                    // Deep graphite canvas (0xFF0A0D14)
    surface = DarkCardSurface,                    // Primary Surface & Card Background (0xFF131822)
    surfaceVariant = DarkCardSurfaceVariant,     // Micro-container Surface (0xFF1A2130)
    onBackground = DarkTextPrimary,               // High-contrast crisp text (0xFFF1F5F9)
    onSurface = DarkTextPrimary,                  // High-contrast crisp text
    onSurfaceVariant = DarkTextSecondary,         // Neutral muted text (0xFF94A3B8)
    outline = DarkBorderStroke,                   // Mechanical borders (0xFF263248)
    outlineVariant = DarkSubtleBorder             // Subtle 10% white stroke
)

val LightSleekColorScheme = lightColorScheme(
    primary = LightPrimaryAccent,                // Rich Athletic Emerald
    onPrimary = Color.White,
    primaryContainer = LightPrimaryContainer,     // Soft mint container
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondaryAccent,            // Ocean Azure
    onSecondary = Color.White,
    secondaryContainer = LightCardSurfaceVariant,// Slate-100 container
    onSecondaryContainer = LightTextPrimary,
    tertiary = LightTertiaryFlame,                // Vivid Sports Orange
    onTertiary = Color.White,
    background = LightCanvasBg,                   // Pristine Slate-50 background (0xFFF8FAFC)
    surface = LightCardSurface,                   // Pure White Cards (0xFFFFFFFF)
    surfaceVariant = LightCardSurfaceVariant,    // Elevated subtle container (0xFFF1F5F9)
    onBackground = LightTextPrimary,              // Midnight Slate-900 high contrast (0xFF0F172A)
    onSurface = LightTextPrimary,                 // Midnight Slate-900 high contrast
    onSurfaceVariant = LightTextSecondary,        // Slate-500 secondary text (0xFF64748B)
    outline = LightBorderStroke,                  // Slate-200 border (0xFFE2E8F0)
    outlineVariant = LightSubtleBorder            // Fine divider
)

@Composable
fun FitPalTheme(
    darkTheme: Boolean = isDarkThemeGlobal ?: isSystemInDarkTheme(),
    fontFamilyName: String = globalFontFamilyName,
    fontSizeScale: Float = globalFontSizeScale,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkSleekColorScheme else LightSleekColorScheme
    val dynamicTypography = androidx.compose.runtime.remember(fontFamilyName, fontSizeScale) {
        createFitPalTypography(fontFamilyName, fontSizeScale)
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = dynamicTypography,
        shapes = FitPalShapes,
        content = content
    )
}
