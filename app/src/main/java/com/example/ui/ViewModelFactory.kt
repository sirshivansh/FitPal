package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.FitPalApplication
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.diary.DiaryViewModel
import com.example.ui.exercise.ExerciseViewModel
import com.example.ui.foodlogging.FoodLogViewModel
import com.example.ui.profile.ProfileViewModel
import com.example.ui.progress.ProgressViewModel

@Suppress("UNCHECKED_CAST")
class ViewModelFactory(private val application: FitPalApplication) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> {
                ProfileViewModel(
                    application.userRepository,
                    application.progressRepository
                ) as T
            }
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> {
                DashboardViewModel(
                    application.userRepository,
                    application.foodRepository,
                    application.exerciseRepository,
                    application.progressRepository
                ) as T
            }
            modelClass.isAssignableFrom(DiaryViewModel::class.java) -> {
                DiaryViewModel(
                    application.userRepository,
                    application.foodRepository,
                    application.exerciseRepository
                ) as T
            }
            modelClass.isAssignableFrom(FoodLogViewModel::class.java) -> {
                FoodLogViewModel(
                    application.foodRepository
                ) as T
            }
            modelClass.isAssignableFrom(ExerciseViewModel::class.java) -> {
                ExerciseViewModel(
                    application.exerciseRepository,
                    application.userRepository
                ) as T
            }
            modelClass.isAssignableFrom(ProgressViewModel::class.java) -> {
                ProgressViewModel(
                    application.progressRepository,
                    application.foodRepository,
                    application.userRepository,
                    application.exerciseRepository
                ) as T
            }
            modelClass.isAssignableFrom(com.example.ui.habit.HabitViewModel::class.java) -> {
                com.example.ui.habit.HabitViewModel(
                    application.habitRepository
                ) as T
            }
            modelClass.isAssignableFrom(com.example.ui.achievements.AchievementViewModel::class.java) -> {
                com.example.ui.achievements.AchievementViewModel(
                    application.userRepository,
                    application.foodRepository,
                    application.exerciseRepository,
                    application.progressRepository
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
