package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1, // Single-user profile, constant ID
    val name: String,
    val age: Int,
    val gender: String, // "male" or "female"
    val heightCm: Float,
    val currentWeightKg: Float,
    val goalWeightKg: Float,
    val bmr: Float,
    val dailyCalorieGoal: Int,
    val proteinGoalGrams: Int,
    val carbsGoalGrams: Int,
    val fatGoalGrams: Int,
    val weightGoalType: String, // "lose", "maintain", "gain"
    val calorieAdjustment: Int = 0, // Selected deficit or surplus (e.g. 500)
    val proteinMultiplier: Float = 1.6f, // Selected multiplier (e.g. 1.6 g/kg)
    val fatMultiplier: Float = 0.8f, // Selected multiplier (e.g. 0.8 g/kg)
    val maintenanceCalories: Int? = null
)

@Entity(tableName = "daily_activity_calorie_log")
data class DailyActivityCalorieLog(
    @PrimaryKey val date: String, // YYYY-MM-DD
    val additionalCalories: Int
)

@Entity(tableName = "food_entry")
data class FoodEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val mealType: String, // "Breakfast", "Lunch", "Dinner", "Snacks"
    val foodName: String,
    val gramsConsumed: Float,
    val caloriesConsumed: Float,
    val proteinGrams: Float,
    val carbsGrams: Float,
    val fatGrams: Float,
    val isCustomFood: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "exercise_entry")
data class ExerciseEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val exerciseName: String,
    val category: String, // Cardio, Strength Training, Flexibility, Sports, Daily Activities
    val durationMinutes: Int,
    val intensity: String, // "Light", "Moderate", "Vigorous"
    val metValue: Float,
    val caloriesBurned: Float,
    val isCustomExercise: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "food_database")
data class FoodDatabase(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String,
    val caloriesPer100g: Float,
    val proteinPer100g: Float,
    val carbsPer100g: Float,
    val fatPer100g: Float,
    val defaultServingGrams: Float
)

@Entity(tableName = "exercise_database")
data class ExerciseDatabase(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String,
    val metValueLight: Float,
    val metValueModerate: Float,
    val metValueVigorous: Float
)

@Entity(tableName = "weight_log")
data class WeightLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val weightKg: Float
)

@Entity(tableName = "water_log")
data class WaterLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val glassesCount: Int
)

@Entity(tableName = "custom_food")
data class CustomFood(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val caloriesPer100g: Float,
    val proteinPer100g: Float,
    val carbsPer100g: Float,
    val fatPer100g: Float,
    val defaultServingGrams: Float
)

@Entity(tableName = "custom_exercise")
data class CustomExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val metValue: Float
)

@Entity(tableName = "habit")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val colorHex: String, // e.g. "#9C27B0"
    val iconName: String, // e.g. "meditation", "side_hustle", "drums", "running", "coffee"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "habit_completion")
data class HabitCompletion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val date: String // YYYY-MM-DD
)

@Entity(tableName = "progress_photo")
data class ProgressPhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val imagePath: String,
    val notes: String = ""
)
