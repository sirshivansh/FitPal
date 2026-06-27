package com.example.ui.profile

import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.UserProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onProfileSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.userProfile.collectAsState()

    var name by remember { mutableStateOf("") }
    var ageStr by remember { mutableStateOf("") }
    var heightStr by remember { mutableStateOf("") }
    var weightStr by remember { mutableStateOf("") }
    var goalWeightStr by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var weightGoalType by remember { mutableStateOf("Maintain") }
    var calorieAdjustment by remember { mutableStateOf(0) }
    var proteinMultiplier by remember { mutableStateOf(1.6f) }
    var fatMultiplier by remember { mutableStateOf(0.8f) }
    var maintenanceCaloriesStr by remember { mutableStateOf("") }
    var showFormulaInfoDialog by remember { mutableStateOf(false) }

    // Dynamic state loading when profile exists
    LaunchedEffect(profile) {
        profile?.let { p ->
            name = p.name
            ageStr = p.age.toString()
            heightStr = p.heightCm.toInt().toString()
            weightStr = p.currentWeightKg.toString()
            goalWeightStr = p.goalWeightKg.toString()
            gender = p.gender.replaceFirstChar { it.uppercase() }
            weightGoalType = p.weightGoalType.replaceFirstChar { it.uppercase() }
            calorieAdjustment = p.calorieAdjustment
            proteinMultiplier = p.proteinMultiplier
            fatMultiplier = p.fatMultiplier
            maintenanceCaloriesStr = p.maintenanceCalories?.toString() ?: ""
        }
    }

    val isNewProfile = profile == null
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isNewProfile) "Welcome to FitPal!" else "Edit Profile",
                        fontWeight = FontWeight.Bold
                    )
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
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isNewProfile) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Setup your metabolic needs",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Get personalized, accurate calorie goals.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        IconButton(
                            onClick = { showFormulaInfoDialog = true },
                            modifier = Modifier.testTag("onboarding_formula_info_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Formula Details",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // NAME
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Your Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_name_input")
            )

            // AGE, HEIGHT, WEIGHT (GRID)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = ageStr,
                    onValueChange = { ageStr = it },
                    label = { Text("Age") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("profile_age_input")
                )

                OutlinedTextField(
                    value = heightStr,
                    onValueChange = { heightStr = it },
                    label = { Text("Height (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("profile_height_input")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = weightStr,
                    onValueChange = { weightStr = it },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("profile_weight_input")
                )

                OutlinedTextField(
                    value = goalWeightStr,
                    onValueChange = { goalWeightStr = it },
                    label = { Text("Goal (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("profile_goal_weight_input")
                )
            }

            // GENDER SELECTOR
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Biological Sex",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val genders = listOf("Male", "Female")
                    genders.forEach { option ->
                        val isSelected = gender == option
                        Button(
                            onClick = { gender = option },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("gender_${option.lowercase()}")
                        ) {
                            Text(option)
                        }
                    }
                }
            }

            // MAINTENANCE CALORIES (TDEE)
            OutlinedTextField(
                value = maintenanceCaloriesStr,
                onValueChange = { newVal ->
                    if (newVal.all { it.isDigit() }) {
                        maintenanceCaloriesStr = newVal
                    }
                },
                label = { Text("Maintenance Calories (TDEE) - Optional") },
                placeholder = { Text("e.g. 2500 kcal") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_maintenance_input"),
                supportingText = {
                    Text("If skipped, BMR is shown and you can enter TDEE later on the dashboard.")
                }
            )

            // WEIGHT GOAL TYPE
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Your Goal",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val goals = listOf("Lose", "Maintain", "Gain")
                    goals.forEach { option ->
                        val isSelected = weightGoalType == option
                        Button(
                            onClick = {
                                weightGoalType = option
                                when (option) {
                                    "Lose" -> {
                                        if (calorieAdjustment != 250 && calorieAdjustment != 500 && calorieAdjustment != 750) {
                                            calorieAdjustment = 500
                                        }
                                    }
                                    "Gain" -> {
                                        if (calorieAdjustment != 200 && calorieAdjustment != 300 && calorieAdjustment != 500) {
                                            calorieAdjustment = 300
                                        }
                                    }
                                    "Maintain" -> {
                                        calorieAdjustment = 0
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("goal_${option.lowercase()}")
                        ) {
                            Text(option, fontSize = 12.sp)
                        }
                    }
                }
            }

            // CALORIE ADJUSTMENT SELECTOR (If Lose or Gain)
            if (weightGoalType != "Maintain") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val title = if (weightGoalType == "Lose") "Daily Calorie Deficit" else "Daily Calorie Surplus"
                    Text(
                        text = title,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val options = if (weightGoalType == "Lose") listOf(250, 500, 750) else listOf(200, 300, 500)
                        options.forEach { opt ->
                            val isSelected = calorieAdjustment == opt
                            OutlinedButton(
                                onClick = { calorieAdjustment = opt },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("adj_${opt}")
                            ) {
                                Text("${opt} kcal", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // PROTEIN MULTIPLIER SELECTOR
            val currentWeight = weightStr.toFloatOrNull() ?: 70f
            val calculatedProtein = (currentWeight * proteinMultiplier).roundToInt()
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Protein Multiplier (g/kg)",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Target: ${calculatedProtein}g",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val proteinOptions = listOf(0.8f, 1.0f, 1.2f, 1.6f, 1.8f, 2.0f)
                    proteinOptions.forEach { opt ->
                        val isSelected = proteinMultiplier == opt
                        OutlinedButton(
                            onClick = { proteinMultiplier = opt },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("protein_mult_${opt}"),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text("${opt}x", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // FAT MULTIPLIER SELECTOR
            val calculatedFat = (currentWeight * fatMultiplier).roundToInt()
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Fat Multiplier (g/kg)",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Target: ${calculatedFat}g",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val fatOptions = listOf(0.5f, 0.7f, 0.8f, 1.0f, 1.2f)
                    fatOptions.forEach { opt ->
                        val isSelected = fatMultiplier == opt
                        OutlinedButton(
                            onClick = { fatMultiplier = opt },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("fat_mult_${opt}"),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text("${opt}x", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SAVE BUTTON
            val view = androidx.compose.ui.platform.LocalView.current
            Button(
                onClick = {
                    val age = ageStr.toIntOrNull() ?: 25
                    val height = heightStr.toFloatOrNull() ?: 170f
                    val weight = weightStr.toFloatOrNull() ?: 70f
                    val goalWeight = goalWeightStr.toFloatOrNull() ?: 70f

                    if (name.isNotBlank()) {
                        com.example.util.HapticFeedbackHelper.triggerConfirm(view)
                        viewModel.createOrUpdateProfile(
                            name = name,
                            age = age,
                            gender = gender,
                            heightCm = height,
                            currentWeightKg = weight,
                            goalWeightKg = goalWeight,
                            weightGoalType = weightGoalType,
                            calorieAdjustment = calorieAdjustment,
                            proteinMultiplier = proteinMultiplier,
                            fatMultiplier = fatMultiplier,
                            maintenanceCalories = maintenanceCaloriesStr.toIntOrNull()
                        )
                        onProfileSaved()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("profile_save_button"),
                enabled = name.isNotBlank() && ageStr.isNotBlank() && heightStr.isNotBlank() && weightStr.isNotBlank() && goalWeightStr.isNotBlank()
            ) {
                Text(
                    text = if (isNewProfile) "Calculate & Begin!" else "Save Changes",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }

    if (showFormulaInfoDialog) {
        AlertDialog(
            onDismissRequest = { showFormulaInfoDialog = false },
            icon = { Icon(Icons.Default.Functions, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Mifflin-St Jeor Equation", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "We use the gold-standard Mifflin-St Jeor equation to precisely estimate your Basal Metabolic Rate (BMR):",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Men:\nBMR = 10 × weight(kg) + 6.25 × height(cm) - 5 × age(y) + 5",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                            Text(
                                text = "Women:\nBMR = 10 × weight(kg) + 6.25 × height(cm) - 5 × age(y) - 161",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Text(
                        text = "This calculates your rest energy requirements. Your goal adjustments are then securely calculated offline to provide accurate macros.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showFormulaInfoDialog = false }) {
                    Text("Got It", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
