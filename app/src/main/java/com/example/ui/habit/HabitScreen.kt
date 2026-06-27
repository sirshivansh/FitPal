package com.example.ui.habit

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.Habit
import com.example.data.local.entity.HabitCompletion
import com.example.util.DateUtils
import com.example.util.HapticFeedbackHelper
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitScreen(
    viewModel: HabitViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val habits by viewModel.habits.collectAsState()
    val completions by viewModel.completions.collectAsState()

    var showAddHabitDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FitPal Habits", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("habit_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.resetHabits() },
                        modifier = Modifier.testTag("habit_reset_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset Defaults")
                    }
                    Button(
                        onClick = { showAddHabitDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("habit_add_new_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.testTag("habit_screen")
    ) { innerPadding ->
        if (habits.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Spa,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Build Lasting Habits",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Click 'Add' or click the reset button at the top to load default custom habits.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(habits, key = { it.id }) { habit ->
                    HabitCard(
                        habit = habit,
                        completions = completions.filter { it.habitId == habit.id },
                        onToggleToday = {
                            viewModel.toggleHabitToday(habit.id, DateUtils.getTodayDateString())
                        },
                        onToggleDate = { date ->
                            viewModel.toggleHabitForDate(habit.id, date)
                        },
                        onDelete = {
                            viewModel.deleteHabit(habit)
                        }
                    )
                }
            }
        }

        if (showAddHabitDialog) {
            AddHabitDialog(
                onDismiss = { showAddHabitDialog = false },
                onAdd = { name, desc, colorHex, iconName ->
                    viewModel.addCustomHabit(name, desc, colorHex, iconName)
                    showAddHabitDialog = false
                }
            )
        }
    }
}

@Composable
fun HabitCard(
    habit: Habit,
    completions: List<HabitCompletion>,
    onToggleToday: () -> Unit,
    onToggleDate: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = DateUtils.getTodayDateString()
    val isCompletedToday = completions.any { it.date == today }
    val view = LocalView.current
    val context = LocalContext.current

    // Heatmap grid sizing
    val numRows = 5
    val numCols = 25
    val totalDays = numRows * numCols // 125 days
    val dates = remember { DateUtils.getDaysRange(totalDays) }

    // Habit accent color
    val accentColor = remember(habit.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(habit.colorHex))
        } catch (e: Exception) {
            Color(0xFF56AB7C)
        }
    }

    // Calculation of streaks
    val streakInfo = remember(completions) {
        calculateStreak(completions, dates)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("habit_card_${habit.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header: Icon, Info, Checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Circular icon background
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getHabitIcon(habit.iconName),
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = habit.name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = habit.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Trash icon to delete habit
                    IconButton(
                        onClick = {
                            HapticFeedbackHelper.triggerRejectOrDelete(view)
                            onDelete()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Habit",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Complete Today Button
                    IconButton(
                        onClick = {
                            if (!isCompletedToday) {
                                HapticFeedbackHelper.triggerHabitCompleted(context, view)
                            } else {
                                HapticFeedbackHelper.triggerLightTap(view)
                            }
                            onToggleToday()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCompletedToday) accentColor else MaterialTheme.colorScheme.surfaceVariant
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Log Today",
                            tint = if (isCompletedToday) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // STATS ROW: Current Streak & Total Completed
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Whatshot,
                        contentDescription = "Streak",
                        tint = if (streakInfo.currentStreak > 0) Color(0xFFE67E22) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Streak: ${streakInfo.currentStreak} days",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (streakInfo.currentStreak > 0) Color(0xFFE67E22) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "Total: ${completions.size} logged",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // HEATMAP MATRIX GRID
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (c in 0 until numCols) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (r in 0 until numRows) {
                                val dateIndex = c * numRows + r
                                if (dateIndex < dates.size) {
                                    val dateStr = dates[dateIndex]
                                    val isCompleted = completions.any { it.date == dateStr }
                                    val isToday = dateStr == today

                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(
                                                when {
                                                    isCompleted -> accentColor
                                                    isToday -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                                }
                                            )
                                            .border(
                                                width = if (isToday) 1.dp else 0.dp,
                                                color = if (isToday) accentColor else Color.Transparent,
                                                shape = RoundedCornerShape(2.dp)
                                            )
                                            .clickable {
                                                if (!isCompleted) {
                                                    HapticFeedbackHelper.triggerHabitCompleted(context, view)
                                                } else {
                                                    HapticFeedbackHelper.triggerRejectOrDelete(view)
                                                }
                                                onToggleDate(dateStr)
                                                val formatted = try {
                                                    val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                                    val displayer = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                                                    parser.parse(dateStr)?.let { displayer.format(it) } ?: dateStr
                                                } catch (e: Exception) {
                                                    dateStr
                                                }
                                                val msg = if (!isCompleted) "Completed habit on $formatted!" else "Cleared log for $formatted"
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                    )
                                } else {
                                    Spacer(modifier = Modifier.size(10.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Row helper labels under heatmap
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, start = 2.dp, end = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Older",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 9.sp
                )
                Text(
                    text = "Today",
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                )
            }
        }
    }
}

// Helper icons mapping
fun getHabitIcon(iconName: String): ImageVector {
    return when (iconName) {
        "meditation" -> Icons.Default.Spa
        "side_hustle" -> Icons.Default.Code
        "drums" -> Icons.Default.MusicNote
        "running" -> Icons.Default.DirectionsRun
        "coffee" -> Icons.Default.LocalCafe
        "water" -> Icons.Default.WaterDrop
        "book" -> Icons.Default.MenuBook
        "sleep" -> Icons.Default.Bedtime
        else -> Icons.Default.Star
    }
}

data class StreakInfo(val currentStreak: Int, val maxStreak: Int)

fun calculateStreak(completions: List<HabitCompletion>, dates: List<String>): StreakInfo {
    if (completions.isEmpty()) return StreakInfo(0, 0)
    
    val completionDates = completions.map { it.date }.toSet()
    val today = DateUtils.getTodayDateString()
    
    // Sort all dates
    val sortedDates = dates.sorted()
    
    var currentStreak = 0
    var tempStreak = 0
    var maxStreak = 0

    // Loop through past days up to today
    for (d in sortedDates) {
        if (completionDates.contains(d)) {
            tempStreak++
            if (tempStreak > maxStreak) {
                maxStreak = tempStreak
            }
        } else {
            if (d != today) {
                tempStreak = 0
            }
        }
    }

    // Calculate current streak backward from today
    var checkDate = today
    if (completionDates.contains(checkDate)) {
        currentStreak = 1
        while (true) {
            checkDate = DateUtils.getPreviousDay(checkDate)
            if (completionDates.contains(checkDate)) {
                currentStreak++
            } else {
                break
            }
        }
    } else {
        // If not completed today, check if completed yesterday to keep streak active
        val yesterday = DateUtils.getPreviousDay(today)
        if (completionDates.contains(yesterday)) {
            currentStreak = 1
            checkDate = yesterday
            while (true) {
                checkDate = DateUtils.getPreviousDay(checkDate)
                if (completionDates.contains(checkDate)) {
                    currentStreak++
                } else {
                    break
                }
            }
        } else {
            currentStreak = 0
        }
    }

    return StreakInfo(currentStreak, maxStreak)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, description: String, colorHex: String, iconName: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    
    val colors = listOf(
        Pair("Purple", "#9B59B6"),
        Pair("Pink", "#E91E63"),
        Pair("Orange", "#D35400"),
        Pair("Yellow", "#F1C40F"),
        Pair("Blue", "#3498DB"),
        Pair("Green", "#2ECC71")
    )
    var selectedColor by remember { mutableStateOf(colors[0]) }

    val icons = listOf(
        Pair("Spa", "meditation"),
        Pair("Code", "side_hustle"),
        Pair("Drums", "drums"),
        Pair("Run", "running"),
        Pair("Cafe", "coffee"),
        Pair("Water", "water"),
        Pair("Book", "book"),
        Pair("Sleep", "sleep")
    )
    var selectedIcon by remember { mutableStateOf(icons[0]) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("add_habit_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "New Custom Habit",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Habit Name") },
                    placeholder = { Text("e.g. Read Book") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_habit_name_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Daily Goal / Description") },
                    placeholder = { Text("e.g. 20 pages before bed") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_habit_desc_input")
                )

                // Color Selection Row
                Column {
                    Text(
                        text = "Pick Theme Color",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        colors.forEach { colorPair ->
                            val parsedColor = Color(android.graphics.Color.parseColor(colorPair.second))
                            val isSelected = selectedColor == colorPair
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(parsedColor)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = colorPair },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (colorPair.first == "Yellow") Color.Black else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Icon Selection Row
                Column {
                    Text(
                        text = "Pick Habit Icon",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        icons.take(4).forEach { iconPair ->
                            val isSelected = selectedIcon == iconPair
                            IconButton(
                                onClick = { selectedIcon = iconPair },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                    )
                            ) {
                                Icon(
                                    imageVector = getHabitIcon(iconPair.second),
                                    contentDescription = iconPair.first,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        icons.drop(4).forEach { iconPair ->
                            val isSelected = selectedIcon == iconPair
                            IconButton(
                                onClick = { selectedIcon = iconPair },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                    )
                            ) {
                                Icon(
                                    imageVector = getHabitIcon(iconPair.second),
                                    contentDescription = iconPair.first,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag("add_habit_dismiss")) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onAdd(name, description, selectedColor.second, selectedIcon.second)
                            }
                        },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.testTag("add_habit_confirm")
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}
