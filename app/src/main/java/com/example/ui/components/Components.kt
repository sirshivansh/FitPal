package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material3.*
import kotlin.math.roundToInt
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.DateUtils

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
fun MacroCircularIndicator(
    label: String,
    currentGrams: Float,
    goalGrams: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val progress = if (goalGrams > 0) currentGrams / goalGrams.toFloat() else 0f
    val isExcess = progress > 1f
    val displayPercent = (progress * 100f).toInt()
    
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800),
        label = "MacroCircularProgress"
    )

    Column(
        modifier = modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(80.dp)
        ) {
            // Background track
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    color = color.copy(alpha = 0.15f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            
            // Foreground active track
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    color = if (isExcess) Color(0xFFE74C3C) else color,
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            
            // Center percentage text
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$displayPercent%",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isExcess) Color(0xFFE74C3C) else MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        
        val diff = goalGrams - currentGrams.toInt()
        val infoText = if (diff >= 0) "${diff}g left" else "${-diff}g over"
        val infoColor = if (isExcess) Color(0xFFE74C3C) else MaterialTheme.colorScheme.onSurfaceVariant
        
        Text(
            text = infoText,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = infoColor
        )
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

/**
 * Custom Modifier for sleek, bouncy button feel.
 * Adds interactive tactile spring scale feedback to UI elements when clicked!
 */
@Composable
fun Modifier.bounceClick(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = tween(durationMillis = 100),
        label = "BounceScale"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = ripple(bounded = true, radius = 24.dp),
            onClick = onClick
        )
}

/**
 * Premium Horizontal Weekly Calendar Strip.
 * Shows days of the week surrounding the selected date as circular/pill containers.
 * The active day is styled with high-contrast primary color and text.
 */
@Composable
fun HorizontalWeekStrip(
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val weekDays = remember(selectedDate) {
        DateUtils.getWeekStripDays(selectedDate)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        weekDays.forEach { dateItem ->
            val isSelected = dateItem == selectedDate
            val dayLetter = DateUtils.getDayOfWeekLetter(dateItem)
            val dayNum = DateUtils.getDayOfMonth(dateItem)
            
            val containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            }
            
            val contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            }

            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.08f else 1.0f,
                animationSpec = tween(durationMillis = 300),
                label = "CalendarDayScale"
            )

            Card(
                modifier = Modifier
                    .width(46.dp)
                    .height(68.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .bounceClick {
                        onDateSelected(dateItem)
                    }
                    .testTag("calendar_day_$dateItem"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = containerColor),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 0.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = dayLetter,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = contentColor.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dayNum,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = contentColor,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CaloricAndMacroProgressBarCard(
    calorieConsumed: Int,
    calorieGoal: Int,
    calorieBurned: Int,
    proteinConsumed: Float,
    proteinGoal: Int,
    carbsConsumed: Float,
    carbsGoal: Int,
    fatConsumed: Float,
    fatGoal: Int,
    modifier: Modifier = Modifier
) {
    val netCalories = calorieConsumed - calorieBurned
    val remainingCalories = calorieGoal - netCalories
    val isCalorieExcess = remainingCalories < 0
    val calorieProgressFraction = if (calorieGoal > 0) netCalories.toFloat() / calorieGoal.toFloat() else 0f
    val safeCalorieProgress = calorieProgressFraction.coerceIn(0f, 1f)

    val animatedCalorieProgress by animateFloatAsState(
        targetValue = safeCalorieProgress,
        animationSpec = tween(durationMillis = 800),
        label = "CalorieProgress"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Daily Intake Progress",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Active status label
                Text(
                    text = if (isCalorieExcess) "Over Budget" else "On Track",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCalorieExcess) Color(0xFFE76F51) else Color(0xFF2D6A4F),
                    modifier = Modifier
                        .background(
                            color = if (isCalorieExcess) Color(0xFFE76F51).copy(alpha = 0.1f) else Color(0xFF2D6A4F).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Calorie Linear Progress
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "Calories (Net)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Net = Eaten ($calorieConsumed) - Active ($calorieBurned)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Text(
                        text = "$netCalories / $calorieGoal kcal",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = animatedCalorieProgress)
                            .clip(CircleShape)
                            .background(if (isCalorieExcess) Color(0xFFE76F51) else MaterialTheme.colorScheme.primary)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (remainingCalories >= 0) "$remainingCalories kcal remaining" else "${-remainingCalories} kcal over budget",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (isCalorieExcess) Color(0xFFE76F51) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(calorieProgressFraction * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isCalorieExcess) Color(0xFFE76F51) else MaterialTheme.colorScheme.primary
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Macronutrient Progress bars Stacked
            Text(
                text = "Macronutrient Breakdown",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Protein bar
                MacroProgressRow(
                    label = "Protein",
                    consumed = proteinConsumed,
                    goal = proteinGoal,
                    color = MacroProteinColor,
                    testTagPrefix = "protein"
                )

                // Carbs bar
                MacroProgressRow(
                    label = "Carbohydrates",
                    consumed = carbsConsumed,
                    goal = carbsGoal,
                    color = MacroCarbsColor,
                    testTagPrefix = "carbs"
                )

                // Fat bar
                MacroProgressRow(
                    label = "Fats",
                    consumed = fatConsumed,
                    goal = fatGoal,
                    color = MacroFatColor,
                    testTagPrefix = "fat"
                )
            }
        }
    }
}

@Composable
fun MacroProgressRow(
    label: String,
    consumed: Float,
    goal: Int,
    color: Color,
    testTagPrefix: String
) {
    val progressFraction = if (goal > 0) consumed / goal.toFloat() else 0f
    val safeProgress = progressFraction.coerceIn(0f, 1f)
    val percentage = (progressFraction * 100).toInt()
    val isExcess = progressFraction > 1f

    val animatedProgress by animateFloatAsState(
        targetValue = safeProgress,
        animationSpec = tween(durationMillis = 800),
        label = "${label}Progress"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("progress_row_$testTagPrefix"),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(color, CircleShape)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "${consumed.roundToInt()}g of ${goal}g",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Progress track
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
                    .background(if (isExcess) Color(0xFFE76F51) else color)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val remaining = goal - consumed.roundToInt()
            Text(
                text = if (remaining >= 0) "${remaining}g remaining" else "${-remaining}g over limit",
                style = MaterialTheme.typography.bodySmall,
                color = if (isExcess) Color(0xFFE76F51) else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = if (isExcess) Color(0xFFE76F51) else color
            )
        }
    }
}


