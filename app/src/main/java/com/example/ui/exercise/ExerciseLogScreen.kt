package com.example.ui.exercise

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.SearchResultExercise
import com.example.ui.components.EmptyState
import com.example.util.Calculations
import com.example.util.DateUtils
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseLogScreen(
    viewModel: ExerciseViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Search Database", "Quick Manual", "Custom Creator")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log Workout", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("exercise_log_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Selector
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                        modifier = Modifier.testTag("exercise_tab_$index")
                    )
                }
            }

            when (selectedTab) {
                0 -> SearchExerciseTab(viewModel, onBack)
                1 -> ManualExerciseTab(viewModel, onBack)
                2 -> CustomExerciseTab(viewModel)
            }
        }
    }
}

// === TAB 1: EXERCISE SEARCH TAB ===
@Composable
fun SearchExerciseTab(
    viewModel: ExerciseViewModel,
    onBack: () -> Unit
) {
    val view = androidx.compose.ui.platform.LocalView.current
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val profile by viewModel.userProfile.collectAsState()

    var activeExerciseForCalculation by remember { mutableStateOf<SearchResultExercise?>(null) }
    var durationMinutesString by remember { mutableStateOf("30") }
    var intensitySelected by remember { mutableStateOf("Moderate") }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.searchExercises(it) },
            placeholder = { Text("Search 60+ exercises (e.g., Running, Squats, Yoga)...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.searchExercises("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("exercise_search_input")
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (query.isBlank()) {
            // SHOW CATEGORIES OR DIRECT PROMPT
            EmptyState(
                title = "Browse Physical Activities",
                description = "Type an exercise name to search. Categories include Cardio, Strength, Flexibility, Sports, and Daily Activities.",
                icon = Icons.Default.DirectionsRun,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
        } else {
            // SHOW SEARCH RESULTS
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                results.forEach { exercise ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                            .clickable {
                                activeExerciseForCalculation = exercise
                                durationMinutesString = "30"
                                intensitySelected = "Moderate"
                            }
                            .padding(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(exercise.name, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(exercise.category, fontSize = 9.sp) },
                                    modifier = Modifier.height(18.dp)
                                )
                            }
                            Text(
                                text = "MET: L: ${exercise.metValueLight} | M: ${exercise.metValueModerate} | V: ${exercise.metValueVigorous}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.AddCircle, contentDescription = "Add", tint = MaterialTheme.colorScheme.secondary)
                    }
                }

                if (results.isEmpty()) {
                    EmptyState(
                        title = "No matched templates",
                        description = "Use the 'Custom Creator' tab to configure a permanent exercise profile.",
                        icon = Icons.Default.Info,
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                    )
                }
            }
        }

        // CONFIRMATION DIALOG (CALORIES BURNED PREVIEW)
        activeExerciseForCalculation?.let { exercise ->
            val met = when (intensitySelected) {
                "Light" -> exercise.metValueLight
                "Vigorous" -> exercise.metValueVigorous
                else -> exercise.metValueModerate
            }

            val duration = durationMinutesString.toIntOrNull() ?: 0
            val weight = profile?.currentWeightKg ?: 70f
            val burned = Calculations.calculateExerciseCalories(met, weight, duration)

            var caloriesBurnedString by remember(burned) {
                mutableStateOf(burned.roundToInt().toString())
            }

            AlertDialog(
                onDismissRequest = { activeExerciseForCalculation = null },
                title = { Text("Log Workout", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Selected Workout: ${exercise.name}",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        OutlinedTextField(
                            value = durationMinutesString,
                            onValueChange = { durationMinutesString = it },
                            label = { Text("Duration (minutes)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("exercise_duration_input")
                        )

                        // INTENSITY RATIO SELECTOR
                        Column {
                            Text(
                                text = "Intensity Level",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val options = listOf("Light", "Moderate", "Vigorous")
                                options.forEach { option ->
                                    val isSelected = intensitySelected == option
                                    Button(
                                        onClick = { intensitySelected = option },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .testTag("intensity_${option.lowercase()}")
                                    ) {
                                        Text(option, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = caloriesBurnedString,
                            onValueChange = { newVal ->
                                if (newVal.all { it.isDigit() }) {
                                    caloriesBurnedString = newVal
                                }
                            },
                            label = { Text("Calories Burned (kcal)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("exercise_calories_input")
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Calculated MET Reference:", style = MaterialTheme.typography.labelSmall)
                                }
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Estimated MET:")
                                    Text("$met", fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Estimated BMR baseline:")
                                    Text("${(profile?.bmr?.toInt() ?: 1600)} kcal", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            com.example.util.HapticFeedbackHelper.triggerConfirm(view)
                            val durationVal = durationMinutesString.toIntOrNull() ?: 30
                            val metVal = when (intensitySelected) {
                                "Light" -> exercise.metValueLight
                                "Vigorous" -> exercise.metValueVigorous
                                else -> exercise.metValueModerate
                            }
                            val calories = caloriesBurnedString.toFloatOrNull() ?: burned

                            viewModel.addExerciseEntry(
                                date = DateUtils.getTodayDateString(),
                                exerciseName = exercise.name,
                                category = exercise.category,
                                durationMinutes = durationVal,
                                intensity = intensitySelected,
                                metValue = metVal,
                                isCustom = exercise.isCustom,
                                caloriesBurned = calories
                            )
                            activeExerciseForCalculation = null
                            onBack()
                        },
                        modifier = Modifier.testTag("dialog_add_exercise")
                    ) {
                        Text("Add Workout")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { activeExerciseForCalculation = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

// === TAB 2: MANUAL EXERCISE TAB ===
@Composable
fun ManualExerciseTab(
    viewModel: ExerciseViewModel,
    onBack: () -> Unit
) {
    val view = androidx.compose.ui.platform.LocalView.current
    var name by remember { mutableStateOf("") }
    var durationMinutesStr by remember { mutableStateOf("") }
    var caloriesBurnedStr by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Quick Manual Workout Entry",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Directly enter logged workouts tracked via Apple Watch, Garmin, Fitbit, or other smart wearables.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Exercise Name (e.g., Cross-trainer Cardio)") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("manual_exercise_name")
        )

        OutlinedTextField(
            value = durationMinutesStr,
            onValueChange = { durationMinutesStr = it },
            label = { Text("Duration (minutes)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("manual_exercise_duration")
        )

        OutlinedTextField(
            value = caloriesBurnedStr,
            onValueChange = { caloriesBurnedStr = it },
            label = { Text("Calories Burned (kcal)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("manual_exercise_calories")
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                com.example.util.HapticFeedbackHelper.triggerConfirm(view)
                val duration = durationMinutesStr.toIntOrNull() ?: 0
                val calories = caloriesBurnedStr.toFloatOrNull() ?: 0f

                viewModel.addManualExercise(
                    date = DateUtils.getTodayDateString(),
                    exerciseName = name,
                    durationMinutes = duration,
                    caloriesBurned = calories
                )
                onBack()
            },
            enabled = name.isNotBlank() && durationMinutesStr.isNotBlank() && caloriesBurnedStr.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("manual_exercise_save")
        ) {
            Text("Log Workout", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

// === TAB 3: CUSTOM EXERCISE TEMPLATE CREATOR ===
@Composable
fun CustomExerciseTab(
    viewModel: ExerciseViewModel
) {
    val context = LocalContext.current
    val view = androidx.compose.ui.platform.LocalView.current
    var name by remember { mutableStateOf("") }
    var metStr by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Cardio") }

    val categories = listOf("Cardio", "Strength Training", "Flexibility", "Sports", "Daily Activities")
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Create Custom Exercise Profile",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Define a permanent exercise profile with a set MET value. MET indicates energy expenditure compared to quiet sitting (1 MET).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Exercise Name (e.g., Peloton HIIT)") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("custom_exercise_name")
        )

        OutlinedTextField(
            value = metStr,
            onValueChange = { metStr = it },
            label = { Text("MET Factor (e.g., 8.5 for vigorous biking)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("custom_exercise_met")
        )

        // CATEGORY CHOOSER
        Column {
            Text(
                text = "Exercise Category",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            categories.forEach { cat ->
                val isSelected = selectedCategory == cat
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                        .clickable { selectedCategory = cat }
                        .padding(10.dp)
                        .testTag("category_${cat.replace(" ", "_").lowercase()}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = isSelected, onClick = { selectedCategory = cat })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(cat, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                com.example.util.HapticFeedbackHelper.triggerConfirm(view)
                val met = metStr.toFloatOrNull() ?: 5.0f
                viewModel.createCustomExercise(name, selectedCategory, met)
                
                Toast.makeText(context, "'$name' exercise template created successfully!", Toast.LENGTH_SHORT).show()
                name = ""
                metStr = ""
                selectedCategory = "Cardio"
            },
            enabled = name.isNotBlank() && metStr.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("custom_exercise_save")
        ) {
            Text("Create Template & Save", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
