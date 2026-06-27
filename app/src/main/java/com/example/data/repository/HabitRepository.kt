package com.example.data.repository

import com.example.data.local.dao.HabitDao
import com.example.data.local.entity.Habit
import com.example.data.local.entity.HabitCompletion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class HabitRepository(private val habitDao: HabitDao) {

    val allHabits: Flow<List<Habit>> = habitDao.getAllHabits()
    val allCompletions: Flow<List<HabitCompletion>> = habitDao.getAllHabitCompletions()

    suspend fun insertHabit(habit: Habit): Long {
        return habitDao.insertHabit(habit)
    }

    suspend fun deleteHabit(habit: Habit) {
        habitDao.deleteHabit(habit)
    }

    suspend fun completeHabit(habitId: Long, date: String) {
        habitDao.insertHabitCompletion(HabitCompletion(habitId = habitId, date = date))
    }

    suspend fun uncompleteHabit(habitId: Long, date: String) {
        habitDao.deleteHabitCompletion(habitId, date)
    }

    suspend fun clearAllHabitData() {
        habitDao.clearHabits()
        habitDao.clearHabitCompletions()
    }

    suspend fun seedDefaultHabits() {
        val existing = habitDao.getAllHabits().first()
        if (existing.isEmpty()) {
            val defaultHabits = listOf(
                Habit(
                    name = "Meditation",
                    description = "Meditate daily for 15 minutes",
                    colorHex = "#D35400", // Warm Vibrant Orange/Red
                    iconName = "meditation"
                ),
                Habit(
                    name = "Side Hustle",
                    description = "Work on my app business",
                    colorHex = "#F39C12", // Bright Yellow/Gold
                    iconName = "side_hustle"
                ),
                Habit(
                    name = "Play Drums",
                    description = "Exercise drumming for at least 30 minutes",
                    colorHex = "#3498DB", // Electric Blue
                    iconName = "drums"
                ),
                Habit(
                    name = "Running",
                    description = "Go for a jog every other day",
                    colorHex = "#2ECC71", // Fresh Green
                    iconName = "running"
                ),
                Habit(
                    name = "Limit Coffee Intake",
                    description = "Drink a maximum of two cups a day",
                    colorHex = "#E91E63", // Rose/Pink
                    iconName = "coffee"
                )
            )
            for (habit in defaultHabits) {
                val id = habitDao.insertHabit(habit)
                // Let's seed some random historical completions for a beautiful initial layout!
                // We'll generate completions with a random probability over the past 60 days
                val totalDays = 125
                val dates = com.example.util.DateUtils.getDaysRange(totalDays)
                val fillRate = when (habit.name) {
                    "Meditation" -> 0.45f
                    "Side Hustle" -> 0.65f
                    "Play Drums" -> 0.35f
                    "Running" -> 0.55f
                    else -> 0.70f // Limit Coffee
                }
                for (date in dates) {
                    // Skip today sometimes, let user log today!
                    if (date == com.example.util.DateUtils.getTodayDateString()) continue
                    if (java.util.Random().nextFloat() < fillRate) {
                        habitDao.insertHabitCompletion(HabitCompletion(habitId = id, date = date))
                    }
                }
            }
        }
    }
}
