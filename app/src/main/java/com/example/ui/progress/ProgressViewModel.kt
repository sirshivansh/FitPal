package com.example.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.FoodEntry
import com.example.data.local.entity.WeightLog
import com.example.data.repository.FoodRepository
import com.example.data.repository.ProgressRepository
import com.example.data.repository.UserRepository
import com.example.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ProgressViewModel(
    private val progressRepository: ProgressRepository,
    private val foodRepository: FoodRepository,
    private val userRepository: UserRepository,
    private val exerciseRepository: com.example.data.repository.ExerciseRepository
) : ViewModel() {

    val weightLogs: StateFlow<List<WeightLog>> = progressRepository.allWeightLogs
        .map { list -> list.sortedBy { it.date } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val userProfile = userRepository.userProfile
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Collect past 7 days of food entries for macro analytics
    val recentFoodEntries: StateFlow<List<FoodEntry>> = flow {
        val days = DateUtils.getDaysRange(7)
        val flows = days.map { date -> foodRepository.getFoodEntriesForDate(date) }
        
        // Combine flows of individual days into a single flow of combined list
        combine(flows) { arrays ->
            arrays.flatMap { it.toList() }
        }.collect { emit(it) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Collect past 7 days of exercise entries for comparison chart
    val recentExerciseEntries: StateFlow<List<com.example.data.local.entity.ExerciseEntry>> = flow {
        val days = DateUtils.getDaysRange(7)
        val flows = days.map { date -> exerciseRepository.getExerciseEntriesForDate(date) }
        
        combine(flows) { arrays ->
            arrays.flatMap { it.toList() }
        }.collect { emit(it) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun logWeight(date: String, weight: Float) {
        viewModelScope.launch {
            progressRepository.insertWeightLog(WeightLog(date = date, weightKg = weight))
            
            // Also update current weight in user profile!
            val profile = userRepository.getUserProfileSync()
            if (profile != null) {
                userRepository.saveProfile(profile.copy(currentWeightKg = weight))
            }
        }
    }

    fun deleteWeightLog(weightLog: WeightLog) {
        viewModelScope.launch {
            progressRepository.deleteWeightLog(weightLog)
        }
    }

    // Streaks logic: calculate consecutive days with at least one food log
    fun calculateStreak(allEntries: List<FoodEntry>): Int {
        if (allEntries.isEmpty()) return 0
        val loggedDates = allEntries.map { it.date }.toSet().sorted()
        if (loggedDates.isEmpty()) return 0

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        var currentStreak = 0
        var bestStreak = 0

        // Parse date strings to calendar
        val calendars = loggedDates.map { dateStr ->
            val date = dateFormat.parse(dateStr) ?: return 0
            val cal = Calendar.getInstance()
            cal.time = date
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal
        }

        if (calendars.isEmpty()) return 0

        currentStreak = 1
        bestStreak = 1

        for (i in 1 until calendars.size) {
            val prev = calendars[i - 1].clone() as Calendar
            prev.add(Calendar.DAY_OF_YEAR, 1)
            
            val current = calendars[i]
            
            if (current.equals(prev)) {
                currentStreak++
            } else if (current.after(prev)) {
                // Gap in days, reset
                if (currentStreak > bestStreak) {
                    bestStreak = currentStreak
                }
                currentStreak = 1
            }
        }

        return maxOf(currentStreak, bestStreak)
    }
}
