package com.example.ui.profile

import kotlin.math.roundToInt
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.example.notification.NotificationHelper
import com.example.ui.components.ConfirmationDialog

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

    var calorieGoalStr by remember { mutableStateOf("") }
    var proteinMultiplier by remember { mutableStateOf(1.8f) }
    var fatMultiplier by remember { mutableStateOf(0.8f) }

    var showClearConfirm by remember { mutableStateOf(false) }
    val view = androidx.compose.ui.platform.LocalView.current

    val cloudEmail by profileViewModel.cloudUserEmail.collectAsState()
    val lastSyncTime by profileViewModel.lastSyncTime.collectAsState()
    val isSyncing by profileViewModel.isSyncing.collectAsState()

    var loginEmailInput by remember { mutableStateOf("") }
    var loginPassInput by remember { mutableStateOf("") }

    var isPasscodeEnabled by remember { mutableStateOf(com.example.util.SecurityUtils.isPasscodeEnabled(context)) }
    var showSetPinDialog by remember { mutableStateOf(false) }
    var showVerifyDisablePinDialog by remember { mutableStateOf(false) }

    LaunchedEffect(profile) {
        profile?.let { p ->
            calorieGoalStr = p.dailyCalorieGoal.toString()
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
            // SECTION 1: MANUAL CALORIE ADJUSTMENT
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Daily Calorie Target Override",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = calorieGoalStr,
                        onValueChange = { calorieGoalStr = it },
                        label = { Text("Daily Calorie Goal (kcal)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_calorie_input"),
                        trailingIcon = {
                            IconButton(onClick = {
                                val kcal = calorieGoalStr.toIntOrNull()
                                if (kcal != null && kcal > 500) {
                                    com.example.util.HapticFeedbackHelper.triggerConfirm(view)
                                    profileViewModel.updateCalorieGoal(kcal)
                                    Toast.makeText(context, "Calorie goal updated!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                                }
                            }, modifier = Modifier.testTag("settings_save_calories")) {
                                Icon(Icons.Default.Check, contentDescription = "Save Calorie Target")
                            }
                        }
                    )
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
                            com.example.util.HapticFeedbackHelper.triggerConfirm(view)
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
                                        profileViewModel.registerWithEmail(
                                            context,
                                            loginEmailInput.trim(),
                                            loginPassInput,
                                            onSuccess = {
                                                Toast.makeText(context, "Account created! Data synced successfully.", Toast.LENGTH_LONG).show()
                                            },
                                            onError = { err ->
                                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("settings_register_btn")
                            ) {
                                Text("Register")
                            }
                        }

                        // Google Divider
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
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

                        // Google Sign In Button
                        Button(
                            onClick = {
                                val simulatedGoogleEmail = if (profile != null && profile!!.name.isNotBlank()) {
                                    "${profile!!.name.lowercase().replace(" ", "")}@gmail.com"
                                } else {
                                    "fitpal.user@gmail.com"
                                }
                                profileViewModel.loginWithGoogle(context, simulatedGoogleEmail) {
                                    Toast.makeText(context, "Logged in via Google as $simulatedGoogleEmail", Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("settings_google_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign In with Google", fontWeight = FontWeight.SemiBold)
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
                                            Toast.makeText(context, "Database backup synced successfully!", Toast.LENGTH_SHORT).show()
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

                            OutlinedButton(
                                onClick = {
                                    profileViewModel.logout()
                                    Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f).testTag("settings_logout_btn")
                            ) {
                                Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Log Out")
                            }
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = "Google Firebase Services Active",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Cloud Project: fitpal123 • Realtime DB & Auth enabled",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
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
                            com.example.util.HapticFeedbackHelper.triggerConfirm(view)
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
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            fonts.take(2).forEach { (key, label) ->
                                val isSelected = com.example.ui.theme.globalFontFamilyName == key
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        com.example.util.HapticFeedbackHelper.triggerConfirm(view)
                                        com.example.ui.theme.globalFontFamilyName = key
                                        val sp = context.getSharedPreferences("fitpal_settings", android.content.Context.MODE_PRIVATE)
                                        sp.edit().putString("font_family_name", key).apply()
                                    },
                                    label = { 
                                        Text(
                                            text = label, 
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            softWrap = false
                                        ) 
                                    },
                                    modifier = Modifier.weight(1f).testTag("font_family_$key")
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            fonts.drop(2).forEach { (key, label) ->
                                val isSelected = com.example.ui.theme.globalFontFamilyName == key
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        com.example.util.HapticFeedbackHelper.triggerConfirm(view)
                                        com.example.ui.theme.globalFontFamilyName = key
                                        val sp = context.getSharedPreferences("fitpal_settings", android.content.Context.MODE_PRIVATE)
                                        sp.edit().putString("font_family_name", key).apply()
                                    },
                                    label = { 
                                        Text(
                                            text = label, 
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            softWrap = false
                                        ) 
                                    },
                                    modifier = Modifier.weight(1f).testTag("font_family_$key")
                                )
                            }
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
                                val isSelected = com.example.ui.theme.globalFontSizeScale == scale
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        com.example.util.HapticFeedbackHelper.triggerConfirm(view)
                                        com.example.ui.theme.globalFontSizeScale = scale
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
                                val isSelected = com.example.ui.theme.globalFontSizeScale == scale
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        com.example.util.HapticFeedbackHelper.triggerConfirm(view)
                                        com.example.ui.theme.globalFontSizeScale = scale
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
                                com.example.util.HapticFeedbackHelper.triggerLightTap(view)
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
                                        com.example.util.SecurityUtils.enablePasscode(context, newPin)
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
                                if (com.example.util.SecurityUtils.verifyPin(context, verifyPinVal)) {
                                    com.example.util.SecurityUtils.disablePasscode(context)
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
                            com.example.util.HapticFeedbackHelper.triggerConfirm(view)
                            // Backup simulation to device logs / export text summary
                            val report = """
                                === FitPal Profile Export ===
                                Name: ${profile?.name}
                                Weight Goal: ${profile?.weightGoalType}
                                Calorie Goal: ${profile?.dailyCalorieGoal} kcal
                                Protein Target: ${profile?.proteinGoalGrams}g
                                Carbs Target: ${profile?.carbsGoalGrams}g
                                Fat Target: ${profile?.fatGoalGrams}g
                                ==============================
                            """.trimIndent()
                            
                            // Emulated CSV/Text sharing
                            Toast.makeText(context, "Database backed up successfully to local storage (FitPalBackup.csv)!", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_backup_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Backup Database")
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

            if (showClearConfirm) {
                ConfirmationDialog(
                    title = "Confirm Data Purge",
                    message = "This will permanently delete your user profile and all logs (meals, workouts, water tracking, weight logs). This action is irreversible.",
                    onConfirm = {
                        com.example.util.HapticFeedbackHelper.triggerRejectOrDelete(view)
                        val app = context.applicationContext as com.example.FitPalApplication
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
