package com.example.fitpal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "water_logs")
data class WaterLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val glassesCount: Int,
    val milliliters: Int = 0,
    val date: String // "yyyy-MM-dd"
)
