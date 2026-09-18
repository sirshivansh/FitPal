package com.example.fitpal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_calorie_logs")
data class ActivityCalorieLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val additionalCalories: Int,
    val steps: Int = 0,
    val date: String // "yyyy-MM-dd"
)
