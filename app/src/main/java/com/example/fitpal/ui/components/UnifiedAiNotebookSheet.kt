package com.example.fitpal.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitpal.data.api.GeminiWorkoutService
import com.example.fitpal.data.api.ParsedNotepadFood
import com.example.fitpal.data.api.ParsedWorkoutItem
import com.example.fitpal.data.local.entity.FoodEntry
import com.example.fitpal.data.local.entity.WorkoutEntry
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedAiNotebookSheet(
    userWeightKg: Float,
    targetDate: String,
    onDismiss: () -> Unit,
    onSaveAll: (foods: List<FoodEntry>, workouts: List<WorkoutEntry>) -> Unit,
    onSaveSteps: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    var isParsing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var parsedFoods by remember { mutableStateOf<List<ParsedNotepadFood>>(emptyList()) }
    var parsedWorkouts by remember { mutableStateOf<List<ParsedWorkoutItem>>(emptyList()) }

    val quickExamples = listOf(
        "Breakfast: 3 eggs scrambled, 2 slices toast, coffee. Afternoon: 40 min chest workout, bench press 4x8 @ 80kg.",
        "Lunch: Chicken breast 200g, brown rice 150g, broccoli. Evening: 5k run in 28 mins.",
        "Post-workout shake: 1 scoop whey, 1 banana, 300ml almond milk. 30 min morning walk."
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("unified_ai_notebook_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Unified AI Notebook",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_unified_sheet_btn")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Text(
                text = "Log everything at once. Write your meals, snacks, and workouts together in one note — FitPal's AI will parse and categorize everything accurately.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 6.dp)
            )

            // Examples
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(quickExamples) { example ->
                    SuggestionChip(
                        onClick = { inputText = example },
                        label = {
                            Text(
                                text = example.take(38) + "...",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Text Field
            OutlinedTextField(
                value = inputText,
                onValueChange = {
                    inputText = it
                    errorMessage = null
                },
                label = { Text("Log your day (foods and workouts)") },
                placeholder = {
                    Text("e.g. Oatmeal with blueberries. 45 min gym session: bench press 4x8, pullups 3x10.")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 160.dp)
                    .testTag("unified_text_input"),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = RoundedCornerShape(14.dp)
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = errorMessage ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (inputText.isBlank()) {
                        errorMessage = "Please enter your log text."
                        return@Button
                    }
                    isParsing = true
                    errorMessage = null
                    coroutineScope.launch {
                        try {
                            val result = GeminiWorkoutService.parseUnifiedNotebook(inputText, userWeightKg)
                            parsedFoods = result.foods
                            parsedWorkouts = result.workouts
                            if (result.foods.isEmpty() && result.workouts.isEmpty()) {
                                errorMessage = "Could not identify any foods or workouts. Try adding specific meals or exercise names."
                            }
                        } catch (e: Exception) {
                            errorMessage = "Parsing error: ${e.localizedMessage ?: "Unknown error"}"
                        } finally {
                            isParsing = false
                        }
                    }
                },
                enabled = !isParsing && inputText.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("parse_unified_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isParsing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Parsing Foods & Workouts...")
                } else {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Parse Entire Day", fontWeight = FontWeight.Bold)
                }
            }

            // Results Display
            AnimatedVisibility(visible = parsedFoods.isNotEmpty() || parsedWorkouts.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text(
                        text = "Parsed Entries: ${parsedFoods.size} foods, ${parsedWorkouts.size} workouts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Food section
                        if (parsedFoods.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Foods (${parsedFoods.sumOf { it.calories }.roundToInt()} kcal)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            items(parsedFoods) { food ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = food.foodName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "P: ${food.protein.roundToInt()}g • C: ${food.carbs.roundToInt()}g • F: ${food.fat.roundToInt()}g",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "${food.calories.roundToInt()} kcal",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        // Workouts section
                        if (parsedWorkouts.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Workouts (-${parsedWorkouts.sumOf { it.estimatedCalories }} kcal)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            items(parsedWorkouts) { w ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = w.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "${w.workoutType} • ${w.durationMinutes}m • ${w.intensity}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "${w.estimatedCalories} kcal",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val foodEntities = parsedFoods.map { f ->
                                FoodEntry(
                                    foodName = f.foodName,
                                    mealType = "Snack", // Default meal bucket, can be adjusted in diary
                                    caloriesConsumed = f.calories,
                                    proteinGrams = f.protein,
                                    carbsGrams = f.carbs,
                                    fatGrams = f.fat,
                                    gramsConsumed = f.gramsConsumed,
                                    date = targetDate
                                )
                            }

                            val workoutEntities = parsedWorkouts.map { w ->
                                WorkoutEntry.createWithExercises(
                                    date = targetDate,
                                    title = w.title,
                                    workoutType = w.workoutType,
                                    durationMinutes = w.durationMinutes,
                                    intensity = w.intensity,
                                    estimatedCalories = w.estimatedCalories,
                                    userWeightKgAtTime = userWeightKg,
                                    confidence = w.confidence,
                                    source = "AI",
                                    distanceKm = w.distanceKm,
                                    exercises = w.exercises,
                                    notes = w.notes
                                )
                            }

                            val parsedSteps = parsedWorkouts.mapNotNull { it.steps }.sum()
                            if (parsedSteps > 0 && onSaveSteps != null) {
                                onSaveSteps(parsedSteps)
                            }

                            onSaveAll(foodEntities, workoutEntities)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("confirm_save_unified_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save All to Diary", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
