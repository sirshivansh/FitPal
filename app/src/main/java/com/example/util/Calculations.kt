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
     * Calculate Target Calorie Goal based on Goal Adjustments
     * Maintain: Target Calories = Daily Calories
     * Weight Loss: Target Calories = Daily Calories - Selected Deficit
     * Weight Gain: Target Calories = Daily Calories + Selected Surplus
     */
    fun calculateTargetCalorieGoal(
        dailyCalories: Float,
        weightGoalType: String,
        adjustmentValue: Int // 250, 500, 750 for Lose; 200, 300, 500 for Gain; 0 for Maintain
    ): Int {
        val target = when (weightGoalType.lowercase()) {
            "lose" -> dailyCalories - adjustmentValue
            "gain" -> dailyCalories + adjustmentValue
            else -> dailyCalories
        }
        return target.roundToInt().coerceAtLeast(1000)
    }

    /**
     * Custom Macro Calculations using protein and fat multipliers (g/kg).
     * Protein = Weight * Protein Multiplier
     * Protein Calories = Protein * 4
     * Fat = Weight * Fat Multiplier
     * Fat Calories = Fat * 9
     * Remaining Calories = Target Calories - Protein Calories - Fat Calories
     * Carbs = Remaining Calories / 4
     */
    fun calculateMacrosCustom(
        targetCalories: Int,
        weightKg: Float,
        proteinMultiplier: Float,
        fatMultiplier: Float
    ): Triple<Int, Int, Int> {
        val proteinGrams = (weightKg * proteinMultiplier).roundToInt().coerceAtLeast(0)
        val proteinCalories = proteinGrams * 4

        val fatGrams = (weightKg * fatMultiplier).roundToInt().coerceAtLeast(0)
        val fatCalories = fatGrams * 9

        val remainingCalories = targetCalories - proteinCalories - fatCalories
        val carbsGrams = (remainingCalories / 4f).roundToInt().coerceAtLeast(0)

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
