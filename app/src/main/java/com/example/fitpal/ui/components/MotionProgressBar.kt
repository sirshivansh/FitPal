package com.example.fitpal.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Premium motion animated fitness progress bar.
 * Features:
 * - Physics-spring responsive progress fill
 * - Subtle ambient light shimmer traveling along the active fill
 * - Goal completion state with soft breathing halo/glow and celebratory color transition
 */
@Composable
fun MotionProgressBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    height: Dp = 10.dp,
    isGoalCompleted: Boolean = false,
    completionColor: Color = MaterialTheme.colorScheme.primary,
    showShimmer: Boolean = true,
    shape: Shape = CircleShape,
    testTag: String? = null
) {
    val safeProgress = progress.coerceIn(0f, 1f)

    // Fluid spring-based animation when progress changes
    val animatedProgress by animateFloatAsState(
        targetValue = safeProgress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "MotionProgressBarProgress"
    )

    // Infinite transition for subtle shimmer and breathing glow
    val infiniteTransition = rememberInfiniteTransition(label = "MotionBarShimmer")

    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -0.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerOffset"
    )

    val breathingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathingAlpha"
    )

    val activeColor = if (isGoalCompleted) completionColor else color

    Box(
        modifier = modifier
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .fillMaxWidth()
            .height(height)
            .then(
                if (isGoalCompleted) {
                    Modifier.drawBehind {
                        // Subtle ambient glow behind the bar when goal is achieved
                        drawRoundRect(
                            color = activeColor.copy(alpha = breathingAlpha * 0.4f),
                            size = Size(size.width, size.height + 4.dp.toPx()),
                            topLeft = Offset(0f, -2.dp.toPx()),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height, size.height)
                        )
                    }
                } else Modifier
            )
            .clip(shape)
            .background(trackColor)
    ) {
        if (animatedProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = animatedProgress)
                    .clip(shape)
                    .background(activeColor)
            ) {
                // Subtle shimmer light beam sweeping across the progress fill
                if (showShimmer && animatedProgress > 0.05f) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val barWidth = size.width
                        val shimmerWidth = barWidth * 0.4f
                        val startX = (barWidth + shimmerWidth) * shimmerOffset - shimmerWidth

                        val shimmerBrush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = if (isGoalCompleted) 0.35f else 0.22f),
                                Color.Transparent
                            ),
                            start = Offset(startX, 0f),
                            end = Offset(startX + shimmerWidth, 0f)
                        )

                        drawRect(
                            brush = shimmerBrush,
                            size = size
                        )
                    }
                }
            }
        }
    }
}

/**
 * Animated Goal Completion Pill Badge.
 * Pops in with a spring bounce when the goal is completed,
 * accompanied by a subtle breathing glow and checkmark icon.
 */
@Composable
fun GoalCompletionBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    icon: ImageVector = Icons.Default.Check,
    isCompleted: Boolean = true
) {
    AnimatedVisibility(
        visible = isCompleted,
        enter = fadeIn(animationSpec = tween(300)) + scaleIn(
            initialScale = 0.7f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ),
        exit = fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.8f),
        modifier = modifier
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "GoalBadgeGlow")
        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.12f,
            targetValue = 0.28f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "BadgeGlowAlpha"
        )

        Surface(
            shape = RoundedCornerShape(50),
            color = color.copy(alpha = glowAlpha),
            border = BorderStroke(1.dp, color.copy(alpha = 0.55f)),
            modifier = Modifier.graphicsLayer {
                shadowElevation = 2f
            }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.6.sp
                    ),
                    color = color
                )
            }
        }
    }
}

/**
 * Milestone Celebration Card with animated sparkles and glowing border.
 */
@Composable
fun MilestoneCelebrationCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    icon: ImageVector = Icons.Default.EmojiEvents
) {
    val infiniteTransition = rememberInfiniteTransition(label = "MilestoneCardTransition")
    
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "IconScale"
    )

    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BorderAlpha"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.08f)
        ),
        border = BorderStroke(1.dp, color.copy(alpha = borderAlpha))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.2f),
                modifier = Modifier
                    .size(42.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = Icons.Default.Stars,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
