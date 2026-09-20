package com.example.fitpal.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.fitpal.R

// Modern geometric sans font family inspired by Nothing OS design language
val ModernFontFamily = FontFamily(
    Font(R.font.space_grotesk, FontWeight.Normal),
    Font(R.font.space_grotesk, FontWeight.Medium),
    Font(R.font.space_grotesk, FontWeight.SemiBold),
    Font(R.font.space_grotesk, FontWeight.Bold)
)

// Monospaced tabular numeral font family for perfect numerical alignment in metric cards
val TabularMonoFontFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium),
    Font(R.font.jetbrains_mono, FontWeight.SemiBold),
    Font(R.font.jetbrains_mono, FontWeight.Bold)
)

val MonoFontFamily = TabularMonoFontFamily

// Tabular metric numerals for technical sports-tech readability and alignment
val MetricNumeralXLarge = TextStyle(
    fontFamily = TabularMonoFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 38.sp,
    letterSpacing = (-0.5).sp,
    fontFeatureSettings = "tnum",
    color = Color.Unspecified
)

val MetricNumeralLarge = TextStyle(
    fontFamily = TabularMonoFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 30.sp,
    letterSpacing = (-0.5).sp,
    fontFeatureSettings = "tnum",
    color = Color.Unspecified
)

val MetricNumeralMedium = TextStyle(
    fontFamily = TabularMonoFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 22.sp,
    letterSpacing = (-0.2).sp,
    fontFeatureSettings = "tnum",
    color = Color.Unspecified
)

val MetricNumeralSmall = TextStyle(
    fontFamily = TabularMonoFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
    letterSpacing = 0.sp,
    fontFeatureSettings = "tnum",
    color = Color.Unspecified
)

val MetricNumeralTiny = TextStyle(
    fontFamily = TabularMonoFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    letterSpacing = 0.5.sp,
    fontFeatureSettings = "tnum",
    color = Color.Unspecified
)

fun createFitPalTypography(
    fontFamilyName: String = "SansSerif",
    scale: Float = 1.0f
): Typography {
    val family = when (fontFamilyName) {
        "Serif" -> FontFamily.Serif
        "Monospace" -> FontFamily.Monospace
        "Cursive" -> FontFamily.Cursive
        else -> ModernFontFamily
    }

    fun scaleSp(sp: Float): androidx.compose.ui.unit.TextUnit = (sp * scale).sp

    return Typography(
        displayLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Bold,
            fontSize = scaleSp(40f),
            letterSpacing = (-1.0).sp,
            color = Color.Unspecified
        ),
        displayMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Bold,
            fontSize = scaleSp(32f),
            letterSpacing = (-0.8).sp,
            color = Color.Unspecified
        ),
        displaySmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Bold,
            fontSize = scaleSp(28f),
            letterSpacing = (-0.5).sp,
            color = Color.Unspecified
        ),
        headlineLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Bold,
            fontSize = scaleSp(26f),
            letterSpacing = (-0.5).sp,
            color = Color.Unspecified
        ),
        headlineMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Bold,
            fontSize = scaleSp(22f),
            letterSpacing = (-0.3).sp,
            color = Color.Unspecified
        ),
        headlineSmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Bold,
            fontSize = scaleSp(19f),
            letterSpacing = (-0.2).sp,
            color = Color.Unspecified
        ),
        titleLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Bold,
            fontSize = scaleSp(18f),
            letterSpacing = (-0.2).sp,
            color = Color.Unspecified
        ),
        titleMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = scaleSp(15f),
            letterSpacing = 0.sp,
            color = Color.Unspecified
        ),
        titleSmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = scaleSp(13f),
            letterSpacing = 0.sp,
            color = Color.Unspecified
        ),
        bodyLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = scaleSp(15f),
            letterSpacing = 0.15.sp,
            color = Color.Unspecified
        ),
        bodyMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = scaleSp(13f),
            letterSpacing = 0.2.sp,
            color = Color.Unspecified
        ),
        bodySmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = scaleSp(12f),
            letterSpacing = 0.3.sp,
            color = Color.Unspecified
        ),
        labelLarge = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = scaleSp(14f),
            letterSpacing = 0.1.sp,
            color = Color.Unspecified
        ),
        labelMedium = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = scaleSp(11f),
            letterSpacing = 1.0.sp,
            color = Color.Unspecified
        ),
        labelSmall = TextStyle(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = scaleSp(10f),
            letterSpacing = 1.2.sp,
            color = Color.Unspecified
        )
    )
}

val FitPalTypography = createFitPalTypography("SansSerif", 1.0f)
