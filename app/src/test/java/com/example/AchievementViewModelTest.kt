package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.FoodEntry
import com.example.data.repository.ExerciseRepository
import com.example.data.repository.FoodRepository
import com.example.data.repository.ProgressRepository
import com.example.data.repository.UserRepository
import com.example.ui.achievements.AchievementViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AchievementViewModelTest {

    private lateinit var db: AppDatabase
    private lateinit var userRepository: UserRepository
    private lateinit var foodRepository: FoodRepository
    private lateinit var exerciseRepository: ExerciseRepository
    private lateinit var progressRepository: ProgressRepository
    private lateinit var viewModel: AchievementViewModel

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        
        userRepository = UserRepository(db.userProfileDao())
        foodRepository = FoodRepository(db.foodDao())
        exerciseRepository = ExerciseRepository(db.exerciseDao())
        progressRepository = ProgressRepository(
            db.weightLogDao(),
            db.waterLogDao(),
            db.dailyActivityCalorieDao(),
            db.progressPhotoDao()
        )
        
        viewModel = AchievementViewModel(
            userRepository,
            foodRepository,
            exerciseRepository,
            progressRepository
        )
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testDefaultAchievementsAreLocked() {
        val achievements = viewModel.calculateAchievements(
            profile = null,
            foodEntries = emptyList(),
            exerciseEntries = emptyList(),
            weightLogs = emptyList(),
            waterLogs = emptyList()
        )
        assertTrue(achievements.isNotEmpty())
        // All should be locked initially because no logs exist
        assertTrue(achievements.all { !it.isUnlocked })
    }

    @Test
    fun testHydrationHeroAchievementUnlocks() {
        val waterLogs = listOf(
            com.example.data.local.entity.WaterLog(date = "2026-06-26", glassesCount = 8)
        )
        val achievements = viewModel.calculateAchievements(
            profile = null,
            foodEntries = emptyList(),
            exerciseEntries = emptyList(),
            weightLogs = emptyList(),
            waterLogs = waterLogs
        )
        val hydrationHero = achievements.first { it.id == "water_8" }
        
        assertTrue(hydrationHero.isUnlocked)
        assertEquals(8f, hydrationHero.currentProgress)
    }

    @Test
    fun testConsistencyStreakAchievement() {
        val foodEntries = listOf(
            FoodEntry(date = "2026-06-24", mealType = "Breakfast", foodName = "Apple", gramsConsumed = 100f, caloriesConsumed = 52f, proteinGrams = 0.3f, carbsGrams = 14f, fatGrams = 0.2f),
            FoodEntry(date = "2026-06-25", mealType = "Breakfast", foodName = "Apple", gramsConsumed = 100f, caloriesConsumed = 52f, proteinGrams = 0.3f, carbsGrams = 14f, fatGrams = 0.2f),
            FoodEntry(date = "2026-06-26", mealType = "Breakfast", foodName = "Apple", gramsConsumed = 100f, caloriesConsumed = 52f, proteinGrams = 0.3f, carbsGrams = 14f, fatGrams = 0.2f)
        )
        val achievements = viewModel.calculateAchievements(
            profile = null,
            foodEntries = foodEntries,
            exerciseEntries = emptyList(),
            weightLogs = emptyList(),
            waterLogs = emptyList()
        )
        val streak3 = achievements.first { it.id == "streak_3" }
        
        assertTrue(streak3.isUnlocked)
        assertEquals(3f, streak3.currentProgress)
    }
}
