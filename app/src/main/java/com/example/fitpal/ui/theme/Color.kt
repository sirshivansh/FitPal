package com.example.fitpal.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// DARK THEME (Sleek Glassmorphism Dark)
// ==========================================
// Canvas & Surfaces - Deep obsidian graphite canvas with translucent frosted slate-glass surfaces
val DarkCanvasBg = Color(0xFF090D16)           // Ultra-deep space graphite canvas
val DarkCardSurface = Color(0xEB131C2D)        // Frosted sleek slate glass (92% opacity)
val DarkCardSurfaceVariant = Color(0xCC1A2438) // Translucent micro-container / glass track
val DarkBorderStroke = Color(0x33FFFFFF)       // Specular frosted border (20% white)
val DarkSubtleBorder = Color(0x1AFFFFFF)       // Subtle 10% white stroke

// Glassmorphic Atmospheric Tokens
val GlassDarkFrostedSurface = Color(0xD9141D2C)
val GlassDarkSpecularRim = Color(0x59FFFFFF)
val GlassLightFrostedSurface = Color(0xEBFFFFFF)
val GlassLightSpecularRim = Color(0x99FFFFFF)

// Primary Athletic Neon Accent - Vibrant Electric Mint / Sports Green
val DarkPrimaryAccent = Color(0xFF00E599)       // Vibrant Electric Mint (#00E599) - crisp, energetic, athletic
val DarkPrimaryAccentGlow = Color(0x3300E599)   // 20% glow
val DarkPrimaryContainer = Color(0xFF0D281E)    // Deep athletic container
val DarkOnPrimaryContainer = Color(0xFF00E599)  // High contrast on container

// Secondary & Functional Accents
val DarkSecondaryAccent = Color(0xFF0EA5E9)     // Electric Azure
val DarkTertiaryFlame = Color(0xFFFF5722)       // Athletic Flame Coral (burn / calories)
val DarkTextPrimary = Color(0xFFF1F5F9)         // High-contrast slate-100 crisp text
val DarkTextSecondary = Color(0xFF94A3B8)       // Muted slate-400 secondary text

// ==========================================
// LIGHT THEME (Clean High-Contrast Studio Light)
// ==========================================
// Canvas & Surfaces - Luminous, crisp studio aesthetic
val LightCanvasBg = Color(0xFFF8FAFC)          // Pristine Slate-50 background
val LightCardSurface = Color(0xFFFFFFFF)       // Pure white crisp cards
val LightCardSurfaceVariant = Color(0xFFF1F5F9)// Elevated subtle Slate-100 container
val LightBorderStroke = Color(0xFFE2E8F0)      // Slate-200 1dp border
val LightSubtleBorder = Color(0xFFEDF2F7)      // Fine divider

// Primary Athletic Emerald Accent
val LightPrimaryAccent = Color(0xFF059669)     // Rich Athletic Emerald (#059669) - sharp contrast on white
val LightPrimaryContainer = Color(0xFFD1FAE5)  // Soft mint badge container
val LightOnPrimaryContainer = Color(0xFF065F46)// Deep forest on mint

// Secondary & Functional Accents
val LightSecondaryAccent = Color(0xFF0284C7)   // Ocean Azure
val LightTertiaryFlame = Color(0xFFEA580C)     // Vivid Sports Orange
val LightTextPrimary = Color(0xFF0F172A)       // Midnight Slate-900 high contrast text
val LightTextSecondary = Color(0xFF64748B)     // Slate-500 secondary text

// ==========================================
// METRIC & MACRONUTRIENT ACCENTS (Balanced for both)
// ==========================================
val MacroProteinColor = Color(0xFF0284C7)      // Vibrant Aqua Blue
val MacroCarbsColor = Color(0xFFF59E0B)        // Solar Amber
val MacroFatColor = Color(0xFFF43F5E)          // Vibrant Rose
val MetricWaterColor = Color(0xFF06B6D4)       // Cyan Blue
val MetricCalorieFlame = Color(0xFFFF5722)     // Athletic Flame

// Aliases & Backwards Compatibility (So existing code continues to compile smoothly)
val VividElectricLime = DarkPrimaryAccent
val ElectricLimeGlow = DarkPrimaryAccentGlow
val DarkOledCharcoal = DarkCanvasBg
val DarkMutedGreenBlack = DarkCardSurface
val DarkElevatedSurface = DarkCardSurfaceVariant
val DarkSurfaceStroke = DarkBorderStroke
val MutedForestOlive = DarkBorderStroke
val DarkForestContainer = DarkPrimaryContainer
val CrispWhite = DarkTextPrimary
val MutedGrey = DarkTextSecondary
val DarkSubtleGrey = DarkBorderStroke
val WarmBrownBronze = Color(0xFF6D4C41)
val EarthyGold = MacroCarbsColor
val CoralOrange = DarkTertiaryFlame
val VibrantCyan = MetricWaterColor
val ActiveBlue = DarkSecondaryAccent
