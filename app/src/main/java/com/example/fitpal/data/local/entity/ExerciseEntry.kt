package com.example.fitpal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercise_entries")
data class ExerciseEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val exerciseName: String,
    val durationMinutes: Double,
    val intensity: String,
    val caloriesBurned: Double,
    val date: String // "yyyy-MM-dd"
)
