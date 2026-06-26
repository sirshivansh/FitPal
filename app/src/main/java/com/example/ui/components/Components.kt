package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Color definitions for macros as specified in core features
val MacroProteinColor = Color(0xFF3F8EFC) // Sleek Protein Blue
val MacroCarbsColor = Color(0xFFF4A261)   // Sleek Carbs Sandy Orange
val MacroFatColor = Color(0xFFE76F51)     // Sleek Fat Terracotta

@Composable
fun CalorieRing(
    consumed: Int,
    goal: Int,
    burned: Int,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    strokeWidth: Dp = 16.dp
) {
    val netCalories = consumed - burned
    val remaining = goal - netCalories
    val fraction = if (goal > 0) netCalories.toFloat() / goal.toFloat() else 0f
    val safeFraction = fraction.coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = safeFraction,
        animationSpec = tween(durationMillis = 800),
        label = "CalorieRingProgress"
    )

    // Dynamic coloring based on calorie limits
    val colorLimit = when {
        remaining < 0 -> Color(0xFFE76F51) // Crimson/Terracotta (Over limit)
        remaining <= 200 -> Color(0xFFF4A261) // Orange (Approaching)
        else -> Color(0xFF2D6A4F) // Forest Green (Sleek Healthy Primary)
    }

    val trackColor = MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Draw the background and foreground tracks
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            
            // Background Track - Sleek Border Color
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke
            )
            
            // Progress Track
            drawArc(
                color = colorLimit,
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                style = stroke
            )
        }

        // Concentric statistics overlay
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = if (remaining >= 0) "$remaining" else "${kotlin.math.abs(remaining)}",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 38.sp
                ),
                color = if (remaining >= 0) MaterialTheme.colorScheme.onSurface else Color(0xFFE76F51)
            )
            
            Text(
                text = if (remaining >= 0) "KCAL LEFT" else "KCAL OVER",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "$consumed", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(text = "Eaten", style = MaterialTheme.typography.labelSmall)
                }
                
                Divider(modifier = Modifier.height(24.dp).width(1.dp))
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "$burned", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(text = "Active", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun MacroProgressBar(
    label: String,
    currentGrams: Float,
    goalGrams: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val progress = if (goalGrams > 0) currentGrams / goalGrams.toFloat() else 0f
    val safeProgress = progress.coerceIn(0f, 1f)
    val percentage = (progress * 100f).coerceIn(0f, 999f).toInt()

    val animatedProgress by animateFloatAsState(
        targetValue = safeProgress,
        animationSpec = tween(durationMillis = 800),
        label = "MacroProgress"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = "${currentGrams.toInt()}g / ${goalGrams}g ($percentage%)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Progress Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outlineVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = animatedProgress)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Info, contentDescription = null) },
        title = { Text(text = title, fontWeight = FontWeight.Bold) },
        text = { Text(text = message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.testTag("dialog_confirm_button")
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
