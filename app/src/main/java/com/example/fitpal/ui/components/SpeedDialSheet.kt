package com.example.fitpal.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.fitpal.util.rememberWindowDimensions

/**
 * Multi-Action Speed Dial Component for FitPal.
 * Expands from the center floating "+" button.
 * Provides instant tactile shortcuts:
 * 1. Log Workout / Routine (Start workout, AI Workout Notebook)
 * 2. Log Food / Water (Manual Food, AI Food Notepad, Hydration Quick-Log)
 * 3. Record Weight & Body Measurements
 * 4. Check-in Habits
 * Adapts as a Bottom Sheet on Phones (<600dp) and an anchored Modal Dialog on Tablets (>=600dp).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedDialSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onLogFoodManual: () -> Unit,
    onLogFoodAi: () -> Unit,
    onLogWater: () -> Unit,
    onStartWorkout: () -> Unit,
    onAiWorkoutNotebook: () -> Unit,
    onRecordWeight: (Float) -> Unit,
    onCheckinHabits: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    val windowDimensions = rememberWindowDimensions()
    val isTablet = windowDimensions.isTablet

    var showQuickWeightDialog by remember { mutableStateOf(false) }

    if (showQuickWeightDialog) {
        var weightInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showQuickWeightDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Scale, contentDescription = null, tint = Color(0xFFCEFD1A))
                    Text("Log Body Weight", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter current weight in kilograms. Updates your real-time BMR and dynamic TDEE calculations.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF717886)
                    )
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        label = { Text("Weight (kg)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().testTag("speed_dial_weight_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = weightInput.toFloatOrNull()
                        if (parsed != null && parsed > 0) {
                            onRecordWeight(parsed)
                            showQuickWeightDialog = false
                            onDismiss()
                        }
                    },
                    modifier = Modifier.testTag("speed_dial_save_weight_btn")
                ) {
                    Text("Save Weight", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickWeightDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (isTablet) {
        // TABLET: Anchored Centered Modal Dialog
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF111317),
                border = BorderStroke(1.dp, Color(255, 255, 255, 20)),
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .testTag("speed_dial_tablet_modal")
            ) {
                SpeedDialContent(
                    onDismiss = onDismiss,
                    onLogFoodManual = { onDismiss(); onLogFoodManual() },
                    onLogFoodAi = { onDismiss(); onLogFoodAi() },
                    onLogWater = { onDismiss(); onLogWater() },
                    onStartWorkout = { onDismiss(); onStartWorkout() },
                    onAiWorkoutNotebook = { onDismiss(); onAiWorkoutNotebook() },
                    onOpenWeightDialog = { showQuickWeightDialog = true },
                    onCheckinHabits = { onDismiss(); onCheckinHabits() }
                )
            }
        }
    } else {
        // PHONE: Bottom Sheet
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
            modifier = modifier.testTag("speed_dial_bottom_sheet")
        ) {
            SpeedDialContent(
                onDismiss = onDismiss,
                onLogFoodManual = { onDismiss(); onLogFoodManual() },
                onLogFoodAi = { onDismiss(); onLogFoodAi() },
                onLogWater = { onDismiss(); onLogWater() },
                onStartWorkout = { onDismiss(); onStartWorkout() },
                onAiWorkoutNotebook = { onDismiss(); onAiWorkoutNotebook() },
                onOpenWeightDialog = { showQuickWeightDialog = true },
                onCheckinHabits = { onDismiss(); onCheckinHabits() }
            )
        }
    }
}

@Composable
private fun SpeedDialContent(
    onDismiss: () -> Unit,
    onLogFoodManual: () -> Unit,
    onLogFoodAi: () -> Unit,
    onLogWater: () -> Unit,
    onStartWorkout: () -> Unit,
    onAiWorkoutNotebook: () -> Unit,
    onOpenWeightDialog: () -> Unit,
    onCheckinHabits: () -> Unit
) {
    val acidLime = Color(0xFFCEFD1A)
    val flameCoral = Color(0xFFFF5722)
    val electricAzure = Color(0xFF0EA5E9)
    val amberSun = Color(0xFFF59E0B)
    val neutralGrey = Color(0xFF717886)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "QUICK ACTIONS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Black
                    ),
                    color = acidLime
                )
                Text(
                    text = "Log activity, nutrition or measurements",
                    style = MaterialTheme.typography.bodySmall,
                    color = neutralGrey
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp).testTag("speed_dial_close_btn")
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = neutralGrey)
            }
        }

        // Section 1: Nutrition & Fueling
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "NUTRITION & HYDRATION",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.8.sp),
                color = neutralGrey
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionTile(
                    title = "Log Food",
                    subtitle = "Manual Entry",
                    icon = Icons.Default.Restaurant,
                    tint = electricAzure,
                    testTag = "speed_action_log_food_manual",
                    modifier = Modifier.weight(1f),
                    onClick = onLogFoodManual
                )
                QuickActionTile(
                    title = "AI Food Note",
                    subtitle = "Natural Language",
                    icon = Icons.Default.AutoAwesome,
                    tint = acidLime,
                    testTag = "speed_action_log_food_ai",
                    modifier = Modifier.weight(1f),
                    onClick = onLogFoodAi
                )
                QuickActionTile(
                    title = "Water +1",
                    subtitle = "Log 250ml",
                    icon = Icons.Default.LocalDrink,
                    tint = Color(0xFF06B6D4),
                    testTag = "speed_action_log_water",
                    modifier = Modifier.weight(1f),
                    onClick = onLogWater
                )
            }
        }

        // Section 2: Training & Performance
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "WORKOUTS & EXERCISE",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.8.sp),
                color = neutralGrey
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionTile(
                    title = "Log Workout",
                    subtitle = "Exercise / Sets",
                    icon = Icons.Default.FitnessCenter,
                    tint = flameCoral,
                    testTag = "speed_action_log_workout",
                    modifier = Modifier.weight(1f),
                    onClick = onStartWorkout
                )
                QuickActionTile(
                    title = "AI Notebook",
                    subtitle = "Workout Parser",
                    icon = Icons.Default.Bolt,
                    tint = amberSun,
                    testTag = "speed_action_ai_workout",
                    modifier = Modifier.weight(1f),
                    onClick = onAiWorkoutNotebook
                )
            }
        }

        // Section 3: Biometrics & Routine
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "BIOMETRICS & HABITS",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.8.sp),
                color = neutralGrey
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionTile(
                    title = "Log Weight",
                    subtitle = "Scale / Bodyfat",
                    icon = Icons.Default.Scale,
                    tint = Color(0xFFA855F7),
                    testTag = "speed_action_log_weight",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenWeightDialog
                )
                QuickActionTile(
                    title = "Habit Check",
                    subtitle = "30-Day Grid",
                    icon = Icons.Default.CheckCircle,
                    tint = acidLime,
                    testTag = "speed_action_checkin_habits",
                    modifier = Modifier.weight(1f),
                    onClick = onCheckinHabits
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun QuickActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF181B22),
        border = BorderStroke(1.dp, Color(255, 255, 255, 18)),
        modifier = modifier.testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    ),
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Color(0xFF717886)
                )
            }
        }
    }
}
