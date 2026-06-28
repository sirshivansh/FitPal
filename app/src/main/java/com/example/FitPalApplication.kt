package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.repository.*
import com.example.notification.NotificationHelper

class FitPalApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
    }

    private val database by lazy { AppDatabase.getDatabase(this) }

    val userRepository by lazy { UserRepository(database.userProfileDao()) }
    val foodRepository by lazy { FoodRepository(database.foodDao()) }
    val exerciseRepository by lazy { ExerciseRepository(database.exerciseDao()) }
    val habitRepository by lazy { HabitRepository(database.habitDao()) }
    val progressRepository by lazy {
        ProgressRepository(
            database.weightLogDao(),
            database.waterLogDao(),
            database.dailyActivityCalorieDao(),
            database.progressPhotoDao()
        )
    }
}
