package com.example.ui.foodlogging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.CustomFood
import com.example.data.local.entity.FoodEntry
import com.example.data.repository.FoodRepository
import com.example.data.repository.SearchResultFood
import com.example.util.Calculations
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FoodLogViewModel(
    private val foodRepository: FoodRepository
) : ViewModel() {

    private val _searchResults = MutableStateFlow<List<SearchResultFood>>(emptyList())
    val searchResults: StateFlow<List<SearchResultFood>> = _searchResults.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val recentFoods: StateFlow<List<FoodEntry>> = foodRepository.recentFoods
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val customFoods: StateFlow<List<CustomFood>> = foodRepository.customFoods
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun searchFoods(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _searchResults.value = foodRepository.searchFoods(query)
        }
    }

    fun addFoodToDiary(
        date: String,
        mealType: String,
        foodName: String,
        grams: Float,
        caloriesPer100g: Float,
        proteinPer100g: Float,
        carbsPer100g: Float,
        fatPer100g: Float,
        isCustom: Boolean
    ) {
        viewModelScope.launch {
            val calculated = Calculations.calculateFoodMacros(
                caloriesPer100g = caloriesPer100g,
                proteinPer100g = proteinPer100g,
                carbsPer100g = carbsPer100g,
                fatPer100g = fatPer100g,
                gramsConsumed = grams
            )

            val entry = FoodEntry(
                date = date,
                mealType = mealType,
                foodName = foodName,
                gramsConsumed = grams,
                caloriesConsumed = calculated.calories,
                proteinGrams = calculated.protein,
                carbsGrams = calculated.carbs,
                fatGrams = calculated.fat,
                isCustomFood = isCustom
            )
            foodRepository.insertFoodEntry(entry)
        }
    }

    fun addManualEntry(
        date: String,
        mealType: String,
        foodName: String,
        calories: Float,
        protein: Float,
        carbs: Float,
        fat: Float,
        servingDescription: String
    ) {
        viewModelScope.launch {
            val displayName = if (servingDescription.isNotBlank()) {
                "$foodName ($servingDescription)"
            } else {
                foodName
            }

            val entry = FoodEntry(
                date = date,
                mealType = mealType,
                foodName = displayName,
                gramsConsumed = 100f, // manual standard
                caloriesConsumed = calories,
                proteinGrams = protein,
                carbsGrams = carbs,
                fatGrams = fat,
                isCustomFood = true
            )
            foodRepository.insertFoodEntry(entry)
        }
    }

    fun createCustomFood(
        name: String,
        caloriesPer100g: Float,
        proteinPer100g: Float,
        carbsPer100g: Float,
        fatPer100g: Float,
        defaultServingGrams: Float
    ) {
        viewModelScope.launch {
            val custom = CustomFood(
                name = name,
                caloriesPer100g = caloriesPer100g,
                proteinPer100g = proteinPer100g,
                carbsPer100g = carbsPer100g,
                fatPer100g = fatPer100g,
                defaultServingGrams = defaultServingGrams
            )
            foodRepository.insertCustomFood(custom)
        }
    }

    fun deleteCustomFood(food: CustomFood) {
        viewModelScope.launch {
            foodRepository.deleteCustomFood(food)
        }
    }
}
