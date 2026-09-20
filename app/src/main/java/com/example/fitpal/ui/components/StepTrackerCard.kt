package com.example.fitpal.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitpal.ui.theme.*
import com.example.fitpal.util.StepExpenditureResult

@Composable
fun StepTrackerCard(
    currentSteps: Int,
    stepResult: StepExpenditureResult,
    onSaveSteps: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var inputStepText by remember { mutableStateOf(currentSteps.toString()) }

    val electricLime = MaterialTheme.colorScheme.primary
    val cardBg = MaterialTheme.colorScheme.surface
    val elevatedBg = MaterialTheme.colorScheme.surfaceVariant
    val crispWhite = MaterialTheme.colorScheme.onSurface
    val mutedGrey = MaterialTheme.colorScheme.onSurfaceVariant

    GlassCard(
        modifier = modifier.testTag("step_tracker_card"),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
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
                                imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                                contentDescription = "Steps",
                                tint = electricLime,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Daily Steps & Activity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = crispWhite
                        )
                        Text(
                            text = "STEP METABOLIC BURN",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                letterSpacing = 0.8.sp
                            ),
                            color = mutedGrey
                        )
                    }
                }

                IconButton(
                    onClick = {
                        inputStepText = currentSteps.toString()
                        showEditDialog = true
                    },
                    modifier = Modifier.testTag("edit_steps_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit steps",
                        tint = electricLime,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "$currentSteps",
                        style = MetricNumeralXLarge.copy(fontSize = 32.sp),
                        color = crispWhite
                    )
                    Text(
                        text = "/ STEPS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = mutedGrey,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = electricLime.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, electricLime.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "+${stepResult.estimatedCalories} kcal",
                            style = MetricNumeralSmall,
                            color = electricLime
                        )
                        Text(
                            text = "${stepResult.activeStepsAboveBaseline} active steps",
                            style = MetricNumeralTiny,
                            color = crispWhite.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Daily Step Goal Progress Bar & Completion State
            val dailyStepGoal = 10000
            val stepFraction = if (dailyStepGoal > 0) currentSteps.toFloat() / dailyStepGoal.toFloat() else 0f
            val isStepGoalReached = currentSteps >= dailyStepGoal
            val stepPct = (stepFraction * 100).toInt()

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
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
                        Text(
                            text = if (isStepGoalReached) "10,000 Step Goal Reached!" else "${(dailyStepGoal - currentSteps).coerceAtLeast(0)} steps to goal",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = if (isStepGoalReached) electricLime else mutedGrey
                        )
                        if (isStepGoalReached) {
                            GoalCompletionBadge(
                                text = "GOAL HIT 🏆",
                                color = electricLime,
                                isCompleted = true
                            )
                        }
                    }

                    Text(
                        text = "$stepPct%",
                        style = MetricNumeralTiny.copy(fontWeight = FontWeight.Bold),
                        color = if (isStepGoalReached) electricLime else crispWhite
                    )
                }

                // Smooth Motion-Animated Step Bar with Light Shimmer and Goal Breathing Glow
                MotionProgressBar(
                    progress = stepFraction,
                    color = electricLime,
                    trackColor = elevatedBg,
                    height = 8.dp,
                    isGoalCompleted = isStepGoalReached,
                    completionColor = electricLime,
                    showShimmer = true,
                    testTag = "step_motion_progress_bar"
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick add buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onSaveSteps(currentSteps + 1000) },
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = crispWhite),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Text("+1,000", style = MetricNumeralTiny.copy(fontWeight = FontWeight.Bold))
                }
                OutlinedButton(
                    onClick = { onSaveSteps(currentSteps + 2500) },
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = crispWhite),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Text("+2,500", style = MetricNumeralTiny.copy(fontWeight = FontWeight.Bold))
                }
                OutlinedButton(
                    onClick = { onSaveSteps(currentSteps + 5000) },
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = crispWhite),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Text("+5,000", style = MetricNumeralTiny.copy(fontWeight = FontWeight.Bold))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stepResult.explanation,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = mutedGrey,
                lineHeight = 15.sp
            )
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Set Daily Steps", fontWeight = FontWeight.Bold, color = crispWhite) },
            text = {
                OutlinedTextField(
                    value = inputStepText,
                    onValueChange = { inputStepText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Step Count") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("step_count_input_field")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val steps = inputStepText.toIntOrNull() ?: currentSteps
                        onSaveSteps(steps)
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = electricLime, contentColor = Color.Black),
                    modifier = Modifier.testTag("save_steps_confirm_btn")
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = mutedGrey)
                }
            },
            containerColor = cardBg
        )
    }
}
