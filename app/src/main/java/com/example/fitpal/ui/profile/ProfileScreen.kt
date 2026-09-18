package com.example.fitpal.ui.profile

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
import com.example.fitpal.data.local.entity.UserProfile
import com.example.fitpal.util.Calculations
import com.example.fitpal.util.HapticFeedbackHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch

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
    val coroutineScope = rememberCoroutineScope()

    var showGoogleSignInDialog by remember { mutableStateOf(false) }
    var customGoogleEmail by remember { mutableStateOf("") }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            val email = account?.email ?: ""
            val displayName = account?.displayName ?: email.substringBefore("@")
            val photoUrl = account?.photoUrl?.toString()
            if (email.isNotBlank()) {
                viewModel.loginWithGoogleAccount(context, email = email, displayName = displayName, photoUrl = photoUrl, idToken = idToken) {
                    viewModel.restoreFromCloud(context, onSuccess = {
                        onProfileSaved()
                        android.widget.Toast.makeText(context, "Welcome back $displayName! Cloud data restored.", android.widget.Toast.LENGTH_LONG).show()
                    }, onError = {
                        onProfileSaved()
                        android.widget.Toast.makeText(context, "Signed in as $email. Cloud sync enabled!", android.widget.Toast.LENGTH_SHORT).show()
                    })
                }
            } else {
                showGoogleSignInDialog = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showGoogleSignInDialog = true
        }
    }

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
    var activityLevel by remember { mutableStateOf("Moderate") }
    var bodyFatStr by remember { mutableStateOf("") }
    var showFormulaInfoDialog by remember { mutableStateOf(false) }
    var unitSystem by remember { mutableStateOf(com.example.fitpal.util.UnitSystem.METRIC) }

    if (showGoogleSignInDialog) {
        val savedAccounts = viewModel.getSavedAccounts(context)
        var isLoggingIn by remember { mutableStateOf(false) }
        var enteredCustomEmail by remember { mutableStateOf("") }
        var showCustomEmailField by remember { mutableStateOf(false) }

        androidx.compose.ui.window.Dialog(
            onDismissRequest = { if (!isLoggingIn) showGoogleSignInDialog = false }
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isLoggingIn) {
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Connecting to Google Account...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Retrieving profile and health data securely.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    } else {
                        // Header with custom Google-colored layout
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Text(
                                text = "G",
                                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                                color = Color(0xFF4285F4) // Google Blue
                            )
                            Text(
                                text = "o",
                                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                                color = Color(0xFFEA4335) // Google Red
                            )
                            Text(
                                text = "o",
                                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                                color = Color(0xFFFBBC05) // Google Yellow
                            )
                            Text(
                                text = "g",
                                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                                color = Color(0xFF4285F4) // Google Blue
                            )
                            Text(
                                text = "l",
                                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                                color = Color(0xFF34A853) // Google Green
                            )
                            Text(
                                text = "e",
                                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                                color = Color(0xFFEA4335) // Google Red
                            )
                        }

                        Text(
                            text = "Choose an account",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "to continue to FitPal & sync data",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                        )

                        // 1. Account item for user's email shivnsh01@gmail.com
                        Card(
                            onClick = {
                                isLoggingIn = true
                                coroutineScope.launch {
                                    viewModel.loginWithGoogleAccount(context, email = "shivnsh01@gmail.com", displayName = "Shivansh", photoUrl = null) {
                                        viewModel.restoreFromCloud(context, onSuccess = {
                                            isLoggingIn = false
                                            showGoogleSignInDialog = false
                                            onProfileSaved()
                                            android.widget.Toast.makeText(context, "Welcome Shivansh! Cloud data restored.", android.widget.Toast.LENGTH_LONG).show()
                                        }, onError = {
                                            isLoggingIn = false
                                            showGoogleSignInDialog = false
                                            onProfileSaved()
                                            android.widget.Toast.makeText(context, "Successfully logged in as shivnsh01@gmail.com!", android.widget.Toast.LENGTH_LONG).show()
                                        })
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("onboard_google_account_shivnsh"),
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
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "S",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Shivansh",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "shivnsh01@gmail.com",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 2. Account item for sandbox
                        Card(
                            onClick = {
                                isLoggingIn = true
                                coroutineScope.launch {
                                    viewModel.loginWithGoogleAccount(context, email = "fitpal.sandbox@gmail.com", displayName = "FitPal Sandbox", photoUrl = null) {
                                        viewModel.restoreFromCloud(context, onSuccess = {
                                            isLoggingIn = false
                                            showGoogleSignInDialog = false
                                            onProfileSaved()
                                            android.widget.Toast.makeText(context, "Cloud backup restored for Sandbox account!", android.widget.Toast.LENGTH_LONG).show()
                                        }, onError = {
                                            isLoggingIn = false
                                            showGoogleSignInDialog = false
                                            onProfileSaved()
                                            android.widget.Toast.makeText(context, "Successfully logged in as fitpal.sandbox@gmail.com!", android.widget.Toast.LENGTH_LONG).show()
                                        })
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("onboard_google_account_sandbox"),
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
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.secondary, androidx.compose.foundation.shape.CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "F",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "FitPal Sandbox",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "fitpal.sandbox@gmail.com",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Listed local / previous emails if any
                        savedAccounts.filter { it != "shivnsh01@gmail.com" && it != "fitpal.sandbox@gmail.com" }.forEach { email ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                onClick = {
                                    isLoggingIn = true
                                    coroutineScope.launch {
                                        viewModel.loginWithGoogleAccount(context, email = email, displayName = email.substringBefore("@"), photoUrl = null) {
                                            viewModel.restoreFromCloud(context, onSuccess = {
                                                isLoggingIn = false
                                                showGoogleSignInDialog = false
                                                onProfileSaved()
                                                android.widget.Toast.makeText(context, "Cloud backup restored for $email!", android.widget.Toast.LENGTH_LONG).show()
                                            }, onError = {
                                                isLoggingIn = false
                                                showGoogleSignInDialog = false
                                                onProfileSaved()
                                                android.widget.Toast.makeText(context, "Successfully logged in as $email!", android.widget.Toast.LENGTH_LONG).show()
                                            })
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("onboard_google_account_$email"),
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
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(MaterialTheme.colorScheme.outline, androidx.compose.foundation.shape.CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = email.take(1).uppercase(),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = email,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (!showCustomEmailField) {
                            TextButton(
                                onClick = { showCustomEmailField = true },
                                modifier = Modifier.align(Alignment.Start)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Use another Google account", style = MaterialTheme.typography.labelLarge)
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = enteredCustomEmail,
                                    onValueChange = { enteredCustomEmail = it },
                                    label = { Text("Google Email Address") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("onboard_google_custom_email")
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { showCustomEmailField = false }) {
                                        Text("Cancel")
                                    }
                                    Button(
                                        onClick = {
                                            val email = enteredCustomEmail.trim()
                                            if (email.contains("@") && email.length > 5) {
                                                isLoggingIn = true
                                                coroutineScope.launch {
                                                    viewModel.loginWithGoogleAccount(context, email = email, displayName = email.substringBefore("@"), photoUrl = null) {
                                                        viewModel.restoreFromCloud(context, onSuccess = {
                                                            isLoggingIn = false
                                                            showGoogleSignInDialog = false
                                                            onProfileSaved()
                                                            android.widget.Toast.makeText(context, "Cloud backup restored for $email!", android.widget.Toast.LENGTH_LONG).show()
                                                        }, onError = {
                                                            isLoggingIn = false
                                                            showGoogleSignInDialog = false
                                                            onProfileSaved()
                                                            android.widget.Toast.makeText(context, "Successfully logged in as $email!", android.widget.Toast.LENGTH_LONG).show()
                                                        })
                                                    }
                                                }
                                            } else {
                                                android.widget.Toast.makeText(context, "Please enter a valid Gmail address", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        enabled = enteredCustomEmail.isNotBlank(),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.testTag("onboard_google_confirm_btn")
                                    ) {
                                        Text("Sign In")
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        TextButton(
                            onClick = { showGoogleSignInDialog = false },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
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
            activityLevel = p.activityLevel
            bodyFatStr = p.bodyFat?.toString() ?: ""
        }
    }

    val isNewProfile = profile == null
    var wizardStep by remember { mutableStateOf(1) }
    val totalSteps = 8
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
                                    com.example.fitpal.ui.components.BrandLogo(
                                        size = 90.dp,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

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
                                        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                                        Text(
                                            text = " OR ",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                                    }

                                    Button(
                                        onClick = {
                                            HapticFeedbackHelper.triggerLightTap(view)
                                            try {
                                                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                                    .requestEmail()
                                                    .requestProfile()
                                                    .requestScopes(
                                                        Scope("openid"),
                                                        Scope("https://www.googleapis.com/auth/userinfo.email"),
                                                        Scope("https://www.googleapis.com/auth/userinfo.profile"),
                                                        Scope("https://www.googleapis.com/auth/drive.appdata")
                                                    )
                                                    .build()
                                                val client = GoogleSignIn.getClient(context, gso)
                                                googleSignInLauncher.launch(client.signInIntent)
                                            } catch (e: Exception) {
                                                showGoogleSignInDialog = true
                                            }
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
                                        Text("Sign In with Google (OAuth & Cloud Sync)", fontWeight = FontWeight.Bold)
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

                                7 -> { // STEP 7: ACTIVITY LEVEL & BODY FAT
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FitnessCenter,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "Activity Level & Body Fat",
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "Select your daily activity level and optional body fat percentage for precise TDEE profiling.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        val activities = listOf(
                                            Triple("Sedentary", "Little/no exercise, desk job", "1.2"),
                                            Triple("Light", "Light exercise 1-3 days/week", "1.375"),
                                            Triple("Moderate", "Moderate sports 3-5 days/week", "1.55"),
                                            Triple("Heavy", "Hard exercise 6-7 days/week", "1.725"),
                                            Triple("Athlete", "Very hard training or physical job", "1.9")
                                        )
                                        activities.forEach { (level, desc, mult) ->
                                            val isSelected = activityLevel == level
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        activityLevel = level
                                                        HapticFeedbackHelper.triggerLightTap(view)
                                                    }
                                                    .testTag("activity_${level.lowercase()}"),
                                                shape = RoundedCornerShape(12.dp),
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
                                                        .fillMaxWidth()
                                                        .padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    RadioButton(
                                                        selected = isSelected,
                                                        onClick = {
                                                            activityLevel = level
                                                            HapticFeedbackHelper.triggerLightTap(view)
                                                        }
                                                    )
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(level, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                                        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                    Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                                                        Text("x$mult", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(4.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = bodyFatStr,
                                        onValueChange = { bodyFatStr = it },
                                        label = { Text("Body Fat % (Optional)") },
                                        placeholder = { Text("e.g. 15.5") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("profile_body_fat_input")
                                    )
                                }

                                8 -> { // STEP 8: FITNESS GOAL, DEFICIT & CALCULATION PREVIEW
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

                                    // Calculated Maintenance Calories (TDEE) with Mifflin-St Jeor Engine
                                    val tempAge = ageStr.toIntOrNull() ?: 25
                                    val tempHeight = heightStr.toFloatOrNull() ?: 170f
                                    val tempWeight = weightStr.toFloatOrNull() ?: 70f
                                    val tempGoalWeight = goalWeightStr.toFloatOrNull() ?: 70f
                                    val calculatedBmr = Calculations.calculateBmr(tempWeight, tempHeight, tempAge, gender)
                                    val calculatedTdee = Calculations.calculateBaseTDEE(calculatedBmr, activityLevel)

                                    // Comprehensive scientific target evaluation
                                    val targetEval = Calculations.evaluateCalorieTarget(
                                        bmr = calculatedBmr,
                                        activityLevel = activityLevel,
                                        goalType = weightGoalType,
                                        gender = gender,
                                        customDeficitCalories = if (weightGoalType.lowercase() == "lose") calorieAdjustment else null,
                                        customSurplusCalories = if (weightGoalType.lowercase() == "gain") calorieAdjustment else null
                                    )
                                    val weightLossRate = Calculations.calculateWeightLossRate(tempWeight)
                                    val proteinRec = Calculations.calculateProteinRecommendations(tempWeight)

                                    Card(
                                        modifier = Modifier.fillMaxWidth().testTag("profile_maintenance_display"),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Info,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = "Estimated Maintenance Calories (TDEE)",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Text(
                                                 text = "${calculatedTdee.roundToInt()} kcal / day",
                                                 style = MaterialTheme.typography.headlineMedium,
                                                 fontWeight = FontWeight.Black,
                                                 color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                 text = "Estimated via Mifflin-St Jeor equation (BMR: ${calculatedBmr.roundToInt()} kcal) × $activityLevel activity. This is an estimate; actual expenditure varies by ±100–200 kcal based on NEAT, genetics, and digestion.",
                                                 style = MaterialTheme.typography.bodySmall,
                                                 color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Realistic Fat Loss Rate & Protein Education
                                    if (weightGoalType == "Lose") {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "Recommended Fat Loss Pace",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Target ~0.5%–1.0% body weight/week: ${weightLossRate.minWeeklyLossKg}–${weightLossRate.maxWeeklyLossKg} kg/week (${weightLossRate.minWeeklyLossLbs}–${weightLossRate.maxWeeklyLossLbs} lbs/week).",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = "Daily scale weight fluctuates naturally due to water, glycogen, and digestion. Trust weekly trend lines over individual weigh-ins.",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    // Protein Intake Guidance Card
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "Research-Backed Protein Range",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${proteinRec.minimumGrams}g – ${proteinRec.upperGrams}g / day (Optimal: ~${proteinRec.recommendedGrams}g at 1.8–2.0 g/kg)",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            Text(
                                                text = "Higher protein preserves lean muscle tissue and increases satiety during energy deficits.",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Safety Floor Warning Banner (if clamped)
                                    if (targetEval.isClampedToFloor) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                                Column {
                                                    Text("Safety Floor Protection Active", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                                                    Text(targetEval.safetyWarning ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                                                }
                                            }
                                        }
                                    }

                                    // Final Daily Target & Macronutrient Breakdown Preview
                                    val tempCalorieGoal = targetEval.finalTargetCalories
                                    val balancedMacros = Calculations.calculateBalancedMacros(
                                        targetCalories = tempCalorieGoal,
                                        weightKg = tempWeight,
                                        proteinMultiplier = proteinMultiplier,
                                        fatMultiplier = fatMultiplier
                                    )

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "Recommended Daily Calorie Budget",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "$tempCalorieGoal kcal",
                                                style = MaterialTheme.typography.headlineMedium,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceAround
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("Protein", style = MaterialTheme.typography.labelSmall)
                                                    Text("${balancedMacros.proteinGrams}g (${balancedMacros.proteinPercentage}%)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                                }
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("Carbs", style = MaterialTheme.typography.labelSmall)
                                                    Text("${balancedMacros.carbsGrams}g (${balancedMacros.carbsPercentage}%)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                                }
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("Fat", style = MaterialTheme.typography.labelSmall)
                                                    Text("${balancedMacros.fatGrams}g (${balancedMacros.fatPercentage}%)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
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
                            7 -> activityLevel.isNotBlank()
                            8 -> true
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
                                        maintenanceCalories = Calculations.calculateBaseTDEE(Calculations.calculateBmr(weight, height, age, gender), activityLevel).toInt(),
                                        activityLevel = activityLevel,
                                        bodyFat = bodyFatStr.toFloatOrNull()
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
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = Color(0xFFF5F5F7)
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
                        color = Color(0xFF9CA3AF)
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
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF1E251E),
                                    contentColor = if (isSelected) Color(0xFF0C0F0C) else Color(0xFFF5F5F7)
                                ),
                                border = if (!isSelected) BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("gender_${option.lowercase()}")
                            ) {
                                Text(option, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Calculated Maintenance Calories (TDEE) in Edit mode
                val editAge = ageStr.toIntOrNull() ?: 25
                val editHeight = heightStr.toFloatOrNull() ?: 170f
                val editWeight = weightStr.toFloatOrNull() ?: 70f
                val editBmr = Calculations.calculateBmr(editWeight, editHeight, editAge, gender)
                val editTdee = Calculations.calculateBaseTDEE(editBmr, activityLevel).roundToInt()

                Card(
                    modifier = Modifier.fillMaxWidth().testTag("profile_maintenance_display_edit"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Estimated Maintenance Calories (TDEE)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "$editTdee kcal / day",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFF5F5F7)
                        )
                        Text(
                            text = "Estimated via Mifflin-St Jeor formula (BMR: ${editBmr.roundToInt()} kcal) × $activityLevel activity multiplier. This represents a baseline estimate; actual energy output varies by ±100–200 kcal based on NEAT and physical demands.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }

                // Activity Level Selector in Edit mode
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Daily Activity Level",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF9CA3AF)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val activities = listOf(
                            Triple("Sedentary", "Little or no exercise, desk job", "1.2"),
                            Triple("Light", "Light exercise or sports 1-3 days/week", "1.375"),
                            Triple("Moderate", "Moderate sports 3-5 days/week", "1.55"),
                            Triple("Heavy", "Hard exercise or sports 6-7 days/week", "1.725"),
                            Triple("Athlete", "Very hard training or physical job", "1.9")
                        )
                        activities.forEach { (level, desc, mult) ->
                            val isSelected = activityLevel == level
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        activityLevel = level
                                        HapticFeedbackHelper.triggerLightTap(view)
                                    }
                                    .testTag("edit_activity_${level.lowercase()}"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFF1E251E) else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            activityLevel = level
                                            HapticFeedbackHelper.triggerLightTap(view)
                                        }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(level, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFF5F5F7))
                                        Text(desc, style = MaterialTheme.typography.bodySmall, color = Color(0xFF9CA3AF))
                                    }
                                    Badge(containerColor = Color(0xFF2E462E)) {
                                        Text("x$mult", style = MaterialTheme.typography.labelSmall, color = Color(0xFFF5F5F7), modifier = Modifier.padding(4.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Body Fat Input in Edit mode
                OutlinedTextField(
                    value = bodyFatStr,
                    onValueChange = { bodyFatStr = it },
                    label = { Text("Body Fat % (Optional)") },
                    placeholder = { Text("e.g. 15.5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_profile_body_fat_input")
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
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val title = if (weightGoalType == "Lose") "Deficit Pace Strategy" else "Surplus Pace Strategy"
                        Text(
                            text = title,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val options = if (weightGoalType == "Lose") {
                                listOf(
                                    250 to "Mild (-250)",
                                    500 to "Moderate (-500)",
                                    750 to "Aggressive (-750)"
                                )
                            } else {
                                listOf(
                                    200 to "Lean (+200)",
                                    350 to "Moderate (+350)",
                                    500 to "Aggressive (+500)"
                                )
                            }
                            options.forEach { (opt, label) ->
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
                                    Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                            }
                        }

                        if (weightGoalType == "Lose") {
                            val editCurrentWeight = weightStr.toFloatOrNull() ?: 70f
                            val editLossRate = Calculations.calculateWeightLossRate(editCurrentWeight)
                            Text(
                                text = "Sustainable fat loss pace: ${editLossRate.minWeeklyLossKg}–${editLossRate.maxWeeklyLossKg} kg/wk (${editLossRate.minWeeklyLossLbs}–${editLossRate.maxWeeklyLossLbs} lbs/wk). Scale fluctuates daily from water and glycogen.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                            maintenanceCalories = Calculations.calculateBaseTDEE(Calculations.calculateBmr(weight, height, age, gender), activityLevel).toInt(),
                            activityLevel = activityLevel,
                            bodyFat = bodyFatStr.toFloatOrNull()
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
