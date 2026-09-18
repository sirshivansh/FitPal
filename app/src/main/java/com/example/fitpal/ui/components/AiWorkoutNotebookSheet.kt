package com.example.fitpal.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitpal.data.api.GeminiWorkoutService
import com.example.fitpal.data.api.ParsedWorkoutItem
import com.example.fitpal.data.local.entity.WorkoutEntry
import com.example.fitpal.data.local.entity.WorkoutExercise
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiWorkoutNotebookSheet(
    userWeightKg: Float,
    targetDate: String,
    onDismiss: () -> Unit,
    onSaveWorkouts: (List<WorkoutEntry>) -> Unit,
    onSaveSteps: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    var isParsing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var parsedWorkouts by remember { mutableStateOf<List<ParsedWorkoutItem>>(emptyList()) }

    val quickExamples = listOf(
        "Walked 4,000 steps this morning",
        "Bench press 4x8 @ 80kg, incline dumbbell 3x10 @ 24kg, tricep pushdowns 3x12, 45 mins",
        "Ran 5k in 28 mins, moderate pace",
        "30 min brisk walk around the neighborhood",
        "40 min full body dumbbell workout, vigorous intensity"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("ai_workout_notebook_sheet")
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
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI Workout Notebook",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_workout_sheet_btn")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Text(
                text = "Describe your workout naturally. FitPal parses exercises, sets, reps, duration, and calculates metabolic expenditure using scientific MET formulas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 6.dp)
            )

            // Quick Example Chips
            Text(
                text = "Quick examples:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(quickExamples) { example ->
                    SuggestionChip(
                        onClick = { inputText = example },
                        label = {
                            Text(
                                text = example.take(35) + "...",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Freeform Input Field
            OutlinedTextField(
                value = inputText,
                onValueChange = {
                    inputText = it
                    errorMessage = null
                },
                label = { Text("What did you do today?") },
                placeholder = { Text("e.g. Squats 4x10 @ 90kg, Leg Press 3x12, 40 mins") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 90.dp, max = 150.dp)
                    .testTag("workout_text_input"),
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

            // Action Button: Parse
            Button(
                onClick = {
                    if (inputText.isBlank()) {
                        errorMessage = "Please enter your workout details."
                        return@Button
                    }
                    isParsing = true
                    errorMessage = null
                    coroutineScope.launch {
                        try {
                            val results = GeminiWorkoutService.parseWorkoutText(inputText, userWeightKg)
                            parsedWorkouts = results
                            if (results.isEmpty()) {
                                errorMessage = "Could not identify any workout. Please specify duration or exercises."
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
                    .testTag("parse_workout_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isParsing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyzing with Gemini...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Parse Workout", fontWeight = FontWeight.Bold)
                }
            }

            // Parsed Results Review
            AnimatedVisibility(visible = parsedWorkouts.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text(
                        text = "Review & Confirm Workouts (${parsedWorkouts.size}):",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(parsedWorkouts) { item ->
                            ParsedWorkoutPreviewCard(item = item)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val entries = parsedWorkouts.map { parsed ->
                                WorkoutEntry.createWithExercises(
                                    date = targetDate,
                                    title = parsed.title,
                                    workoutType = parsed.workoutType,
                                    durationMinutes = parsed.durationMinutes,
                                    intensity = parsed.intensity,
                                    estimatedCalories = parsed.estimatedCalories,
                                    userWeightKgAtTime = userWeightKg,
                                    confidence = parsed.confidence,
                                    source = "AI",
                                    distanceKm = parsed.distanceKm,
                                    exercises = parsed.exercises,
                                    notes = parsed.notes
                                )
                            }
                            val parsedSteps = parsedWorkouts.mapNotNull { it.steps }.sum()
                            if (parsedSteps > 0 && onSaveSteps != null) {
                                onSaveSteps(parsedSteps)
                            }
                            onSaveWorkouts(entries)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("confirm_save_workouts_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Save to Diary (${parsedWorkouts.sumOf { it.estimatedCalories }} kcal total)",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ParsedWorkoutPreviewCard(item: ParsedWorkoutItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${item.workoutType} • ${item.durationMinutes} mins • ${item.intensity} intensity",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "${item.estimatedCalories} kcal",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${item.confidence} confidence",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            if (item.exercises.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(6.dp))

                item.exercises.forEach { ex ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "• ${ex.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val det = buildString {
                            if (ex.sets != null && ex.reps != null) append("${ex.sets}×${ex.reps}")
                            if (ex.weightKg != null) append(" @ ${ex.weightKg.roundToInt()}kg")
                        }
                        if (det.isNotBlank()) {
                            Text(
                                text = det,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.calculationExplanation,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}
