package com.example.ui.profile

import kotlin.math.roundToInt
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.UserProfile
import com.example.util.Calculations
import com.example.util.HapticFeedbackHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onProfileSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.userProfile.collectAsState()

    val context = LocalContext.current
    val view = LocalView.current

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
    var showGoogleSignInDialog by remember { mutableStateOf(false) }
    var customGoogleEmail by remember { mutableStateOf("") }

    if (showGoogleSignInDialog) {
        val savedAccounts = viewModel.getSavedAccounts(context)
        AlertDialog(
            onDismissRequest = { showGoogleSignInDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign In with Google", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Choose an account to continue to FitPal and load your previously saved profile and database logs instantly.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (savedAccounts.isNotEmpty()) {
                        Text(
                            text = "Detected Accounts:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        savedAccounts.forEach { email ->
                            Card(
                                onClick = {
                                    HapticFeedbackHelper.triggerConfirm(view)
                                    viewModel.loginWithGoogle(context, email) {
                                        showGoogleSignInDialog = false
                                        onProfileSaved()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("onboard_google_account_$email"),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = email,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    
                    Text(
                        text = "Or enter another Google account email:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = customGoogleEmail,
                        onValueChange = { customGoogleEmail = it },
                        label = { Text("Google Email") },
                        placeholder = { Text("example@gmail.com") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboard_google_custom_email")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val emailToUse = customGoogleEmail.trim()
                        if (emailToUse.isNotBlank() && emailToUse.contains("@")) {
                            HapticFeedbackHelper.triggerConfirm(view)
                            viewModel.loginWithGoogle(context, emailToUse) {
                                showGoogleSignInDialog = false
                                onProfileSaved()
                            }
                        } else {
                            android.widget.Toast.makeText(context, "Please enter a valid Google email address", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = customGoogleEmail.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("onboard_google_confirm_btn")
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showGoogleSignInDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

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
    var wizardStep by remember { mutableStateOf(1) }
    val totalSteps = 7
    val scrollState = rememberScrollState()

    // Background modern gradient
    val bgGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.background
        )
    )

    if (isNewProfile) {
        // STEP-BY-STEP WIZARD (ONBOARDING MODE)
        Scaffold(
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgGradient)
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. Top Section - Welcome / Progress
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Setup FitPal",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Step $wizardStep of $totalSteps",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        LinearProgressIndicator(
                            progress = wizardStep.toFloat() / totalSteps.toFloat(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 2. Middle Section - Interactive Step Content
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            when (wizardStep) {
                                1 -> { // STEP 1: WELCOME & NAME
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Handshake,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "Welcome to FitPal!",
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "Let's personalize your metabolic profile. First, what should we call you?",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    OutlinedTextField(
                                        value = name,
                                        onValueChange = { name = it },
                                        label = { Text("Your Name") },
                                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("profile_name_input")
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Divider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                                        Text(
                                            text = " OR ",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                        Divider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                                    }

                                    Button(
                                        onClick = {
                                            HapticFeedbackHelper.triggerLightTap(view)
                                            showGoogleSignInDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("onboard_google_sign_in_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Sign In with Google to Load Old Data", fontWeight = FontWeight.Bold)
                                    }
                                }

                                2 -> { // STEP 2: BIOLOGICAL SEX
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Wc,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "Biological Sex",
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "This is required to run standard physiological equations (like Mifflin-St Jeor) to estimate base BMR.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        val genders = listOf("Male", "Female")
                                        genders.forEach { option ->
                                            val isSelected = gender == option
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(64.dp)
                                                    .clickable {
                                                        gender = option
                                                        HapticFeedbackHelper.triggerLightTap(view)
                                                    }
                                                    .testTag("gender_${option.lowercase()}"),
                                                shape = RoundedCornerShape(16.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                ),
                                                border = BorderStroke(
                                                    width = if (isSelected) 2.dp else 1.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(horizontal = 20.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (option == "Male") Icons.Default.Male else Icons.Default.Female,
                                                        contentDescription = null,
                                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = option,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Spacer(modifier = Modifier.weight(1f))
                                                    RadioButton(
                                                        selected = isSelected,
                                                        onClick = {
                                                            gender = option
                                                            HapticFeedbackHelper.triggerLightTap(view)
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                3 -> { // STEP 3: AGE ONLY
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "How old are you?",
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "Your age is essential to correctly evaluate your basal metabolic rate.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    OutlinedTextField(
                                        value = ageStr,
                                        onValueChange = { ageStr = it },
                                        label = { Text("Age (years)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("profile_age_input")
                                    )
                                }

                                4 -> { // STEP 4: HEIGHT ONLY
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Height,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "What is your height?",
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "Your height helps us measure precise energy expenditure calculations.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    OutlinedTextField(
                                        value = heightStr,
                                        onValueChange = { heightStr = it },
                                        label = { Text("Height (cm)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("profile_height_input")
                                    )
                                }

                                5 -> { // STEP 5: CURRENT WEIGHT ONLY
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Scale,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "What is your current weight?",
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "This represents your starting scale weight for our target calorie calculations.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    OutlinedTextField(
                                        value = weightStr,
                                        onValueChange = { weightStr = it },
                                        label = { Text("Current Weight (kg)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("profile_weight_input")
                                    )
                                }

                                6 -> { // STEP 6: GOAL WEIGHT ONLY
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Flag,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "What is your target weight?",
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "What weight goal are you looking to safely maintain, lose, or gain?",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    OutlinedTextField(
                                        value = goalWeightStr,
                                        onValueChange = { goalWeightStr = it },
                                        label = { Text("Goal Weight (kg)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("profile_goal_weight_input")
                                    )
                                }

                                7 -> { // STEP 7: FITNESS GOAL, DEFICIT & CALCULATION PREVIEW
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Stars,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "Your Goal & Macros",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "We will dynamically calculate your calorie budget and macronutrient ranges.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    // Goal Selector
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
                                                    HapticFeedbackHelper.triggerLightTap(view)
                                                    when (option) {
                                                        "Lose" -> if (calorieAdjustment == 0) calorieAdjustment = 500
                                                        "Gain" -> if (calorieAdjustment == 0) calorieAdjustment = 300
                                                        "Maintain" -> calorieAdjustment = 0
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("goal_${option.lowercase()}")
                                            ) {
                                                Text(option, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    // Deficit / Surplus Slider Selector
                                    if (weightGoalType != "Maintain") {
                                        val options = if (weightGoalType == "Lose") listOf(250, 500, 750) else listOf(200, 300, 500)
                                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = if (weightGoalType == "Lose") "Calorie Deficit" else "Calorie Surplus",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                options.forEach { opt ->
                                                    val isSelected = calorieAdjustment == opt
                                                    OutlinedButton(
                                                        onClick = {
                                                            calorieAdjustment = opt
                                                            HapticFeedbackHelper.triggerLightTap(view)
                                                        },
                                                        colors = ButtonDefaults.outlinedButtonColors(
                                                            containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                                                        ),
                                                        shape = RoundedCornerShape(12.dp),
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

                                    // Optional TDEE
                                    OutlinedTextField(
                                        value = maintenanceCaloriesStr,
                                        onValueChange = { newVal ->
                                            if (newVal.all { it.isDigit() }) {
                                                maintenanceCaloriesStr = newVal
                                            }
                                        },
                                        label = { Text("TDEE (Maintenance kcal) - Optional") },
                                        placeholder = { Text("e.g. 2400") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("profile_maintenance_input"),
                                        supportingText = {
                                            Text("If left blank, we estimate TDEE from BMR.", fontSize = 10.sp)
                                        }
                                    )

                                    // Interactive Immediate Calorie/Macro Preview Box (Ensures NO 0g values!)
                                    val tempAge = ageStr.toIntOrNull() ?: 25
                                    val tempHeight = heightStr.toFloatOrNull() ?: 170f
                                    val tempWeight = weightStr.toFloatOrNull() ?: 70f
                                    val calculatedBmr = Calculations.calculateBmr(tempWeight, tempHeight, tempAge, gender)
                                    val calculatedTdee = maintenanceCaloriesStr.toIntOrNull()?.toFloat() ?: (calculatedBmr * 1.375f)
                                    val tempCalorieGoal = Calculations.calculateTargetCalorieGoal(calculatedTdee, weightGoalType, calorieAdjustment, calculatedBmr, gender)
                                    val (tempCarbs, tempProtein, tempFat) = Calculations.calculateMacrosCustom(tempCalorieGoal, tempWeight, proteinMultiplier, fatMultiplier)

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "Estimated Daily Budget",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "$tempCalorieGoal kcal",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceAround
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("Protein", style = MaterialTheme.typography.labelSmall)
                                                    Text("${tempProtein}g", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                                }
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("Carbs", style = MaterialTheme.typography.labelSmall)
                                                    Text("${tempCarbs}g", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                                }
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("Fat", style = MaterialTheme.typography.labelSmall)
                                                    Text("${tempFat}g", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 3. Bottom Section - Nav Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (wizardStep > 1) {
                            OutlinedButton(
                                onClick = {
                                    HapticFeedbackHelper.triggerLightTap(view)
                                    wizardStep--
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Back")
                            }
                        } else {
                            Spacer(modifier = Modifier.width(48.dp))
                        }

                        val isStepValid = when (wizardStep) {
                            1 -> name.isNotBlank()
                            2 -> gender.isNotBlank()
                            3 -> ageStr.toIntOrNull() != null && ageStr.toInt().let { it in 1..120 }
                            4 -> heightStr.toFloatOrNull() != null && heightStr.toFloat().let { it in 30f..300f }
                            5 -> weightStr.toFloatOrNull() != null && weightStr.toFloat().let { it in 10f..500f }
                            6 -> goalWeightStr.toFloatOrNull() != null && goalWeightStr.toFloat().let { it in 10f..500f }
                            7 -> true
                            else -> false
                        }

                        Button(
                            onClick = {
                                if (wizardStep < totalSteps) {
                                    HapticFeedbackHelper.triggerLightTap(view)
                                    wizardStep++
                                } else {
                                    // Submit
                                    val age = ageStr.toIntOrNull() ?: 25
                                    val height = heightStr.toFloatOrNull() ?: 170f
                                    val weight = weightStr.toFloatOrNull() ?: 70f
                                    val goalWeight = goalWeightStr.toFloatOrNull() ?: 70f

                                    HapticFeedbackHelper.triggerConfirm(view)
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
                            enabled = isStepValid,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("profile_save_button")
                        ) {
                            Text(
                                text = if (wizardStep < totalSteps) "Next" else "Begin Journey!",
                                fontWeight = FontWeight.Bold
                            )
                            if (wizardStep < totalSteps) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = "Next")
                            }
                        }
                    }
                }
            }
        }
    } else {
        // UNIFIED PROFILE EDITOR (EDITING MODE)
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Edit Profile", fontWeight = FontWeight.Black) },
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
                // Name Input
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

                // Age & Height
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

                // Weights
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

                // Biological Sex Select
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
                        listOf("Male", "Female").forEach { option ->
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

                // Optional TDEE
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
                        Text("Estimated from BMR if skipped.")
                    }
                )

                // Goal type selection
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
                        listOf("Lose", "Maintain", "Gain").forEach { option ->
                            val isSelected = weightGoalType == option
                            Button(
                                onClick = {
                                    weightGoalType = option
                                    when (option) {
                                        "Lose" -> if (calorieAdjustment == 0) calorieAdjustment = 500
                                        "Gain" -> if (calorieAdjustment == 0) calorieAdjustment = 300
                                        "Maintain" -> calorieAdjustment = 0
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

                // Calorie Adjustment Selector (If Lose or Gain)
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

                // Protein Multiplier
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
                        listOf(0.8f, 1.0f, 1.2f, 1.6f, 1.8f, 2.0f).forEach { opt ->
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

                // Fat Multiplier
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
                        listOf(0.5f, 0.7f, 0.8f, 1.0f, 1.2f).forEach { opt ->
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
                val saveEnabled = name.isNotBlank() && ageStr.isNotBlank() && heightStr.isNotBlank() && weightStr.isNotBlank() && goalWeightStr.isNotBlank()
                Button(
                    onClick = {
                        val age = ageStr.toIntOrNull() ?: 25
                        val height = heightStr.toFloatOrNull() ?: 170f
                        val weight = weightStr.toFloatOrNull() ?: 70f
                        val goalWeight = goalWeightStr.toFloatOrNull() ?: 70f

                        HapticFeedbackHelper.triggerConfirm(view)
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
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("profile_save_button"),
                    enabled = saveEnabled
                ) {
                    Text(
                        text = "Save Changes",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
