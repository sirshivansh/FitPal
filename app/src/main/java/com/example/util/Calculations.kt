package com.example.util

import kotlin.math.roundToInt

object Calculations {

    /**
     * BMR using Mifflin-St Jeor equation.
     * Male: 10 * weight(kg) + 6.25 * height(cm) - 5 * age + 5
     * Female: 10 * weight(kg) + 6.25 * height(cm) - 5 * age - 161
     */
    fun calculateBmr(
        weightKg: Float,
        heightCm: Float,
        age: Int,
        gender: String
    ): Float {
        val base = (10f * weightKg) + (6.25f * heightCm) - (5f * age)
        return if (gender.lowercase() == "male") {
            base + 5f
        } else {
            base - 161f
        }
    }

    /**
     * Calculate Daily Calories (before goal adjustments)
     * Daily Calories = BMR + Additional Activity Calories
     */
    fun calculateDailyCalories(bmr: Float, additionalActivityCalories: Int): Float {
        return bmr + additionalActivityCalories
    }

    /**
     * Calculate Target Calorie Goal based on Goal Adjustments with adaptive safety floors.
     * Maintain: Target Calories = Daily Calories
     * Weight Loss: Target Calories = Daily Calories - Selected Deficit
     * Weight Gain: Target Calories = Daily Calories + Selected Surplus
     */
    fun calculateTargetCalorieGoal(
        dailyCalories: Float,
        weightGoalType: String,
        adjustmentValue: Int, // 250, 500, 750 for Lose; 200, 300, 500 for Gain; 0 for Maintain
        bmr: Float = 0f,
        gender: String = "male"
    ): Int {
        val target = when (weightGoalType.lowercase()) {
            "lose" -> dailyCalories - adjustmentValue
            "gain" -> dailyCalories + adjustmentValue
            else -> dailyCalories
        }
        
        // Ensure a medically sound and healthy caloric floor to prevent starvation.
        // For males, minimum is 1500 kcal or BMR * 0.85.
        // For females, minimum is 1200 kcal or BMR * 0.85.
        val genderMin = if (gender.lowercase() == "female") 1200f else 1500f
        val safetyFloor = if (bmr > 0f) {
            maxOf(genderMin, bmr * 0.85f)
        } else {
            genderMin
        }
        
        return target.roundToInt().coerceAtLeast(safetyFloor.roundToInt())
    }

    /**
     * Custom Macro Calculations using protein and fat multipliers (g/kg).
     * Protein = Weight * Protein Multiplier
     * Protein Calories = Protein * 4
     * Fat = Weight * Fat Multiplier
     * Fat Calories = Fat * 9
     * Remaining Calories = Target Calories - Protein Calories - Fat Calories
     * Carbs = Remaining Calories / 4
     * 
     * Includes dynamic scaling to prevent low-carb starvation on calorie restriction.
     */
    fun calculateMacrosCustom(
        targetCalories: Int,
        weightKg: Float,
        proteinMultiplier: Float,
        fatMultiplier: Float
    ): Triple<Int, Int, Int> {
        val rawProteinGrams = (weightKg * proteinMultiplier).coerceAtLeast(10f)
        val rawProteinCalories = rawProteinGrams * 4f

        val rawFatGrams = (weightKg * fatMultiplier).coerceAtLeast(10f)
        val rawFatCalories = rawFatGrams * 9f

        val rawTotalMacsCalories = rawProteinCalories + rawFatCalories
        
        // To prevent extreme low-carb starvation (especially for heavier individuals on a caloric deficit),
        // we allocate at least 40% of target calories to carbohydrates.
        // If protein and fat consume more than 60% of target calories, we scale them down proportionally.
        val maxAllocatedCalories = targetCalories * 0.60f
        
        val (proteinGrams, fatGrams) = if (rawTotalMacsCalories > maxAllocatedCalories) {
            val scale = maxAllocatedCalories / rawTotalMacsCalories
            val scaledProtein = (rawProteinGrams * scale).roundToInt().coerceAtLeast(40)
            val scaledFat = (rawFatGrams * scale).roundToInt().coerceAtLeast(30)
            Pair(scaledProtein, scaledFat)
        } else {
            Pair(rawProteinGrams.roundToInt(), rawFatGrams.roundToInt())
        }

        val proteinCalories = proteinGrams * 4
        val fatCalories = fatGrams * 9
        val remainingCalories = targetCalories - proteinCalories - fatCalories
        
        // Carbs receive the remaining calorie allocation, enforced to at least 40% of target calories or 130g (RDA).
        val minCarbsGrams = ((targetCalories * 0.40f) / 4f).roundToInt().coerceAtLeast(130)
        val carbsGrams = (remainingCalories / 4f).roundToInt().coerceAtLeast(minCarbsGrams)

        return Triple(carbsGrams, proteinGrams, fatGrams)
    }

    /**
     * Calculate macros consumed based on grams eaten
     */
    fun calculateFoodMacros(
        caloriesPer100g: Float,
        proteinPer100g: Float,
        carbsPer100g: Float,
        fatPer100g: Float,
        gramsConsumed: Float
    ): FoodMacros {
        val ratio = gramsConsumed / 100f
        return FoodMacros(
            calories = caloriesPer100g * ratio,
            protein = proteinPer100g * ratio,
            carbs = carbsPer100g * ratio,
            fat = fatPer100g * ratio
        )
    }

    /**
     * Exercise Calories Burned
     * caloriesBurned = METValue * weightKg * (durationMinutes / 60)
     */
    fun calculateExerciseCalories(
        metValue: Float,
        weightKg: Float,
        durationMinutes: Int
    ): Float {
        return metValue * weightKg * (durationMinutes.toFloat() / 60f)
    }
}

data class FoodMacros(
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float
)
