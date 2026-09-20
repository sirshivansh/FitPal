package com.example.fitpal.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitpal.data.api.GeminiWorkoutService
import com.example.fitpal.data.api.ParsedWorkoutItem
import com.example.fitpal.data.api.UnifiedNotebookParseResult
import com.example.fitpal.data.local.entity.*
import com.example.fitpal.data.repository.ProfileRepository
import com.example.fitpal.data.repository.FoodRepository
import com.example.fitpal.data.repository.ExerciseRepository
import com.example.fitpal.util.DateUtils
import com.example.fitpal.util.Calculations
import com.example.fitpal.util.DynamicExpenditureResult
import com.example.fitpal.util.ObservedMaintenanceResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DashboardViewModel(
    val profileRepository: ProfileRepository,
    val foodRepository: FoodRepository,
    val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(DateUtils.getTodayDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    val userProfile: StateFlow<UserProfile?> = profileRepository.getUserProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val foodEntries: StateFlow<List<FoodEntry>> = _selectedDate
        .flatMapLatest { date -> foodRepository.getFoodEntries(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exerciseEntries: StateFlow<List<ExerciseEntry>> = _selectedDate
        .flatMapLatest { date -> exerciseRepository.getExerciseEntries(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val workoutEntries: StateFlow<List<WorkoutEntry>> = _selectedDate
        .flatMapLatest { date -> exerciseRepository.getWorkoutsForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val waterLog: StateFlow<WaterLog?> = _selectedDate
        .flatMapLatest { date -> profileRepository.getWaterLog(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activityCalorieLog: StateFlow<ActivityCalorieLog?> = _selectedDate
        .flatMapLatest { date -> profileRepository.getActivityLog(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val weightLogs: StateFlow<List<WeightLog>> = profileRepository.getAllWeights()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFoodEntries: StateFlow<List<FoodEntry>> = foodRepository.getAllFoodEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weightTrendReport: StateFlow<com.example.fitpal.util.WeightTrendAnalysis> = weightLogs
        .map { Calculations.calculateWeightTrend(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            Calculations.calculateWeightTrend(emptyList())
        )

    val observedMaintenance: StateFlow<ObservedMaintenanceResult> = combine(
        weightLogs,
        allFoodEntries
    ) { weights, foods ->
        Calculations.calculateObservedMaintenance(weights, foods)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        Calculations.calculateObservedMaintenance(emptyList(), emptyList())
    )

    val dynamicExpenditure: StateFlow<DynamicExpenditureResult> = combine(
        userProfile,
        workoutEntries,
        exerciseEntries,
        activityCalorieLog,
        foodEntries
    ) { profile, workouts, exercises, activity, foods ->
        val weight = profile?.currentWeightKg ?: 70f
        val height = profile?.heightCm ?: 175f
        val age = profile?.age ?: 28
        val gender = profile?.gender ?: "Male"
        val bodyFat = profile?.bodyFat
        val goalType = profile?.weightGoalType ?: "maintain"
        val customAdj = profile?.calorieAdjustment?.takeIf { it != 0 }

        // Combined workout burn from rich WorkoutEntries and any standalone ExerciseEntries (additive ledger)
        val standaloneExerciseBurn = exercises
            .filter { ex -> workouts.none { w -> w.title.equals(ex.exerciseName, ignoreCase = true) } }
            .sumOf { it.caloriesBurned.toInt() }
        val workoutBurn = workouts.sumOf { it.estimatedCalories } + standaloneExerciseBurn

        val totalSteps = activity?.steps ?: 0
        val consumed = foods.sumOf { it.caloriesConsumed.toInt() }

        Calculations.evaluateDynamicDailyExpenditure(
            age = age,
            gender = gender,
            heightCm = height,
            currentWeightKg = weight,
            bodyFatPercent = bodyFat,
            goalType = goalType,
            customAdjustmentKcal = customAdj,
            loggedWorkoutCalories = workoutBurn,
            totalSteps = totalSteps,
            alreadyLoggedWalkingMinutes = 0,
            consumedCalories = consumed
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        Calculations.evaluateDynamicDailyExpenditure(
            age = 28,
            gender = "Male",
            heightCm = 175f,
            currentWeightKg = 70f
        )
    )

    private val _eatBackExerciseCalories = MutableStateFlow(false)
    val eatBackExerciseCalories: StateFlow<Boolean> = _eatBackExerciseCalories.asStateFlow()

    fun setEatBackExerciseCalories(enabled: Boolean) {
        _eatBackExerciseCalories.value = enabled
    }

    val progressPhotos: StateFlow<List<ProgressPhoto>> = profileRepository.getAllPhotos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setDate(date: String) {
        _selectedDate.value = date
    }

    fun logSteps(steps: Int) {
        viewModelScope.launch {
            val date = _selectedDate.value
            val current = profileRepository.getActivityLogSync(date)
            if (current != null) {
                profileRepository.insertActivityLog(current.copy(steps = steps))
            } else {
                profileRepository.insertActivityLog(ActivityCalorieLog(additionalCalories = 0, steps = steps, date = date))
            }
        }
    }

    fun addSteps(deltaSteps: Int) {
        viewModelScope.launch {
            val date = _selectedDate.value
            val current = profileRepository.getActivityLogSync(date)
            val currentSteps = current?.steps ?: 0
            val newSteps = currentSteps + deltaSteps
            if (current != null) {
                profileRepository.insertActivityLog(current.copy(steps = newSteps))
            } else {
                profileRepository.insertActivityLog(ActivityCalorieLog(additionalCalories = 0, steps = newSteps, date = date))
            }
        }
    }

    fun addProgressPhoto(date: String, filePath: String, note: String) {
        viewModelScope.launch {
            profileRepository.insertPhoto(ProgressPhoto(imagePath = filePath, note = note, date = date))
        }
    }

    fun deleteProgressPhoto(photo: ProgressPhoto) {
        viewModelScope.launch {
            profileRepository.deletePhoto(photo)
        }
    }

    fun setAdditionalActivityCalories(calories: Int) {
        viewModelScope.launch {
            val date = _selectedDate.value
            val current = profileRepository.getActivityLogSync(date)
            if (current != null) {
                profileRepository.insertActivityLog(current.copy(additionalCalories = calories))
            } else {
                profileRepository.insertActivityLog(ActivityCalorieLog(additionalCalories = calories, date = date))
            }
        }
    }

    fun insertWorkout(workout: WorkoutEntry) {
        viewModelScope.launch {
            exerciseRepository.insertWorkout(workout)
        }
    }

    fun insertFood(food: FoodEntry) {
        viewModelScope.launch {
            foodRepository.insert(food)
        }
    }

    fun deleteWorkout(workout: WorkoutEntry) {
        viewModelScope.launch {
            exerciseRepository.deleteWorkout(workout)
        }
    }

    fun resetWater() {
        viewModelScope.launch {
            val date = _selectedDate.value
            profileRepository.insertWater(WaterLog(glassesCount = 0, milliliters = 0, date = date))
        }
    }

    fun logCustomWater(amountMl: Int) {
        viewModelScope.launch {
            val date = _selectedDate.value
            val current = profileRepository.getWaterLogSync(date)
            val currentMl = if (current != null) {
                if (current.milliliters > 0) current.milliliters else current.glassesCount * 250
            } else 0
            val newMl = (currentMl + amountMl).coerceAtLeast(0)
            val newGlasses = (newMl / 250).coerceAtLeast(0)
            if (current != null) {
                profileRepository.insertWater(current.copy(glassesCount = newGlasses, milliliters = newMl))
            } else {
                profileRepository.insertWater(WaterLog(glassesCount = newGlasses, milliliters = newMl, date = date))
            }
        }
    }

    fun incrementWater() {
        logCustomWater(250)
    }

    fun decrementWater() {
        logCustomWater(-250)
    }

    fun logWeight(weight: Float) {
        viewModelScope.launch {
            val date = _selectedDate.value
            profileRepository.insertWeight(WeightLog(weightKg = weight, date = date))
            val profile = profileRepository.getUserProfileSync()
            if (profile != null) {
                val bmr = Calculations.calculateBmr(weight, profile.heightCm, profile.age, profile.gender)
                val tdee = Calculations.calculateBaseTDEE(bmr, profile.activityLevel).toInt()
                profileRepository.insertProfile(
                    profile.copy(
                        currentWeightKg = weight,
                        bmr = bmr,
                        maintenanceCalories = tdee
                    )
                )
            }
        }
    }

    fun deleteWeight(weight: WeightLog) {
        viewModelScope.launch {
            profileRepository.deleteWeight(weight)
        }
    }

    fun logWeightForDate(weight: Float, customDate: String) {
        viewModelScope.launch {
            profileRepository.insertWeight(WeightLog(weightKg = weight, date = customDate))
            val profile = profileRepository.getUserProfileSync()
            if (profile != null) {
                val bmr = Calculations.calculateBmr(weight, profile.heightCm, profile.age, profile.gender)
                val tdee = Calculations.calculateBaseTDEE(bmr, profile.activityLevel).toInt()
                profileRepository.insertProfile(
                    profile.copy(
                        currentWeightKg = weight,
                        bmr = bmr,
                        maintenanceCalories = tdee
                    )
                )
            }
        }
    }

    fun saveMaintenanceCalories(calories: Int) {
        viewModelScope.launch {
            val profile = profileRepository.getUserProfileSync()
            if (profile != null) {
                profileRepository.insertProfile(profile.copy(maintenanceCalories = calories))
            }
        }
    }
}
