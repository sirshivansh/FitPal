package com.example.fitpal.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.fitpal.ui.theme.*

/**
 * Atmospheric background canvas that paints subtle, ambient, multi-stop aurora radial glows
 * beneath the application layers. This brings translucent frosted glass surfaces to life with
 * deep, luminous light play, refraction, and specular contrast.
 */
@Composable
fun GlassAtmosphericBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isDarkThemeGlobal ?: isSystemInDarkTheme()
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val canvasBg = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(canvasBg)
            .drawBehind {
                // Base subtle gradient wash
                val w = size.width
                val h = size.height

                // Top-left primary ambient glow (Electric Mint / Athletic Green)
                val mintAlpha = if (isDark) 0.16f else 0.10f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(primary.copy(alpha = mintAlpha), Color.Transparent),
                        center = Offset(w * 0.15f, h * 0.12f),
                        radius = w * 0.75f
                    ),
                    center = Offset(w * 0.15f, h * 0.12f),
                    radius = w * 0.75f
                )

                // Middle-right secondary glow (Electric Azure / Ocean Cyan)
                val azureAlpha = if (isDark) 0.13f else 0.08f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(secondary.copy(alpha = azureAlpha), Color.Transparent),
                        center = Offset(w * 0.88f, h * 0.38f),
                        radius = w * 0.65f
                    ),
                    center = Offset(w * 0.88f, h * 0.38f),
                    radius = w * 0.65f
                )

                // Bottom-left tertiary glow (Flame Coral / Warm Bronze)
                val flameAlpha = if (isDark) 0.11f else 0.06f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(tertiary.copy(alpha = flameAlpha), Color.Transparent),
                        center = Offset(w * 0.12f, h * 0.78f),
                        radius = w * 0.70f
                    ),
                    center = Offset(w * 0.12f, h * 0.78f),
                    radius = w * 0.70f
                )
            }
    ) {
        content()
    }
}

/**
 * Returns a frosted glass gradient fill brush depending on light or dark theme.
 */
@Composable
fun glassSurfaceBrush(isElevated: Boolean = false): Brush {
    val isDark = isDarkThemeGlobal ?: isSystemInDarkTheme()
    return if (isDark) {
        if (isElevated) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xE61E2A3E), // 90% opacity elevated glass slate
                    Color(0xB3111827)  // 70% opacity deep glass
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xD9141D2C), // 85% opacity frosted slate glass
                    Color(0xA60D1420)  // 65% opacity deep backdrop
                )
            )
        }
    } else {
        if (isElevated) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xF5FFFFFF), // 96% frosted white
                    Color(0xD9E2E8F0)  // 85% soft slate
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xEBFFFFFF), // 92% frosted white
                    Color(0xC4F1F5F9)  // 77% luminous frosted slate
                )
            )
        }
    }
}

/**
 * Returns a specular frosted glass border stroke brush.
 * High specular gleam at the top-left graduating to a soft, subtle edge at the bottom-right.
 */
@Composable
fun glassBorderBrush(gleamAlpha: Float = 0.35f): Brush {
    val isDark = isDarkThemeGlobal ?: isSystemInDarkTheme()
    return if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = gleamAlpha),
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = gleamAlpha * 0.45f)
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.95f),
                Color(0x55CBD5E1),
                Color.White.copy(alpha = 0.60f)
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }
}

/**
 * Custom Modifier that turns any container into a frosted Glassmorphic surface.
 */
@Composable
fun Modifier.glassmorphism(
    shape: Shape = RoundedCornerShape(20.dp),
    isElevated: Boolean = false,
    gleamAlpha: Float = 0.32f,
    borderWidth: Dp = 1.dp
): Modifier {
    val surfaceBrush = glassSurfaceBrush(isElevated = isElevated)
    val borderBrush = glassBorderBrush(gleamAlpha = gleamAlpha)

    return this
        .clip(shape)
        .background(surfaceBrush)
        .border(width = borderWidth, brush = borderBrush, shape = shape)
}

/**
 * Sleek Glassmorphism Card Container.
 * Features translucent frosted glass gradient, precision specular highlight border,
 * and an authentic top edge specular gleam line.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    isElevated: Boolean = false,
    gleamAlpha: Float = 0.32f,
    borderWidth: Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isDarkThemeGlobal ?: isSystemInDarkTheme()
    val baseModifier = if (onClick != null) {
        modifier.bounceClick(onClick = onClick)
    } else {
        modifier
    }

    Box(
        modifier = baseModifier
            .fillMaxWidth()
            .glassmorphism(
                shape = shape,
                isElevated = isElevated,
                gleamAlpha = gleamAlpha,
                borderWidth = borderWidth
            )
    ) {
        // Delicate interior top specular reflection line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .padding(horizontal = 16.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            if (isDark) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.65f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

/**
 * Compact Glassmorphism pill/capsule for status badges, tags, and small metric chips.
 */
@Composable
fun GlassPill(
    modifier: Modifier = Modifier,
    tintColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable RowScope.() -> Unit
) {
    val isDark = isDarkThemeGlobal ?: isSystemInDarkTheme()
    val bgAlpha = if (isDark) 0.16f else 0.12f
    val borderAlpha = if (isDark) 0.40f else 0.35f

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(tintColor.copy(alpha = bgAlpha))
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        tintColor.copy(alpha = borderAlpha),
                        Color.White.copy(alpha = if (isDark) 0.15f else 0.40f),
                        tintColor.copy(alpha = borderAlpha * 0.6f)
                    )
                ),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        content = content
    )
}
