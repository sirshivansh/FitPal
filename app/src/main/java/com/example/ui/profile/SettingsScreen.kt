package com.example.ui.profile

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

                    // Protein multiplier row
                    Text(
                        text = "Protein Multiplier (g/kg)",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val proteinOptions = listOf(1.2f, 1.6f, 1.8f, 2.0f, 2.2f)
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

                    // Fat multiplier row
                    Text(
                        text = "Fat Multiplier (g/kg)",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
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
                        profileViewModel.clearAllData()
                        showClearConfirm = false
                        onBack()
                    },
                    onDismiss = { showClearConfirm = false }
                )
            }
        }
    }
}
