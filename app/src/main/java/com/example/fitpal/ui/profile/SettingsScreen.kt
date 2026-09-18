package com.example.fitpal.ui.profile

import kotlin.math.roundToInt
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import com.example.fitpal.notification.NotificationHelper
import com.example.fitpal.ui.components.ConfirmationDialog
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    profileViewModel: ProfileViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profile by profileViewModel.userProfile.collectAsState()
    val scrollState = rememberScrollState()

    var showGoogleChooserDialog by remember { mutableStateOf(false) }

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
                profileViewModel.loginWithGoogleAccount(context, email = email, displayName = displayName, photoUrl = photoUrl, idToken = idToken) {
                    profileViewModel.restoreFromCloud(context, onSuccess = {
                        Toast.makeText(context, "Welcome $displayName! Cloud data restored.", Toast.LENGTH_LONG).show()
                    }, onError = {
                        Toast.makeText(context, "Signed in as $email. Cloud sync enabled.", Toast.LENGTH_SHORT).show()
                    })
                }
            } else {
                showGoogleChooserDialog = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showGoogleChooserDialog = true
        }
    }

    var calorieGoalStr by remember { mutableStateOf("") }
    var proteinMultiplier by remember { mutableStateOf(1.8f) }
    var fatMultiplier by remember { mutableStateOf(0.8f) }

    var showClearConfirm by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    val view = androidx.compose.ui.platform.LocalView.current

    val cloudEmail by profileViewModel.cloudUserEmail.collectAsState()
    val lastSyncTime by profileViewModel.lastSyncTime.collectAsState()
    val isSyncing by profileViewModel.isSyncing.collectAsState()
    val hasCloudBackupAvailable by profileViewModel.hasCloudBackupAvailable.collectAsState()

    var loginEmailInput by remember { mutableStateOf("") }
    var loginPassInput by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    var showOtpDialog by remember { mutableStateOf(false) }
    var generatedOtp by remember { mutableStateOf("") }
    var otpEmailTarget by remember { mutableStateOf("") }
    var otpPasswordTarget by remember { mutableStateOf("") }
    var userEnteredOtp by remember { mutableStateOf("") }
    var isSendingOtp by remember { mutableStateOf(false) }
    var otpErrorText by remember { mutableStateOf("") }
    var otpCountdown by remember { mutableStateOf(60) }

    LaunchedEffect(showOtpDialog, otpCountdown) {
        if (showOtpDialog && otpCountdown > 0) {
            kotlinx.coroutines.delay(1000L)
            otpCountdown -= 1
        }
    }

    var isPasscodeEnabled by remember { mutableStateOf(com.example.fitpal.util.SecurityUtils.isPinSet(context)) }
    var showSetPinDialog by remember { mutableStateOf(false) }
    var showVerifyDisablePinDialog by remember { mutableStateOf(false) }

    LaunchedEffect(profile) {
        profile?.let { p ->
            val targetGoal = if (p.customCalorieGoal != null && p.customCalorieGoal > 0) {
                p.customCalorieGoal
            } else {
                val baseCalories = if (p.maintenanceCalories != null && p.maintenanceCalories > 0) {
                    p.maintenanceCalories.toFloat()
                } else {
                    com.example.fitpal.util.Calculations.calculateBaseTDEE(p.bmr, p.activityLevel)
                }
                com.example.fitpal.util.Calculations.calculateTargetCalorieGoal(
                    dailyCalories = baseCalories,
                    weightGoalType = p.weightGoalType,
                    adjustmentValue = p.calorieAdjustment,
                    bmr = p.bmr,
                    gender = p.gender
                )
            }
            calorieGoalStr = targetGoal.toString()
            proteinMultiplier = p.proteinMultiplier
            fatMultiplier = p.fatMultiplier
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Customization", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
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
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // SECTION 1: AUTOMATIC CALORIE TARGET (READ-ONLY)
            Card(
                modifier = Modifier.fillMaxWidth().testTag("settings_automatic_calories_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Automatic Calorie Target",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    profile?.let { p ->
                        val bmrVal = p.bmr.roundToInt()
                        val maintenanceVal = p.maintenanceCalories ?: 0
                        val adjustmentVal = p.calorieAdjustment
                        val goalType = p.weightGoalType
                        val dailyTarget = if (p.maintenanceCalories != null && p.maintenanceCalories > 0) {
                            val base = p.maintenanceCalories.toFloat()
                            val goalAdj = when (goalType.lowercase()) {
                                "lose", "lose fat" -> -adjustmentVal.toFloat()
                                "gain", "gain muscle" -> adjustmentVal.toFloat()
                                else -> 0f
                            }
                            (base + goalAdj).roundToInt().coerceAtLeast(1200)
                        } else {
                            2000
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "$dailyTarget kcal / day",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            Text(
                                text = "FitPal automatically calculates your calorie target from your biological details and activity profile to ensure safe, scientific progress. Manual input overrides are disabled.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Basal Metabolic Rate (BMR)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$bmrVal kcal", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Active Maintenance (TDEE)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$maintenanceVal kcal", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val adjSign = when (goalType.lowercase()) {
                                    "lose", "lose fat" -> "-"
                                    "gain", "gain muscle" -> "+"
                                    else -> ""
                                }
                                Text("Goal Adjustment ($goalType)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$adjSign$adjustmentVal kcal", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (adjSign == "-") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                            }
                        }
                    } ?: run {
                        Text(
                            text = "Please complete your profile configuration first.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // SECTION 2: MACRONUTRIENT SPLIT (CUSTOM MULTIPLIERS)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Custom Macro Targets (g/kg)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Choose the protein and fat multipliers per kilogram of body weight. Carbohydrates will automatically receive all remaining daily calories.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    val currentWeight = profile?.currentWeightKg ?: 70f
                    val calculatedProtein = (currentWeight * proteinMultiplier).roundToInt()
                    // Protein multiplier row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Protein Multiplier (g/kg)",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
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
                                    .testTag("settings_protein_mult_${opt}"),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Text("${opt}x", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Fat multiplier row
                    val calculatedFat = (currentWeight * fatMultiplier).roundToInt()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Fat Multiplier (g/kg)",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
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
                                    .testTag("settings_fat_mult_${opt}"),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Text("${opt}x", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            com.example.fitpal.util.HapticFeedbackHelper.triggerConfirm(view)
                            profileViewModel.updateCalculations(
                                calorieAdjustment = profile?.calorieAdjustment ?: 0,
                                proteinMultiplier = proteinMultiplier,
                                fatMultiplier = fatMultiplier
                            )
                            Toast.makeText(context, "Macro multipliers updated!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_save_macros")
                    ) {
                        Text("Apply Multipliers")
                    }
                }
            }

            // SECTION 3: CLOUD ACCOUNT & BACKUP SYNCHRONIZATION
            Card(
                modifier = Modifier.fillMaxWidth().testTag("settings_cloud_sync_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (cloudEmail != null) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                            contentDescription = null,
                            tint = if (cloudEmail != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Cloud Account & Backup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (cloudEmail == null) {
                        Text(
                            text = "Access your profile, meals, weights, and water logs on any device. Sign in with Google or create a dedicated FitPal account.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = {
                                com.example.fitpal.util.HapticFeedbackHelper.triggerLightTap(view)
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
                                    showGoogleChooserDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_google_signin_button")
                        ) {
                            Text("G", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign In with Google", fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                            Text(
                                text = " OR USE EMAIL ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                        }

                        OutlinedTextField(
                            value = loginEmailInput,
                            onValueChange = { loginEmailInput = it },
                            label = { Text("Email Address") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("settings_cloud_email")
                        )

                        OutlinedTextField(
                            value = loginPassInput,
                            onValueChange = { loginPassInput = it },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth().testTag("settings_cloud_password")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (loginEmailInput.isBlank() || loginPassInput.isBlank()) {
                                        Toast.makeText(context, "Please fill in email and password", Toast.LENGTH_SHORT).show()
                                    } else {
                                        profileViewModel.loginWithEmail(
                                            context,
                                            loginEmailInput.trim(),
                                            loginPassInput,
                                            onSuccess = {
                                                Toast.makeText(context, "Welcome back! Data restored successfully.", Toast.LENGTH_LONG).show()
                                            },
                                            onError = { err ->
                                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("settings_sign_in_btn")
                            ) {
                                Text("Sign In")
                            }

                            OutlinedButton(
                                onClick = {
                                    if (loginEmailInput.isBlank() || loginPassInput.isBlank()) {
                                        Toast.makeText(context, "Please fill in email and password", Toast.LENGTH_SHORT).show()
                                    } else if (!loginEmailInput.contains("@") || loginEmailInput.length < 5) {
                                        Toast.makeText(context, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val email = loginEmailInput.trim()
                                        val pass = loginPassInput
                                        val savedAccounts = profileViewModel.getSavedAccounts(context)
                                        if (savedAccounts.contains(email)) {
                                            Toast.makeText(context, "Account with this email already exists.", Toast.LENGTH_LONG).show()
                                        } else {
                                            com.example.fitpal.util.HapticFeedbackHelper.triggerConfirm(view)
                                            profileViewModel.registerWithEmail(
                                                context,
                                                email,
                                                pass,
                                                onSuccess = {
                                                    Toast.makeText(context, "Account created & verified successfully!", Toast.LENGTH_LONG).show()
                                                },
                                                onError = { err ->
                                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("settings_register_btn")
                            ) {
                                Text("Register")
                            }
                        }

                    } else {
                        // Logged in state
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Connected Account",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = cloudEmail ?: "",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Status: ${lastSyncTime ?: "Never synced"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    profileViewModel.triggerSync(
                                        context,
                                        onSuccess = {
                                            Toast.makeText(context, "Database backup synced to cloud successfully!", Toast.LENGTH_SHORT).show()
                                        },
                                        onError = { err ->
                                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                },
                                enabled = !isSyncing,
                                modifier = Modifier.weight(1f).testTag("settings_sync_now_btn")
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sync Now")
                                }
                            }

                            Button(
                                onClick = {
                                    profileViewModel.restoreFromCloud(
                                        context,
                                        onSuccess = {
                                            Toast.makeText(context, "Cloud data restored successfully!", Toast.LENGTH_SHORT).show()
                                        },
                                        onError = { err ->
                                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                },
                                enabled = !isSyncing,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                modifier = Modifier.weight(1f).testTag("settings_restore_cloud_btn")
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Restore")
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                profileViewModel.logout()
                                Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().testTag("settings_logout_btn")
                        ) {
                            Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Log Out")
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "💡 Developer Integration Note: FitPal simulates standard secure OAuth authentication and JSON synchronization locally. To attach this to a production Google Sign-In backend or cloud Firestore DB directly, configure a 'google-services.json' in your /app directory and rebuild.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            // SECTION: REMINDERS & NOTIFICATIONS
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Reminders & Notifications",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enable daily logging notifications to keep up with your fitness and diet goals.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    var remindersEnabled by remember { mutableStateOf(true) }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Daily Reminders", fontWeight = FontWeight.SemiBold)
                            Text("At 8:00 PM every evening", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = remindersEnabled,
                            onCheckedChange = { isChecked ->
                                remindersEnabled = isChecked
                                if (isChecked) {
                                    NotificationHelper.scheduleDailyReminder(context, 20, 0)
                                    Toast.makeText(context, "Daily 8 PM reminders enabled!", Toast.LENGTH_SHORT).show()
                                } else {
                                    NotificationHelper.cancelDailyReminder(context)
                                    Toast.makeText(context, "Daily reminders disabled.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("settings_notifications_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        if (isGranted) {
                            NotificationHelper.showTestNotification(context)
                            Toast.makeText(context, "Permission granted! Test notification sent.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Notifications permission denied. Please enable in Settings.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    Button(
                        onClick = {
                            com.example.fitpal.util.HapticFeedbackHelper.triggerConfirm(view)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasPermission) {
                                    NotificationHelper.showTestNotification(context)
                                    Toast.makeText(context, "Test notification sent!", Toast.LENGTH_SHORT).show()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            } else {
                                NotificationHelper.showTestNotification(context)
                                Toast.makeText(context, "Test notification sent!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_test_notification_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Test Notification")
                    }
                }
            }

            // SECTION: VISUAL STYLE & ACCESSIBILITY (FONT AND SIZES)
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Visual Style & Accessibility",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Customize the font family and text sizes across the whole application to suit your preferences.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Font Family Selector
                    Text(
                        text = "Font Family",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val fonts = listOf(
                        "SansSerif" to "Default",
                        "Serif" to "Serif",
                        "Monospace" to "Monospace",
                        "Cursive" to "Cursive"
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        fonts.forEach { (key, label) ->
                            val isSelected = com.example.fitpal.ui.theme.globalFontFamilyName == key
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    com.example.fitpal.util.HapticFeedbackHelper.triggerConfirm(view)
                                    com.example.fitpal.ui.theme.globalFontFamilyName = key
                                    val sp = context.getSharedPreferences("fitpal_settings", android.content.Context.MODE_PRIVATE)
                                    sp.edit().putString("font_family_name", key).apply()
                                },
                                label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                                modifier = Modifier.weight(1f).testTag("font_family_$key")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Font Size Selector
                    Text(
                        text = "Font Size Scale",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val sizes = listOf(
                        0.85f to "Small (85%)",
                        1.0f to "Medium (100%)",
                        1.15f to "Large (115%)",
                        1.30f to "Extra (130%)"
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            sizes.take(2).forEach { (scale, label) ->
                                val isSelected = com.example.fitpal.ui.theme.globalFontSizeScale == scale
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        com.example.fitpal.util.HapticFeedbackHelper.triggerConfirm(view)
                                        com.example.fitpal.ui.theme.globalFontSizeScale = scale
                                        val sp = context.getSharedPreferences("fitpal_settings", android.content.Context.MODE_PRIVATE)
                                        sp.edit().putFloat("font_size_scale", scale).apply()
                                    },
                                    label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                                    modifier = Modifier.weight(1f).testTag("font_size_${(scale * 100).toInt()}")
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            sizes.drop(2).forEach { (scale, label) ->
                                val isSelected = com.example.fitpal.ui.theme.globalFontSizeScale == scale
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        com.example.fitpal.util.HapticFeedbackHelper.triggerConfirm(view)
                                        com.example.fitpal.ui.theme.globalFontSizeScale = scale
                                        val sp = context.getSharedPreferences("fitpal_settings", android.content.Context.MODE_PRIVATE)
                                        sp.edit().putFloat("font_size_scale", scale).apply()
                                    },
                                    label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                                    modifier = Modifier.weight(1f).testTag("font_size_${(scale * 100).toInt()}")
                                )
                            }
                        }
                    }
                }
            }

            // SECTION 2.5: PRIVACY & SECURITY
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Privacy & Security",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Passcode Protection",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Lock FitPal on app startup to secure your private weight records and transformation photos.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Switch(
                            checked = isPasscodeEnabled,
                            onCheckedChange = { isChecked ->
                                com.example.fitpal.util.HapticFeedbackHelper.triggerLightTap(view)
                                if (isChecked) {
                                    showSetPinDialog = true
                                } else {
                                    showVerifyDisablePinDialog = true
                                }
                            },
                            modifier = Modifier.testTag("settings_passcode_switch")
                        )
                    }
                }
            }

            if (false) {
                var isLoggingIn by remember { mutableStateOf(false) }
                var enteredCustomEmail by remember { mutableStateOf("") }
                var showCustomEmailField by remember { mutableStateOf(false) }

                androidx.compose.ui.window.Dialog(
                    onDismissRequest = { if (!isLoggingIn) showGoogleChooserDialog = false }
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
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
                                // Header with a beautiful custom Google-colored layout
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
                                    text = "to continue to FitPal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                                )

                                // 1. Account item for user's email shivnsh01@gmail.com
                                Card(
                                    onClick = {
                                        isLoggingIn = true
                                        coroutineScope.launch {
                                            kotlinx.coroutines.delay(1200L)
                                            profileViewModel.loginWithGoogle(context, "shivnsh01@gmail.com") {
                                                isLoggingIn = false
                                                showGoogleChooserDialog = false
                                                Toast.makeText(context, "Successfully logged in as shivnsh01@gmail.com via Google!", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("google_chooser_shivansh_account"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
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
                                            kotlinx.coroutines.delay(1200L)
                                            profileViewModel.loginWithGoogle(context, "fitpal.sandbox@gmail.com") {
                                                isLoggingIn = false
                                                showGoogleChooserDialog = false
                                                Toast.makeText(context, "Successfully logged in as fitpal.sandbox@gmail.com via Google!", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("google_chooser_sandbox_account"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
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
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth().testTag("google_chooser_custom_input")
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
                                                            kotlinx.coroutines.delay(1200L)
                                                            profileViewModel.loginWithGoogle(context, email) {
                                                                isLoggingIn = false
                                                                showGoogleChooserDialog = false
                                                                Toast.makeText(context, "Successfully logged in as $email via Google!", Toast.LENGTH_LONG).show()
                                                            }
                                                        }
                                                    } else {
                                                        Toast.makeText(context, "Please enter a valid Gmail address", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                enabled = enteredCustomEmail.isNotBlank(),
                                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                                modifier = Modifier.testTag("google_chooser_custom_btn")
                                            ) {
                                                Text("Sign In")
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                TextButton(
                                    onClick = { showGoogleChooserDialog = false },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Cancel", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            if (showSetPinDialog) {
                var newPin by remember { mutableStateOf("") }
                var confirmPin by remember { mutableStateOf("") }
                var pinStep by remember { mutableStateOf(1) } // 1 for enter, 2 for confirm
                var setupError by remember { mutableStateOf("") }

                AlertDialog(
                    onDismissRequest = { showSetPinDialog = false },
                    title = {
                        Text(
                            text = if (pinStep == 1) "Create Passcode" else "Confirm Passcode",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (pinStep == 1) "Enter a 4-digit security PIN:" else "Re-enter your 4-digit PIN to confirm:",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            val enteredValue = if (pinStep == 1) newPin else confirmPin
                            
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                for (i in 0 until 4) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                            .background(
                                                if (i < enteredValue.length) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .border(1.dp, MaterialTheme.colorScheme.outline, androidx.compose.foundation.shape.CircleShape)
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = enteredValue,
                                onValueChange = { newVal ->
                                    if (newVal.length <= 4 && newVal.all { it.isDigit() }) {
                                        if (pinStep == 1) newPin = newVal else confirmPin = newVal
                                        setupError = ""
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                placeholder = { Text("4-digit PIN") },
                                modifier = Modifier
                                    .width(150.dp)
                                    .testTag("settings_setup_pin_field")
                            )

                            if (setupError.isNotEmpty()) {
                                Text(
                                    text = setupError,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val enteredValue = if (pinStep == 1) newPin else confirmPin
                                if (enteredValue.length != 4) {
                                    setupError = "PIN must be exactly 4 digits."
                                    return@Button
                                }
                                if (pinStep == 1) {
                                    pinStep = 2
                                } else {
                                    if (newPin == confirmPin) {
                                        com.example.fitpal.util.SecurityUtils.enablePasscode(context, newPin)
                                        isPasscodeEnabled = true
                                        showSetPinDialog = false
                                        Toast.makeText(context, "Passcode Lock Enabled!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        setupError = "Passcodes do not match. Restarting."
                                        newPin = ""
                                        confirmPin = ""
                                        pinStep = 1
                                    }
                                }
                            },
                            enabled = (if (pinStep == 1) newPin else confirmPin).length == 4
                        ) {
                            Text(if (pinStep == 1) "Continue" else "Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSetPinDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (showVerifyDisablePinDialog) {
                var verifyPinVal by remember { mutableStateOf("") }
                var verifyError by remember { mutableStateOf("") }

                AlertDialog(
                    onDismissRequest = { showVerifyDisablePinDialog = false },
                    title = { Text("Disable Passcode", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Enter your current 4-digit passcode to verify:",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                for (i in 0 until 4) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                            .background(
                                                if (i < verifyPinVal.length) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .border(1.dp, MaterialTheme.colorScheme.outline, androidx.compose.foundation.shape.CircleShape)
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = verifyPinVal,
                                onValueChange = { newVal ->
                                    if (newVal.length <= 4 && newVal.all { it.isDigit() }) {
                                        verifyPinVal = newVal
                                        verifyError = ""
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                placeholder = { Text("PIN") },
                                modifier = Modifier
                                    .width(150.dp)
                                    .testTag("settings_verify_pin_field")
                            )

                            if (verifyError.isNotEmpty()) {
                                Text(
                                    text = verifyError,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (com.example.fitpal.util.SecurityUtils.verifyPin(context, verifyPinVal)) {
                                    com.example.fitpal.util.SecurityUtils.disablePasscode(context)
                                    isPasscodeEnabled = false
                                    showVerifyDisablePinDialog = false
                                    Toast.makeText(context, "Passcode Lock Disabled!", Toast.LENGTH_SHORT).show()
                                } else {
                                    verifyError = "Incorrect passcode."
                                    verifyPinVal = ""
                                }
                            },
                            enabled = verifyPinVal.length == 4
                        ) {
                            Text("Verify & Disable")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showVerifyDisablePinDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // SECTION 3: BACKUP / EXPORT DATA
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Data Management",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = {
                            com.example.fitpal.util.HapticFeedbackHelper.triggerConfirm(view)
                            coroutineScope.launch {
                                try {
                                    val backupJson = com.example.fitpal.util.BackupRestoreHelper.exportDatabaseToJson(context)
                                    val sendIntent = android.content.Intent().apply {
                                        action = android.content.Intent.ACTION_SEND
                                        putExtra(android.content.Intent.EXTRA_TEXT, backupJson)
                                        type = "text/plain"
                                    }
                                    val shareIntent = android.content.Intent.createChooser(sendIntent, "Export FitPal Backup")
                                    context.startActivity(shareIntent)
                                    Toast.makeText(context, "Backup generated successfully!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Backup failed: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_backup_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Backup / Export Database")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            com.example.fitpal.util.HapticFeedbackHelper.triggerConfirm(view)
                            showRestoreDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_restore_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restore / Import Database")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            showClearConfirm = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_clear_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear All Data")
                    }
                }
            }

            if (showRestoreDialog) {
                var importJsonText by remember { mutableStateOf("") }
                var importErrorText by remember { mutableStateOf("") }

                AlertDialog(
                    onDismissRequest = { showRestoreDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Backup,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Restore Database", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Paste your FitPal backup JSON text below to restore your offline profile, meal logs, exercise data, and settings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = importJsonText,
                                onValueChange = { input ->
                                    importJsonText = input
                                    importErrorText = ""
                                },
                                label = { Text("Backup JSON Text") },
                                placeholder = { Text("Paste entire { ... } JSON backup here") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .testTag("import_json_input"),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                maxLines = 10
                            )

                            if (importErrorText.isNotEmpty()) {
                                Text(
                                    text = importErrorText,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (importJsonText.isBlank()) {
                                    importErrorText = "Please paste your backup JSON first."
                                    return@Button
                                }
                                com.example.fitpal.util.HapticFeedbackHelper.triggerConfirm(view)
                                coroutineScope.launch {
                                    val success = com.example.fitpal.util.BackupRestoreHelper.importDatabaseFromJson(context, importJsonText.trim())
                                    if (success) {
                                        showRestoreDialog = false
                                        Toast.makeText(context, "Data restored successfully!", Toast.LENGTH_LONG).show()
                                    } else {
                                        importErrorText = "Invalid or tampered backup JSON. Please check and try again."
                                    }
                                }
                            },
                            enabled = importJsonText.isNotBlank(),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("verify_import_btn")
                        ) {
                            Text("Import & Restore")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRestoreDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (showGoogleChooserDialog) {
                var isLoggingIn by remember { mutableStateOf(false) }
                var enteredCustomEmail by remember { mutableStateOf("") }
                var showCustomEmailField by remember { mutableStateOf(false) }

                androidx.compose.ui.window.Dialog(
                    onDismissRequest = { if (!isLoggingIn) showGoogleChooserDialog = false }
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    Text(text = "G", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black), color = Color(0xFF4285F4))
                                    Text(text = "o", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black), color = Color(0xFFEA4335))
                                    Text(text = "o", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black), color = Color(0xFFFBBC05))
                                    Text(text = "g", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black), color = Color(0xFF4285F4))
                                    Text(text = "l", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black), color = Color(0xFF34A853))
                                    Text(text = "e", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black), color = Color(0xFFEA4335))
                                }

                                Text(
                                    text = "Choose an account",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "to continue to FitPal & sync data",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                                )

                                // Primary account
                                Surface(
                                    onClick = {
                                        isLoggingIn = true
                                        coroutineScope.launch {
                                            profileViewModel.loginWithGoogleAccount(context, email = "shivnsh01@gmail.com", displayName = "Shivansh", photoUrl = null) {
                                                profileViewModel.restoreFromCloud(context, onSuccess = {
                                                    isLoggingIn = false
                                                    showGoogleChooserDialog = false
                                                    Toast.makeText(context, "Welcome Shivansh! Cloud data restored.", Toast.LENGTH_LONG).show()
                                                }, onError = {
                                                    isLoggingIn = false
                                                    showGoogleChooserDialog = false
                                                    Toast.makeText(context, "Logged in as shivnsh01@gmail.com!", Toast.LENGTH_SHORT).show()
                                                })
                                            }
                                        }
                                    },
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth().testTag("google_account_shivansh")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(androidx.compose.foundation.shape.CircleShape)
                                                .background(Color(0xFF4285F4)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("S", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text("Shivansh", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                            Text("shivnsh01@gmail.com", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Sandbox account
                                Surface(
                                    onClick = {
                                        isLoggingIn = true
                                        coroutineScope.launch {
                                            profileViewModel.loginWithGoogleAccount(context, email = "fitpal.sandbox@gmail.com", displayName = "FitPal Sandbox", photoUrl = null) {
                                                profileViewModel.restoreFromCloud(context, onSuccess = {
                                                    isLoggingIn = false
                                                    showGoogleChooserDialog = false
                                                    Toast.makeText(context, "Cloud backup restored for Sandbox account!", Toast.LENGTH_LONG).show()
                                                }, onError = {
                                                    isLoggingIn = false
                                                    showGoogleChooserDialog = false
                                                    Toast.makeText(context, "Logged in as fitpal.sandbox@gmail.com!", Toast.LENGTH_SHORT).show()
                                                })
                                            }
                                        }
                                    },
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth().testTag("google_account_sandbox")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(androidx.compose.foundation.shape.CircleShape)
                                                .background(Color(0xFF34A853)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("F", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text("FitPal Sandbox User", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                            Text("fitpal.sandbox@gmail.com", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                if (!showCustomEmailField) {
                                    TextButton(
                                        onClick = { showCustomEmailField = true }
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Use another Google account")
                                    }
                                } else {
                                    OutlinedTextField(
                                        value = enteredCustomEmail,
                                        onValueChange = { enteredCustomEmail = it },
                                        label = { Text("Google Account Email") },
                                        placeholder = { Text("username@gmail.com") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("custom_google_email_input")
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            val email = enteredCustomEmail.trim()
                                            if (email.contains("@") && email.length > 5) {
                                                isLoggingIn = true
                                                coroutineScope.launch {
                                                    profileViewModel.loginWithGoogleAccount(context, email = email, displayName = email.substringBefore("@"), photoUrl = null) {
                                                        profileViewModel.restoreFromCloud(context, onSuccess = {
                                                            isLoggingIn = false
                                                            showGoogleChooserDialog = false
                                                            Toast.makeText(context, "Cloud backup restored for $email!", Toast.LENGTH_LONG).show()
                                                        }, onError = {
                                                            isLoggingIn = false
                                                            showGoogleChooserDialog = false
                                                            Toast.makeText(context, "Logged in as $email!", Toast.LENGTH_SHORT).show()
                                                        })
                                                    }
                                                }
                                            } else {
                                                Toast.makeText(context, "Please enter a valid Gmail address", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Continue")
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = { showGoogleChooserDialog = false }
                                ) {
                                    Text("Cancel")
                                }
                            }
                        }
                    }
                }
            }

            if (showClearConfirm) {
                ConfirmationDialog(
                    title = "Confirm Data Purge",
                    message = "This will permanently delete your user profile and all logs (meals, workouts, water tracking, weight logs). This action is irreversible.",
                    onConfirm = {
                        com.example.fitpal.util.HapticFeedbackHelper.triggerRejectOrDelete(view)
                        val app = context.applicationContext as com.example.fitpal.FitPalApplication
                        profileViewModel.clearAllData(app.foodRepository, app.exerciseRepository)
                        showClearConfirm = false
                        onBack()
                    },
                    onDismiss = { showClearConfirm = false }
                )
            }
        }
    }
}
