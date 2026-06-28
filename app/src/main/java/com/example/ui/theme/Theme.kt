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
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFF56AB7C), // Lighter green for dark theme
    onPrimary = Color(0xFF00391F),
    primaryContainer = Color(0xFF0F5234),
    onPrimaryContainer = Color(0xFFD6E8D1),
    background = Color(0xFF191C1A), // Dark slate
    onBackground = Color(0xFFE1E3DE),
    surface = Color(0xFF1E211F),
    onSurface = Color(0xFFE1E3DE),
    surfaceVariant = Color(0xFF414941),
    onSurfaceVariant = Color(0xFFC1C9C1),
    outline = Color(0xFF8B938B),
    secondary = Color(0xFFC1C9C1),
    onSecondary = Color(0xFF2B322B)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = SleekPrimary,
    onPrimary = Color.White,
    primaryContainer = SleekPrimaryContainer,
    onPrimaryContainer = SleekOnPrimaryContainer,
    background = SleekBackground,
    onBackground = SleekText,
    surface = Color.White,
    onSurface = SleekText,
    surfaceVariant = SleekBottomNavBg,
    onSurfaceVariant = SleekSecondaryText,
    outline = SleekBorder,
    secondary = SleekSecondaryText,
    onSecondary = Color.White,
    tertiary = SleekExerciseBg,
    onTertiary = SleekText
  )

var isDarkThemeGlobal by mutableStateOf<Boolean?>(null)
var globalFontFamilyName by mutableStateOf("SansSerif")
var globalFontSizeScale by mutableStateOf(1.0f)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isDarkThemeGlobal ?: isSystemInDarkTheme(),
  // Set to false by default so our "Sleek Interface" theme is active and visible!
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  val fontFamily = when (globalFontFamilyName) {
    "Serif" -> androidx.compose.ui.text.font.FontFamily.Serif
    "Monospace" -> androidx.compose.ui.text.font.FontFamily.Monospace
    "Cursive" -> androidx.compose.ui.text.font.FontFamily.Cursive
    else -> androidx.compose.ui.text.font.FontFamily.Default
  }

  val scale = globalFontSizeScale
  val defaultTypography = androidx.compose.material3.Typography()

  val customTypography = androidx.compose.material3.Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = fontFamily, fontSize = (defaultTypography.displayLarge.fontSize.value * scale).sp, lineHeight = (defaultTypography.displayLarge.lineHeight.value * scale).sp),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = fontFamily, fontSize = (defaultTypography.displayMedium.fontSize.value * scale).sp, lineHeight = (defaultTypography.displayMedium.lineHeight.value * scale).sp),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = fontFamily, fontSize = (defaultTypography.displaySmall.fontSize.value * scale).sp, lineHeight = (defaultTypography.displaySmall.lineHeight.value * scale).sp),
    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = fontFamily, fontSize = (defaultTypography.headlineLarge.fontSize.value * scale).sp, lineHeight = (defaultTypography.headlineLarge.lineHeight.value * scale).sp),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = fontFamily, fontSize = (defaultTypography.headlineMedium.fontSize.value * scale).sp, lineHeight = (defaultTypography.headlineMedium.lineHeight.value * scale).sp),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = fontFamily, fontSize = (defaultTypography.headlineSmall.fontSize.value * scale).sp, lineHeight = (defaultTypography.headlineSmall.lineHeight.value * scale).sp),
    titleLarge = defaultTypography.titleLarge.copy(fontFamily = fontFamily, fontSize = (defaultTypography.titleLarge.fontSize.value * scale).sp, lineHeight = (defaultTypography.titleLarge.lineHeight.value * scale).sp),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = fontFamily, fontSize = (defaultTypography.titleMedium.fontSize.value * scale).sp, lineHeight = (defaultTypography.titleMedium.lineHeight.value * scale).sp),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = fontFamily, fontSize = (defaultTypography.titleSmall.fontSize.value * scale).sp, lineHeight = (defaultTypography.titleSmall.lineHeight.value * scale).sp),
    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = fontFamily, fontSize = (defaultTypography.bodyLarge.fontSize.value * scale).sp, lineHeight = (defaultTypography.bodyLarge.lineHeight.value * scale).sp),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = fontFamily, fontSize = (defaultTypography.bodyMedium.fontSize.value * scale).sp, lineHeight = (defaultTypography.bodyMedium.lineHeight.value * scale).sp),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = fontFamily, fontSize = (defaultTypography.bodySmall.fontSize.value * scale).sp, lineHeight = (defaultTypography.bodySmall.lineHeight.value * scale).sp),
    labelLarge = defaultTypography.labelLarge.copy(fontFamily = fontFamily, fontSize = (defaultTypography.labelLarge.fontSize.value * scale).sp, lineHeight = (defaultTypography.labelLarge.lineHeight.value * scale).sp),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = fontFamily, fontSize = (defaultTypography.labelMedium.fontSize.value * scale).sp, lineHeight = (defaultTypography.labelMedium.lineHeight.value * scale).sp),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = fontFamily, fontSize = (defaultTypography.labelSmall.fontSize.value * scale).sp, lineHeight = (defaultTypography.labelSmall.lineHeight.value * scale).sp)
  )

  MaterialTheme(colorScheme = colorScheme, typography = customTypography, content = content)
}
