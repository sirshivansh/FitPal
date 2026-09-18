package com.example.fitpal

import android.app.Application
import com.example.fitpal.data.local.AppDatabase
import com.example.fitpal.data.repository.FoodRepository
import com.example.fitpal.data.repository.ExerciseRepository
import com.example.fitpal.data.repository.ProfileRepository
import com.example.fitpal.data.sync.CloudSyncManager

class FitPalApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val foodRepository by lazy { FoodRepository(database.foodDao()) }
    val exerciseRepository by lazy { ExerciseRepository(database.exerciseDao(), database.workoutDao()) }
    val cloudSyncManager by lazy { CloudSyncManager.getInstance(this) }
    val profileRepository by lazy { 
        ProfileRepository(
            database.profileDao(), 
            database.weightDao(), 
            database.waterDao(), 
            database.activityCalorieDao(), 
            database.photoDao()
        ) 
    }
}
