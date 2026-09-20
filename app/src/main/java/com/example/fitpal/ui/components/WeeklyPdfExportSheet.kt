package com.example.fitpal.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.fitpal.data.repository.ExerciseRepository
import com.example.fitpal.data.repository.FoodRepository
import com.example.fitpal.data.repository.ProfileRepository
import com.example.fitpal.util.DateUtils
import com.example.fitpal.util.HapticFeedbackHelper
import com.example.fitpal.util.WeeklyPdfReportGenerator
import com.example.fitpal.util.WeeklyReportData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class ReportTimeframe(val label: String, val description: String) {
    CURRENT_WEEK("Current Week", "Monday through Sunday of active week"),
    LAST_7_DAYS("Past 7 Days", "Rolling 7-day window through today"),
    PREVIOUS_WEEK("Previous Week", "Complete 7-day prior Monday - Sunday")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyPdfExportSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    profileRepo: ProfileRepository,
    foodRepo: FoodRepository,
    exerciseRepo: ExerciseRepository,
    selectedDate: String = DateUtils.getTodayDateString(),
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    var selectedTimeframe by remember { mutableStateOf(ReportTimeframe.CURRENT_WEEK) }
    var isLoading by remember { mutableStateOf(true) }
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var reportData by remember { mutableStateOf<WeeklyReportData?>(null) }
    var generatedFile by remember { mutableStateOf<File?>(null) }

    // Helper to compute date list based on selected timeframe
    fun getDatesForTimeframe(timeframe: ReportTimeframe): List<String> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val baseDate = try {
            LocalDate.parse(selectedDate, formatter)
        } catch (e: Exception) {
            LocalDate.now()
        }

        return when (timeframe) {
            ReportTimeframe.CURRENT_WEEK -> DateUtils.getWeekStripDays(baseDate.format(formatter))
            ReportTimeframe.LAST_7_DAYS -> DateUtils.getLast7Days(baseDate.format(formatter))
            ReportTimeframe.PREVIOUS_WEEK -> {
                val lastWeekMonday = baseDate.minusWeeks(1).minusDays((baseDate.dayOfWeek.value - 1).toLong())
                (0..6).map { lastWeekMonday.plusDays(it.toLong()).format(formatter) }
            }
        }
    }

    // Load data whenever timeframe changes
    LaunchedEffect(selectedTimeframe, selectedDate) {
        isLoading = true
        val dates = getDatesForTimeframe(selectedTimeframe)
        try {
            val data = WeeklyPdfReportGenerator.collectWeeklyReportData(
                profileRepo = profileRepo,
                foodRepo = foodRepo,
                exerciseRepo = exerciseRepo,
                dates = dates
            )
            reportData = data
            // Pre-generate PDF in cache
            val file = WeeklyPdfReportGenerator.generateWeeklyReportPdf(context, data)
            generatedFile = file
        } catch (e: Exception) {
            Toast.makeText(context, "Error loading report: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.55f),
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        },
        modifier = modifier.testTag("weekly_pdf_export_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState()),
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "PDF Report",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Export Weekly Report",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Summarized personal fitness & calorie metrics",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Timeframe Segmented Chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Select Reporting Window",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReportTimeframe.values().forEach { timeframe ->
                        val isSelected = selectedTimeframe == timeframe
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                HapticFeedbackHelper.triggerLightTap(view)
                                selectedTimeframe = timeframe
                            },
                            label = {
                                Text(
                                    text = timeframe.label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Date Range Display Badge
            reportData?.let { data ->
                val rangeStr = DateUtils.formatRangeForDisplay(data.startDate, data.endDate)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = rangeStr,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        GoalCompletionBadge(
                            text = "${data.dailySummaries.size} Days Logged",
                            color = MaterialTheme.colorScheme.primary,
                            isCompleted = true
                        )
                    }
                }
            }

            // Report Preview Content
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Aggregating 7-day health metrics...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                reportData?.let { data ->
                    // 4-Quadrant Preview Matrix
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Report Summary Preview",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PreviewMetricBox(
                                title = "Avg Daily Food",
                                value = "${data.avgDailyFoodCalories} kcal",
                                subtext = "Target: ${data.calorieGoal} kcal",
                                icon = Icons.Default.Restaurant,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )

                            PreviewMetricBox(
                                title = "Weekly Active Burn",
                                value = "${data.totalBurnedCalories} kcal",
                                subtext = "Avg ${data.avgDailyBurnedCalories} kcal/d",
                                icon = Icons.Default.LocalFireDepartment,
                                color = Color(0xFFF97316),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PreviewMetricBox(
                                title = "Net Balance",
                                value = "${if (data.netCalorieBalance > 0) "+" else ""}${data.netCalorieBalance} kcal",
                                subtext = "${data.onTargetDays} / 7 on budget",
                                icon = Icons.Default.ShowChart,
                                color = Color(0xFF8B5CF6),
                                modifier = Modifier.weight(1f)
                            )

                            PreviewMetricBox(
                                title = "Daily Avg Protein",
                                value = "${data.avgDailyProteinGrams.toInt()}g",
                                subtext = "Target: ${data.proteinGoal}g",
                                icon = Icons.Default.FitnessCenter,
                                color = Color(0xFF0EA5E9),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PreviewMetricBox(
                                title = "Total Steps",
                                value = String.format(Locale.US, "%,d", data.totalSteps),
                                subtext = "${String.format(Locale.US, "%,d", data.avgDailySteps)} avg/day",
                                icon = Icons.Default.DirectionsWalk,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )

                            PreviewMetricBox(
                                title = "Hydration & Workouts",
                                value = "${String.format(Locale.US, "%.1f", data.totalWaterMl / 1000f)}L",
                                subtext = "${data.totalWorkouts} workouts logged",
                                icon = Icons.Default.WaterDrop,
                                color = Color(0xFF0EA5E9),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // PDF Document Specifications Callout
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = "Ready to Print & Share (A4 PDF)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Includes 7-day intake vs burn chart, daily macro breakdown table, hydration totals, and personal coaching trends.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Action Buttons: Share PDF & View PDF
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                HapticFeedbackHelper.triggerConfirm(view)
                                scope.launch {
                                    isGeneratingPdf = true
                                    try {
                                        val file = generatedFile ?: WeeklyPdfReportGenerator.generateWeeklyReportPdf(context, data)
                                        generatedFile = file
                                        WeeklyPdfReportGenerator.shareReport(context, file)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isGeneratingPdf = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("btn_share_weekly_pdf"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(16.dp),
                            enabled = !isGeneratingPdf
                        ) {
                            if (isGeneratingPdf) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Preparing PDF...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Export & Share PDF Report", fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                HapticFeedbackHelper.triggerLightTap(view)
                                scope.launch {
                                    isGeneratingPdf = true
                                    try {
                                        val file = generatedFile ?: WeeklyPdfReportGenerator.generateWeeklyReportPdf(context, data)
                                        generatedFile = file
                                        WeeklyPdfReportGenerator.viewReport(context, file)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Open error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isGeneratingPdf = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_view_weekly_pdf"),
                            shape = RoundedCornerShape(16.dp),
                            enabled = !isGeneratingPdf
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open & View Document", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewMetricBox(
    title: String,
    value: String,
    subtext: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontSize = 10.sp
            )
        }
    }
}
