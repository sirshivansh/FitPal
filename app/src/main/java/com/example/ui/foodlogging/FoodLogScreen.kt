package com.example.ui.foodlogging

import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.FoodEntry
import com.example.data.repository.SearchResultFood
import com.example.ui.components.EmptyState
import com.example.util.Calculations
import com.example.util.DateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodLogScreen(
    viewModel: FoodLogViewModel,
    mealType: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    logDate: String = DateUtils.getTodayDateString()
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Search Database", "Quick Manual", "Custom Creator")

    // Animations success states
    var isAnimatingSuccess by remember { mutableStateOf(false) }
    val successAnimProgress = remember { androidx.compose.animation.core.Animatable(0f) }
    val scope = rememberCoroutineScope()

    val triggerSuccessAndBack = {
        isAnimatingSuccess = true
        scope.launch {
            successAnimProgress.animateTo(
                targetValue = 1f,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 1400)
            )
            onBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Log $mealType", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("food_log_back_button")) {
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
                            modifier = Modifier.testTag("food_tab_$index")
                        )
                    }
                }

                when (selectedTab) {
                    0 -> SearchFoodTab(viewModel, mealType, onFoodLogged = { triggerSuccessAndBack() }, logDate = logDate)
                    1 -> ManualEntryTab(viewModel, mealType, onFoodLogged = { triggerSuccessAndBack() }, logDate = logDate)
                    2 -> CustomCreatorTab(viewModel, onBack)
                }
            }
        }

        // Beautiful, full-screen interactive logging animation overlay
        if (isAnimatingSuccess) {
            val progress = successAnimProgress.value
            val scale = if (progress < 0.4f) (progress / 0.4f) * 1.3f else (1.0f + (1.0f - progress) * 0.15f)
            val opacity = if (progress < 0.1f) progress / 0.1f else if (progress > 0.82f) ((1f - progress) / 0.18f) else 1f

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.82f * opacity))
                    .clickable(enabled = false) {}, // swallow all click gestures
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f * opacity))
                            .border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f * opacity)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(74.dp)
                                .graphicsLayer(
                                    scaleX = scale.coerceIn(0.1f, 2.5f),
                                    scaleY = scale.coerceIn(0.1f, 2.5f),
                                    alpha = opacity
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Food Logged!",
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp,
                        color = Color.White,
                        modifier = Modifier.graphicsLayer(alpha = opacity)
                    )

                    Text(
                        text = "Diary updated successfully",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.graphicsLayer(alpha = opacity)
                    )
                }

                // Exploding food emojis!
                val emojis = listOf("🍎", "🥑", "🍌", "🥦", "🥑", "🥗", "🍇", "🥕", "🥑", "🎉", "✨", "💪")
                emojis.forEachIndexed { index, emoji ->
                    val angle = (index * 360f / emojis.size) * (Math.PI / 180f)
                    val distanceDp = progress * 165f
                    val xOffset = (Math.cos(angle).toFloat() * distanceDp).dp
                    val yOffset = (Math.sin(angle).toFloat() * distanceDp).dp
                    val rotation = progress * 360f * (if (index % 2 == 0) 1f else -1f)

                    Text(
                        text = emoji,
                        fontSize = 26.sp,
                        modifier = Modifier
                            .offset(x = xOffset, y = yOffset)
                            .graphicsLayer(
                                alpha = ((1f - progress) * opacity).coerceIn(0f, 1f),
                                rotationZ = rotation
                            )
                    )
                }
            }
        }
    }
}

// === TAB 1: FOOD SEARCH TAB ===
@Composable
fun SearchFoodTab(
    viewModel: FoodLogViewModel,
    mealType: String,
    onFoodLogged: () -> Unit,
    logDate: String
) {
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val recentEntries by viewModel.recentFoods.collectAsState()

    var activeFoodForCalculation by remember { mutableStateOf<SearchResultFood?>(null) }
    var gramsString by remember { mutableStateOf("100") }

    val scrollState = rememberScrollState()
    val view = androidx.compose.ui.platform.LocalView.current
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.searchFoods(it) },
            placeholder = { Text("Search 150+ foods (e.g., Dal, Rice, Apple)...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.searchFoods("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("food_search_input")
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (query.isBlank()) {
            // SHOW RECENTS
            Text(
                text = "Recently Logged Foods",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recentEntries.distinctBy { it.foodName }.take(20).forEach { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                            .clickable {
                                activeFoodForCalculation = SearchResultFood(
                                    id = entry.id,
                                    name = entry.foodName,
                                    category = "Recent",
                                    // Reverse calculation to estimate per 100g values
                                    caloriesPer100g = if (entry.gramsConsumed > 0) (entry.caloriesConsumed * 100f) / entry.gramsConsumed else 0f,
                                    proteinPer100g = if (entry.gramsConsumed > 0) (entry.proteinGrams * 100f) / entry.gramsConsumed else 0f,
                                    carbsPer100g = if (entry.gramsConsumed > 0) (entry.carbsGrams * 100f) / entry.gramsConsumed else 0f,
                                    fatPer100g = if (entry.gramsConsumed > 0) (entry.fatGrams * 100f) / entry.gramsConsumed else 0f,
                                    defaultServingGrams = entry.gramsConsumed,
                                    isCustom = entry.isCustomFood
                                )
                                gramsString = entry.gramsConsumed.toInt().toString()
                            }
                            .padding(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.foodName, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Cal: ${entry.caloriesConsumed.toInt()} kcal | P: ${entry.proteinGrams.toInt()}g C: ${entry.carbsGrams.toInt()}g F: ${entry.fatGrams.toInt()}g",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    }
                }

                if (recentEntries.isEmpty()) {
                    EmptyState(
                        title = "No recently logged foods",
                        description = "Type in the search box above to browse the offline nutrition library.",
                        icon = Icons.Default.Search,
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                    )
                }
            }
        } else {
            // SHOW SEARCH RESULTS
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                results.forEach { food ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                            .clickable {
                                activeFoodForCalculation = food
                                gramsString = food.defaultServingGrams.toInt().toString()
                            }
                            .padding(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(food.name, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(food.category, fontSize = 9.sp) },
                                    modifier = Modifier.height(18.dp)
                                )
                            }
                            Text(
                                text = "Per 100g: ${food.caloriesPer100g.toInt()} kcal | P: ${food.proteinPer100g.toInt()}g C: ${food.carbsPer100g.toInt()}g F: ${food.fatPer100g.toInt()}g",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.AddCircle, contentDescription = "Add", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                if (results.isEmpty()) {
                    EmptyState(
                        title = "No offline matches",
                        description = "Try another food name, or use the 'Custom Creator' tab to create a permanent food template.",
                        icon = Icons.Default.Info,
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                    )
                }
            }
        }

        // CONFIRMATION DIALOG / BOTTOM SHIELD (WHEN FOOD SELECTED TO ENTER GRAMS)
        activeFoodForCalculation?.let { food ->
            AlertDialog(
                onDismissRequest = { activeFoodForCalculation = null },
                title = { Text("Log Food Intake", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "You are about to log: ${food.name}",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        OutlinedTextField(
                            value = gramsString,
                            onValueChange = { gramsString = it },
                            label = { Text("Grams Consumed (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("grams_input")
                        )

                        val grams = gramsString.toFloatOrNull() ?: 0f
                        val calculated = Calculations.calculateFoodMacros(
                            caloriesPer100g = food.caloriesPer100g,
                            proteinPer100g = food.proteinPer100g,
                            carbsPer100g = food.carbsPer100g,
                            fatPer100g = food.fatPer100g,
                            gramsConsumed = grams
                        )

                        // Real-time macro calculations preview
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("Calculated Breakdown:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Calories:")
                                    Text("${calculated.calories.toInt()} kcal", fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Protein:")
                                    Text("${calculated.protein.toInt()}g", fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Carbs:")
                                    Text("${calculated.carbs.toInt()}g", fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Fat:")
                                    Text("${calculated.fat.toInt()}g", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            com.example.util.HapticFeedbackHelper.triggerMealLogged(context, view)
                            val grams = gramsString.toFloatOrNull() ?: 100f
                            viewModel.addFoodToDiary(
                                date = logDate,
                                mealType = mealType,
                                foodName = food.name,
                                grams = grams,
                                caloriesPer100g = food.caloriesPer100g,
                                proteinPer100g = food.proteinPer100g,
                                carbsPer100g = food.carbsPer100g,
                                fatPer100g = food.fatPer100g,
                                isCustom = food.isCustom
                            )
                            activeFoodForCalculation = null
                            onFoodLogged()
                        },
                        modifier = Modifier.testTag("dialog_add_to_diary")
                    ) {
                        Text("Add to $mealType")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { activeFoodForCalculation = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

// === TAB 2: MANUAL ENTRY TAB ===
@Composable
fun ManualEntryTab(
    viewModel: FoodLogViewModel,
    mealType: String,
    onFoodLogged: () -> Unit,
    logDate: String
) {
    val view = androidx.compose.ui.platform.LocalView.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var name by remember { mutableStateOf("") }
    var caloriesStr by remember { mutableStateOf("") }
    var proteinStr by remember { mutableStateOf("") }
    var carbsStr by remember { mutableStateOf("") }
    var fatStr by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

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
                    text = "Quick Manual Logging",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Perfect for when you already know the calorie/macro values of homemade meals or branded items directly.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Food Item Name (e.g., Mom's Curry)") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("manual_food_name")
        )

        OutlinedTextField(
            value = caloriesStr,
            onValueChange = { caloriesStr = it },
            label = { Text("Calories (kcal)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("manual_food_calories")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = proteinStr,
                onValueChange = { proteinStr = it },
                label = { Text("Protein (g)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("manual_food_protein")
            )

            OutlinedTextField(
                value = carbsStr,
                onValueChange = { carbsStr = it },
                label = { Text("Carbs (g)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("manual_food_carbs")
            )

            OutlinedTextField(
                value = fatStr,
                onValueChange = { fatStr = it },
                label = { Text("Fat (g)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("manual_food_fat")
            )
        }

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Serving Description / Notes (Optional)") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("manual_food_desc")
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                com.example.util.HapticFeedbackHelper.triggerMealLogged(context, view)
                val calories = caloriesStr.toFloatOrNull() ?: 0f
                val protein = proteinStr.toFloatOrNull() ?: 0f
                val carbs = carbsStr.toFloatOrNull() ?: 0f
                val fat = fatStr.toFloatOrNull() ?: 0f

                viewModel.addManualEntry(
                    date = logDate,
                    mealType = mealType,
                    foodName = name,
                    calories = calories,
                    protein = protein,
                    carbs = carbs,
                    fat = fat,
                    servingDescription = description
                )
                onFoodLogged()
            },
            enabled = name.isNotBlank() && caloriesStr.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("manual_food_save")
        ) {
            Text("One-Tap Save to Diary", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

// === TAB 3: CUSTOM FOOD CREATOR ===
@Composable
fun CustomCreatorTab(
    viewModel: FoodLogViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val view = androidx.compose.ui.platform.LocalView.current
    var name by remember { mutableStateOf("") }
    var caloriesPer100gStr by remember { mutableStateOf("") }
    var proteinPer100gStr by remember { mutableStateOf("") }
    var carbsPer100gStr by remember { mutableStateOf("") }
    var fatPer100gStr by remember { mutableStateOf("") }
    var defaultServingStr by remember { mutableStateOf("100") }

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
                    text = "Create Custom Food Template",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Define and save custom food macro profiles per 100g. Once saved, they will permanently appear in your search results.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Template Name (e.g., Whey Complex Chocolate)") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("custom_food_name")
        )

        OutlinedTextField(
            value = caloriesPer100gStr,
            onValueChange = { caloriesPer100gStr = it },
            label = { Text("Calories per 100g (kcal)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("custom_food_calories")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = proteinPer100gStr,
                onValueChange = { proteinPer100gStr = it },
                label = { Text("Protein (g/100g)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("custom_food_protein")
            )

            OutlinedTextField(
                value = carbsPer100gStr,
                onValueChange = { carbsPer100gStr = it },
                label = { Text("Carbs (g/100g)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("custom_food_carbs")
            )

            OutlinedTextField(
                value = fatPer100gStr,
                onValueChange = { fatPer100gStr = it },
                label = { Text("Fat (g/100g)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("custom_food_fat")
            )
        }

        OutlinedTextField(
            value = defaultServingStr,
            onValueChange = { defaultServingStr = it },
            label = { Text("Default Serving Size (g)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("custom_food_serving")
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                com.example.util.HapticFeedbackHelper.triggerMealLogged(context, view)
                val calories = caloriesPer100gStr.toFloatOrNull() ?: 0f
                val protein = proteinPer100gStr.toFloatOrNull() ?: 0f
                val carbs = carbsPer100gStr.toFloatOrNull() ?: 0f
                val fat = fatPer100gStr.toFloatOrNull() ?: 0f
                val serving = defaultServingStr.toFloatOrNull() ?: 100f

                viewModel.createCustomFood(
                    name = name,
                    caloriesPer100g = calories,
                    proteinPer100g = protein,
                    carbsPer100g = carbs,
                    fatPer100g = fat,
                    defaultServingGrams = serving
                )

                Toast.makeText(context, "'$name' template saved successfully!", Toast.LENGTH_SHORT).show()
                
                // Clear fields
                name = ""
                caloriesPer100gStr = ""
                proteinPer100gStr = ""
                carbsPer100gStr = ""
                fatPer100gStr = ""
                defaultServingStr = "100"
            },
            enabled = name.isNotBlank() && caloriesPer100gStr.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("custom_food_save")
        ) {
            Text("Create Template & Save", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
