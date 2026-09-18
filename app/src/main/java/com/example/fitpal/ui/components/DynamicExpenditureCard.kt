package com.example.fitpal.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitpal.util.DynamicExpenditureResult

@Composable
fun DynamicExpenditureCard(
    result: DynamicExpenditureResult,
    consumedCalories: Int,
    onOpenStepLogger: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showExplanationDialog by remember { mutableStateOf(false) }

    val electricLime = Color(0xFFCEFD1A)
    val cardBg = Color(0xFF111317)
    val elevatedBg = Color(0xFF181B20)
    val crispWhite = Color(0xFFF0F3F6)
    val mutedGrey = Color(0xFF717886)
    val warmBrown = Color(0xFF4A3828)
    val coralBadge = Color(0xFFFF5252)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dynamic_expenditure_card"),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = elevatedBg,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "Energy",
                                tint = electricLime,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Dynamic Energy Expenditure",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = crispWhite
                        )
                        Text(
                            text = "METABOLIC ENGINE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                letterSpacing = 0.8.sp
                            ),
                            color = mutedGrey
                        )
                    }
                }

                IconButton(
                    onClick = { showExplanationDialog = true },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("expenditure_how_calculated_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                        contentDescription = "How calculated?",
                        tint = electricLime,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Subtitle explanation
            Text(
                text = "Personalized from your profile baseline + actual logged activity. No arbitrary multiplier guesses.",
                style = MaterialTheme.typography.bodySmall,
                color = mutedGrey,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3-Pillar Breakdown Chips (Baseline + Workouts + Net Steps)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Baseline Non-Exercise
                ExpenditurePillarItem(
                    title = "Baseline Living",
                    subtitle = if (result.isKatchMcArdle) "Katch-McArdle" else "BMR × 1.20",
                    value = "${result.baselineExpenditure}",
                    unit = "kcal",
                    containerColor = elevatedBg,
                    modifier = Modifier.weight(1f)
                )

                // Logged Workouts
                ExpenditurePillarItem(
                    title = "Workouts",
                    subtitle = "Actual logged",
                    value = "+${result.loggedWorkoutCalories}",
                    unit = "kcal",
                    containerColor = if (result.loggedWorkoutCalories > 0)
                        Color(0xFF2E462E)
                    else elevatedBg,
                    modifier = Modifier.weight(1f)
                )

                // Net Active Steps
                ExpenditurePillarItem(
                    title = "Active Steps",
                    subtitle = "Above baseline",
                    value = "+${result.loggedStepCalories}",
                    unit = "kcal",
                    containerColor = if (result.loggedStepCalories > 0)
                        Color(0xFF2E462E)
                    else elevatedBg,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenStepLogger() }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Total Burn vs Dynamic Calorie Target summary bar
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = Color.White.copy(alpha = 0.08f)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Estimated Burn",
                        style = MaterialTheme.typography.bodySmall,
                        color = mutedGrey
                    )
                    Text(
                        text = "${result.totalEstimatedExpenditure} kcal",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = crispWhite
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = electricLime.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, electricLime.copy(alpha = 0.4f)),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Dynamic Calorie Target",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = mutedGrey
                        )
                        Text(
                            text = "${result.dynamicCalorieTarget} kcal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = electricLime
                        )
                    }
                }
            }

            if (result.isClampedToFloor) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = coralBadge.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, coralBadge.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Calorie target safely bounded to ${result.safetyFloor} kcal physiological minimum.",
                        style = MaterialTheme.typography.labelSmall,
                        color = crispWhite,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }

    if (showExplanationDialog) {
        AlertDialog(
            onDismissRequest = { showExplanationDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = null,
                        tint = electricLime
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "How is this calculated?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = crispWhite
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "FitPal uses a dynamic metabolic model rather than fixed, arbitrary activity multipliers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = mutedGrey
                    )

                    CalculationBreakdownRow(
                        step = "1. Basal Metabolic Rate (BMR)",
                        formula = if (result.isKatchMcArdle)
                            "370 + (21.6 × Lean Body Mass)"
                        else
                            "Mifflin-St Jeor equation based on age, gender, height & current weight",
                        value = "${result.bmr} kcal"
                    )

                    CalculationBreakdownRow(
                        step = "2. Conservative Baseline Living",
                        formula = "BMR (${result.bmr}) × 1.20 covers basic sedentary movement, digestion (TEF), and organ function.",
                        value = "${result.baselineExpenditure} kcal"
                    )

                    CalculationBreakdownRow(
                        step = "3. Actual Logged Workouts",
                        formula = "Derived via scientific MET (Metabolic Equivalent of Task) values tailored to exercise type, intensity, and your body weight.",
                        value = "+${result.loggedWorkoutCalories} kcal"
                    )

                    CalculationBreakdownRow(
                        step = "4. Relevant Net Steps (No Double-Counting)",
                        formula = "First 3,000 steps and any logged walking workout steps are automatically deducted. Remaining active steps burn ~0.00045 kcal/step/kg.",
                        value = "+${result.loggedStepCalories} kcal"
                    )

                    CalculationBreakdownRow(
                        step = "5. Goal Adjustment",
                        formula = if (result.goalAdjustment != 0) "Caloric deficit or surplus applied to your daily expenditure" else "Weight maintenance (0 adjustment)",
                        value = "${if (result.goalAdjustment > 0) "+" else ""}${result.goalAdjustment} kcal"
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showExplanationDialog = false },
                    modifier = Modifier.testTag("close_expenditure_explanation_btn")
                ) {
                    Text("Got it", color = electricLime, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = cardBg
        )
    }
}

@Composable
private fun ExpenditurePillarItem(
    title: String,
    subtitle: String,
    value: String,
    unit: String,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    val crispWhite = Color(0xFFF5F5F7)
    val mutedGrey = Color(0xFF9CA3AF)
    val electricLime = Color(0xFF20AA1D)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = crispWhite,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = mutedGrey,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (value.startsWith("+")) electricLime else crispWhite
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = mutedGrey
                )
            }
        }
    }
}

@Composable
private fun CalculationBreakdownRow(
    step: String,
    formula: String,
    value: String
) {
    val elevatedBg = Color(0xFF1E251E)
    val crispWhite = Color(0xFFF5F5F7)
    val mutedGrey = Color(0xFF9CA3AF)
    val electricLime = Color(0xFF20AA1D)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(elevatedBg)
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = step,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = crispWhite
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = electricLime
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = formula,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = mutedGrey
        )
    }
}
