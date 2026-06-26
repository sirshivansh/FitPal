package com.example.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.*
import com.example.util.DateUtils
import com.example.util.Calculations
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToSettings: () -> Unit,
    onQuickAddFood: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.userProfile.collectAsState()
    val date by viewModel.selectedDate.collectAsState()
    val foodEntries by viewModel.foodEntries.collectAsState()
    val exerciseEntries by viewModel.exerciseEntries.collectAsState()
    val waterLog by viewModel.waterLog.collectAsState()

    val scrollState = rememberScrollState()

    val activityCalorieLog by viewModel.activityCalorieLog.collectAsState()
    val additionalActivityCalories = activityCalorieLog?.additionalCalories ?: 0

    var showTdeeDialog by remember { mutableStateOf(false) }
    var tdeeInputText by remember { mutableStateOf("") }

    // Aggregate values
    val totalFoodCalories = foodEntries.sumOf { it.caloriesConsumed.toDouble() }.toInt()
    val totalProtein = foodEntries.sumOf { it.proteinGrams.toDouble() }.toFloat()
    val totalCarbs = foodEntries.sumOf { it.carbsGrams.toDouble() }.toFloat()
    val totalFat = foodEntries.sumOf { it.fatGrams.toDouble() }.toFloat()

    val totalExerciseCalories = exerciseEntries.sumOf { it.caloriesBurned.toDouble() }.toInt()

    val isMaintenanceAvailable = profile != null && profile!!.maintenanceCalories != null && profile!!.maintenanceCalories!! > 0

    val calorieGoal = if (isMaintenanceAvailable) {
        Calculations.calculateTargetCalorieGoal(
            dailyCalories = profile!!.maintenanceCalories!!.toFloat(),
            weightGoalType = profile!!.weightGoalType,
            adjustmentValue = profile!!.calorieAdjustment
        )
    } else {
        0
    }

    val macroGoals = if (profile != null && calorieGoal > 0) {
        Calculations.calculateMacrosCustom(
            targetCalories = calorieGoal,
            weightKg = profile!!.currentWeightKg,
            proteinMultiplier = profile!!.proteinMultiplier,
            fatMultiplier = profile!!.fatMultiplier
        )
    } else {
        Triple(0, 0, 0)
    }

    val carbsGoal = macroGoals.first
    val proteinGoal = macroGoals.second
    val fatGoal = macroGoals.third

    val netCalories = totalFoodCalories - totalExerciseCalories
    val remainingCalories = if (isMaintenanceAvailable) {
        calorieGoal - netCalories + additionalActivityCalories
    } else {
        0
    }

    // Healthy floors warning system
    val showWarning = when {
        totalFoodCalories > 0 && profile != null -> {
            if (profile!!.gender.lowercase() == "female" && netCalories < 1200) true
            else profile!!.gender.lowercase() == "male" && netCalories < 1500
        }
        else -> false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FitPal Dashboard", fontWeight = FontWeight.Black) },
                actions = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("dashboard_settings_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onQuickAddFood,
                modifier = Modifier
                    .testTag("dashboard_quick_add_fab")
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(24.dp), // matched to [1.5rem] in theme design
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Quick Add Food", modifier = Modifier.size(28.dp))
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(bottom = 80.dp), // spacing for FAB
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // DATE NAVIGATION BAR (Styled to match the Sleek Interface header)
            val isDateToday = date == DateUtils.getTodayDateString()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.setDate(DateUtils.getPreviousDay(date)) },
                    modifier = Modifier.testTag("dashboard_prev_day")
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day")
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (isDateToday) "TODAY" else "SELECTED DAY",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = DateUtils.formatDateForDisplay(date),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                }

                IconButton(
                    onClick = { viewModel.setDate(DateUtils.getNextDay(date)) },
                    modifier = Modifier.testTag("dashboard_next_day")
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Day")
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // HEALTH SAFETY LIMIT WARNING BANNER
                if (showWarning) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Nutritional Warning",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Your daily net calories are below the recommended healthy minimum (${if (profile?.gender == "female") "1200" else "1500"} kcal). Please ensure you eat enough to fuel your metabolism.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                // CALORIE CONCENTRIC PROGRESS RING OR BMR FALLBACK
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Calorie Budget",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (isMaintenanceAvailable) {
                            CalorieRing(
                                consumed = totalFoodCalories,
                                goal = calorieGoal,
                                burned = totalExerciseCalories,
                                size = 180.dp
                            )
                        } else {
                            // Show BMR and prompt for Maintenance Calories
                            val bmrVal = profile?.bmr?.toInt() ?: 1500
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // BMR display circle/box
                                Box(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "$bmrVal",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "kcal",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }

                                Text(
                                    text = "Your BMR (Basal Metabolic Rate): $bmrVal kcal",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Text(
                                    text = "BMR is the amount of energy your body requires to function at complete rest. To calculate a proper target calorie goal, we must apply your calorie adjustment (deficit/surplus) to your Maintenance Calories (TDEE) instead of BMR.",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Text(
                                    text = "Please enter your Maintenance Calories (TDEE) to calculate your calorie target. A calorie deficit cannot be applied directly to BMR.",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.error
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        tdeeInputText = ""
                                        showTdeeDialog = true
                                    },
                                    modifier = Modifier.testTag("set_tdee_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Set Maintenance Calories (TDEE)", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // ADDITIONAL ACTIVITY CALORIES CARD
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Additional Calories Burned Today",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Optionally enter calories burned manually through physical activities to raise today's goal budget.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        var activityCaloriesText by remember(additionalActivityCalories) {
                            mutableStateOf(if (additionalActivityCalories == 0) "" else additionalActivityCalories.toString())
                        }
                        val view = androidx.compose.ui.platform.LocalView.current

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = activityCaloriesText,
                                onValueChange = { newVal ->
                                    if (newVal.all { it.isDigit() }) {
                                        activityCaloriesText = newVal
                                    }
                                },
                                label = { Text("Calories (kcal)") },
                                placeholder = { Text("e.g. 300") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("additional_calories_input")
                            )

                            Button(
                                onClick = {
                                    val kCal = activityCaloriesText.toIntOrNull() ?: 0
                                    viewModel.setAdditionalActivityCalories(kCal)
                                    com.example.util.HapticFeedbackHelper.triggerConfirm(view)
                                },
                                modifier = Modifier
                                    .height(56.dp)
                                    .testTag("additional_calories_save_button")
                            ) {
                                Text("Save", fontWeight = FontWeight.Bold)
                            }
                        }

                        // Activity suggestion chips
                        Text(
                            text = "Examples:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val examples = listOf(
                                "Walking" to 150,
                                "Gym" to 300,
                                "Running" to 400
                            )
                            examples.forEach { (name, kcal) ->
                                AssistChip(
                                    onClick = {
                                        activityCaloriesText = kcal.toString()
                                        viewModel.setAdditionalActivityCalories(kcal)
                                        com.example.util.HapticFeedbackHelper.triggerConfirm(view)
                                    },
                                    label = { Text("$name (+$kcal)") },
                                    modifier = Modifier.testTag("activity_suggest_$name")
                                )
                            }
                        }
                    }
                }

                // MACRONUTRIENT BARS
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Macronutrient Targets",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Divider(color = MaterialTheme.colorScheme.outlineVariant)

                        MacroProgressBar(
                            label = "Protein",
                            currentGrams = totalProtein,
                            goalGrams = proteinGoal,
                            color = MacroProteinColor,
                            modifier = Modifier.testTag("dashboard_protein_bar")
                        )

                        MacroProgressBar(
                            label = "Carbohydrates",
                            currentGrams = totalCarbs,
                            goalGrams = carbsGoal,
                            color = MacroCarbsColor,
                            modifier = Modifier.testTag("dashboard_carbs_bar")
                        )

                        MacroProgressBar(
                            label = "Fat",
                            currentGrams = totalFat,
                            goalGrams = fatGoal,
                            color = MacroFatColor,
                            modifier = Modifier.testTag("dashboard_fat_bar")
                        )
                    }
                }

                // WATER TRACKING CARD
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Water Intake Tracker",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Stay hydrated throughout the day",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = { viewModel.resetWater() },
                                modifier = Modifier.testTag("water_reset_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reset Water Log",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Glasses visual grid
                        val glassCount = waterLog?.glassesCount ?: 0
                        val view = androidx.compose.ui.platform.LocalView.current
                        val onIncrement = {
                            viewModel.incrementWater()
                            if (glassCount == 7) {
                                com.example.util.HapticFeedbackHelper.triggerConfirm(view)
                            } else {
                                com.example.util.HapticFeedbackHelper.triggerLightTap(view)
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = {
                                    if (glassCount > 0) {
                                        viewModel.decrementWater()
                                        com.example.util.HapticFeedbackHelper.triggerLightTap(view)
                                    }
                                },
                                enabled = glassCount > 0,
                                modifier = Modifier.testTag("water_minus_button")
                            ) {
                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove Glass", modifier = Modifier.size(28.dp))
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                             // Interactive water indicator
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(vertical = 12.dp, horizontal = 24.dp)
                                    .clickable { onIncrement() }
                                    .testTag("water_glass_container")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalDrink,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(42.dp)
                                )
                                Text(
                                    text = "$glassCount / 8 Glasses",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${glassCount * 250} ml logged",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            IconButton(
                                onClick = { onIncrement() },
                                modifier = Modifier.testTag("water_plus_button")
                            ) {
                                Icon(Icons.Default.AddCircleOutline, contentDescription = "Add Glass", modifier = Modifier.size(28.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Standard progress line
                        LinearProgressIndicator(
                            progress = (glassCount / 8f).coerceIn(0f, 1f),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                        )
                    }
                }
            }
        }
    }

    if (showTdeeDialog) {
        val view = androidx.compose.ui.platform.LocalView.current
        AlertDialog(
            onDismissRequest = { showTdeeDialog = false },
            title = { Text("Set Maintenance Calories") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter your estimated Daily Maintenance Calories (TDEE). This represents the calories you burn daily including physical activity, and will be used to safely calculate your goal target.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = tdeeInputText,
                        onValueChange = { newVal ->
                            if (newVal.all { it.isDigit() }) {
                                tdeeInputText = newVal
                            }
                        },
                        label = { Text("Maintenance Calories (kcal)") },
                        placeholder = { Text("e.g. 2500") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("dialog_tdee_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val tdeeVal = tdeeInputText.toIntOrNull()
                        if (tdeeVal != null && tdeeVal > 500) {
                            com.example.util.HapticFeedbackHelper.triggerConfirm(view)
                            viewModel.saveMaintenanceCalories(tdeeVal)
                            showTdeeDialog = false
                        }
                    },
                    enabled = tdeeInputText.isNotBlank(),
                    modifier = Modifier.testTag("dialog_tdee_save")
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTdeeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
