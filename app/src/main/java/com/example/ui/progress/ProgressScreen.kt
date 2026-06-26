package com.example.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import com.example.data.local.entity.WeightLog
import com.example.ui.components.*
import com.example.util.DateUtils
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    viewModel: ProgressViewModel,
    modifier: Modifier = Modifier
) {
    val weightLogs by viewModel.weightLogs.collectAsState()
    val profile by viewModel.userProfile.collectAsState()
    val recentFood by viewModel.recentFoodEntries.collectAsState()

    val scrollState = rememberScrollState()

    // Weight log input
    var weightInput by remember { mutableStateOf("") }
    var weightDateInput by remember { mutableStateOf(DateUtils.getTodayDateString()) }

    // Streak calculation
    val currentStreak = viewModel.calculateStreak(recentFood)

    // Weight stats
    val startWeight = weightLogs.firstOrNull()?.weightKg ?: profile?.currentWeightKg ?: 70f
    val currentWeight = weightLogs.lastOrNull()?.weightKg ?: profile?.currentWeightKg ?: 70f
    val goalWeight = profile?.goalWeightKg ?: 70f
    val weightChange = currentWeight - startWeight

    // BMI Calculation
    val heightM = (profile?.heightCm ?: 170f) / 100f
    val startBmi = if (heightM > 0) startWeight / (heightM * heightM) else 0f
    val currentBmi = if (heightM > 0) currentWeight / (heightM * heightM) else 0f
    val goalBmi = if (heightM > 0) goalWeight / (heightM * heightM) else 0f

    // Macro analytics over past 7 days
    val totalProtein = recentFood.sumOf { it.proteinGrams.toDouble() }.toFloat()
    val totalCarbs = recentFood.sumOf { it.carbsGrams.toDouble() }.toFloat()
    val totalFat = recentFood.sumOf { it.fatGrams.toDouble() }.toFloat()
    val grandTotalMacros = totalProtein + totalCarbs + totalFat

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Progress & Analytics", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // STREAKS COUNTER HERO BLOCK
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Logging Streak",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Log food daily to hit goals",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFFF5722),
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$currentStreak days",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // BMI PROGRESS BLOCK
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "BMI Progress Meter",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Starting BMI", style = MaterialTheme.typography.labelSmall)
                            Text(String.format(Locale.US, "%.1f", startBmi), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Current BMI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(String.format(Locale.US, "%.1f", currentBmi), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Goal BMI", style = MaterialTheme.typography.labelSmall)
                            Text(String.format(Locale.US, "%.1f", goalBmi), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val bmiCategory = when {
                        currentBmi < 18.5f -> "Underweight"
                        currentBmi < 25.0f -> "Normal (Healthy)"
                        currentBmi < 30.0f -> "Overweight"
                        else -> "Obese"
                    }
                    Text(
                        text = "Current Status: $bmiCategory",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // WEIGHT HISTORY LINE CHART (CUSTOM CANVAS)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Weight Trend (kg)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val changeText = if (weightChange > 0) "+${String.format(Locale.US, "%.1f", weightChange)} kg"
                                              else "${String.format(Locale.US, "%.1f", weightChange)} kg"
                            Text(
                                text = "Overall change: $changeText",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (weightChange <= 0) Color(0xFF4CAF50) else Color(0xFFE53935)
                            )
                        }

                        val view = androidx.compose.ui.platform.LocalView.current
                        IconButton(onClick = {
                            if (weightLogs.isNotEmpty()) {
                                com.example.util.HapticFeedbackHelper.triggerRejectOrDelete(view)
                                viewModel.deleteWeightLog(weightLogs.last())
                            }
                        }, modifier = Modifier.testTag("delete_last_weight")) {
                            Icon(Icons.Default.Undo, contentDescription = "Delete Last Weight Log")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Draw Line Chart
                    if (weightLogs.size > 1) {
                        WeightTrendLineChart(weightLogs = weightLogs, modifier = Modifier.height(180.dp).fillMaxWidth())
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.LightGray.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Log weight on multiple days to view trends",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // LOG CURRENT WEIGHT SHEET
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Log Body Weight",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = weightInput,
                            onValueChange = { weightInput = it },
                            label = { Text("Weight (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("weight_log_input")
                        )

                        val view = androidx.compose.ui.platform.LocalView.current
                        Button(
                            onClick = {
                                val weight = weightInput.toFloatOrNull()
                                if (weight != null && weight > 20f) {
                                    com.example.util.HapticFeedbackHelper.triggerConfirm(view)
                                    viewModel.logWeight(weightDateInput, weight)
                                    weightInput = ""
                                }
                            },
                            enabled = weightInput.isNotBlank(),
                            modifier = Modifier
                                .height(56.dp)
                                .testTag("weight_log_save_button")
                        ) {
                            Text("Log")
                        }
                    }
                }
            }

            // MACRO DISTRIBUTION DOUGHNUT CHART (CUSTOM CANVAS)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Weekly Macro Distribution",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Averages over the past 7 days",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    if (grandTotalMacros > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            MacroDoughnutChart(
                                protein = totalProtein,
                                carbs = totalCarbs,
                                fat = totalFat,
                                modifier = Modifier.size(140.dp)
                            )

                            // Legend and distribution text
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                val pPct = ((totalProtein / grandTotalMacros) * 100f).roundToInt()
                                val cPct = ((totalCarbs / grandTotalMacros) * 100f).roundToInt()
                                val fPct = ((totalFat / grandTotalMacros) * 100f).roundToInt()

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(2.dp)).background(MacroProteinColor))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Protein: $pPct% (${totalProtein.toInt()}g)", style = MaterialTheme.typography.bodySmall)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(2.dp)).background(MacroCarbsColor))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Carbs: $cPct% (${totalCarbs.toInt()}g)", style = MaterialTheme.typography.bodySmall)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(2.dp)).background(MacroFatColor))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Fats: $fPct% (${totalFat.toInt()}g)", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.LightGray.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Log food diaries to compile nutrition analytics",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeightTrendLineChart(
    weightLogs: List<WeightLog>,
    modifier: Modifier = Modifier
) {
    val strokeColor = MaterialTheme.colorScheme.primary
    val gridColor = Color.LightGray.copy(alpha = 0.5f)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val paddingLeft = 40f
        val paddingRight = 20f
        val paddingTop = 20f
        val paddingBottom = 40f

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom

        val maxWeight = weightLogs.maxOf { it.weightKg } + 2f
        val minWeight = (weightLogs.minOf { it.weightKg } - 2f).coerceAtLeast(0f)
        val weightRange = if (maxWeight - minWeight > 0) maxWeight - minWeight else 1f

        // Draw horizontal grid lines and weight labels
        val gridLinesCount = 4
        for (i in 0..gridLinesCount) {
            val ratio = i.toFloat() / gridLinesCount.toFloat()
            val y = paddingTop + chartHeight * (1f - ratio)
            drawLine(
                color = gridColor,
                start = Offset(paddingLeft, y),
                end = Offset(width - paddingRight, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw the weight trend line
        val points = weightLogs.mapIndexed { index, log ->
            val xFraction = if (weightLogs.size > 1) index.toFloat() / (weightLogs.size - 1).toFloat() else 0f
            val yFraction = (log.weightKg - minWeight) / weightRange

            val x = paddingLeft + xFraction * chartWidth
            val y = paddingTop + chartHeight * (1f - yFraction)
            Offset(x, y)
        }

        for (i in 0 until points.size - 1) {
            drawLine(
                color = strokeColor,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Draw data circles
        points.forEach { pt ->
            drawCircle(
                color = strokeColor,
                radius = 5.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = Color.White,
                radius = 2.dp.toPx(),
                center = pt
            )
        }
    }
}

@Composable
fun MacroDoughnutChart(
    protein: Float,
    carbs: Float,
    fat: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val total = protein + carbs + fat
        if (total == 0f) return@Canvas

        val pSweep = (protein / total) * 360f
        val cSweep = (carbs / total) * 360f
        val fSweep = (fat / total) * 360f

        val stroke = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Butt)
        val canvasSize = size.minDimension

        // Draw Protein Arc
        drawArc(
            color = MacroProteinColor,
            startAngle = -90f,
            sweepAngle = pSweep,
            useCenter = false,
            style = stroke,
            size = Size(canvasSize, canvasSize)
        )

        // Draw Carbs Arc
        drawArc(
            color = MacroCarbsColor,
            startAngle = -90f + pSweep,
            sweepAngle = cSweep,
            useCenter = false,
            style = stroke,
            size = Size(canvasSize, canvasSize)
        )

        // Draw Fats Arc
        drawArc(
            color = MacroFatColor,
            startAngle = -90f + pSweep + cSweep,
            sweepAngle = fSweep,
            useCenter = false,
            style = stroke,
            size = Size(canvasSize, canvasSize)
        )
    }
}
