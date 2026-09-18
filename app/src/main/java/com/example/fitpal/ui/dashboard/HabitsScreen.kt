package com.example.fitpal.ui.dashboard

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitpal.data.repository.ExerciseRepository
import com.example.fitpal.data.repository.FoodRepository
import com.example.fitpal.data.repository.ProfileRepository
import com.example.fitpal.util.DateUtils
import com.example.fitpal.util.HapticFeedbackHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    profileRepository: ProfileRepository,
    foodRepository: FoodRepository,
    exerciseRepository: ExerciseRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Key stats
    val dates = remember { DateUtils.getLast30Days() }
    var selectedDate by remember { mutableStateOf(DateUtils.getTodayDateString()) }

    // Dynamic background grid states (date -> list of completed habits)
    var habitsStateMap by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }

    // Shared preferences for manual check-offs
    val sharedPrefs = remember { context.getSharedPreferences("fitpal_habits_prefs", Context.MODE_PRIVATE) }

    // Core list of 5 scientific habits
    val habitList = listOf(
        "Log Food" to "Track at least 1 food entry today",
        "Hydration" to "Drink 8 or more glasses of water (2.0L)",
        "Log Weight" to "Keep weight updated for real-time BMR profiling",
        "Active Step Goal" to "Hit 8,000 steps or active workouts",
        "Consistent Routine" to "Custom check-off: No junk foods/refined sugars"
    )

    // Function to reload habits data
    val reloadData: () -> Unit = {
        scope.launch {
            isLoading = true
            val newMap = mutableMapOf<String, List<String>>()
            
            withContext(Dispatchers.IO) {
                for (dateStr in dates) {
                    val completed = mutableListOf<String>()

                    // 1. Log Food
                    val foodEntries = foodRepository.getFoodEntriesSync(dateStr)
                    if (foodEntries.isNotEmpty()) {
                        completed.add("Log Food")
                    }

                    // 2. Hydration
                    val waterLog = profileRepository.getWaterLogSync(dateStr)
                    if (waterLog != null && waterLog.glassesCount >= 8) {
                        completed.add("Hydration")
                    }

                    // 3. Log Weight
                    // Check if there was a weight logged on this date
                    val allWeights = profileRepository.getAllWeights().firstOrNull() ?: emptyList()
                    val weightLogged = allWeights.any { it.date == dateStr }
                    if (weightLogged) {
                        completed.add("Log Weight")
                    }

                    // 4. Step/Exercise
                    val activity = profileRepository.getActivityLogSync(dateStr)
                    val exerciseEntries = exerciseRepository.getExerciseEntries(dateStr).firstOrNull() ?: emptyList()
                    val stepsCount = activity?.steps ?: 0
                    if (stepsCount >= 8000 || exerciseEntries.isNotEmpty() || (activity?.additionalCalories ?: 0) > 0) {
                        completed.add("Active Step Goal")
                    }

                    // 5. Consistent Routine (manual check-off persisted)
                    val isChecked = sharedPrefs.getBoolean("habit_checked_routine_$dateStr", false)
                    if (isChecked) {
                        completed.add("Consistent Routine")
                    }

                    newMap[dateStr] = completed
                }
            }

            habitsStateMap = newMap
            isLoading = false
        }
    }

    // Trigger initial load
    LaunchedEffect(Unit) {
        reloadData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Habit Tracker Grid", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = {
                        HapticFeedbackHelper.triggerLightTap(view)
                        onBack()
                    }, modifier = Modifier.testTag("habits_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero card summarizing achievements
            val totalPossible = dates.size * 5
            val totalCompleted = habitsStateMap.values.sumOf { it.size }
            val completionPercentage = if (totalPossible > 0) (totalCompleted.toFloat() / totalPossible * 100).toInt() else 0

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$completionPercentage%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Consistency Engine",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "You completed $totalCompleted habit items in the last 30 days. Real-time updates based on diary logging!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Contribution Grid Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "30-Day Contribution Map",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Tap any square to view day",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                // CONTRIBUTION GRID (GitHub-style calendar grid)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(6), // 6 columns x 5 rows = 30 days
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            userScrollEnabled = false
                        ) {
                            items(dates) { dateStr ->
                                val completedList = habitsStateMap[dateStr] ?: emptyList()
                                val count = completedList.size
                                val isSelected = selectedDate == dateStr

                                // Github-style shade variations based on count (0 to 5 completed)
                                val baseColor = MaterialTheme.colorScheme.primary
                                val alpha = when (count) {
                                    0 -> 0.05f
                                    1 -> 0.2f
                                    2 -> 0.4f
                                    3 -> 0.6f
                                    4 -> 0.8f
                                    else -> 1.0f
                                }
                                val blockColor = baseColor.copy(alpha = alpha)

                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(blockColor)
                                        .clickable {
                                            selectedDate = dateStr
                                            HapticFeedbackHelper.triggerLightTap(view)
                                        }
                                        .testTag("habit_day_$dateStr"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color.Transparent)
                                                .border(BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface), RoundedCornerShape(6.dp))
                                        )
                                    }
                                    
                                    val dateObj = LocalDate.parse(dateStr)
                                    Text(
                                        text = dateObj.dayOfMonth.toString(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (alpha >= 0.6f) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Grid Legend
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Less", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(4.dp))
                            (0..5).forEach { c ->
                                val alpha = when (c) {
                                    0 -> 0.05f
                                    1 -> 0.2f
                                    2 -> 0.4f
                                    3 -> 0.6f
                                    4 -> 0.8f
                                    else -> 1.0f
                                }
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text("More", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // DETAILS SECTION FOR SELECTED DATE
            val selectedDateDisplay = DateUtils.formatDateForDisplay(selectedDate)
            val selectedCompletedList = habitsStateMap[selectedDate] ?: emptyList()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Habits on $selectedDateDisplay",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
                Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                    Text("${selectedCompletedList.size} / 5 Done", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                }
            }

            // List of habits for selected date
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                habitList.forEach { (habitName, habitDesc) ->
                    val isDone = selectedCompletedList.contains(habitName)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDone) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(1.dp, if (isDone) MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Checked icon/box
                            if (habitName == "Consistent Routine") {
                                // Manual toggle!
                                Checkbox(
                                    checked = isDone,
                                    onCheckedChange = { checkState ->
                                        HapticFeedbackHelper.triggerLightTap(view)
                                        sharedPrefs.edit().putBoolean("habit_checked_routine_$selectedDate", checkState).apply()
                                        reloadData()
                                    },
                                    modifier = Modifier.testTag("habit_toggle_routine")
                                )
                            } else {
                                // Automatic indicators from database
                                Icon(
                                    imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = if (isDone) "Completed" else "Incomplete",
                                    tint = if (isDone) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = habitName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isDone) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = habitDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Auto-source tags
                            if (habitName != "Consistent Routine") {
                                Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.primary)
                                        Text("Auto Log", style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
