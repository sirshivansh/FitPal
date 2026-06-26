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

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
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

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
