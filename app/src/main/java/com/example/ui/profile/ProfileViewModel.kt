package com.example.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.UserProfile
import com.example.data.local.entity.WeightLog
import com.example.data.repository.ProgressRepository
import com.example.data.repository.UserRepository
import com.example.util.Calculations
import com.example.util.DateUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepository: UserRepository,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    val userProfile: StateFlow<UserProfile?> = userRepository.userProfile
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun createOrUpdateProfile(
        name: String,
        age: Int,
        gender: String,
        heightCm: Float,
        currentWeightKg: Float,
        goalWeightKg: Float,
        weightGoalType: String,
        calorieAdjustment: Int,
        proteinMultiplier: Float,
        fatMultiplier: Float,
        maintenanceCalories: Int?
    ) {
        viewModelScope.launch {
            val bmr = Calculations.calculateBmr(currentWeightKg, heightCm, age, gender)
            val calorieGoal = if (maintenanceCalories != null && maintenanceCalories > 0) {
                Calculations.calculateTargetCalorieGoal(maintenanceCalories.toFloat(), weightGoalType, calorieAdjustment)
            } else {
                0
            }
            val (carbs, protein, fat) = if (calorieGoal > 0) {
                Calculations.calculateMacrosCustom(calorieGoal, currentWeightKg, proteinMultiplier, fatMultiplier)
            } else {
                Triple(0, 0, 0)
            }

            val profile = UserProfile(
                name = name,
                age = age,
                gender = gender,
                heightCm = heightCm,
                currentWeightKg = currentWeightKg,
                goalWeightKg = goalWeightKg,
                bmr = bmr,
                dailyCalorieGoal = calorieGoal,
                proteinGoalGrams = protein,
                carbsGoalGrams = carbs,
                fatGoalGrams = fat,
                weightGoalType = weightGoalType,
                calorieAdjustment = calorieAdjustment,
                proteinMultiplier = proteinMultiplier,
                fatMultiplier = fatMultiplier,
                maintenanceCalories = maintenanceCalories
            )
            userRepository.saveProfile(profile)

            // Auto-log initial/updated weight to Weight history
            val todayStr = DateUtils.getTodayDateString()
            progressRepository.insertWeightLog(WeightLog(date = todayStr, weightKg = currentWeightKg))
        }
    }

    fun updateCalculations(calorieAdjustment: Int, proteinMultiplier: Float, fatMultiplier: Float) {
        viewModelScope.launch {
            val current = userRepository.getUserProfileSync() ?: return@launch
            val calorieGoal = if (current.maintenanceCalories != null && current.maintenanceCalories > 0) {
                Calculations.calculateTargetCalorieGoal(current.maintenanceCalories.toFloat(), current.weightGoalType, calorieAdjustment)
            } else {
                0
            }
            val (carbs, protein, fat) = if (calorieGoal > 0) {
                Calculations.calculateMacrosCustom(calorieGoal, current.currentWeightKg, proteinMultiplier, fatMultiplier)
            } else {
                Triple(0, 0, 0)
            }

            val updated = current.copy(
                dailyCalorieGoal = calorieGoal,
                proteinGoalGrams = protein,
                carbsGoalGrams = carbs,
                fatGoalGrams = fat,
                calorieAdjustment = calorieAdjustment,
                proteinMultiplier = proteinMultiplier,
                fatMultiplier = fatMultiplier
            )
            userRepository.saveProfile(updated)
        }
    }

    fun updateCalorieGoal(calorieGoal: Int) {
        viewModelScope.launch {
            val current = userRepository.getUserProfileSync() ?: return@launch
            val (carbs, protein, fat) = Calculations.calculateMacrosCustom(
                calorieGoal,
                current.currentWeightKg,
                current.proteinMultiplier,
                current.fatMultiplier
            )
            val updated = current.copy(
                dailyCalorieGoal = calorieGoal,
                proteinGoalGrams = protein,
                carbsGoalGrams = carbs,
                fatGoalGrams = fat
            )
            userRepository.saveProfile(updated)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            userRepository.clearProfile()
        }
    }
}
