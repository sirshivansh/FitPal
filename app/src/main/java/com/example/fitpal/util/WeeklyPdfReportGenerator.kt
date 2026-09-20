package com.example.fitpal.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.fitpal.data.local.entity.UserProfile
import com.example.fitpal.data.repository.ExerciseRepository
import com.example.fitpal.data.repository.FoodRepository
import com.example.fitpal.data.repository.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class DayReportSummary(
    val date: String,
    val dayOfWeek: String,
    val foodCalories: Int,
    val exerciseCalories: Int,
    val stepCalories: Int,
    val totalBurned: Int,
    val netCalories: Int,
    val calorieGoal: Int,
    val proteinGrams: Float,
    val carbsGrams: Float,
    val fatGrams: Float,
    val waterMl: Int,
    val steps: Int,
    val workoutCount: Int,
    val workoutDetails: String
)

data class WeeklyReportData(
    val startDate: String,
    val endDate: String,
    val userProfile: UserProfile?,
    val dailySummaries: List<DayReportSummary>,
    val totalFoodCalories: Int,
    val avgDailyFoodCalories: Int,
    val totalBurnedCalories: Int,
    val avgDailyBurnedCalories: Int,
    val netCalorieBalance: Int,
    val avgDailyNetCalories: Int,
    val totalProteinGrams: Float,
    val avgDailyProteinGrams: Float,
    val totalCarbsGrams: Float,
    val avgDailyCarbsGrams: Float,
    val totalFatGrams: Float,
    val avgDailyFatGrams: Float,
    val totalWaterMl: Int,
    val avgDailyWaterMl: Int,
    val totalSteps: Int,
    val avgDailySteps: Int,
    val totalWorkouts: Int,
    val calorieGoal: Int,
    val proteinGoal: Int,
    val onTargetDays: Int
)

object WeeklyPdfReportGenerator {

    suspend fun collectWeeklyReportData(
        profileRepo: ProfileRepository,
        foodRepo: FoodRepository,
        exerciseRepo: ExerciseRepository,
        dates: List<String>
    ): WeeklyReportData = withContext(Dispatchers.IO) {
        val profile = profileRepo.getUserProfileSync()
        val calorieGoal = profile?.let {
            if (it.customCalorieGoal != null && it.customCalorieGoal > 0) it.customCalorieGoal
            else (it.maintenanceCalories ?: 2000) + it.calorieAdjustment
        } ?: 2000
        val proteinGoal = profile?.let {
            Calculations.calculateMacrosCustom(calorieGoal, it.currentWeightKg, it.proteinMultiplier, it.fatMultiplier).second
        } ?: 150

        val dailySummaries = mutableListOf<DayReportSummary>()

        for (dateStr in dates) {
            val foods = foodRepo.getFoodEntriesSync(dateStr)
            val exercises = exerciseRepo.getExerciseEntriesSync(dateStr)
            val workouts = exerciseRepo.getWorkoutsForDate(dateStr).firstOrNull() ?: emptyList()
            val waterLog = profileRepo.getWaterLogSync(dateStr)
            val activityLog = profileRepo.getActivityLogSync(dateStr)

            val foodCals = foods.sumOf { it.caloriesConsumed }.toInt()
            val exerciseCals = exercises.sumOf { it.caloriesBurned }.toInt()
            val stepCals = activityLog?.additionalCalories ?: 0
            val totalBurn = exerciseCals + stepCals
            val netCals = foodCals - totalBurn

            val protein = foods.sumOf { it.proteinGrams }.toFloat()
            val carbs = foods.sumOf { it.carbsGrams }.toFloat()
            val fat = foods.sumOf { it.fatGrams }.toFloat()
            val water = waterLog?.milliliters ?: 0
            val steps = activityLog?.steps ?: 0

            val workoutNames = if (workouts.isNotEmpty()) {
                workouts.joinToString(", ") { "${it.title.ifBlank { it.workoutType }} (${it.durationMinutes}m)" }
            } else if (exercises.isNotEmpty()) {
                exercises.joinToString(", ") { "${it.exerciseName} (${it.durationMinutes.toInt()}m)" }
            } else {
                "Rest Day"
            }

            dailySummaries.add(
                DayReportSummary(
                    date = dateStr,
                    dayOfWeek = DateUtils.getDayOfWeekShort(dateStr),
                    foodCalories = foodCals,
                    exerciseCalories = exerciseCals,
                    stepCalories = stepCals,
                    totalBurned = totalBurn,
                    netCalories = netCals,
                    calorieGoal = calorieGoal,
                    proteinGrams = protein,
                    carbsGrams = carbs,
                    fatGrams = fat,
                    waterMl = water,
                    steps = steps,
                    workoutCount = maxOf(workouts.size, exercises.size),
                    workoutDetails = workoutNames
                )
            )
        }

        val totalFood = dailySummaries.sumOf { it.foodCalories }
        val daysCount = if (dailySummaries.isNotEmpty()) dailySummaries.size else 1
        val avgDailyFood = totalFood / daysCount
        val totalBurned = dailySummaries.sumOf { it.totalBurned }
        val avgDailyBurned = totalBurned / daysCount
        val netBalance = totalFood - totalBurned
        val avgDailyNet = netBalance / daysCount
        val totalProtein = dailySummaries.sumOf { it.proteinGrams.toDouble() }.toFloat()
        val avgDailyProtein = totalProtein / daysCount
        val totalCarbs = dailySummaries.sumOf { it.carbsGrams.toDouble() }.toFloat()
        val avgDailyCarbs = totalCarbs / daysCount
        val totalFat = dailySummaries.sumOf { it.fatGrams.toDouble() }.toFloat()
        val avgDailyFat = totalFat / daysCount
        val totalWater = dailySummaries.sumOf { it.waterMl }
        val avgDailyWater = totalWater / daysCount
        val totalSteps = dailySummaries.sumOf { it.steps }
        val avgDailySteps = totalSteps / daysCount
        val totalWorkouts = dailySummaries.sumOf { it.workoutCount }
        val onTargetDays = dailySummaries.count { it.foodCalories in (calorieGoal - 200)..(calorieGoal + 100) }

        WeeklyReportData(
            startDate = dates.firstOrNull() ?: "",
            endDate = dates.lastOrNull() ?: "",
            userProfile = profile,
            dailySummaries = dailySummaries,
            totalFoodCalories = totalFood,
            avgDailyFoodCalories = avgDailyFood,
            totalBurnedCalories = totalBurned,
            avgDailyBurnedCalories = avgDailyBurned,
            netCalorieBalance = netBalance,
            avgDailyNetCalories = avgDailyNet,
            totalProteinGrams = totalProtein,
            avgDailyProteinGrams = avgDailyProtein,
            totalCarbsGrams = totalCarbs,
            avgDailyCarbsGrams = avgDailyCarbs,
            totalFatGrams = totalFat,
            avgDailyFatGrams = avgDailyFat,
            totalWaterMl = totalWater,
            avgDailyWaterMl = avgDailyWater,
            totalSteps = totalSteps,
            avgDailySteps = avgDailySteps,
            totalWorkouts = totalWorkouts,
            calorieGoal = calorieGoal,
            proteinGoal = proteinGoal,
            onTargetDays = onTargetDays
        )
    }

    suspend fun generateWeeklyReportPdf(context: Context, data: WeeklyReportData): File = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        val pageWidth = 595 // Standard A4 width in points
        val pageHeight = 842 // Standard A4 height in points
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        // Color definitions
        val primaryColor = Color.rgb(16, 185, 129) // Emerald primary (#10B981)
        val primaryDark = Color.rgb(5, 150, 105)
        val darkBg = Color.rgb(15, 23, 42) // Slate 900 (#0F172A)
        val cardBg = Color.rgb(248, 250, 252) // Slate 50
        val cardStroke = Color.rgb(226, 232, 240) // Slate 200
        val textPrimary = Color.rgb(30, 41, 59) // Slate 800
        val textSecondary = Color.rgb(100, 116, 139) // Slate 500
        val orangeBurn = Color.rgb(249, 115, 22) // Orange burn (#F97316)
        val blueWater = Color.rgb(14, 165, 233) // Blue water (#0EA5E9)
        val purpleAccent = Color.rgb(139, 92, 246) // Purple (#8B5CF6)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. TOP HEADER ACCENT STRIP
        paint.color = primaryColor
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 6f, paint)

        // 2. HEADER BANNER (Slate 900)
        paint.color = darkBg
        canvas.drawRect(0f, 6f, pageWidth.toFloat(), 88f, paint)

        // Brand & Title
        paint.color = primaryColor
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10f
        canvas.drawText("FITPAL HEALTH & FITNESS ENGINE", 36f, 26f, paint)

        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 18f
        canvas.drawText("Weekly Fitness & Calorie Summary Report", 36f, 48f, paint)

        paint.color = Color.rgb(148, 163, 184)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9.5f
        val dateRangeStr = DateUtils.formatRangeForDisplay(data.startDate, data.endDate)
        canvas.drawText("Date Range: $dateRangeStr  •  Personal Tracking Log", 36f, 64f, paint)

        // User profile pill on top right
        val userName = data.userProfile?.name?.ifBlank { "Member" } ?: "Member"
        val userGoal = data.userProfile?.weightGoalType ?: "Fitness & Health"
        val weightStr = data.userProfile?.currentWeightKg?.let { " • ${it}kg" } ?: ""

        val userBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(40, 255, 255, 255)
            style = Paint.Style.FILL
        }
        val userBadgeRect = RectF(pageWidth - 210f, 22f, pageWidth - 36f, 72f)
        canvas.drawRoundRect(userBadgeRect, 8f, 8f, userBadgePaint)

        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10.5f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText(userName, pageWidth - 200f, 40f, paint)

        paint.color = Color.rgb(203, 213, 225)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8.5f
        canvas.drawText("Goal: $userGoal$weightStr", pageWidth - 200f, 54f, paint)
        canvas.drawText("Calorie Target: ${data.calorieGoal} kcal/day", pageWidth - 200f, 66f, paint)

        // 3. EXECUTIVE KPI CARDS (4 horizontally spaced cards)
        val kpiTop = 100f
        val kpiHeight = 58f
        val marginX = 36f
        val usableWidth = pageWidth - (marginX * 2)
        val gap = 10f
        val cardWidth = (usableWidth - (gap * 3)) / 4f

        val kpiList = listOf(
            Triple("Avg Daily Intake", "${data.avgDailyFoodCalories} kcal", "Target: ${data.calorieGoal} kcal"),
            Triple("Total Active Burn", "${data.totalBurnedCalories} kcal", "Avg: ${data.avgDailyBurnedCalories} kcal/d"),
            Triple("Net Calorie Balance", "${if (data.netCalorieBalance > 0) "+" else ""}${data.netCalorieBalance} kcal", "Daily Avg: ${data.avgDailyNetCalories} kcal"),
            Triple("Total Steps Logged", String.format(Locale.US, "%,d", data.totalSteps), "Avg: ${String.format(Locale.US, "%,d", data.avgDailySteps)}/d")
        )

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cardStroke
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cardBg
            style = Paint.Style.FILL
        }

        kpiList.forEachIndexed { index, (title, value, subtext) ->
            val left = marginX + index * (cardWidth + gap)
            val rect = RectF(left, kpiTop, left + cardWidth, kpiTop + kpiHeight)
            canvas.drawRoundRect(rect, 8f, 8f, fillPaint)
            canvas.drawRoundRect(rect, 8f, 8f, strokePaint)

            // Accent bar on top of card
            val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = when (index) {
                    0 -> primaryColor
                    1 -> orangeBurn
                    2 -> purpleAccent
                    else -> blueWater
                }
            }
            canvas.drawRoundRect(RectF(left, kpiTop, left + cardWidth, kpiTop + 3.5f), 4f, 4f, accentPaint)

            // Title
            paint.color = textSecondary
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 8.5f
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText(title, left + 10f, kpiTop + 18f, paint)

            // Value
            paint.color = textPrimary
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 12.5f
            canvas.drawText(value, left + 10f, kpiTop + 36f, paint)

            // Subtext
            paint.color = textSecondary
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 7.5f
            canvas.drawText(subtext, left + 10f, kpiTop + 49f, paint)
        }

        // 4. WEEKLY CALORIE INTAKE VS EXPENDITURE VISUAL BAR CHART
        val chartTop = 172f
        val chartHeight = 135f
        val chartRect = RectF(marginX, chartTop, pageWidth - marginX, chartTop + chartHeight)
        canvas.drawRoundRect(chartRect, 8f, 8f, fillPaint)
        canvas.drawRoundRect(chartRect, 8f, 8f, strokePaint)

        // Chart Title & Legend
        paint.color = textPrimary
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Weekly Calorie Intake & Burn Comparison", marginX + 14f, chartTop + 18f, paint)

        // Legend items on right
        val legendPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val legendRight = pageWidth - marginX - 14f
        var currentLegendX = legendRight

        // Burned legend
        paint.color = textSecondary
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val burnedText = "Burned"
        val burnedTextWidth = paint.measureText(burnedText)
        currentLegendX -= burnedTextWidth
        canvas.drawText(burnedText, currentLegendX, chartTop + 18f, paint)
        currentLegendX -= 12f
        legendPaint.color = orangeBurn
        canvas.drawCircle(currentLegendX + 4f, chartTop + 15f, 3.5f, legendPaint)

        // Consumed legend
        currentLegendX -= 16f
        val eatenText = "Consumed"
        val eatenTextWidth = paint.measureText(eatenText)
        currentLegendX -= eatenTextWidth
        canvas.drawText(eatenText, currentLegendX, chartTop + 18f, paint)
        currentLegendX -= 12f
        legendPaint.color = primaryColor
        canvas.drawCircle(currentLegendX + 4f, chartTop + 15f, 3.5f, legendPaint)

        // Target Line legend
        currentLegendX -= 16f
        val targetText = "Target (${data.calorieGoal})"
        val targetTextWidth = paint.measureText(targetText)
        currentLegendX -= targetTextWidth
        canvas.drawText(targetText, currentLegendX, chartTop + 18f, paint)
        currentLegendX -= 14f
        legendPaint.color = Color.rgb(148, 163, 184)
        legendPaint.strokeWidth = 1.5f
        canvas.drawLine(currentLegendX, chartTop + 15f, currentLegendX + 10f, chartTop + 15f, legendPaint)

        // Draw Chart Bars
        val plotLeft = marginX + 32f
        val plotRight = pageWidth - marginX - 20f
        val plotBottom = chartTop + chartHeight - 24f
        val plotTop = chartTop + 32f
        val plotWidth = plotRight - plotLeft
        val plotH = plotBottom - plotTop

        // Compute max calorie for scale
        val maxCalorieInWeek = maxOf(
            data.dailySummaries.maxOfOrNull { maxOf(it.foodCalories, it.totalBurned) } ?: 2500,
            data.calorieGoal + 500
        ).toFloat()

        // Target line
        val targetY = plotBottom - ((data.calorieGoal / maxCalorieInWeek) * plotH)
        val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 1f
            pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
        }
        canvas.drawLine(plotLeft, targetY, plotRight, targetY, dashPaint)

        // Bottom baseline
        val baselinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cardStroke
            strokeWidth = 1f
        }
        canvas.drawLine(plotLeft, plotBottom, plotRight, plotBottom, baselinePaint)

        val colWidth = plotWidth / (if (data.dailySummaries.isNotEmpty()) data.dailySummaries.size else 7)
        val barWidth = 11f
        val barGap = 3f

        data.dailySummaries.forEachIndexed { index, day ->
            val colCenter = plotLeft + (index * colWidth) + (colWidth / 2f)

            // Day label
            paint.color = textSecondary
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 8.5f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(day.dayOfWeek, colCenter, plotBottom + 13f, paint)

            val eatenH = ((day.foodCalories / maxCalorieInWeek) * plotH).coerceAtLeast(2f)
            val burnedH = ((day.totalBurned / maxCalorieInWeek) * plotH).coerceAtLeast(2f)

            // Food bar (Emerald)
            val eatenLeft = colCenter - barWidth - (barGap / 2f)
            val eatenTop = plotBottom - eatenH
            val eatenRect = RectF(eatenLeft, eatenTop, eatenLeft + barWidth, plotBottom)
            paint.color = primaryColor
            canvas.drawRoundRect(eatenRect, 3f, 3f, paint)

            // Burned bar (Orange)
            val burnedLeft = colCenter + (barGap / 2f)
            val burnedTop = plotBottom - burnedH
            val burnedRect = RectF(burnedLeft, burnedTop, burnedLeft + barWidth, plotBottom)
            paint.color = orangeBurn
            canvas.drawRoundRect(burnedRect, 3f, 3f, paint)

            // Value text on top of bar if space permits
            paint.color = textPrimary
            paint.textSize = 7f
            if (day.foodCalories > 0) {
                canvas.drawText("${day.foodCalories}", eatenLeft + (barWidth / 2f), (eatenTop - 3f).coerceAtLeast(plotTop), paint)
            }
        }

        // 5. 7-DAY COMPREHENSIVE PERFORMANCE TABLE
        val tableTop = 320f
        val rowHeight = 22f
        val headerHeight = 24f

        paint.color = textPrimary
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10.5f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("7-Day Nutritional & Fitness Breakdown", marginX, tableTop - 8f, paint)

        // Column widths
        // Total usable: 523pt
        val colDateW = 75f
        val colFoodW = 52f
        val colBurnW = 52f
        val colNetW = 54f
        val colProtW = 52f
        val colCarbW = 52f
        val colFatW = 46f
        val colWaterW = 60f
        val colStepsW = 80f

        // Table Header
        val headerRect = RectF(marginX, tableTop, pageWidth - marginX, tableTop + headerHeight)
        paint.color = darkBg
        canvas.drawRoundRect(headerRect, 6f, 6f, paint)

        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8f
        paint.textAlign = Paint.Align.LEFT

        var curX = marginX + 10f
        canvas.drawText("DATE / DAY", curX, tableTop + 15f, paint)
        curX += colDateW
        canvas.drawText("EATEN", curX, tableTop + 15f, paint)
        curX += colFoodW
        canvas.drawText("BURNED", curX, tableTop + 15f, paint)
        curX += colBurnW
        canvas.drawText("NET CALS", curX, tableTop + 15f, paint)
        curX += colNetW
        canvas.drawText("PROTEIN", curX, tableTop + 15f, paint)
        curX += colProtW
        canvas.drawText("CARBS", curX, tableTop + 15f, paint)
        curX += colCarbW
        canvas.drawText("FAT", curX, tableTop + 15f, paint)
        curX += colFatW
        canvas.drawText("WATER", curX, tableTop + 15f, paint)
        curX += colWaterW
        canvas.drawText("STEPS", curX, tableTop + 15f, paint)

        // Table Rows
        var rowY = tableTop + headerHeight
        data.dailySummaries.forEachIndexed { i, day ->
            // Zebra striping
            val rowRect = RectF(marginX, rowY, pageWidth - marginX, rowY + rowHeight)
            paint.color = if (i % 2 == 0) Color.rgb(255, 255, 255) else Color.rgb(248, 250, 252)
            canvas.drawRect(rowRect, paint)

            // Divider line
            paint.color = Color.rgb(241, 245, 249)
            paint.strokeWidth = 0.5f
            canvas.drawLine(marginX, rowY + rowHeight, pageWidth - marginX, rowY + rowHeight, paint)

            // Content
            paint.color = textPrimary
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 8f
            paint.textAlign = Paint.Align.LEFT

            curX = marginX + 10f
            // Date / Day
            val dateLabel = "${day.dayOfWeek}, ${DateUtils.formatDateForDisplayShort(day.date)}"
            canvas.drawText(dateLabel, curX, rowY + 14f, paint)
            curX += colDateW

            // Food
            canvas.drawText("${day.foodCalories} kcal", curX, rowY + 14f, paint)
            curX += colFoodW

            // Burned
            canvas.drawText("${day.totalBurned} kcal", curX, rowY + 14f, paint)
            curX += colBurnW

            // Net
            val netColor = if (day.netCalories <= day.calorieGoal) primaryDark else orangeBurn
            paint.color = netColor
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("${day.netCalories} kcal", curX, rowY + 14f, paint)
            curX += colNetW

            // Macros
            paint.color = textPrimary
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("${day.proteinGrams.toInt()}g", curX, rowY + 14f, paint)
            curX += colProtW

            canvas.drawText("${day.carbsGrams.toInt()}g", curX, rowY + 14f, paint)
            curX += colCarbW

            canvas.drawText("${day.fatGrams.toInt()}g", curX, rowY + 14f, paint)
            curX += colFatW

            // Water
            canvas.drawText(if (day.waterMl > 0) "${day.waterMl}ml" else "-", curX, rowY + 14f, paint)
            curX += colWaterW

            // Steps
            canvas.drawText(if (day.steps > 0) String.format(Locale.US, "%,d", day.steps) else "-", curX, rowY + 14f, paint)

            rowY += rowHeight
        }

        // Summary / Average Row
        val totalRowRect = RectF(marginX, rowY, pageWidth - marginX, rowY + rowHeight + 2f)
        paint.color = Color.rgb(241, 245, 249)
        canvas.drawRect(totalRowRect, paint)

        paint.color = darkBg
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8.5f
        paint.textAlign = Paint.Align.LEFT

        curX = marginX + 10f
        canvas.drawText("DAILY AVERAGE", curX, rowY + 15f, paint)
        curX += colDateW
        canvas.drawText("${data.avgDailyFoodCalories} kcal", curX, rowY + 15f, paint)
        curX += colFoodW
        canvas.drawText("${data.avgDailyBurnedCalories} kcal", curX, rowY + 15f, paint)
        curX += colBurnW
        canvas.drawText("${data.avgDailyNetCalories} kcal", curX, rowY + 15f, paint)
        curX += colNetW
        canvas.drawText("${data.avgDailyProteinGrams.toInt()}g", curX, rowY + 15f, paint)
        curX += colProtW
        canvas.drawText("${data.avgDailyCarbsGrams.toInt()}g", curX, rowY + 15f, paint)
        curX += colCarbW
        canvas.drawText("${data.avgDailyFatGrams.toInt()}g", curX, rowY + 15f, paint)
        curX += colFatW
        canvas.drawText("${data.avgDailyWaterMl}ml", curX, rowY + 15f, paint)
        curX += colWaterW
        canvas.drawText(String.format(Locale.US, "%,d", data.avgDailySteps), curX, rowY + 15f, paint)

        // 6. MACRONUTRIENT DISTRIBUTION & WORKOUT HIGHLIGHTS (2 side-by-side cards)
        val lowerSectionTop = rowY + rowHeight + 16f
        val lowerCardH = 110f
        val halfW = (usableWidth - gap) / 2f

        // Card A: Macronutrient Ratio Analysis
        val macroRect = RectF(marginX, lowerSectionTop, marginX + halfW, lowerSectionTop + lowerCardH)
        canvas.drawRoundRect(macroRect, 8f, 8f, fillPaint)
        canvas.drawRoundRect(macroRect, 8f, 8f, strokePaint)

        paint.color = textPrimary
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9.5f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Macronutrient Distribution", marginX + 12f, lowerSectionTop + 18f, paint)

        val totalMacroGrams = (data.totalProteinGrams + data.totalCarbsGrams + data.totalFatGrams).coerceAtLeast(1f)
        val pPct = ((data.totalProteinGrams / totalMacroGrams) * 100).toInt()
        val cPct = ((data.totalCarbsGrams / totalMacroGrams) * 100).toInt()
        val fPct = 100 - pPct - cPct

        // Stacked macro bar
        val barTop = lowerSectionTop + 28f
        val barH = 10f
        val barTotalW = halfW - 24f
        val pW = (pPct / 100f) * barTotalW
        val cW = (cPct / 100f) * barTotalW
        val fW = (fPct / 100f) * barTotalW

        var macroBarX = marginX + 12f
        paint.color = primaryColor
        canvas.drawRoundRect(RectF(macroBarX, barTop, macroBarX + pW, barTop + barH), 2f, 2f, paint)
        macroBarX += pW
        paint.color = blueWater
        canvas.drawRoundRect(RectF(macroBarX, barTop, macroBarX + cW, barTop + barH), 2f, 2f, paint)
        macroBarX += cW
        paint.color = orangeBurn
        canvas.drawRoundRect(RectF(macroBarX, barTop, macroBarX + fW, barTop + barH), 2f, 2f, paint)

        // Labels
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = textPrimary
        val macroTextY = barTop + barH + 16f
        canvas.drawText("• Protein: ${data.totalProteinGrams.toInt()}g total ($pPct%)  •  Target: ${data.proteinGoal}g/d", marginX + 12f, macroTextY, paint)
        canvas.drawText("• Carbs: ${data.totalCarbsGrams.toInt()}g total ($cPct%)", marginX + 12f, macroTextY + 14f, paint)
        canvas.drawText("• Fat: ${data.totalFatGrams.toInt()}g total ($fPct%)", marginX + 12f, macroTextY + 28f, paint)

        val totalWaterLiters = String.format(Locale.US, "%.1f", data.totalWaterMl / 1000f)
        canvas.drawText("• Hydration Total: $totalWaterLiters Liters (${data.avgDailyWaterMl} ml/day)", marginX + 12f, macroTextY + 42f, paint)

        // Card B: Personal Coaching Insights & Highlights
        val insightRect = RectF(marginX + halfW + gap, lowerSectionTop, pageWidth - marginX, lowerSectionTop + lowerCardH)
        canvas.drawRoundRect(insightRect, 8f, 8f, fillPaint)
        canvas.drawRoundRect(insightRect, 8f, 8f, strokePaint)

        paint.color = textPrimary
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9.5f
        canvas.drawText("Personal Progress & Habit Insights", marginX + halfW + gap + 12f, lowerSectionTop + 18f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8f
        val insightX = marginX + halfW + gap + 12f
        var insightY = lowerSectionTop + 34f

        val netDeficit = (data.calorieGoal * data.dailySummaries.size) - data.totalFoodCalories
        val deficitText = if (netDeficit > 0) {
            "• Calorie Deficit: ${netDeficit} kcal under target (~${String.format(Locale.US, "%.1f", netDeficit / 3500f)} lbs fat loss pace)"
        } else {
            "• Calorie Surplus: ${-netDeficit} kcal over target"
        }
        canvas.drawText(deficitText, insightX, insightY, paint)
        insightY += 14f

        val workoutText = "• Workouts: ${data.totalWorkouts} sessions logged across 7 days"
        canvas.drawText(workoutText, insightX, insightY, paint)
        insightY += 14f

        val stepGoalText = if (data.avgDailySteps >= 8000) {
            "• Active Steps: Excellent daily average of ${String.format(Locale.US, "%,d", data.avgDailySteps)} steps/day"
        } else {
            "• Active Steps: ${String.format(Locale.US, "%,d", data.avgDailySteps)} avg daily steps logged"
        }
        canvas.drawText(stepGoalText, insightX, insightY, paint)
        insightY += 14f

        val adherenceText = "• Calorie Target Adherence: ${data.onTargetDays} of 7 days strictly on budget"
        canvas.drawText(adherenceText, insightX, insightY, paint)
        insightY += 14f

        val streakText = "• Personal Status: Weekly progress logged with local Room integrity"
        canvas.drawText(streakText, insightX, insightY, paint)

        // 7. FOOTER
        val footerY = pageHeight - 32f
        paint.color = Color.rgb(203, 213, 225)
        paint.strokeWidth = 0.5f
        canvas.drawLine(marginX, footerY - 10f, pageWidth - marginX, footerY - 10f, paint)

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm", Locale.US))
        paint.color = textSecondary
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 7.5f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Generated by FitPal on $timestamp  •  Confidential personal health tracking report", marginX, footerY, paint)

        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("fitpal.app  •  Page 1 of 1", pageWidth - marginX, footerY, paint)

        document.finishPage(page)

        // Save PDF to cacheDir/shared_reports/
        val reportsDir = File(context.cacheDir, "shared_reports")
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }

        val pdfFile = File(reportsDir, "FitPal_Weekly_Report_${data.startDate}_to_${data.endDate}.pdf")
        val outputStream = FileOutputStream(pdfFile)
        document.writeTo(outputStream)
        outputStream.flush()
        outputStream.close()
        document.close()

        pdfFile
    }

    fun shareReport(context: Context, file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "FitPal Weekly Fitness & Calorie Report")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Here is my summarized FitPal weekly fitness and calorie metrics report."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share Weekly PDF Report"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not share PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun viewReport(context: Context, file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(viewIntent)
        } catch (e: Exception) {
            // Fallback to share chooser if no dedicated PDF viewer exists
            shareReport(context, file)
        }
    }
}
