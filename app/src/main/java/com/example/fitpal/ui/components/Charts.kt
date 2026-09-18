package com.example.fitpal.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitpal.data.local.entity.FoodEntry
import com.example.fitpal.data.local.entity.WeightLog
import com.example.fitpal.util.DateUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

// Color definitions matching the dark sleek fitness theme
private val BarProteinColor = Color(0xFF2FD6D6) // Vivid Cyan Protein
private val BarCarbsColor = Color(0xFFA29362)   // Earthy Gold Carbs
private val BarFatColor = Color(0xFFAE422A)     // Coral Orange Fat
private val CoralOrange = Color(0xFFAE422A)    // Functional Accent Coral
private val ElectricLime = Color(0xFF20AA1D)    // Primary Accent
private val SurfaceStroke = Color(0x14FFFFFF)   // Color.White.copy(alpha = 0.08f)
private val CardBackground = Color(0xFF141A14)  // Dark Muted Green-Black
private val ElevatedSurface = Color(0xFF1E251E) // Layered Elevated Surface
private val MutedText = Color(0xFF9CA3AF)       // Neutral Muted Grey
private val CrispText = Color(0xFFF5F5F7)       // High Contrast White

data class MacroDayData(
    val dateStr: String,
    val label: String,
    val protein: Float,
    val carbs: Float,
    val fat: Float
)

@Composable
fun WeeklyMacroBarChart(
    allFoodEntries: List<FoodEntry>,
    modifier: Modifier = Modifier
) {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val displayFormatter = DateTimeFormatter.ofPattern("E", Locale.US)
    val today = LocalDate.now()
    
    // Generate the last 7 days of dates
    val last7Days = remember {
        (0..6).map { today.minusDays(it.toLong()) }.reversed()
    }
    
    // Compute daily macro distribution
    val dailyMacros = remember(allFoodEntries, last7Days) {
        last7Days.map { date ->
            val dateStr = date.format(formatter)
            val label = date.format(displayFormatter)
            val dayEntries = allFoodEntries.filter { it.date == dateStr }
            val protein = dayEntries.sumOf { it.proteinGrams.toDouble() }.toFloat()
            val carbs = dayEntries.sumOf { it.carbsGrams.toDouble() }.toFloat()
            val fat = dayEntries.sumOf { it.fatGrams.toDouble() }.toFloat()
            MacroDayData(dateStr, label, protein, carbs, fat)
        }
    }
    
    var selectedDayIndex by remember { mutableStateOf<Int?>(null) }
    
    // Find maximum macro value across all days for Y-axis scaling
    val maxVal = remember(dailyMacros) {
        dailyMacros.flatMap { listOf(it.protein, it.carbs, it.fat) }.maxOrNull() ?: 50f
    }
    val maxScale = remember(maxVal) {
        if (maxVal > 0f) maxVal * 1.2f else 100f // 20% breathing room
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weekly_macro_chart_card"),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, SurfaceStroke),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Weekly Macro Insights",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CrispText
                    )
                    Text(
                        text = "MACRONUTRIENT DISTRIBUTION PAST 7 DAYS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedText
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = ElevatedSurface,
                    border = BorderStroke(1.dp, SurfaceStroke),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = ElectricLime,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem("Protein", BarProteinColor)
                Spacer(modifier = Modifier.width(16.dp))
                LegendItem("Carbs", BarCarbsColor)
                Spacer(modifier = Modifier.width(16.dp))
                LegendItem("Fat", BarFatColor)
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Bar Chart Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                // Background grid lines and axes drawing
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val chartHeight = h - 40f
                    val chartWidth = w - 60f
                    val leftPadding = 50f

                    // Y-Axis grid lines (draw 4 lines)
                    for (i in 0..3) {
                        val y = chartHeight * (i / 3f)
                        val value = maxScale * (1f - i / 3f)
                        
                        // Draw grid line
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            start = Offset(leftPadding, y),
                            end = Offset(w, y),
                            strokeWidth = 1f
                        )
                    }
                }

                // Interaction Area & Bar Rendering
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 24.dp, bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    dailyMacros.forEachIndexed { index, dayData ->
                        val isSelected = selectedDayIndex == index
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable {
                                    selectedDayIndex = if (isSelected) null else index
                                },
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    // Protein Bar
                                    val animatedProtein by animateFloatAsState(
                                        targetValue = if (maxScale > 0) dayData.protein / maxScale else 0f,
                                        animationSpec = tween(durationMillis = 600)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight(animatedProtein.coerceIn(0.01f, 1f))
                                            .weight(1f)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(BarProteinColor)
                                    )

                                    // Carbs Bar
                                    val animatedCarbs by animateFloatAsState(
                                        targetValue = if (maxScale > 0) dayData.carbs / maxScale else 0f,
                                        animationSpec = tween(durationMillis = 600, delayMillis = 100)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight(animatedCarbs.coerceIn(0.01f, 1f))
                                            .weight(1f)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(BarCarbsColor)
                                    )

                                    // Fat Bar
                                    val animatedFat by animateFloatAsState(
                                        targetValue = if (maxScale > 0) dayData.fat / maxScale else 0f,
                                        animationSpec = tween(durationMillis = 600, delayMillis = 200)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight(animatedFat.coerceIn(0.01f, 1f))
                                            .weight(1f)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(BarFatColor)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = dayData.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Interactive Tooltip Info
            selectedDayIndex?.let { index ->
                val dayData = dailyMacros[index]
                val dateDisplay = remember(dayData.dateStr) {
                    DateUtils.formatDateForDisplayShort(dayData.dateStr)
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$dateDisplay Metrics:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "P: ${dayData.protein.roundToInt()}g",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = BarProteinColor
                            )
                            Text(
                                text = "C: ${dayData.carbs.roundToInt()}g",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = BarCarbsColor
                            )
                            Text(
                                text = "F: ${dayData.fat.roundToInt()}g",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = BarFatColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun WeightLineChart(
    weightLogs: List<WeightLog>,
    modifier: Modifier = Modifier
) {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val today = LocalDate.now()
    val thirtyDaysAgoStr = remember { today.minusDays(30).format(formatter) }

    // Filter logs for the last 30 days and sort chronologically
    val recentLogs = remember(weightLogs, thirtyDaysAgoStr) {
        weightLogs
            .filter { it.date >= thirtyDaysAgoStr }
            .sortedBy { it.date }
    }

    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weight_progress_chart_card"),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, SurfaceStroke),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Group to distinct days
            val groupedPoints = remember(recentLogs) {
                if (recentLogs.isEmpty()) {
                    listOf("Mon" to 48.0f, "Wed" to 47.8f, "Thu" to 47.6f, "Sun" to 47.5f)
                } else {
                    recentLogs.groupBy { it.date }
                        .mapValues { it.value.last().weightKg }
                        .toList()
                        .sortedBy { it.first }
                }
            }

            val weights = groupedPoints.map { it.second }
            val latestWeight = weights.lastOrNull() ?: 47.5f

            // Sleek Header with Bold High-Impact Number & Tracked Metric Label
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "My Progress",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CrispText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${String.format(Locale.US, "%.1f", latestWeight)}kg",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 36.sp,
                            letterSpacing = (-0.5).sp
                        ),
                        color = ElectricLime
                    )
                    Text(
                        text = "WEIGHT MILESTONE • 30-DAY PROGRESS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedText
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = ElevatedSurface,
                    border = BorderStroke(1.dp, SurfaceStroke),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = null,
                            tint = ElectricLime,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (recentLogs.size < 2 && weightLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(ElevatedSurface, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Log body weight on multiple days to reveal\nyour personalized progress curve.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedText
                    )
                }
            } else {
                val minW = weights.minOrNull() ?: 40f
                val maxW = weights.maxOrNull() ?: 50f
                val wRange = maxW - minW
                
                // Add margins so the curve has breathing room
                val yMin = if (wRange > 0f) minW - wRange * 0.25f else minW - 3f
                val yMax = if (wRange > 0f) maxW + wRange * 0.25f else maxW + 3f
                val yDiff = yMax - yMin

                val primaryColor = ElectricLime

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                selectedPointIndex = if (selectedPointIndex == null) groupedPoints.lastIndex else null
                            }
                    ) {
                        val w = size.width
                        val h = size.height
                        val leftPadding = 50f
                        val rightPadding = 24f
                        val topPadding = 24f
                        val bottomPadding = 32f
                        
                        val chartW = w - leftPadding - rightPadding
                        val chartH = h - topPadding - bottomPadding

                        // Dotted dash effect for guides
                        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

                        // Draw Grid Lines (Y-Axis ticks)
                        val gridCount = 3
                        for (i in 0..gridCount) {
                            val ratio = i.toFloat() / gridCount
                            val y = topPadding + chartH * ratio
                            
                            drawLine(
                                color = Color.White.copy(alpha = 0.05f),
                                start = Offset(leftPadding, y),
                                end = Offset(w - rightPadding, y),
                                strokeWidth = 1f
                            )
                        }

                        // Plot points
                        val pointsCount = groupedPoints.size
                        val coordinates = groupedPoints.mapIndexed { index, pair ->
                            val xRatio = if (pointsCount > 1) index.toFloat() / (pointsCount - 1) else 0.5f
                            val yRatio = if (yDiff > 0f) (pair.second - yMin) / yDiff else 0.5f
                            
                            val px = leftPadding + xRatio * chartW
                            val py = topPadding + (1f - yRatio) * chartH
                            Offset(px, py)
                        }

                        // Dotted vertical milestone guides
                        coordinates.forEach { pt ->
                            drawLine(
                                color = Color.White.copy(alpha = 0.08f),
                                start = Offset(pt.x, topPadding),
                                end = Offset(pt.x, topPadding + chartH),
                                strokeWidth = 1.5f,
                                pathEffect = dashEffect
                            )
                        }

                        // Draw Gradient Area under Line
                        val gradientPath = Path().apply {
                            if (coordinates.isNotEmpty()) {
                                moveTo(coordinates.first().x, topPadding + chartH)
                                coordinates.forEach { lineTo(it.x, it.y) }
                                lineTo(coordinates.last().x, topPadding + chartH)
                                close()
                            }
                        }
                        drawPath(
                            path = gradientPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(primaryColor.copy(alpha = 0.22f), Color.Transparent),
                                startY = topPadding,
                                endY = topPadding + chartH
                            )
                        )

                        // Draw Smooth Bezier Curve
                        val linePath = Path().apply {
                            if (coordinates.isNotEmpty()) {
                                moveTo(coordinates.first().x, coordinates.first().y)
                                for (i in 1 until coordinates.size) {
                                    val prev = coordinates[i - 1]
                                    val curr = coordinates[i]
                                    val controlX = (prev.x + curr.x) / 2
                                    cubicTo(
                                        controlX, prev.y,
                                        controlX, curr.y,
                                        curr.x, curr.y
                                    )
                                }
                            }
                        }
                        drawPath(
                            path = linePath,
                            color = primaryColor,
                            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Draw Dot Markers & Highlight on Points
                        coordinates.forEachIndexed { idx, point ->
                            val isLatest = idx == coordinates.lastIndex
                            // Outer glowing halo
                            drawCircle(
                                color = if (isLatest) primaryColor.copy(alpha = 0.35f) else primaryColor.copy(alpha = 0.15f),
                                radius = if (isLatest) 9.dp.toPx() else 6.dp.toPx(),
                                center = point
                            )
                            // Intermediate ring
                            drawCircle(
                                color = if (isLatest) CrispText else ElevatedSurface,
                                radius = if (isLatest) 5.dp.toPx() else 3.5.dp.toPx(),
                                center = point
                            )
                            // Core center
                            drawCircle(
                                color = primaryColor,
                                radius = 2.5.dp.toPx(),
                                center = point
                            )
                        }
                    }

                    // Floating Pill Callout on Latest / Selected Point
                    val displayIdx = selectedPointIndex ?: (groupedPoints.lastIndex)
                    if (displayIdx in groupedPoints.indices) {
                        val ptData = groupedPoints[displayIdx]
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 4.dp, end = 8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(ElevatedSurface)
                                .border(1.dp, primaryColor.copy(alpha = 0.5f), RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${String.format(Locale.US, "%.1f", ptData.second)} kg",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ElectricLime
                            )
                        }
                    }
                }

                // X-Axis Day Markers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    groupedPoints.forEach { item ->
                        val dayLabel = remember(item.first) {
                            try {
                                val d = LocalDate.parse(item.first)
                                d.format(DateTimeFormatter.ofPattern("E", Locale.US))
                            } catch (_: Exception) {
                                item.first
                            }
                        }
                        Text(
                            text = dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedText,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Summary Progress Banner in Elevated Surface
                val initialW = weights.first()
                val finalW = weights.last()
                val diffW = finalW - initialW
                
                Surface(
                    color = ElevatedSurface,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, SurfaceStroke),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "LOGGED RANGE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedText
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${String.format(Locale.US, "%.1f", initialW)} kg  →  ${String.format(Locale.US, "%.1f", finalW)} kg",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = CrispText
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "NET CHANGE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedText
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (diffW > 0) "+${String.format(Locale.US, "%.1f", diffW)} kg" else "${String.format(Locale.US, "%.1f", diffW)} kg",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (diffW <= 0) ElectricLime else CoralOrange
                            )
                        }
                    }
                }
            }
        }
    }
}
