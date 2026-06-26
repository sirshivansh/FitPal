package com.example.ui.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.ExerciseEntry
import com.example.data.local.entity.FoodEntry
import com.example.data.repository.ExerciseRepository
import com.example.data.repository.FoodRepository
import com.example.data.repository.UserRepository
import com.example.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class DiaryViewModel(
    private val userRepository: UserRepository,
    private val foodRepository: FoodRepository,
    private val exerciseRepository: ExerciseRepository
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

    private var recentlyDeletedFoodEntry: FoodEntry? = null
    private var recentlyDeletedExerciseEntry: ExerciseEntry? = null

    fun setDate(date: String) {
        _selectedDate.value = date
    }

    fun deleteFoodEntry(entry: FoodEntry) {
        recentlyDeletedFoodEntry = entry
        viewModelScope.launch {
            foodRepository.deleteFoodEntry(entry)
        }
    }

    fun undoDeleteFoodEntry() {
        val entry = recentlyDeletedFoodEntry ?: return
        viewModelScope.launch {
            foodRepository.insertFoodEntry(entry)
            recentlyDeletedFoodEntry = null
        }
    }

    fun deleteExerciseEntry(entry: ExerciseEntry) {
        recentlyDeletedExerciseEntry = entry
        viewModelScope.launch {
            exerciseRepository.deleteExerciseEntry(entry)
        }
    }

    fun undoDeleteExerciseEntry() {
        val entry = recentlyDeletedExerciseEntry ?: return
        viewModelScope.launch {
            exerciseRepository.insertExerciseEntry(entry)
            recentlyDeletedExerciseEntry = null
        }
    }

    fun copyYesterdayMeals() {
        viewModelScope.launch {
            val currentDate = _selectedDate.value
            val yesterdayDate = DateUtils.getPreviousDay(currentDate)
            foodRepository.copyMeals(fromLocalValue = yesterdayDate, toLocalValue = currentDate)
        }
    }
}
