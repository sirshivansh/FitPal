package com.example.ui.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.CustomExercise
import com.example.data.local.entity.ExerciseEntry
import com.example.data.repository.ExerciseRepository
import com.example.data.repository.SearchResultExercise
import com.example.data.repository.UserRepository
import com.example.util.Calculations
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ExerciseViewModel(
    private val exerciseRepository: ExerciseRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _searchResults = MutableStateFlow<List<SearchResultExercise>>(emptyList())
    val searchResults: StateFlow<List<SearchResultExercise>> = _searchResults.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val userProfile = userRepository.userProfile
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun searchExercises(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _searchResults.value = exerciseRepository.searchExercises(query)
        }
    }

    fun addExerciseEntry(
        date: String,
        exerciseName: String,
        category: String,
        durationMinutes: Int,
        intensity: String,
        metValue: Float,
        isCustom: Boolean,
        caloriesBurned: Float
    ) {
        viewModelScope.launch {
            val entry = ExerciseEntry(
                date = date,
                exerciseName = exerciseName,
                category = category,
                durationMinutes = durationMinutes,
                intensity = intensity,
                metValue = metValue,
                caloriesBurned = caloriesBurned,
                isCustomExercise = isCustom
            )
            exerciseRepository.insertExerciseEntry(entry)
        }
    }

    fun addManualExercise(
        date: String,
        exerciseName: String,
        durationMinutes: Int,
        caloriesBurned: Float
    ) {
        viewModelScope.launch {
            val entry = ExerciseEntry(
                date = date,
                exerciseName = exerciseName,
                category = "Cardio",
                durationMinutes = durationMinutes,
                intensity = "Moderate",
                metValue = 0f,
                caloriesBurned = caloriesBurned,
                isCustomExercise = true
            )
            exerciseRepository.insertExerciseEntry(entry)
        }
    }

    fun createCustomExercise(
        name: String,
        category: String,
        metValue: Float
    ) {
        viewModelScope.launch {
            val custom = CustomExercise(
                name = name,
                category = category,
                metValue = metValue
            )
            exerciseRepository.insertCustomExercise(custom)
        }
    }
}
