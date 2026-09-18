package com.example.fitpal.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.fitpal.data.local.entity.FoodEntry

object ShareHelper {
    fun shareDailyNutritionImage(
        context: Context,
        dateStr: String,
        isDarkTheme: Boolean,
        totalFoodCalories: Int,
        totalExerciseCalories: Int,
        calorieGoal: Int,
        proteinGrams: Int,
        proteinGoal: Int,
        carbsGrams: Int,
        carbsGoal: Int,
        fatGrams: Int,
        fatGoal: Int
    ) {
        val report = """
            🍏 FitPal Daily Nutrition Report for $dateStr 🍏
            ------------------------------------------------
            🔥 Calories: $totalFoodCalories / $calorieGoal kcal
            🏃 Active Burn: $totalExerciseCalories kcal
            
            🥩 Protein: ${proteinGrams}g / ${proteinGoal}g
            🍞 Carbs: ${carbsGrams}g / ${carbsGoal}g
            🥑 Fat: ${fatGrams}g / ${fatGoal}g
            
            Generated with FitPal App
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "My FitPal Nutrition Summary")
            putExtra(Intent.EXTRA_TEXT, report)
        }
        context.startActivity(Intent.createChooser(intent, "Share Daily Summary"))
    }

    /**
     * One-click "Share What I Ate Today" with meal breakdown, daily macro targets,
     * native share sheet invocation, and automatic clipboard fallback.
     */
    fun shareWhatIAteToday(
        context: Context,
        dateStr: String,
        foodEntries: List<FoodEntry>,
        totalCalories: Int,
        calorieGoal: Int,
        totalProtein: Float,
        proteinGoal: Int,
        totalCarbs: Float,
        carbsGoal: Int,
        totalFat: Float,
        fatGoal: Int
    ) {
        val mealCategories = listOf("Breakfast", "Lunch", "Dinner", "Snacks")
        val breakdown = StringBuilder()

        mealCategories.forEach { meal ->
            val items = foodEntries.filter { it.mealType.equals(meal, ignoreCase = true) }
            if (items.isNotEmpty()) {
                val mealKcal = items.sumOf { it.caloriesConsumed.toInt() }
                breakdown.append("\n🍽️ $meal ($mealKcal kcal):\n")
                items.forEach { item ->
                    val gramsStr = if (item.gramsConsumed > 0) " • ${item.gramsConsumed.toInt()}g" else ""
                    breakdown.append("   • ${item.foodName} (${item.caloriesConsumed.toInt()} kcal$gramsStr)\n")
                }
            }
        }

        if (foodEntries.isEmpty()) {
            breakdown.append("\nNo meals logged yet today.\n")
        }

        val report = """
            🥗 What I Ate Today - FitPal ($dateStr) 🥗
            ========================================
            $breakdown
            📊 Daily Nutrition & Macro Summary:
            🔥 Calories: $totalCalories / $calorieGoal kcal
            🥩 Protein:  ${totalProtein.toInt()}g / ${proteinGoal}g
            🍞 Carbs:    ${totalCarbs.toInt()}g / ${carbsGoal}g
            🥑 Fats:     ${totalFat.toInt()}g / ${fatGoal}g
            ========================================
            Logged with FitPal (AI Nutrition & Fitness)
        """.trimIndent()

        // 1. Copy to clipboard
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("What I Ate Today - FitPal", report)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Meal summary copied to clipboard!", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
        }

        // 2. Open native sharing chooser
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TITLE, "What I Ate Today - FitPal")
                putExtra(Intent.EXTRA_SUBJECT, "What I Ate Today - FitPal ($dateStr)")
                putExtra(Intent.EXTRA_TEXT, report)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(sendIntent, "Share What I Ate Today").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: Exception) {
        }
    }
}
