package com.example.ui.achievements

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val category: AchievementCategory,
    val currentProgress: Float,
    val maxProgress: Float,
    val isUnlocked: Boolean,
    val rewardText: String
)

enum class AchievementCategory(val displayName: String) {
    STREAKS("Streaks"),
    WEIGHT("Weight Goals"),
    NUTRITION("Nutrition"),
    EXERCISE("Exercise")
}
