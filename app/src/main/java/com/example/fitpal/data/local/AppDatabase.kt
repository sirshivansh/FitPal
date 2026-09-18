package com.example.fitpal.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.fitpal.data.local.dao.*
import com.example.fitpal.data.local.entity.*

@Database(
    entities = [
        UserProfile::class,
        FoodEntry::class,
        ExerciseEntry::class,
        WorkoutEntry::class,
        WaterLog::class,
        ActivityCalorieLog::class,
        WeightLog::class,
        ProgressPhoto::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): UserProfileDao
    abstract fun foodDao(): FoodDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun waterDao(): WaterDao
    abstract fun activityCalorieDao(): ActivityCalorieDao
    abstract fun weightDao(): WeightDao
    abstract fun photoDao(): PhotoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fitpal_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
