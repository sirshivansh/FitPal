package com.example.fitpal.ui.diary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitpal.data.local.entity.ExerciseEntry
import com.example.fitpal.data.local.entity.FoodEntry
import com.example.fitpal.data.local.entity.WorkoutEntry
import com.example.fitpal.ui.components.EmptyState
import com.example.fitpal.ui.components.AiWorkoutNotebookSheet
import com.example.fitpal.ui.components.UnifiedAiNotebookSheet
import com.example.fitpal.ui.components.DynamicExpenditureCard
import com.example.fitpal.ui.components.GlassCard
import com.example.fitpal.ui.components.MotionProgressBar
import com.example.fitpal.ui.components.GoalCompletionBadge
import com.example.fitpal.ui.theme.*
import com.example.fitpal.util.Calculations
import com.example.fitpal.util.DateUtils
import com.example.fitpal.util.ShareHelper
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(
    viewModel: DiaryViewModel,
    onAddFoodForMeal: (String, String) -> Unit,
    onAddExercise: () -> Unit,
    modifier: Modifier = Modifier
) {
    val date by viewModel.selectedDate.collectAsState()
    val foodEntries by viewModel.foodEntries.collectAsState()
    val exerciseEntries by viewModel.exerciseEntries.collectAsState()
    val workoutEntries by viewModel.workoutEntries.collectAsState()
    val dynamicExpenditure by viewModel.dynamicExpenditure.collectAsState()
    val activityCalorieLog by viewModel.activityCalorieLog.collectAsState()
    val profile by viewModel.userProfile.collectAsState()
    val view = androidx.compose.ui.platform.LocalView.current
    val context = androidx.compose.ui.platform.LocalContext.current

    val totalActiveBurn = dynamicExpenditure.loggedWorkoutCalories + dynamicExpenditure.loggedStepCalories
    val totalFoodCalories = foodEntries.sumOf { it.caloriesConsumed.toDouble() }.toInt()
    val totalExerciseCalories = exerciseEntries.sumOf { it.caloriesBurned.toDouble() }.toInt()

    val totalProteinConsumed = foodEntries.sumOf { it.proteinGrams.toDouble() }.toFloat()
    val totalCarbsConsumed = foodEntries.sumOf { it.carbsGrams.toDouble() }.toFloat()
    val totalFatConsumed = foodEntries.sumOf { it.fatGrams.toDouble() }.toFloat()

    val adjustedCalorieGoal = if (profile != null) {
        if (profile!!.customCalorieGoal != null && profile!!.customCalorieGoal!! > 0) {
            profile!!.customCalorieGoal!!
        } else {
            dynamicExpenditure.dynamicCalorieTarget
        }
    } else {
        dynamicExpenditure.dynamicCalorieTarget
    }

    val calorieGoal = adjustedCalorieGoal

    val macroGoals = if (profile != null) {
        Calculations.calculateMacrosCustom(
            targetCalories = adjustedCalorieGoal,
            weightKg = profile!!.currentWeightKg,
            proteinMultiplier = profile!!.proteinMultiplier,
            fatMultiplier = profile!!.fatMultiplier
        )
    } else {
        val baseCarbs = 250
        val baseProtein = 140
        val baseFat = 65
        val scale = adjustedCalorieGoal.toFloat() / 2000f
        Triple(
            (baseCarbs * scale).toInt(),
            (baseProtein * scale).toInt(),
            (baseFat * scale).toInt()
        )
    }

    val carbsGoal = macroGoals.first
    val proteinGoal = macroGoals.second
    val fatGoal = macroGoals.third

    val snackbarHostState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val snackHost = remember { SnackbarHostState() }

    val scrollState = rememberScrollState()

    var foodEntryToDelete by remember { mutableStateOf<FoodEntry?>(null) }
    var exerciseEntryToDelete by remember { mutableStateOf<ExerciseEntry?>(null) }
    var workoutToDelete by remember { mutableStateOf<WorkoutEntry?>(null) }
    var showWeeklyPdfSheet by remember { mutableStateOf(false) }

    var showAddFoodDialogForMeal by remember { mutableStateOf<String?>(null) }
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showAiWorkoutSheet by remember { mutableStateOf(false) }
    var showUnifiedNotebookSheet by remember { mutableStateOf(false) }

    // AI Notepad States
    var showNotepadDialog by remember { mutableStateOf(false) }
    var notepadContent by remember { mutableStateOf("") }
    var isNotepadAnalyzing by remember { mutableStateOf(false) }
    var notepadResults by remember { mutableStateOf<List<com.example.fitpal.data.api.ParsedNotepadFood>>(emptyList()) }
    var selectedResultsToLog by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var resultMealTypes by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }

    // Filter meals
    val breakfast = foodEntries.filter { it.mealType.lowercase() == "breakfast" }
    val lunch = foodEntries.filter { it.mealType.lowercase() == "lunch" }
    val dinner = foodEntries.filter { it.mealType.lowercase() == "dinner" }
    val snacks = foodEntries.filter { it.mealType.lowercase() == "snacks" }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Fitness Diary", fontWeight = FontWeight.Black) },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TextButton(
                            onClick = {
                                com.example.fitpal.util.HapticFeedbackHelper.triggerLightTap(view)
                                showNotepadDialog = true
                            },
                            modifier = Modifier.testTag("diary_ai_notepad")
                        ) {
                            Icon(Icons.Default.EditNote, contentDescription = "AI Food")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AI Food", style = MaterialTheme.typography.labelMedium)
                        }

                        TextButton(
                            onClick = {
                                com.example.fitpal.util.HapticFeedbackHelper.triggerLightTap(view)
                                showAiWorkoutSheet = true
                            },
                            modifier = Modifier.testTag("diary_ai_workout")
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = "AI Workout")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AI Workout", style = MaterialTheme.typography.labelMedium)
                        }

                        TextButton(
                            onClick = {
                                com.example.fitpal.util.HapticFeedbackHelper.triggerLightTap(view)
                                showUnifiedNotebookSheet = true
                            },
                            modifier = Modifier.testTag("diary_unified_note")
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = "Unified Note")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Unified", style = MaterialTheme.typography.labelMedium)
                        }

                        TextButton(
                            onClick = {
                                com.example.fitpal.util.HapticFeedbackHelper.triggerConfirm(view)
                                viewModel.copyYesterdayMeals()
                                scope.launch {
                                    snackHost.showSnackbar("Copied yesterday's meals to today!")
                                }
                            },
                            modifier = Modifier.testTag("diary_copy_yesterday")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy Yesterday", style = MaterialTheme.typography.labelMedium)
                        }

                        IconButton(
                            onClick = {
                                showWeeklyPdfSheet = true
                                com.example.fitpal.util.HapticFeedbackHelper.triggerLightTap(view)
                            },
                            modifier = Modifier.testTag("diary_export_weekly_pdf_top")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "Export Weekly PDF Report",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = {
                                com.example.fitpal.util.HapticFeedbackHelper.triggerConfirm(view)
                                ShareHelper.shareWhatIAteToday(
                                    context = context,
                                    dateStr = DateUtils.formatDateForDisplay(date),
                                    foodEntries = foodEntries,
                                    totalCalories = totalFoodCalories,
                                    calorieGoal = calorieGoal,
                                    totalProtein = totalProteinConsumed,
                                    proteinGoal = proteinGoal,
                                    totalCarbs = totalCarbsConsumed,
                                    carbsGoal = carbsGoal,
                                    totalFat = totalFatConsumed,
                                    fatGoal = fatGoal
                                )
                            },
                            modifier = Modifier.testTag("diary_share_what_i_ate_top")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share What I Ate", tint = Color(0xFF20AA1D))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        },
        snackbarHost = { SnackbarHost(snackHost) },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .verticalScroll(scrollState)
                    .padding(bottom = 80.dp), // Bottom padding
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
                    modifier = Modifier.testTag("diary_prev_day")
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
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("diary_date_display")
                    )
                }

                IconButton(
                    onClick = { viewModel.setDate(DateUtils.getNextDay(date)) },
                    modifier = Modifier.testTag("diary_next_day")
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Day")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // NET SUMMARY CARD
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Food Intake", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
                            Text("$totalFoodCalories", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color(0xFFF5F5F7))
                            Text("kcal", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
                        }
                        
                        Box(modifier = Modifier.height(40.dp).width(1.dp).background(Color.White.copy(alpha = 0.08f)))
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Active Burn", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
                            Text("-$totalActiveBurn", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color(0xFF20AA1D))
                            Text("kcal", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
                        }
                        
                        Box(modifier = Modifier.height(40.dp).width(1.dp).background(Color.White.copy(alpha = 0.08f)))
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Net Balance", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
                            val net = totalFoodCalories - totalActiveBurn
                            Text("$net", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color(0xFF20AA1D))
                            Text("kcal", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Premium linear budget indicator with subtle motion and completion state
                    val progressFraction = if (adjustedCalorieGoal > 0) {
                        totalFoodCalories.toFloat() / adjustedCalorieGoal.toFloat()
                    } else 0f
                    val isBudgetMet = progressFraction in 0.90f..1.05f
                    val isOverBudget = progressFraction > 1.05f

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                    text = "Daily Budget: $adjustedCalorieGoal kcal",
                                    style = MetricNumeralTiny,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (isBudgetMet) {
                                    GoalCompletionBadge(
                                        text = "ON TARGET 🎯",
                                        color = MaterialTheme.colorScheme.primary,
                                        isCompleted = true
                                    )
                                }
                            }
                            val pct = (progressFraction * 100).roundToInt()
                            Text(
                                text = "$pct% Eaten",
                                style = MetricNumeralTiny.copy(fontWeight = FontWeight.Bold),
                                color = if (isOverBudget) MacroFatColor else MaterialTheme.colorScheme.primary
                            )
                        }

                        MotionProgressBar(
                            progress = progressFraction,
                            color = if (isOverBudget) MacroFatColor else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            height = 8.dp,
                            isGoalCompleted = isBudgetMet,
                            completionColor = MaterialTheme.colorScheme.primary,
                            showShimmer = true,
                            testTag = "diary_budget_motion_progress_bar"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // DYNAMIC EXPENDITURE ENGINE CARD
            DynamicExpenditureCard(
                result = dynamicExpenditure,
                consumedCalories = totalFoodCalories,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // SHARE WHAT I ATE TODAY CARD
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("share_what_i_ate_card"),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Share What I Ate Today",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF5F5F7)
                        )
                        Text(
                            text = "${foodEntries.size} items • $totalFoodCalories kcal • Copied to clipboard & share sheet",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            com.example.fitpal.util.HapticFeedbackHelper.triggerConfirm(view)
                            ShareHelper.shareWhatIAteToday(
                                context = context,
                                dateStr = DateUtils.formatDateForDisplay(date),
                                foodEntries = foodEntries,
                                totalCalories = totalFoodCalories,
                                calorieGoal = calorieGoal,
                                totalProtein = totalProteinConsumed,
                                proteinGoal = proteinGoal,
                                totalCarbs = totalCarbsConsumed,
                                carbsGoal = carbsGoal,
                                totalFat = totalFatConsumed,
                                fatGoal = fatGoal
                            )
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF20AA1D),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("share_what_i_ate_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // MEAL SECTIONS
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MealSectionView(
                    sectionName = "Breakfast",
                    entries = breakfast,
                    onAdd = { showAddFoodDialogForMeal = "Breakfast" },
                    onDelete = { entry -> foodEntryToDelete = entry },
                    modifier = Modifier.testTag("section_breakfast")
                )

                MealSectionView(
                    sectionName = "Lunch",
                    entries = lunch,
                    onAdd = { showAddFoodDialogForMeal = "Lunch" },
                    onDelete = { entry -> foodEntryToDelete = entry },
                    modifier = Modifier.testTag("section_lunch")
                )

                MealSectionView(
                    sectionName = "Dinner",
                    entries = dinner,
                    onAdd = { showAddFoodDialogForMeal = "Dinner" },
                    onDelete = { entry -> foodEntryToDelete = entry },
                    modifier = Modifier.testTag("section_dinner")
                )

                MealSectionView(
                    sectionName = "Snacks",
                    entries = snacks,
                    onAdd = { showAddFoodDialogForMeal = "Snacks" },
                    onDelete = { entry -> foodEntryToDelete = entry },
                    modifier = Modifier.testTag("section_snacks")
                )

                // EXERCISE & WORKOUT SECTION
                ExerciseSectionView(
                    entries = exerciseEntries,
                    workouts = workoutEntries,
                    onAdd = { showAddExerciseDialog = true },
                    onAddWorkout = { showAiWorkoutSheet = true },
                    onDelete = { entry -> exerciseEntryToDelete = entry },
                    onDeleteWorkout = { workout -> workoutToDelete = workout },
                    modifier = Modifier.testTag("section_exercises")
                )
            }

            if (foodEntries.isEmpty() && exerciseEntries.isEmpty()) {
                EmptyState(
                    title = "Diary is empty today",
                    description = "Start logging your foods or exercises to map out your calorie trends offline.",
                    icon = Icons.Default.MenuBook,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }

            if (foodEntryToDelete != null) {
                val entry = foodEntryToDelete!!
                AlertDialog(
                    onDismissRequest = { foodEntryToDelete = null },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete Meal Entry?")
                        }
                    },
                    text = {
                        Text("Are you sure you want to remove '${entry.foodName}' from your ${entry.mealType} log? This action cannot be undone.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val toDelete = entry
                                foodEntryToDelete = null
                                viewModel.deleteFoodEntry(toDelete)
                                scope.launch {
                                    val result = snackHost.showSnackbar(
                                        message = "Deleted ${toDelete.foodName}",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.undoDeleteFoodEntry()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("confirm_delete_food_btn")
                        ) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { foodEntryToDelete = null },
                            modifier = Modifier.testTag("cancel_delete_food_btn")
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (exerciseEntryToDelete != null) {
                val entry = exerciseEntryToDelete!!
                AlertDialog(
                    onDismissRequest = { exerciseEntryToDelete = null },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete Exercise Entry?")
                        }
                    },
                    text = {
                        Text("Are you sure you want to remove '${entry.exerciseName}' from your exercise log? This action cannot be undone.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val toDelete = entry
                                exerciseEntryToDelete = null
                                viewModel.deleteExerciseEntry(toDelete)
                                scope.launch {
                                    val result = snackHost.showSnackbar(
                                        message = "Deleted ${toDelete.exerciseName}",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.undoDeleteExerciseEntry()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("confirm_delete_exercise_btn")
                        ) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { exerciseEntryToDelete = null },
                            modifier = Modifier.testTag("cancel_delete_exercise_btn")
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (showAddFoodDialogForMeal != null) {
                val mealType = showAddFoodDialogForMeal!!
                val view = androidx.compose.ui.platform.LocalView.current
                
                var foodQuery by remember { mutableStateOf("") }
                var isSearching by remember { mutableStateOf(false) }
                var searchResults by remember { mutableStateOf<List<com.example.fitpal.data.api.FoodSearchResult>>(emptyList()) }
                
                var foodNameInput by remember { mutableStateOf("") }
                var caloriesInput by remember { mutableStateOf("") }
                var carbsInput by remember { mutableStateOf("") }
                var proteinInput by remember { mutableStateOf("") }
                var fatInput by remember { mutableStateOf("") }
                var weightInput by remember { mutableStateOf("100") }
                
                var baseCalories by remember { mutableStateOf(0.0) }
                var baseCarbs by remember { mutableStateOf(0.0) }
                var baseProtein by remember { mutableStateOf(0.0) }
                var baseFat by remember { mutableStateOf(0.0) }
                
                AlertDialog(
                    onDismissRequest = { showAddFoodDialogForMeal = null },
                    title = { Text("Log $mealType", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Search online via Gemini AI, or enter nutrition facts manually for offline quick-logging.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = foodQuery,
                                    onValueChange = { foodQuery = it },
                                    placeholder = { Text("Search (e.g. Avocado)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).testTag("food_search_input")
                                )
                                Button(
                                    onClick = {
                                        if (foodQuery.isNotBlank()) {
                                            isSearching = true
                                            scope.launch {
                                                try {
                                                    val results = com.example.fitpal.data.api.GeminiFoodService.searchFood(foodQuery)
                                                    searchResults = results
                                                    isSearching = false
                                                    if (results.isEmpty()) {
                                                        com.example.fitpal.util.HapticFeedbackHelper.triggerLightTap(view)
                                                    } else {
                                                        com.example.fitpal.util.HapticFeedbackHelper.triggerConfirm(view)
                                                    }
                                                } catch (e: Exception) {
                                                    isSearching = false
                                                    com.example.fitpal.util.HapticFeedbackHelper.triggerLightTap(view)
                                                    snackHost.showSnackbar("Search failed: ${e.localizedMessage ?: e.message}")
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isSearching,
                                    modifier = Modifier.testTag("food_search_btn")
                                ) {
                                    if (isSearching) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            
                            if (searchResults.isNotEmpty()) {
                                Text(
                                    text = "Matching Foods (Tap to autofill):",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(4.dp)
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        searchResults.forEach { result ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        com.example.fitpal.util.HapticFeedbackHelper.triggerLightTap(view)
                                                        foodNameInput = result.foodName
                                                        baseCalories = result.calories
                                                        baseCarbs = result.carbs
                                                        baseProtein = result.protein
                                                        baseFat = result.fat
                                                        
                                                        val grams = weightInput.toDoubleOrNull() ?: 100.0
                                                        val scale = grams / 100.0
                                                        caloriesInput = (result.calories * scale).roundToInt().toString()
                                                        carbsInput = if (result.carbs > 0) String.format("%.1f", result.carbs * scale) else "0"
                                                        proteinInput = if (result.protein > 0) String.format("%.1f", result.protein * scale) else "0"
                                                        fatInput = if (result.fat > 0) String.format("%.1f", result.fat * scale) else "0"
                                                    }
                                                    .padding(8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(result.foodName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text("per 100g: C: ${result.carbs}g | P: ${result.protein}g | F: ${result.fat}g", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                                                    Text("${result.calories.roundToInt()} kcal", color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(2.dp))
                                                }
                                            }
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                        }
                                    }
                                }
                            } else if (foodQuery.isNotBlank() && !isSearching && searchResults.isEmpty()) {
                                Text(
                                    text = "No online search results. Fill fields below to quick-log manually.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            
                            Text("Nutrition Details:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            
                            OutlinedTextField(
                                value = foodNameInput,
                                onValueChange = { foodNameInput = it },
                                label = { Text("Food Name *") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("food_name_input_field")
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = caloriesInput,
                                    onValueChange = { newVal ->
                                        if (newVal.all { it.isDigit() }) {
                                            caloriesInput = newVal
                                            baseCalories = (newVal.toDoubleOrNull() ?: 0.0) / ((weightInput.toDoubleOrNull() ?: 100.0) / 100.0)
                                        }
                                    },
                                    label = { Text("Calories * (kcal)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).testTag("food_calories_input_field")
                                )
                                OutlinedTextField(
                                    value = weightInput,
                                    onValueChange = { newVal ->
                                        if (newVal.all { it.isDigit() }) {
                                            weightInput = newVal
                                            val grams = newVal.toDoubleOrNull() ?: 100.0
                                            val scale = grams / 100.0
                                            
                                            if (baseCalories > 0) caloriesInput = (baseCalories * scale).roundToInt().toString()
                                            if (baseCarbs > 0) carbsInput = String.format("%.1f", baseCarbs * scale)
                                            if (baseProtein > 0) proteinInput = String.format("%.1f", baseProtein * scale)
                                            if (baseFat > 0) fatInput = String.format("%.1f", baseFat * scale)
                                        }
                                    },
                                    label = { Text("Grams Consumed") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).testTag("food_weight_input_field")
                                )
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = carbsInput,
                                    onValueChange = { newVal ->
                                        carbsInput = newVal
                                        baseCarbs = (newVal.toDoubleOrNull() ?: 0.0) / ((weightInput.toDoubleOrNull() ?: 100.0) / 100.0)
                                    },
                                    label = { Text("Carbs (g)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).testTag("food_carbs_input_field")
                                )
                                OutlinedTextField(
                                    value = proteinInput,
                                    onValueChange = { newVal ->
                                        proteinInput = newVal
                                        baseProtein = (newVal.toDoubleOrNull() ?: 0.0) / ((weightInput.toDoubleOrNull() ?: 100.0) / 100.0)
                                    },
                                    label = { Text("Protein (g)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).testTag("food_protein_input_field")
                                )
                                OutlinedTextField(
                                    value = fatInput,
                                    onValueChange = { newVal ->
                                        fatInput = newVal
                                        baseFat = (newVal.toDoubleOrNull() ?: 0.0) / ((weightInput.toDoubleOrNull() ?: 100.0) / 100.0)
                                    },
                                    label = { Text("Fat (g)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).testTag("food_fat_input_field")
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val calories = caloriesInput.toDoubleOrNull()
                                val grams = weightInput.toDoubleOrNull() ?: 100.0
                                if (foodNameInput.isNotBlank() && calories != null) {
                                    val carbs = carbsInput.toDoubleOrNull() ?: 0.0
                                    val protein = proteinInput.toDoubleOrNull() ?: 0.0
                                    val fat = fatInput.toDoubleOrNull() ?: 0.0
                                    
                                    scope.launch {
                                        viewModel.foodRepository.insert(
                                            com.example.fitpal.data.local.entity.FoodEntry(
                                                foodName = foodNameInput,
                                                caloriesConsumed = calories,
                                                proteinGrams = protein,
                                                carbsGrams = carbs,
                                                fatGrams = fat,
                                                gramsConsumed = grams,
                                                mealType = mealType,
                                                date = date
                                            )
                                        )
                                        showAddFoodDialogForMeal = null
                                        com.example.fitpal.util.HapticFeedbackHelper.triggerConfirm(view)
                                    }
                                }
                            },
                            enabled = foodNameInput.isNotBlank() && caloriesInput.isNotBlank(),
                            modifier = Modifier.testTag("submit_food_log_btn")
                        ) {
                            Text("Log Food")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showAddFoodDialogForMeal = null },
                            modifier = Modifier.testTag("dismiss_food_log_btn")
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (showAddExerciseDialog) {
                val view = androidx.compose.ui.platform.LocalView.current
                
                var exerciseNameInput by remember { mutableStateOf("") }
                var durationInput by remember { mutableStateOf("") }
                var intensityInput by remember { mutableStateOf("Medium") }
                var caloriesInput by remember { mutableStateOf("") }
                var autoEstimateMode by remember { mutableStateOf(true) }
                
                val weightKg = profile?.currentWeightKg ?: 70f
                
                LaunchedEffect(durationInput, intensityInput, autoEstimateMode) {
                    if (autoEstimateMode) {
                        val duration = durationInput.toDoubleOrNull() ?: 0.0
                        if (duration > 0) {
                            val met = when (intensityInput.lowercase()) {
                                "low" -> 4.0
                                "high" -> 10.0
                                else -> 7.0
                            }
                            val burn = duration * met * 3.5 * weightKg / 200.0
                            caloriesInput = burn.roundToInt().toString()
                        } else {
                            caloriesInput = ""
                        }
                    }
                }
                
                AlertDialog(
                    onDismissRequest = { showAddExerciseDialog = false },
                    title = { Text("Log Workout", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Estimate and track active energy burn automatically based on duration and intensity.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            OutlinedTextField(
                                value = exerciseNameInput,
                                onValueChange = { exerciseNameInput = it },
                                label = { Text("Workout / Activity Name *") },
                                placeholder = { Text("e.g. Running, Yoga, Swimming") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("exercise_name_input_field")
                            )
                            
                            OutlinedTextField(
                                value = durationInput,
                                onValueChange = { newVal ->
                                    if (newVal.all { it.isDigit() }) {
                                        durationInput = newVal
                                    }
                                },
                                label = { Text("Duration * (minutes)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("exercise_duration_input_field")
                            )
                            
                            Text("Intensity Level:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Low", "Medium", "High").forEach { lvl ->
                                    val isSelected = intensityInput == lvl
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { 
                                            com.example.fitpal.util.HapticFeedbackHelper.triggerLightTap(view)
                                            intensityInput = lvl 
                                        },
                                        label = { Text(lvl) },
                                        modifier = Modifier.weight(1f).testTag("exercise_intensity_chip_$lvl")
                                    )
                                }
                            }
                            
                            OutlinedTextField(
                                value = caloriesInput,
                                onValueChange = { newVal ->
                                    if (newVal.all { it.isDigit() }) {
                                        caloriesInput = newVal
                                        autoEstimateMode = false
                                    }
                                },
                                label = { Text("Calories Burned (kcal)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                trailingIcon = {
                                    if (!autoEstimateMode) {
                                        TextButton(
                                            onClick = { 
                                                autoEstimateMode = true 
                                                com.example.fitpal.util.HapticFeedbackHelper.triggerLightTap(view)
                                            }
                                        ) {
                                            Text("Reset")
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("exercise_calories_input_field")
                            )
                            
                            if (autoEstimateMode && durationInput.isNotBlank()) {
                                Text(
                                    text = "⚡ Estimated from profile weight (${weightKg} kg)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val duration = durationInput.toDoubleOrNull()
                                val calories = caloriesInput.toDoubleOrNull()
                                if (exerciseNameInput.isNotBlank() && duration != null && calories != null) {
                                    scope.launch {
                                        viewModel.exerciseRepository.insert(
                                            com.example.fitpal.data.local.entity.ExerciseEntry(
                                                exerciseName = exerciseNameInput,
                                                durationMinutes = duration,
                                                intensity = intensityInput,
                                                caloriesBurned = calories,
                                                date = date
                                            )
                                        )
                                        showAddExerciseDialog = false
                                        com.example.fitpal.util.HapticFeedbackHelper.triggerConfirm(view)
                                    }
                                }
                            },
                            enabled = exerciseNameInput.isNotBlank() && durationInput.isNotBlank() && caloriesInput.isNotBlank(),
                            modifier = Modifier.testTag("submit_exercise_log_btn")
                        ) {
                            Text("Log Workout")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showAddExerciseDialog = false },
                            modifier = Modifier.testTag("dismiss_exercise_log_btn")
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // AI FOOD NOTEPAD DIALOG
            if (showNotepadDialog) {
                AlertDialog(
                    onDismissRequest = { showNotepadDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "AI Food Notepad",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
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
                                "Type or paste food weights and names in any format (bullets, lists, or normal notepad style). Gemini will analyze it, calculate calories & macros, and prompt you to confirm before logging.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // FORMATTING HELPERS
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Format:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Button(
                                    onClick = {
                                        val count = notepadContent.split("\n").filter { it.trim().matches(Regex("^\\d+\\..*")) }.size + 1
                                        notepadContent += (if (notepadContent.isEmpty() || notepadContent.endsWith("\n")) "" else "\n") + "$count. "
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                                    modifier = Modifier.height(28.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("1. Numbered", style = MaterialTheme.typography.labelSmall)
                                }

                                Button(
                                    onClick = {
                                        notepadContent += (if (notepadContent.isEmpty() || notepadContent.endsWith("\n")) "" else "\n") + "• "
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                                    modifier = Modifier.height(28.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("• Bulleted", style = MaterialTheme.typography.labelSmall)
                                }

                                TextButton(
                                    onClick = { notepadContent = "" },
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Clear", style = MaterialTheme.typography.labelSmall, color = Color.Red)
                                }
                            }

                            // TEXTFIELD REPRESENTING NOTEPAD
                            OutlinedTextField(
                                value = notepadContent,
                                onValueChange = { notepadContent = it },
                                placeholder = {
                                    Text(
                                        "Example:\n1. 200g Greek yogurt\n• 1 large banana\n3. 40g raw almonds",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .testTag("notepad_text_input"),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )

                            // ANALYZING STATE OR RESULTS
                            if (isNotepadAnalyzing) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Gemini AI analyzing weights and macros...", style = MaterialTheme.typography.bodyMedium)
                                }
                            } else if (notepadResults.isNotEmpty()) {
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                                Text(
                                    "Extracted Macro Results (${notepadResults.size})",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Confirm weights and select a meal category to log each item:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 240.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    notepadResults.forEachIndexed { index, item ->
                                        val isChecked = selectedResultsToLog.contains(index)
                                        val currentMeal = resultMealTypes[index] ?: "Breakfast"

                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            border = BorderStroke(
                                                width = 1.dp,
                                                color = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                            ),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = isChecked,
                                                    onCheckedChange = { checked ->
                                                        selectedResultsToLog = if (checked) {
                                                            selectedResultsToLog + index
                                                        } else {
                                                            selectedResultsToLog - index
                                                        }
                                                    },
                                                    modifier = Modifier.testTag("notepad_check_$index")
                                                )

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        item.foodName,
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                    Text(
                                                        "${item.gramsConsumed.roundToInt()}g  •  ${item.calories.roundToInt()} kcal",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Text(
                                                        "P: ${item.protein.roundToInt()}g | C: ${item.carbs.roundToInt()}g | F: ${item.fat.roundToInt()}g",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                // MEAL CATEGORY SELECTOR DROPDOWN
                                                var dropdownExpanded by remember { mutableStateOf(false) }
                                                Box {
                                                    TextButton(
                                                        onClick = { dropdownExpanded = true },
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(32.dp)
                                                    ) {
                                                        Text(
                                                            currentMeal,
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    }
                                                    DropdownMenu(
                                                        expanded = dropdownExpanded,
                                                        onDismissRequest = { dropdownExpanded = false }
                                                    ) {
                                                        listOf("Breakfast", "Lunch", "Dinner", "Snacks").forEach { meal ->
                                                            DropdownMenuItem(
                                                                text = { Text(meal, style = MaterialTheme.typography.bodyMedium) },
                                                                onClick = {
                                                                    resultMealTypes = resultMealTypes + (index to meal)
                                                                    dropdownExpanded = false
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        if (notepadResults.isEmpty()) {
                            Button(
                                onClick = {
                                    val text = notepadContent.trim()
                                    if (text.isNotBlank()) {
                                        com.example.fitpal.util.HapticFeedbackHelper.triggerLightTap(view)
                                        isNotepadAnalyzing = true
                                        scope.launch {
                                            try {
                                                val parsed = com.example.fitpal.data.api.GeminiFoodService.parseNotepadEntries(text)
                                                isNotepadAnalyzing = false
                                                if (parsed.isNotEmpty()) {
                                                    notepadResults = parsed
                                                    selectedResultsToLog = parsed.indices.toSet()
                                                    
                                                    // Prepopulate smart default meal types based on current hour
                                                    val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                                                    val defaultMeal = when {
                                                        currentHour < 11 -> "Breakfast"
                                                        currentHour < 15 -> "Lunch"
                                                        currentHour < 18 -> "Snacks"
                                                        else -> "Dinner"
                                                    }
                                                    resultMealTypes = parsed.indices.associateWith { defaultMeal }
                                                } else {
                                                    snackHost.showSnackbar("Failed to analyze text. Please ensure your API key is active or simplify the entry.")
                                                }
                                            } catch (e: Exception) {
                                                isNotepadAnalyzing = false
                                                com.example.fitpal.util.HapticFeedbackHelper.triggerLightTap(view)
                                                snackHost.showSnackbar("Analysis failed: ${e.localizedMessage ?: e.message}")
                                            }
                                        }
                                    }
                                },
                                enabled = notepadContent.isNotBlank() && !isNotepadAnalyzing,
                                modifier = Modifier.testTag("notepad_analyze_btn")
                            ) {
                                Text("Analyze with Gemini")
                            }
                        } else {
                            Button(
                                onClick = {
                                    com.example.fitpal.util.HapticFeedbackHelper.triggerConfirm(view)
                                    scope.launch {
                                        var loggedCount = 0
                                        notepadResults.forEachIndexed { index, item ->
                                            if (selectedResultsToLog.contains(index)) {
                                                val mealType = resultMealTypes[index] ?: "Breakfast"
                                                viewModel.foodRepository.insert(
                                                    com.example.fitpal.data.local.entity.FoodEntry(
                                                        foodName = item.foodName,
                                                        caloriesConsumed = item.calories,
                                                        proteinGrams = item.protein,
                                                        carbsGrams = item.carbs,
                                                        fatGrams = item.fat,
                                                        gramsConsumed = item.gramsConsumed,
                                                        mealType = mealType,
                                                        date = date
                                                    )
                                                )
                                                loggedCount++
                                            }
                                        }
                                        if (loggedCount > 0) {
                                            snackHost.showSnackbar("Successfully logged $loggedCount foods!")
                                            showNotepadDialog = false
                                            notepadContent = ""
                                            notepadResults = emptyList()
                                            selectedResultsToLog = emptySet()
                                        }
                                    }
                                },
                                enabled = selectedResultsToLog.isNotEmpty(),
                                modifier = Modifier.testTag("notepad_log_all_btn")
                            ) {
                                Text("Add Selected (${selectedResultsToLog.size})")
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                if (notepadResults.isNotEmpty()) {
                                    // Let them reset back to edit mode
                                    notepadResults = emptyList()
                                    selectedResultsToLog = emptySet()
                                } else {
                                    showNotepadDialog = false
                                }
                            },
                            modifier = Modifier.testTag("notepad_cancel_btn")
                        ) {
                            Text(if (notepadResults.isNotEmpty()) "Edit Text" else "Close")
                        }
                    }
                )
            }
            } // End of Column
        } // End of Box
    } // End of Scaffold

    if (workoutToDelete != null) {
        val toDelete = workoutToDelete!!
        AlertDialog(
            onDismissRequest = { workoutToDelete = null },
            title = { Text("Delete Workout", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to remove '${toDelete.title}' from your workout log?") },
            confirmButton = {
                Button(
                    onClick = {
                        workoutToDelete = null
                        viewModel.deleteWorkout(toDelete)
                        com.example.fitpal.util.HapticFeedbackHelper.triggerRejectOrDelete(view)
                        scope.launch {
                            val result = snackHost.showSnackbar(
                                message = "Deleted ${toDelete.title}",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.undoDeleteWorkout()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { workoutToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAiWorkoutSheet) {
        AiWorkoutNotebookSheet(
            userWeightKg = profile?.currentWeightKg ?: 70f,
            targetDate = date,
            onDismiss = { showAiWorkoutSheet = false },
            onSaveWorkouts = { workouts ->
                workouts.forEach { viewModel.insertWorkout(it) }
            },
            onSaveSteps = { deltaSteps ->
                viewModel.addSteps(deltaSteps)
            }
        )
    }

    if (showUnifiedNotebookSheet) {
        UnifiedAiNotebookSheet(
            userWeightKg = profile?.currentWeightKg ?: 70f,
            targetDate = date,
            onDismiss = { showUnifiedNotebookSheet = false },
            onSaveAll = { foods, workouts ->
                foods.forEach { viewModel.insertFood(it) }
                workouts.forEach { viewModel.insertWorkout(it) }
            },
            onSaveSteps = { deltaSteps ->
                viewModel.addSteps(deltaSteps)
            }
        )
    }

    if (showWeeklyPdfSheet) {
        val app = context.applicationContext as com.example.fitpal.FitPalApplication
        com.example.fitpal.ui.components.WeeklyPdfExportSheet(
            isOpen = showWeeklyPdfSheet,
            onDismiss = { showWeeklyPdfSheet = false },
            profileRepo = app.profileRepository,
            foodRepo = app.foodRepository,
            exerciseRepo = app.exerciseRepository,
            selectedDate = date
        )
    }
}

@Composable
fun MealSectionView(
    sectionName: String,
    entries: List<FoodEntry>,
    onAdd: () -> Unit,
    onDelete: (FoodEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }

    val totalCal = entries.sumOf { it.caloriesConsumed.toDouble() }.toInt()
    val totalProtein = entries.sumOf { it.proteinGrams.toDouble() }.toInt()
    val totalCarbs = entries.sumOf { it.carbsGrams.toDouble() }.toInt()
    val totalFat = entries.sumOf { it.fatGrams.toDouble() }.toInt()

    // Determine category icon and color
    val categoryIcon = when (sectionName.lowercase()) {
        "breakfast" -> Icons.Default.Coffee
        "lunch" -> Icons.Default.Restaurant
        "dinner" -> Icons.Default.LocalPizza
        else -> Icons.Default.Cake
    }
    
    val categoryColor = when (sectionName.lowercase()) {
        "breakfast" -> Color(0xFFA29362)
        "lunch" -> Color(0xFF20AA1D)
        "dinner" -> Color(0xFFAE422A)
        else -> Color(0xFF5B4034)
    }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(categoryColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = null,
                            tint = categoryColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = sectionName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF5F5F7)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$totalCal kcal",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF20AA1D)
                    )
                    IconButton(
                        onClick = onAdd,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF20AA1D).copy(alpha = 0.15f))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add $sectionName Log", tint = Color(0xFF20AA1D), modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Expanded list
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                
                // Show macro totals for the section
                if (entries.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Text("Protein: ${totalProtein}g", style = MetricNumeralTiny, color = MacroProteinColor, fontWeight = FontWeight.Bold)
                        Text("Carbs: ${totalCarbs}g", style = MetricNumeralTiny, color = MacroCarbsColor, fontWeight = FontWeight.Bold)
                        Text("Fats: ${totalFat}g", style = MetricNumeralTiny, color = MacroFatColor, fontWeight = FontWeight.Bold)
                    }
                }

                entries.forEach { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.foodName,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // High-scannability macro capsules!
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${entry.gramsConsumed.toInt()}g",
                                    style = MetricNumeralTiny,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                                Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant))
                                Text(
                                    text = "P: ${entry.proteinGrams.toInt()}g",
                                    style = MetricNumeralTiny,
                                    color = MacroProteinColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .border(width = 0.5.dp, color = MacroProteinColor.copy(alpha = 0.4f), shape = RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                                Text(
                                    text = "C: ${entry.carbsGrams.toInt()}g",
                                    style = MetricNumeralTiny,
                                    color = MacroCarbsColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .border(width = 0.5.dp, color = MacroCarbsColor.copy(alpha = 0.4f), shape = RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                                Text(
                                    text = "F: ${entry.fatGrams.toInt()}g",
                                    style = MetricNumeralTiny,
                                    color = MacroFatColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .border(width = 0.5.dp, color = MacroFatColor.copy(alpha = 0.4f), shape = RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${entry.caloriesConsumed.toInt()} kcal",
                                style = MetricNumeralSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val view = androidx.compose.ui.platform.LocalView.current
                            IconButton(
                                onClick = {
                                    com.example.fitpal.util.HapticFeedbackHelper.triggerRejectOrDelete(view)
                                    onDelete(entry)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete item",
                                    tint = Color(0xFFAE422A).copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                if (entries.isEmpty()) {
                    Text(
                        text = "No foods logged yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF9CA3AF),
                        modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ExerciseSectionView(
    entries: List<ExerciseEntry>,
    workouts: List<WorkoutEntry> = emptyList(),
    onAdd: () -> Unit,
    onAddWorkout: () -> Unit = {},
    onDelete: (ExerciseEntry) -> Unit,
    onDeleteWorkout: (WorkoutEntry) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    val totalBurn = entries.sumOf { it.caloriesBurned.toDouble() }.toInt() +
            workouts.sumOf { it.estimatedCalories.toDouble() }.toInt()

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Workouts & Exercises",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF5F5F7)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "-$totalBurn kcal",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF20AA1D)
                    )
                    IconButton(
                        onClick = onAddWorkout,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF20AA1D).copy(alpha = 0.15f))
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = "AI Workout", tint = Color(0xFF20AA1D), modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = onAdd,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF20AA1D).copy(alpha = 0.15f))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Exercise", tint = Color(0xFF20AA1D), modifier = Modifier.size(16.dp))
                    }
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))

                // Render modern WorkoutEntry items
                workouts.forEach { workout ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = workout.title,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = workout.workoutType.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "${workout.durationMinutes} mins  •  ${workout.intensity.replaceFirstChar { it.uppercase() }} intensity",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val exercises = workout.getExercises()
                            if (exercises.isNotEmpty()) {
                                Text(
                                    text = exercises.joinToString(", ") { "${it.name}${if (it.sets != null && it.reps != null) " (${it.sets}x${it.reps})" else ""}" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "-${workout.estimatedCalories} kcal",
                                style = MetricNumeralSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val view = androidx.compose.ui.platform.LocalView.current
                            IconButton(
                                onClick = {
                                    com.example.fitpal.util.HapticFeedbackHelper.triggerRejectOrDelete(view)
                                    onDeleteWorkout(workout)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete workout",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Render legacy ExerciseEntry items
                entries.forEach { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.exerciseName,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${entry.durationMinutes} mins  |  ${entry.intensity}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF9CA3AF)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "-${entry.caloriesBurned.toInt()} kcal",
                                style = MetricNumeralSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val view = androidx.compose.ui.platform.LocalView.current
                            IconButton(
                                onClick = {
                                    com.example.fitpal.util.HapticFeedbackHelper.triggerRejectOrDelete(view)
                                    onDelete(entry)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete item",
                                    tint = Color(0xFFAE422A).copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                if (entries.isEmpty() && workouts.isEmpty()) {
                    Text(
                        text = "No workouts logged",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF9CA3AF),
                        modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp)
                    )
                }
            }
        }
    }
}
