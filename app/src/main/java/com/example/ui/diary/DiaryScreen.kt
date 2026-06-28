package com.example.ui.diary

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
import com.example.data.local.entity.ExerciseEntry
import com.example.data.local.entity.FoodEntry
import com.example.ui.components.EmptyState
import com.example.util.Calculations
import com.example.util.DateUtils
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
    val profile by viewModel.userProfile.collectAsState()

    val calorieGoal = if (profile != null) {
        val baseCalories = if (profile!!.maintenanceCalories != null && profile!!.maintenanceCalories!! > 0) {
            profile!!.maintenanceCalories!!.toFloat()
        } else {
            profile!!.bmr * 1.375f // Fallback: estimate TDEE from BMR using standard lightly active activity factor
        }
        com.example.util.Calculations.calculateTargetCalorieGoal(
            dailyCalories = baseCalories,
            weightGoalType = profile!!.weightGoalType,
            adjustmentValue = profile!!.calorieAdjustment,
            bmr = profile!!.bmr,
            gender = profile!!.gender
        )
    } else {
        2000
    }

    val snackbarHostState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val snackHost = remember { SnackbarHostState() }

    val scrollState = rememberScrollState()

    // Filter meals
    val breakfast = foodEntries.filter { it.mealType.lowercase() == "breakfast" }
    val lunch = foodEntries.filter { it.mealType.lowercase() == "lunch" }
    val dinner = foodEntries.filter { it.mealType.lowercase() == "dinner" }
    val snacks = foodEntries.filter { it.mealType.lowercase() == "snacks" }

    val totalFoodCalories = foodEntries.sumOf { it.caloriesConsumed.toDouble() }.toInt()
    val totalExerciseCalories = exerciseEntries.sumOf { it.caloriesBurned.toDouble() }.toInt()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fitness Diary", fontWeight = FontWeight.Black) },
                actions = {
                    val view = androidx.compose.ui.platform.LocalView.current
                    TextButton(
                        onClick = {
                            com.example.util.HapticFeedbackHelper.triggerConfirm(view)
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackHost) },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Food Intake", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$totalFoodCalories", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                            Text("kcal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        
                        Box(modifier = Modifier.height(40.dp).width(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)))
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Active Burn", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("-$totalExerciseCalories", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
                            Text("kcal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        
                        Box(modifier = Modifier.height(40.dp).width(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)))
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Net Balance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val net = totalFoodCalories - totalExerciseCalories
                            Text("$net", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            Text("kcal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Premium linear budget indicator
                    val progress = if (totalFoodCalories > 0) {
                        (totalFoodCalories.toFloat() / calorieGoal.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Daily Budget: $calorieGoal kcal",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val pct = (progress * 100).roundToInt()
                            Text(
                                text = "$pct% Eaten",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
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
                    onAdd = { onAddFoodForMeal("Breakfast", date) },
                    onDelete = { entry ->
                        viewModel.deleteFoodEntry(entry)
                        scope.launch {
                            val result = snackHost.showSnackbar(
                                message = "Deleted ${entry.foodName}",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.undoDeleteFoodEntry()
                            }
                        }
                    },
                    modifier = Modifier.testTag("section_breakfast")
                )

                MealSectionView(
                    sectionName = "Lunch",
                    entries = lunch,
                    onAdd = { onAddFoodForMeal("Lunch", date) },
                    onDelete = { entry ->
                        viewModel.deleteFoodEntry(entry)
                        scope.launch {
                            val result = snackHost.showSnackbar(
                                message = "Deleted ${entry.foodName}",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.undoDeleteFoodEntry()
                            }
                        }
                    },
                    modifier = Modifier.testTag("section_lunch")
                )

                MealSectionView(
                    sectionName = "Dinner",
                    entries = dinner,
                    onAdd = { onAddFoodForMeal("Dinner", date) },
                    onDelete = { entry ->
                        viewModel.deleteFoodEntry(entry)
                        scope.launch {
                            val result = snackHost.showSnackbar(
                                message = "Deleted ${entry.foodName}",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.undoDeleteFoodEntry()
                            }
                        }
                    },
                    modifier = Modifier.testTag("section_dinner")
                )

                MealSectionView(
                    sectionName = "Snacks",
                    entries = snacks,
                    onAdd = { onAddFoodForMeal("Snacks", date) },
                    onDelete = { entry ->
                        viewModel.deleteFoodEntry(entry)
                        scope.launch {
                            val result = snackHost.showSnackbar(
                                message = "Deleted ${entry.foodName}",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.undoDeleteFoodEntry()
                            }
                        }
                    },
                    modifier = Modifier.testTag("section_snacks")
                )

                // EXERCISE SECTION
                ExerciseSectionView(
                    entries = exerciseEntries,
                    onAdd = onAddExercise,
                    onDelete = { entry ->
                        viewModel.deleteExerciseEntry(entry)
                        scope.launch {
                            val result = snackHost.showSnackbar(
                                message = "Deleted ${entry.exerciseName}",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.undoDeleteExerciseEntry()
                            }
                        }
                    },
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
        }
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
        "breakfast" -> Color(0xFFE67E22)
        "lunch" -> Color(0xFF2ECC71)
        "dinner" -> Color(0xFFE74C3C)
        else -> Color(0xFF9B59B6)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
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
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = onAdd,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add $sectionName Log", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
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
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Text("Protein: ${totalProtein}g", style = MaterialTheme.typography.labelSmall, color = com.example.ui.components.MacroProteinColor, fontWeight = FontWeight.Bold)
                        Text("Carbs: ${totalCarbs}g", style = MaterialTheme.typography.labelSmall, color = com.example.ui.components.MacroCarbsColor, fontWeight = FontWeight.Bold)
                        Text("Fats: ${totalFat}g", style = MaterialTheme.typography.labelSmall, color = com.example.ui.components.MacroFatColor, fontWeight = FontWeight.Bold)
                    }
                }

                entries.forEach { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.2f))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.foodName,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
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
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                                Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant))
                                Text(
                                    text = "P: ${entry.proteinGrams.toInt()}g",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = com.example.ui.components.MacroProteinColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .border(width = 0.5.dp, color = com.example.ui.components.MacroProteinColor.copy(alpha = 0.4f), shape = RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                                Text(
                                    text = "C: ${entry.carbsGrams.toInt()}g",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = com.example.ui.components.MacroCarbsColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .border(width = 0.5.dp, color = com.example.ui.components.MacroCarbsColor.copy(alpha = 0.4f), shape = RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                                Text(
                                    text = "F: ${entry.fatGrams.toInt()}g",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = com.example.ui.components.MacroFatColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .border(width = 0.5.dp, color = com.example.ui.components.MacroFatColor.copy(alpha = 0.4f), shape = RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${entry.caloriesConsumed.toInt()} kcal",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val view = androidx.compose.ui.platform.LocalView.current
                            IconButton(
                                onClick = {
                                    com.example.util.HapticFeedbackHelper.triggerRejectOrDelete(view)
                                    onDelete(entry)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete item",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
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
    onAdd: () -> Unit,
    onDelete: (ExerciseEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    val totalBurn = entries.sumOf { it.caloriesBurned.toDouble() }.toInt()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Workouts & Exercises",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
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
                        color = MaterialTheme.colorScheme.secondary
                    )
                    IconButton(onClick = onAdd, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Add Workout", tint = MaterialTheme.colorScheme.secondary)
                    }
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))

                entries.forEach { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.exerciseName,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${entry.durationMinutes} mins  |  ${entry.category} (${entry.intensity})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "-${entry.caloriesBurned.toInt()} kcal",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val view = androidx.compose.ui.platform.LocalView.current
                            IconButton(
                                onClick = {
                                    com.example.util.HapticFeedbackHelper.triggerRejectOrDelete(view)
                                    onDelete(entry)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete item",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                if (entries.isEmpty()) {
                    Text(
                        text = "No workouts logged",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 8.dp)
                    )
                }
            }
        }
    }
}
