package com.example.fitpal.data.local.dao

import androidx.room.*
import com.example.fitpal.data.local.entity.FoodEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM food_entries WHERE date = :date")
    fun getFoodEntries(date: String): Flow<List<FoodEntry>>

    @Query("SELECT * FROM food_entries")
    fun getAllFoodEntries(): Flow<List<FoodEntry>>

    @Query("SELECT * FROM food_entries")
    suspend fun getAllFoodEntriesSync(): List<FoodEntry>

    @Query("SELECT * FROM food_entries WHERE date = :date")
    suspend fun getFoodEntriesSync(date: String): List<FoodEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(food: FoodEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(foods: List<FoodEntry>)

    @Delete
    suspend fun deleteFood(food: FoodEntry)

    @Query("DELETE FROM food_entries")
    suspend fun clearAll()
}
