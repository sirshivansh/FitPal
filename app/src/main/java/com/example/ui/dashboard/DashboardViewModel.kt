package com.example.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.DailyActivityCalorieLog
import com.example.data.local.entity.ExerciseEntry
import com.example.data.local.entity.FoodEntry
import com.example.data.local.entity.WaterLog
import com.example.data.repository.ExerciseRepository
import com.example.data.repository.FoodRepository
import com.example.data.repository.ProgressRepository
import com.example.data.repository.UserRepository
import com.example.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val userRepository: UserRepository,
    private val foodRepository: FoodRepository,
    private val exerciseRepository: ExerciseRepository,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(DateUtils.getTodayDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    val userProfile = userRepository.userProfile
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val foodEntries: StateFlow<List<FoodEntry>> = _selectedDate
        .flatMapLatest { date -> foodRepository.getFoodEntriesForDate(date) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val exerciseEntries: StateFlow<List<ExerciseEntry>> = _selectedDate
        .flatMapLatest { date -> exerciseRepository.getExerciseEntriesForDate(date) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val waterLog: StateFlow<WaterLog?> = _selectedDate
        .flatMapLatest { date -> progressRepository.getWaterLogForDate(date) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val activityCalorieLog: StateFlow<DailyActivityCalorieLog?> = _selectedDate
        .flatMapLatest { date -> progressRepository.getActivityCalorieLogForDate(date) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        // Trigger background seeding of offline data
        viewModelScope.launch {
            foodRepository.seedFoodDatabaseIfNeeded()
            exerciseRepository.seedExerciseDatabaseIfNeeded()
        }
    }

    fun setDate(date: String) {
        _selectedDate.value = date
    }

    fun setAdditionalActivityCalories(calories: Int) {
        viewModelScope.launch {
            progressRepository.saveActivityCalorieLog(_selectedDate.value, calories)
        }
    }

    fun saveMaintenanceCalories(calories: Int) {
        viewModelScope.launch {
            val current = userRepository.getUserProfileSync() ?: return@launch
            val calorieGoal = com.example.util.Calculations.calculateTargetCalorieGoal(
                calories.toFloat(),
                current.weightGoalType,
                current.calorieAdjustment
            )
            val (carbs, protein, fat) = com.example.util.Calculations.calculateMacrosCustom(
                calorieGoal,
                current.currentWeightKg,
                current.proteinMultiplier,
                current.fatMultiplier
            )
            val updated = current.copy(
                maintenanceCalories = calories,
                dailyCalorieGoal = calorieGoal,
                proteinGoalGrams = protein,
                carbsGoalGrams = carbs,
                fatGoalGrams = fat
            )
            userRepository.saveProfile(updated)
        }
    }

    fun incrementWater() {
        viewModelScope.launch {
            progressRepository.incrementWaterLog(_selectedDate.value, 1)
        }
    }

    fun decrementWater() {
        viewModelScope.launch {
            progressRepository.incrementWaterLog(_selectedDate.value, -1)
        }
    }

    fun resetWater() {
        viewModelScope.launch {
            progressRepository.resetWaterLog(_selectedDate.value)
        }
    }
}
