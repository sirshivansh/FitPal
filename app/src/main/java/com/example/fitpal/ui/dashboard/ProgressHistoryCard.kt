package com.example.fitpal.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitpal.data.local.entity.UserProfile
import com.example.fitpal.data.local.entity.WeightLog
import com.example.fitpal.util.Calculations
import com.example.fitpal.util.DateUtils
import com.example.fitpal.util.HapticFeedbackHelper
import com.example.fitpal.util.WeightTrendAnalysis
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressHistoryCard(
    weightLogs: List<WeightLog>,
    weightTrendReport: WeightTrendAnalysis?,
    userProfile: UserProfile?,
    onSaveProgress: (Float, String) -> Unit,
    onDeleteWeight: (WeightLog) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var isExpanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<WeightLog?>(null) }

    // Dialog state
    var inputWeight by remember { mutableStateOf("") }
    var inputDate by remember { mutableStateOf(DateUtils.getTodayDateString()) }

    val sortedLogs = remember(weightLogs) {
        weightLogs.sortedByDescending { it.date }
    }

    val startingWeight = remember(sortedLogs) {
        sortedLogs.lastOrNull()?.weightKg ?: userProfile?.currentWeightKg
    }
    val currentWeight = remember(sortedLogs) {
        sortedLogs.firstOrNull()?.weightKg ?: userProfile?.currentWeightKg
    }
    val totalChange = if (startingWeight != null && currentWeight != null) {
        currentWeight - startingWeight
    } else 0f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("progress_history_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Progress History & Trend",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Scientifically tracked 7-day rolling averages",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = {
                        inputWeight = currentWeight?.toString() ?: ""
                        inputDate = DateUtils.getTodayDateString()
                        showAddDialog = true
                        HapticFeedbackHelper.triggerLightTap(view)
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("save_progress_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save Progress", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Stat Summary Cards Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Current / 7-Day Trend Card
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "7-Day Moving Avg",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val avg = weightTrendReport?.currentSevenDayAverageKg
                        Text(
                            text = if (avg != null) "%.1f kg".format(avg) else (currentWeight?.let { "%.1f kg".format(it) } ?: "--"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Scale: ${currentWeight?.let { "%.1f kg".format(it) } ?: "--"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Net Change Card
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Net Change",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val sign = if (totalChange > 0) "+" else ""
                        Text(
                            text = "$sign%.1f kg".format(totalChange),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = if (totalChange <= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        val rate = weightTrendReport?.weeklyChangeKg ?: 0f
                        Text(
                            text = "%.2f kg/wk".format(rate),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Goal Weight Remaining Card
                if (userProfile != null) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Goal Target",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "%.1f kg".format(userProfile.goalWeightKg),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val remaining = currentWeight?.let { abs(it - userProfile.goalWeightKg) } ?: 0f
                            Text(
                                text = "%.1f kg to go".format(remaining),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            // Scientific Trend Narrative
            if (weightTrendReport != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = weightTrendReport.trendSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Expandable History List Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        isExpanded = !isExpanded
                        HapticFeedbackHelper.triggerLightTap(view)
                    }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Weigh-in History (${sortedLogs.size} entries)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // History Entries List
            AnimatedVisibility(visible = isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (sortedLogs.isEmpty()) {
                        Text(
                            text = "No weigh-ins saved yet. Tap 'Save Progress' to log your first weight!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        sortedLogs.take(15).forEachIndexed { index, log ->
                            val prevLog = sortedLogs.getOrNull(index + 1)
                            val diff = prevLog?.let { log.weightKg - it.weightKg }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = DateUtils.formatDateForDisplayShort(log.date),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = log.date,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "%.1f kg".format(log.weightKg),
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Black
                                            )
                                            if (diff != null) {
                                                val diffSign = if (diff > 0) "+" else ""
                                                Text(
                                                    text = "$diffSign%.1f kg".format(diff),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (diff <= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = { entryToDelete = log },
                                            modifier = Modifier.size(32.dp).testTag("delete_weight_${log.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete weigh-in",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                modifier = Modifier.size(18.dp)
                                            )
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

    // Save Progress Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text("Save Progress Weigh-in", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter your current scale weight. FitPal uses this to automatically recalculate your 7-day moving trend and update maintenance estimates.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = inputWeight,
                        onValueChange = { inputWeight = it },
                        label = { Text("Weight (kg)") },
                        placeholder = { Text("e.g. 72.4") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_weight_input")
                    )

                    OutlinedTextField(
                        value = inputDate,
                        onValueChange = { inputDate = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        placeholder = { Text("e.g. 2026-09-15") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_date_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val weight = inputWeight.toFloatOrNull()
                        if (weight != null && weight > 20f && inputDate.isNotBlank()) {
                            onSaveProgress(weight, inputDate)
                            HapticFeedbackHelper.triggerConfirm(view)
                            showAddDialog = false
                        }
                    },
                    modifier = Modifier.testTag("dialog_confirm_save_progress")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (entryToDelete != null) {
        val entry = entryToDelete!!
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text("Delete Weigh-in?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to remove the entry of %.1f kg from %s? Your 7-day trend will be recalculated.".format(entry.weightKg, entry.date))
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteWeight(entry)
                        entryToDelete = null
                        HapticFeedbackHelper.triggerLightTap(view)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
