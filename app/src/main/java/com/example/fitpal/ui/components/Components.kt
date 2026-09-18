package com.example.fitpal.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import kotlin.math.min
import kotlin.math.roundToInt
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import com.example.fitpal.util.DateUtils

// Color definitions for macros as specified in core features
val MacroProteinColor = Color(0xFF2FD6D6) // Vivid Cyan Protein
val MacroCarbsColor = Color(0xFFE5A93C)   // Amber Gold Carbs
val MacroFatColor = Color(0xFFFF5252)     // Coral Red Fat
private val ElectricLime = Color(0xFFCEFD1A) // Precision Acid-Lime
private val DarkBgTrack = Color(0xFF181B20)  // Carbon Surface Track
private val CrispWhite = Color(0xFFF0F3F6)   // Crisp High-Contrast Text
private val MutedGreyText = Color(0xFF717886) // Secondary Label Text

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
        remaining < 0 -> MacroFatColor // Coral Orange (Over limit)
        remaining <= 200 -> MacroCarbsColor // Bronze/Gold (Approaching)
        else -> ElectricLime // Vivid Electric Lime (Healthy Target)
    }

    val trackColor = DarkBgTrack

    Box(
        modifier = modifier
            .size(size)
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        // Draw the background and foreground tracks with strict 1:1 aspect ratio and inset to prevent edge clipping
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(1f)
        ) {
            val strokeWidthPx = strokeWidth.toPx()
            val diameter = min(this.size.width, this.size.height) - strokeWidthPx - 8.dp.toPx()
            val topLeft = Offset(
                x = (this.size.width - diameter) / 2f,
                y = (this.size.height - diameter) / 2f
            )
            val arcSize = Size(diameter, diameter)
            val stroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            
            // Outer subtle mechanical bezel ring
            val bezelDiameter = diameter + strokeWidthPx + 4.dp.toPx()
            val bezelTopLeft = Offset(
                x = (this.size.width - bezelDiameter) / 2f,
                y = (this.size.height - bezelDiameter) / 2f
            )
            drawArc(
                color = Color.White.copy(alpha = 0.05f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = bezelTopLeft,
                size = Size(bezelDiameter, bezelDiameter),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Inner subtle mechanical border ring
            val innerDiameter = diameter - strokeWidthPx - 4.dp.toPx()
            if (innerDiameter > 0f) {
                val innerTopLeft = Offset(
                    x = (this.size.width - innerDiameter) / 2f,
                    y = (this.size.height - innerDiameter) / 2f
                )
                drawArc(
                    color = Color.White.copy(alpha = 0.04f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = innerTopLeft,
                    size = Size(innerDiameter, innerDiameter),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Background Deep Track
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )
            
            // Progress Track with High-Precision Stroke
            if (animatedProgress > 0f) {
                drawArc(
                    color = colorLimit,
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke
                )
            }
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
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 38.sp
                ),
                color = if (remaining >= 0) CrispWhite else MacroFatColor
            )
            
            Text(
                text = if (remaining >= 0) "KCAL REMAINING" else "KCAL OVER",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MutedGreyText
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$consumed",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = CrispWhite
                    )
                    Text(text = "EATEN", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.8.sp), color = MutedGreyText)
                }
                
                Box(modifier = Modifier.height(22.dp).width(1.dp).background(Color.White.copy(alpha = 0.12f)))
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$burned",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = ElectricLime
                    )
                    Text(text = "BURNED", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.8.sp), color = MutedGreyText)
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
            modifier = Modifier
                .size(80.dp)
                .aspectRatio(1f)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .aspectRatio(1f)
            ) {
                val strokeWidthPx = 8.dp.toPx()
                val diameter = min(this.size.width, this.size.height) - strokeWidthPx
                val topLeft = Offset(
                    x = (this.size.width - diameter) / 2f,
                    y = (this.size.height - diameter) / 2f
                )
                val arcSize = Size(diameter, diameter)
                val stroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)

                // Background track
                drawArc(
                    color = color.copy(alpha = 0.15f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke
                )

                // Foreground active track
                if (animatedProgress > 0f) {
                    drawArc(
                        color = if (isExcess) Color(0xFFE74C3C) else color,
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = stroke
                    )
                }
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
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        weekDays.forEach { dateItem ->
            val isSelected = dateItem == selectedDate
            val dayLetter = DateUtils.getDayOfWeekLetter(dateItem)
            val dayNum = DateUtils.getDayOfMonth(dateItem)

            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.05f else 1.0f,
                animationSpec = tween(durationMillis = 200),
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
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) ElectricLime else Color.White.copy(alpha = 0.05f)
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Color(0xFF1E251E) else Color(0xFF141A14)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = dayLetter,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = if (isSelected) ElectricLime else MutedGreyText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dayNum,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = if (isSelected) CrispWhite else MutedGreyText
                    )
                    if (isSelected) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(ElectricLime)
                        )
                    }
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
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111317)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF1E251E),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                tint = ElectricLime,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Daily Intake Progress",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CrispWhite
                        )
                        Text(
                            text = "NET CALORIES & MACRONUTRIENTS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                letterSpacing = 0.8.sp
                            ),
                            color = MutedGreyText
                        )
                    }
                }
                
                // Active status pill
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isCalorieExcess) MacroFatColor.copy(alpha = 0.15f) else ElectricLime.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, if (isCalorieExcess) MacroFatColor.copy(alpha = 0.4f) else ElectricLime.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = if (isCalorieExcess) "OVER BUDGET" else "ON TRACK",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = if (isCalorieExcess) MacroFatColor else ElectricLime,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
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
                            color = CrispWhite
                        )
                        Text(
                            text = "Net = Eaten ($calorieConsumed) - Active ($calorieBurned)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedGreyText
                        )
                    }
                    
                    Text(
                        text = "$netCalories / $calorieGoal kcal",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = CrispWhite
                    )
                }

                // Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(DarkBgTrack)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = animatedCalorieProgress)
                            .clip(CircleShape)
                            .background(if (isCalorieExcess) MacroFatColor else ElectricLime)
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
                        color = if (isCalorieExcess) MacroFatColor else MutedGreyText
                    )
                    Text(
                        text = "${(calorieProgressFraction * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isCalorieExcess) MacroFatColor else ElectricLime
                    )
                }
            }

            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))

            // Macronutrient Progress bars Stacked
            Text(
                text = "MACRONUTRIENT BREAKDOWN",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MutedGreyText
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
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                    color = CrispWhite
                )
            }
            Text(
                text = "${consumed.roundToInt()}g of ${goal}g",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = MutedGreyText
            )
        }

        // Progress track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(DarkBgTrack)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = animatedProgress)
                    .clip(CircleShape)
                    .background(if (isExcess) MacroFatColor else color)
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
                color = if (isExcess) MacroFatColor else MutedGreyText
            )
            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = if (isExcess) MacroFatColor else color
            )
        }
    }
}

@Composable
fun BrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(Color(0xFF1A271A), Color(0xFF0C0F0C))
                )
            )
            .border(
                width = 1.dp,
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(Color(0xFF20AA1D).copy(alpha = 0.7f), Color.White.copy(alpha = 0.12f))
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = androidx.compose.ui.res.painterResource(id = com.example.fitpal.R.drawable.ic_launcher_foreground),
            contentDescription = "FitPal Logo",
            tint = Color.Unspecified,
            modifier = Modifier.fillMaxSize()
        )
    }
}



