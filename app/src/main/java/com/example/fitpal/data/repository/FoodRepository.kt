package com.example.fitpal.data.repository

import com.example.fitpal.data.local.dao.FoodDao
import com.example.fitpal.data.local.entity.FoodEntry
import kotlinx.coroutines.flow.Flow

class FoodRepository(private val foodDao: FoodDao) {
    fun getFoodEntries(date: String): Flow<List<FoodEntry>> = foodDao.getFoodEntries(date)
    fun getAllFoodEntries(): Flow<List<FoodEntry>> = foodDao.getAllFoodEntries()
    suspend fun getFoodEntriesSync(date: String): List<FoodEntry> = foodDao.getFoodEntriesSync(date)
    suspend fun insert(food: FoodEntry) = foodDao.insertFood(food)
    suspend fun delete(food: FoodEntry) = foodDao.deleteFood(food)
    suspend fun clearAll() = foodDao.clearAll()
}
