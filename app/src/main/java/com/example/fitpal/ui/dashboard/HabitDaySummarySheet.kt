package com.example.fitpal.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.fitpal.util.DateUtils
import com.example.fitpal.util.rememberWindowDimensions

data class HabitDayDrillDownData(
    val dateStr: String,
    val caloriesConsumed: Int,
    val calorieTarget: Int,
    val proteinConsumed: Float,
    val proteinTarget: Int,
    val waterGlasses: Int,
    val stepsCount: Int,
    val workoutCount: Int,
    val workoutDurationMinutes: Int,
    val activeCalories: Int,
    val weightKg: Float?,
    val completedHabits: List<String>,
    val missedHabits: List<String>
)

/**
 * Drill-down inspection sheet for any clicked day in the 30-day habit contribution grid.
 * Adaptive: Bottom sheet on Phone (<600dp), Dialog Modal on Tablet (>=600dp).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDaySummarySheet(
    data: HabitDayDrillDownData?,
    onDismiss: () -> Unit,
    onNavigateToDiary: (String) -> Unit,
    onNavigateToWorkouts: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (data == null) return

    val windowDimensions = rememberWindowDimensions()
    val isTablet = windowDimensions.isTablet

    if (isTablet) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF111317),
                border = BorderStroke(1.dp, Color(255, 255, 255, 20)),
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .testTag("habit_day_summary_dialog")
            ) {
                HabitDaySummaryContent(
                    data = data,
                    onDismiss = onDismiss,
                    onNavigateToDiary = { onDismiss(); onNavigateToDiary(data.dateStr) },
                    onNavigateToWorkouts = { onDismiss(); onNavigateToWorkouts(data.dateStr) }
                )
            }
        }
    } else {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = Color(0xFF111317),
            contentColor = Color(0xFFF1F5F9),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                )
            },
            modifier = modifier.testTag("habit_day_summary_sheet")
        ) {
            HabitDaySummaryContent(
                data = data,
                onDismiss = onDismiss,
                onNavigateToDiary = { onDismiss(); onNavigateToDiary(data.dateStr) },
                onNavigateToWorkouts = { onDismiss(); onNavigateToWorkouts(data.dateStr) }
            )
        }
    }
}

@Composable
private fun HabitDaySummaryContent(
    data: HabitDayDrillDownData,
    onDismiss: () -> Unit,
    onNavigateToDiary: () -> Unit,
    onNavigateToWorkouts: () -> Unit
) {
    val acidLime = Color(0xFFCEFD1A)
    val flameCoral = Color(0xFFFF5722)
    val electricAzure = Color(0xFF0EA5E9)
    val neutralGrey = Color(0xFF717886)
    val displayDate = DateUtils.formatDateForDisplay(data.dateStr)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "METABOLIC DRILL-DOWN",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Black
                    ),
                    color = acidLime
                )
                Text(
                    text = displayDate,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp).testTag("drilldown_close_btn")
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = neutralGrey)
            }
        }

        // Section 1: Calorie & Macro Performance
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF161920),
            border = BorderStroke(1.dp, Color(255, 255, 255, 18)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "NUTRITION INTAKE",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.8.sp),
                    color = neutralGrey
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Calories
                    Column {
                        Text("Calories", style = MaterialTheme.typography.bodySmall, color = neutralGrey)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${data.caloriesConsumed}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color.White
                            )
                            Text(
                                text = " / ${data.calorieTarget} kcal",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = neutralGrey,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }

                    // Protein
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Protein", style = MaterialTheme.typography.bodySmall, color = neutralGrey)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${data.proteinConsumed.toInt()}g",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = electricAzure
                            )
                            Text(
                                text = " / ${data.proteinTarget}g",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = neutralGrey,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Section 2: Activity, Training & Hydration Matrix
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Steps & Active Burn
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF161920),
                border = BorderStroke(1.dp, Color(255, 255, 255, 18)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Steps", style = MaterialTheme.typography.labelSmall, color = neutralGrey)
                    Text(
                        text = "${data.stepsCount}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color.White
                    )
                    Text(
                        text = "${data.activeCalories} kcal burned",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = acidLime
                    )
                }
            }

            // Training
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF161920),
                border = BorderStroke(1.dp, Color(255, 255, 255, 18)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Workouts", style = MaterialTheme.typography.labelSmall, color = neutralGrey)
                    Text(
                        text = "${data.workoutCount} Session${if (data.workoutCount == 1) "" else "s"}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = if (data.workoutCount > 0) flameCoral else neutralGrey
                    )
                    Text(
                        text = "${data.workoutDurationMinutes} mins total",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = neutralGrey
                    )
                }
            }

            // Water & Weight
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF161920),
                border = BorderStroke(1.dp, Color(255, 255, 255, 18)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Water / Scale", style = MaterialTheme.typography.labelSmall, color = neutralGrey)
                    Text(
                        text = "${data.waterGlasses} Glasses",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color(0xFF06B6D4)
                    )
                    Text(
                        text = data.weightKg?.let { "$it kg" } ?: "No weight",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = neutralGrey
                    )
                }
            }
        }

        // Section 3: Habit Compliance List
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HABIT AUDIT",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.8.sp),
                    color = neutralGrey
                )
                Text(
                    text = "${data.completedHabits.size} Completed / ${data.missedHabits.size} Missed",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (data.completedHabits.size >= 4) acidLime else neutralGrey
                    )
                )
            }

            // Completed habits
            data.completedHabits.forEach { habit ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = acidLime,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(habit, style = MaterialTheme.typography.bodySmall, color = Color.White)
                }
            }

            // Missed habits
            data.missedHabits.forEach { habit ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = neutralGrey.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(habit, style = MaterialTheme.typography.bodySmall, color = neutralGrey)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Navigation Action Buttons (Drill into Diary or Workouts for this date)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onNavigateToDiary,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, electricAzure.copy(alpha = 0.5f)),
                modifier = Modifier.weight(1f).testTag("drilldown_view_diary_btn")
            ) {
                Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(16.dp), tint = electricAzure)
                Spacer(modifier = Modifier.width(6.dp))
                Text("View Meals", color = electricAzure, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Button(
                onClick = onNavigateToWorkouts,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = acidLime, contentColor = Color.Black),
                modifier = Modifier.weight(1f).testTag("drilldown_view_workouts_btn")
            ) {
                Icon(Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Day Logs", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
