package com.example.fitpal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val age: Int,
    val gender: String,
    val heightCm: Float,
    val currentWeightKg: Float,
    val goalWeightKg: Float,
    val weightGoalType: String,
    val calorieAdjustment: Int,
    val proteinMultiplier: Float,
    val fatMultiplier: Float,
    val maintenanceCalories: Int?,
    val bmr: Float,
    val activityLevel: String = "Moderate",
    val bodyFat: Float? = null,
    val customCalorieGoal: Int? = null
)
