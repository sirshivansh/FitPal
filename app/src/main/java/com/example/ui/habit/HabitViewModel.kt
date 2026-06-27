package com.example.ui.habit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.Habit
import com.example.data.local.entity.HabitCompletion
import com.example.data.repository.HabitRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HabitViewModel(
    private val habitRepository: HabitRepository
) : ViewModel() {

    init {
        // Seed habits on launch if table is empty
        viewModelScope.launch {
            habitRepository.seedDefaultHabits()
        }
    }

    val habits: StateFlow<List<Habit>> = habitRepository.allHabits
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val completions: StateFlow<List<HabitCompletion>> = habitRepository.allCompletions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun toggleHabitToday(habitId: Long, date: String) {
        viewModelScope.launch {
            val isCompleted = completions.value.any { it.habitId == habitId && it.date == date }
            if (isCompleted) {
                habitRepository.uncompleteHabit(habitId, date)
            } else {
                habitRepository.completeHabit(habitId, date)
            }
        }
    }

    fun toggleHabitForDate(habitId: Long, date: String) {
        viewModelScope.launch {
            val isCompleted = completions.value.any { it.habitId == habitId && it.date == date }
            if (isCompleted) {
                habitRepository.uncompleteHabit(habitId, date)
            } else {
                habitRepository.completeHabit(habitId, date)
            }
        }
    }

    fun addCustomHabit(name: String, description: String, colorHex: String, iconName: String) {
        viewModelScope.launch {
            habitRepository.insertHabit(
                Habit(
                    name = name,
                    description = description,
                    colorHex = colorHex,
                    iconName = iconName
                )
            )
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            habitRepository.deleteHabit(habit)
        }
    }

    fun resetHabits() {
        viewModelScope.launch {
            habitRepository.clearAllHabitData()
            habitRepository.seedDefaultHabits()
        }
    }
}
