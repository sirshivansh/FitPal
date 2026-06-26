package com.example.data.repository

import com.example.data.local.dao.FoodDao
import com.example.data.local.entity.CustomFood
import com.example.data.local.entity.FoodDatabase
import com.example.data.local.entity.FoodEntry
import com.example.data.local.entity.PrepopulatedData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class FoodRepository(private val foodDao: FoodDao) {

    val recentFoods: Flow<List<FoodEntry>> = foodDao.getRecentFoodEntries()
    val allFoodEntries: Flow<List<FoodEntry>> = foodDao.getAllFoodEntries()

    fun getFoodEntriesForDate(date: String): Flow<List<FoodEntry>> {
        return foodDao.getFoodEntriesForDate(date)
    }

    suspend fun insertFoodEntry(entry: FoodEntry) {
        foodDao.insertFoodEntry(entry)
    }

    suspend fun updateFoodEntry(entry: FoodEntry) {
        foodDao.updateFoodEntry(entry)
    }

    suspend fun deleteFoodEntry(entry: FoodEntry) {
        foodDao.deleteFoodEntry(entry)
    }

    // Consolidated food search
    suspend fun searchFoods(query: String): List<SearchResultFood> {
        val seeded = foodDao.searchFoodDatabase(query)
        val custom = foodDao.searchCustomFoods(query)

        val results = mutableListOf<SearchResultFood>()

        // Add custom foods first
        custom.forEach { f ->
            results.add(
                SearchResultFood(
                    id = f.id,
                    name = f.name,
                    category = "Custom",
                    caloriesPer100g = f.caloriesPer100g,
                    proteinPer100g = f.proteinPer100g,
                    carbsPer100g = f.carbsPer100g,
                    fatPer100g = f.fatPer100g,
                    defaultServingGrams = f.defaultServingGrams,
                    isCustom = true
                )
            )
        }

        // Add seeded foods
        seeded.forEach { f ->
            results.add(
                SearchResultFood(
                    id = f.id.toLong(),
                    name = f.name,
                    category = f.category,
                    caloriesPer100g = f.caloriesPer100g,
                    proteinPer100g = f.proteinPer100g,
                    carbsPer100g = f.carbsPer100g,
                    fatPer100g = f.fatPer100g,
                    defaultServingGrams = f.defaultServingGrams,
                    isCustom = false
                )
            )
        }

        return results
    }

    // Custom Foods
    val customFoods: Flow<List<CustomFood>> = foodDao.searchCustomFoodsFlow("")

    suspend fun insertCustomFood(food: CustomFood) {
        foodDao.insertCustomFood(food)
    }

    suspend fun deleteCustomFood(food: CustomFood) {
        foodDao.deleteCustomFood(food)
    }

    // Seeding
    suspend fun seedFoodDatabaseIfNeeded() {
        if (foodDao.getFoodDatabaseCount() == 0) {
            val list = PrepopulatedData.getPrepopulatedFoods()
            foodDao.insertFoodDatabaseList(list)
        }
    }

    // Copy entire meal log from a target date to a destination date
    suspend fun copyMeals(fromLocalValue: String, toLocalValue: String) {
        val previousEntries = foodDao.getFoodEntriesForDate(fromLocalValue).first()
        previousEntries.forEach { entry ->
            val copy = entry.copy(
                id = 0, // auto-generate new ID
                date = toLocalValue,
                timestamp = System.currentTimeMillis()
            )
            foodDao.insertFoodEntry(copy)
        }
    }

    suspend fun clearAllFoodData() {
        foodDao.clearFoodEntries()
        foodDao.clearCustomFoods()
    }
}

// Unified model for search results
data class SearchResultFood(
    val id: Long,
    val name: String,
    val category: String,
    val caloriesPer100g: Float,
    val proteinPer100g: Float,
    val carbsPer100g: Float,
    val fatPer100g: Float,
    val defaultServingGrams: Float,
    val isCustom: Boolean
)
