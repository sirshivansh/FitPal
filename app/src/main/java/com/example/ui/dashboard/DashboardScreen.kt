package com.example.ui.dashboard

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToSettings: () -> Unit,
    onQuickAddFood: (String) -> Unit,
    onNavigateToHabits: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.userProfile.collectAsState()
    val date by viewModel.selectedDate.collectAsState()
    val foodEntries by viewModel.foodEntries.collectAsState()
    val exerciseEntries by viewModel.exerciseEntries.collectAsState()
    val waterLog by viewModel.waterLog.collectAsState()

    val scrollState = rememberScrollState()
    val view = androidx.compose.ui.platform.LocalView.current

    val activityCalorieLog by viewModel.activityCalorieLog.collectAsState()
    val additionalActivityCalories = activityCalorieLog?.additionalCalories ?: 0

    val weightLogs by viewModel.weightLogs.collectAsState()
    val progressPhotos by viewModel.progressPhotos.collectAsState()

    var weightInputText by remember { mutableStateOf("") }
    val loggedWeightForSelectedDate = remember(weightLogs, date) {
        weightLogs.find { it.date == date }
    }
    LaunchedEffect(loggedWeightForSelectedDate) {
        weightInputText = if (loggedWeightForSelectedDate != null) {
            loggedWeightForSelectedDate.weightKg.toString()
        } else {
            ""
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val file = java.io.File(context.filesDir, "progress_${date}_${System.currentTimeMillis()}.jpg")
                    file.outputStream().use { output ->
                        inputStream?.copyTo(output)
                    }
                    viewModel.addProgressPhoto(date, file.absolutePath, "")
                } catch (e: Exception) {
                    android.util.Log.e("DashboardScreen", "Error saving progress photo", e)
                }
            }
        }
    )

    // Timelapse player engine
    var isTimelapsePlaying by remember { mutableStateOf(false) }
    var currentTimelapseIndex by remember { mutableStateOf(0) }
    var timelapseSpeedMs by remember { mutableStateOf(400) } // speed in ms per frame
    var selectedPhotoToView by remember { mutableStateOf<com.example.data.local.entity.ProgressPhoto?>(null) }
    var showBodyProgressMode by remember { mutableStateOf(false) }

    LaunchedEffect(isTimelapsePlaying, progressPhotos) {
        if (isTimelapsePlaying && progressPhotos.isNotEmpty()) {
            while (isTimelapsePlaying) {
                kotlinx.coroutines.delay(timelapseSpeedMs.toLong())
                currentTimelapseIndex = (currentTimelapseIndex + 1) % progressPhotos.size
            }
        }
    }

    var showTdeeDialog by remember { mutableStateOf(false) }
    var showBmrInfoDialog by remember { mutableStateOf(false) }
    var tdeeInputText by remember { mutableStateOf("") }
    var useProgressBarMode by remember { mutableStateOf(true) }

    // Aggregate values
    val totalFoodCalories = foodEntries.sumOf { it.caloriesConsumed.toDouble() }.toInt()
    val totalProtein = foodEntries.sumOf { it.proteinGrams.toDouble() }.toFloat()
    val totalCarbs = foodEntries.sumOf { it.carbsGrams.toDouble() }.toFloat()
    val totalFat = foodEntries.sumOf { it.fatGrams.toDouble() }.toFloat()

    val totalExerciseCalories = exerciseEntries.sumOf { it.caloriesBurned.toDouble() }.toInt()

    val isMaintenanceAvailable = profile != null && profile!!.maintenanceCalories != null && profile!!.maintenanceCalories!! > 0

    val calorieGoal = if (profile != null) {
        val baseCalories = if (isMaintenanceAvailable) {
            profile!!.maintenanceCalories!!.toFloat()
        } else {
            profile!!.bmr * 1.375f // Fallback: estimate TDEE from BMR using standard lightly active activity factor
        }
        Calculations.calculateTargetCalorieGoal(
            dailyCalories = baseCalories,
            weightGoalType = profile!!.weightGoalType,
            adjustmentValue = profile!!.calorieAdjustment,
            bmr = profile!!.bmr,
            gender = profile!!.gender
        )
    } else {
        2000
    }

    val macroGoals = if (profile != null) {
        Calculations.calculateMacrosCustom(
            targetCalories = calorieGoal,
            weightKg = profile!!.currentWeightKg,
            proteinMultiplier = profile!!.proteinMultiplier,
            fatMultiplier = profile!!.fatMultiplier
        )
    } else {
        Triple(250, 140, 65) // Reasonable fallback targets
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
            val isDark = com.example.ui.theme.isDarkThemeGlobal ?: isSystemInDarkTheme()
            TopAppBar(
                title = {
                    Text(
                        text = if (showBodyProgressMode) "Body & Progress" else "FitPal Dashboard",
                        fontWeight = FontWeight.Black
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            showBodyProgressMode = !showBodyProgressMode
                            com.example.util.HapticFeedbackHelper.triggerLightTap(view)
                        },
                        modifier = Modifier.testTag("dashboard_mode_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (showBodyProgressMode) Icons.Default.FitnessCenter else Icons.Default.PhotoCamera,
                            contentDescription = "Toggle Dashboard Mode",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = {
                            com.example.ui.theme.isDarkThemeGlobal = !isDark
                        },
                        modifier = Modifier.testTag("dashboard_theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onNavigateToHabits,
                        modifier = Modifier.testTag("dashboard_habits_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Habit Tracker",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
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
                onClick = { onQuickAddFood(date) },
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
            // DATE NAVIGATION BAR (Premium Calendar Strip Layout)
            val isDateToday = date == DateUtils.getTodayDateString()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.setDate(DateUtils.getPreviousDay(date)) },
                        modifier = Modifier.testTag("dashboard_prev_day")
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day")
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = DateUtils.formatDateForDisplay(date).uppercase(),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = { viewModel.setDate(DateUtils.getNextDay(date)) },
                        modifier = Modifier.testTag("dashboard_next_day")
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Day")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                
                // Horizontal Modern Days Strip Selector
                HorizontalWeekStrip(
                    selectedDate = date,
                    onDateSelected = { newDate ->
                        viewModel.setDate(newDate)
                    }
                )
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Segmented Button Row to easily swap views
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                ) {
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        onClick = {
                            showBodyProgressMode = false
                            com.example.util.HapticFeedbackHelper.triggerLightTap(view)
                        },
                        selected = !showBodyProgressMode
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Fitness Logs", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        onClick = {
                            showBodyProgressMode = true
                            com.example.util.HapticFeedbackHelper.triggerLightTap(view)
                        },
                        selected = showBodyProgressMode
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Body Progress", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (!showBodyProgressMode) {
                    // Modern View Mode Selector (Bars vs Rings)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily Analytics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    useProgressBarMode = true
                                    com.example.util.HapticFeedbackHelper.triggerLightTap(view)
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = if (useProgressBarMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("toggle_bars_view")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("Progress Bars", fontWeight = if (useProgressBarMode) FontWeight.ExtraBold else FontWeight.Medium)
                                }
                            }
                            
                            TextButton(
                                onClick = {
                                    useProgressBarMode = false
                                    com.example.util.HapticFeedbackHelper.triggerLightTap(view)
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = if (!useProgressBarMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("toggle_rings_view")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("Circular Rings", fontWeight = if (!useProgressBarMode) FontWeight.ExtraBold else FontWeight.Medium)
                                }
                            }
                        }
                    }

                    if (useProgressBarMode) {
                        // VISUAL PROGRESS BAR COMPONENT (Default & Highly aesthetic!)
                        CaloricAndMacroProgressBarCard(
                            calorieConsumed = totalFoodCalories,
                            calorieGoal = calorieGoal,
                            calorieBurned = totalExerciseCalories,
                            proteinConsumed = totalProtein,
                            proteinGoal = proteinGoal,
                            carbsConsumed = totalCarbs,
                            carbsGoal = carbsGoal,
                            fatConsumed = totalFat,
                            fatGoal = fatGoal,
                            modifier = Modifier.testTag("dashboard_progress_bar_card")
                        )

                        // HEALTH SAFETY LIMIT WARNING BANNER
                        if (showWarning) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("warning_banner_bars")
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

                        // Prompt to customize TDEE if not set manually
                        if (!isMaintenanceAvailable) {
                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("estimated_tdee_prompt_bars"),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Using estimated TDEE",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "You are currently tracking against physiological estimates. Set your own custom TDEE for exact custom targets.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            tdeeInputText = ""
                                            showTdeeDialog = true
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("set_estimated_tdee_button")
                                    ) {
                                        Text("Set", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        // FALLBACK / DETAILED CIRCULAR RINGS VIEW (Rings Mode)
                        // MACRONUTRIENT TARGETS (Moved to the Top & styled with a share/download button)
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
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Macronutrient Breakdown",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            
                            val context = androidx.compose.ui.platform.LocalContext.current
                            val isDark = com.example.ui.theme.isDarkThemeGlobal ?: isSystemInDarkTheme()
                            IconButton(
                                onClick = {
                                    com.example.util.ShareHelper.shareDailyNutritionImage(
                                        context = context,
                                        dateStr = DateUtils.formatDateForDisplay(date),
                                        isDarkTheme = isDark,
                                        totalFoodCalories = totalFoodCalories,
                                        totalExerciseCalories = totalExerciseCalories,
                                        calorieGoal = calorieGoal,
                                        proteinGrams = totalProtein,
                                        proteinGoal = proteinGoal,
                                        carbsGrams = totalCarbs,
                                        carbsGoal = carbsGoal,
                                        fatGrams = totalFat,
                                        fatGoal = fatGoal
                                    )
                                },
                                modifier = Modifier.testTag("dashboard_share_macros_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share Report as Image",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MacroCircularIndicator(
                                label = "Protein",
                                currentGrams = totalProtein,
                                goalGrams = proteinGoal,
                                color = MacroProteinColor,
                                modifier = Modifier.weight(1f).testTag("dashboard_protein_ring")
                            )

                            MacroCircularIndicator(
                                label = "Carbs",
                                currentGrams = totalCarbs,
                                goalGrams = carbsGoal,
                                color = MacroCarbsColor,
                                modifier = Modifier.weight(1f).testTag("dashboard_carbs_ring")
                            )

                            MacroCircularIndicator(
                                label = "Fat",
                                currentGrams = totalFat,
                                goalGrams = fatGoal,
                                color = MacroFatColor,
                                modifier = Modifier.weight(1f).testTag("dashboard_fat_ring")
                            )
                        }
                    }
                }
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

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Your BMR: $bmrVal kcal",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { showBmrInfoDialog = true },
                                        modifier = Modifier.size(24.dp).testTag("bmr_info_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Read BMR Info",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "Set TDEE (Maintenance Calories) to activate daily budget tracker.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
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

                } // End of !showBodyProgressMode

                if (showBodyProgressMode) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // DAILY BODY WEIGHT LOG CARD
                    Card(
                    modifier = Modifier.fillMaxWidth().testTag("weight_log_card"),
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Daily Body Weight",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Log weight for date: ${DateUtils.formatDateForDisplay(date)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        if (loggedWeightForSelectedDate != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Logged weight on this day:",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "${loggedWeightForSelectedDate.weightKg} kg",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Text(
                                        text = "Saved",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "No weight logged for this day yet. Keep your weight logs daily to trace progress over time.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val view = androidx.compose.ui.platform.LocalView.current
                            OutlinedTextField(
                                value = weightInputText,
                                onValueChange = { weightInputText = it },
                                label = { Text("Weight (kg)") },
                                placeholder = { Text("e.g. 72.5") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("dashboard_weight_input")
                            )

                            Button(
                                onClick = {
                                    val weight = weightInputText.toFloatOrNull()
                                    if (weight != null && weight > 20f) {
                                        viewModel.logWeight(weight)
                                        com.example.util.HapticFeedbackHelper.triggerConfirm(view)
                                    }
                                },
                                enabled = weightInputText.isNotBlank(),
                                modifier = Modifier
                                    .height(56.dp)
                                    .testTag("dashboard_weight_save_button")
                            ) {
                                Text(if (loggedWeightForSelectedDate != null) "Update" else "Log", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // DAILY PROGRESS PHOTOS & TIMELAPSE CARD
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("progress_photos_card"),
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Transformation Gallery & Timelapse",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Auto-aligned square progress tracker",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        val view = androidx.compose.ui.platform.LocalView.current

                        // Button to upload photo
                        val photoForSelectedDate = remember(progressPhotos, date) {
                            progressPhotos.find { it.date == date }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    com.example.util.HapticFeedbackHelper.triggerLightTap(view)
                                    imagePickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.testTag("upload_progress_photo_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Upload Photo for ${DateUtils.formatDateForDisplayShort(date)}")
                            }

                            if (photoForSelectedDate != null) {
                                TextButton(
                                    onClick = {
                                        viewModel.deleteProgressPhoto(photoForSelectedDate)
                                        com.example.util.HapticFeedbackHelper.triggerRejectOrDelete(view)
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.testTag("delete_selected_date_photo")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Remove")
                                }
                            }
                        }

                        if (progressPhotos.isEmpty()) {
                            // Placeholder
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Text(
                                        text = "No progress photos uploaded yet.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Upload photos to trace your body transformation and watch your timelapse progression!",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            // Primary Viewer (Timelapse or Selected Photo)
                            val displayPhoto = if (isTimelapsePlaying) {
                                progressPhotos.getOrNull(currentTimelapseIndex)
                            } else {
                                selectedPhotoToView ?: photoForSelectedDate ?: progressPhotos.lastOrNull()
                            }

                            if (displayPhoto != null) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Main aligned image view
                                    Box(
                                        modifier = Modifier
                                            .size(240.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color.Black)
                                    ) {
                                        AsyncImage(
                                            model = displayPhoto.imagePath,
                                            contentDescription = "Transformation frame",
                                            contentScale = ContentScale.Crop, // Auto center crops to guarantee visual alignment
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        // Overlay Grid to help user align camera on upload, or date label
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .fillMaxWidth()
                                                .background(Color.Black.copy(alpha = 0.6f))
                                                .padding(6.dp)
                                        ) {
                                            Text(
                                                text = DateUtils.formatDateForDisplay(displayPhoto.date),
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }

                                    // Controls Panel
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = {
                                                isTimelapsePlaying = !isTimelapsePlaying
                                                com.example.util.HapticFeedbackHelper.triggerConfirm(view)
                                            },
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer)
                                                .testTag("play_timelapse_button")
                                        ) {
                                            Icon(
                                                imageVector = if (isTimelapsePlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = "Play/Pause Timelapse",
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = if (isTimelapsePlaying) "Playing Timelapse" else "Slideshow Viewer",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = if (isTimelapsePlaying) "Frame ${currentTimelapseIndex + 1} / ${progressPhotos.size}" else "Select photo below to view",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Speed Slider for Timelapse
                                    if (isTimelapsePlaying) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Timelapse speed", style = MaterialTheme.typography.labelSmall)
                                                Text("${timelapseSpeedMs}ms", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }
                                            Slider(
                                                value = timelapseSpeedMs.toFloat(),
                                                onValueChange = { timelapseSpeedMs = it.toInt() },
                                                valueRange = 100f..1000f,
                                                modifier = Modifier.fillMaxWidth().height(24.dp).testTag("timelapse_speed_slider")
                                            )
                                        }
                                    }
                                }
                            }

                            // Horizontal list of thumbnails
                            Text(
                                text = "Timeline History Journey:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                items(progressPhotos) { photo ->
                                    val isSelected = displayPhoto?.id == photo.id
                                    Card(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                isTimelapsePlaying = false
                                                selectedPhotoToView = photo
                                                com.example.util.HapticFeedbackHelper.triggerLightTap(view)
                                            },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            AsyncImage(
                                                model = photo.imagePath,
                                                contentDescription = "Thumbnail",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )

                                            // Small overlay showing day of month or date
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomCenter)
                                                    .fillMaxWidth()
                                                    .background(Color.Black.copy(alpha = 0.5f))
                                                    .padding(2.dp)
                                            ) {
                                                Text(
                                                    text = DateUtils.formatDateForDisplayShort(photo.date),
                                                    color = Color.White,
                                                    style = androidx.compose.ui.text.TextStyle(fontSize = 7.sp),
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                } // End of showBodyProgressMode
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

    if (showBmrInfoDialog) {
        AlertDialog(
            onDismissRequest = { showBmrInfoDialog = false },
            icon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Basal Metabolic Rate (BMR)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "BMR represents the minimum energy your body requires to function at complete rest (heart beat, breathing, and temperature maintenance).",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Why we need TDEE (Maintenance Calories):",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "To calculate a proper daily calorie budget, we must adjust from your Total Daily Energy Expenditure (TDEE) which includes physical activity. Deficits/surpluses cannot be applied directly to BMR because BMR does not include activity, and lowering intake directly below BMR is unsafe for long-term health.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showBmrInfoDialog = false }) {
                    Text("Got It", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
