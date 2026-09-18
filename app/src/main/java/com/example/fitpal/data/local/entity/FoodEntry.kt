package com.example.fitpal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_entries")
data class FoodEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val foodName: String,
    val mealType: String, // "Breakfast", "Lunch", "Dinner", "Snacks"
    val caloriesConsumed: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val gramsConsumed: Double,
    val date: String // "yyyy-MM-dd"
)
