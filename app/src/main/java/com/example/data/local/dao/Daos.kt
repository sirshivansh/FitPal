package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getUserProfileSync(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfile)

    @Update
    suspend fun updateProfile(profile: UserProfile)

    @Query("DELETE FROM user_profile")
    suspend fun clearProfile()
}

@Dao
interface FoodDao {
    // Food Entries (Diary)
    @Query("SELECT * FROM food_entry WHERE date = :date ORDER BY timestamp ASC")
    fun getFoodEntriesForDate(date: String): Flow<List<FoodEntry>>

    @Query("SELECT * FROM food_entry ORDER BY date DESC")
    fun getAllFoodEntries(): Flow<List<FoodEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodEntry(entry: FoodEntry)

    @Update
    suspend fun updateFoodEntry(entry: FoodEntry)

    @Delete
    suspend fun deleteFoodEntry(entry: FoodEntry)

    @Query("SELECT * FROM food_entry ORDER BY timestamp DESC LIMIT 20")
    fun getRecentFoodEntries(): Flow<List<FoodEntry>>

    // Pre-populated Food Database
    @Query("SELECT * FROM food_database WHERE name LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%'")
    suspend fun searchFoodDatabase(query: String): List<FoodDatabase>

    @Query("SELECT * FROM food_database")
    suspend fun getAllFoodDatabase(): List<FoodDatabase>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodDatabaseList(list: List<FoodDatabase>)

    @Query("SELECT COUNT(*) FROM food_database")
    suspend fun getFoodDatabaseCount(): Int

    // Custom Foods
    @Query("SELECT * FROM custom_food WHERE name LIKE '%' || :query || '%'")
    fun searchCustomFoodsFlow(query: String): Flow<List<CustomFood>>

    @Query("SELECT * FROM custom_food WHERE name LIKE '%' || :query || '%'")
    suspend fun searchCustomFoods(query: String): List<CustomFood>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomFood(food: CustomFood)

    @Delete
    suspend fun deleteCustomFood(food: CustomFood)

    @Query("DELETE FROM food_entry")
    suspend fun clearFoodEntries()

    @Query("DELETE FROM custom_food")
    suspend fun clearCustomFoods()
}

@Dao
interface ExerciseDao {
    // Exercise Entries
    @Query("SELECT * FROM exercise_entry WHERE date = :date ORDER BY timestamp ASC")
    fun getExerciseEntriesForDate(date: String): Flow<List<ExerciseEntry>>

    @Query("SELECT * FROM exercise_entry ORDER BY timestamp DESC")
    fun getAllExerciseEntries(): Flow<List<ExerciseEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseEntry(entry: ExerciseEntry)

    @Update
    suspend fun updateExerciseEntry(entry: ExerciseEntry)

    @Delete
    suspend fun deleteExerciseEntry(entry: ExerciseEntry)

    // Pre-populated Exercise Database
    @Query("SELECT * FROM exercise_database WHERE name LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%'")
    suspend fun searchExerciseDatabase(query: String): List<ExerciseDatabase>

    @Query("SELECT * FROM exercise_database")
    suspend fun getAllExerciseDatabase(): List<ExerciseDatabase>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseDatabaseList(list: List<ExerciseDatabase>)

    @Query("SELECT COUNT(*) FROM exercise_database")
    suspend fun getExerciseDatabaseCount(): Int

    // Custom Exercises
    @Query("SELECT * FROM custom_exercise WHERE name LIKE '%' || :query || '%'")
    fun searchCustomExercisesFlow(query: String): Flow<List<CustomExercise>>

    @Query("SELECT * FROM custom_exercise WHERE name LIKE '%' || :query || '%'")
    suspend fun searchCustomExercises(query: String): List<CustomExercise>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomExercise(exercise: CustomExercise)

    @Query("DELETE FROM exercise_entry")
    suspend fun clearExerciseEntries()

    @Query("DELETE FROM custom_exercise")
    suspend fun clearCustomExercises()
}

@Dao
interface WeightLogDao {
    @Query("SELECT * FROM weight_log ORDER BY date ASC")
    fun getAllWeightLogs(): Flow<List<WeightLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightLog(weightLog: WeightLog)

    @Delete
    suspend fun deleteWeightLog(weightLog: WeightLog)

    @Query("DELETE FROM weight_log")
    suspend fun clearWeightLogs()
}

@Dao
interface WaterLogDao {
    @Query("SELECT * FROM water_log WHERE date = :date")
    fun getWaterLogForDate(date: String): Flow<WaterLog?>

    @Query("SELECT * FROM water_log ORDER BY date DESC")
    fun getAllWaterLogs(): Flow<List<WaterLog>>

    @Query("SELECT * FROM water_log WHERE date = :date")
    suspend fun getWaterLogForDateSync(date: String): WaterLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterLog(waterLog: WaterLog)

    @Update
    suspend fun updateWaterLog(waterLog: WaterLog)

    @Query("DELETE FROM water_log")
    suspend fun clearWaterLogs()
}

@Dao
interface DailyActivityCalorieDao {
    @Query("SELECT * FROM daily_activity_calorie_log WHERE date = :date")
    fun getActivityCalorieLogForDate(date: String): Flow<DailyActivityCalorieLog?>

    @Query("SELECT * FROM daily_activity_calorie_log WHERE date = :date")
    suspend fun getActivityCalorieLogForDateSync(date: String): DailyActivityCalorieLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityCalorieLog(log: DailyActivityCalorieLog)

    @Query("DELETE FROM daily_activity_calorie_log")
    suspend fun clearActivityCalorieLogs()
}
