package com.example.fitpal.ui.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitpal.data.local.entity.*
import com.example.fitpal.data.repository.ProfileRepository
import com.example.fitpal.data.repository.FoodRepository
import com.example.fitpal.data.repository.ExerciseRepository
import com.example.fitpal.util.DateUtils
import com.example.fitpal.util.Calculations
import com.example.fitpal.util.DynamicExpenditureResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DiaryViewModel(
    private val profileRepository: ProfileRepository,
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

    val activityCalorieLog: StateFlow<ActivityCalorieLog?> = _selectedDate
        .flatMapLatest { date -> profileRepository.getActivityLog(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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

    private var lastDeletedFood: FoodEntry? = null
    private var lastDeletedExercise: ExerciseEntry? = null
    private var lastDeletedWorkout: WorkoutEntry? = null

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

    fun insertWorkout(workout: WorkoutEntry) {
        viewModelScope.launch {
            exerciseRepository.insertWorkout(workout)
        }
    }

    fun deleteWorkout(workout: WorkoutEntry) {
        viewModelScope.launch {
            lastDeletedWorkout = workout
            exerciseRepository.deleteWorkout(workout)
        }
    }

    fun undoDeleteWorkout() {
        viewModelScope.launch {
            lastDeletedWorkout?.let {
                exerciseRepository.insertWorkout(it)
                lastDeletedWorkout = null
            }
        }
    }

    fun insertFood(food: FoodEntry) {
        viewModelScope.launch {
            foodRepository.insert(food)
        }
    }

    fun deleteFoodEntry(food: FoodEntry) {
        viewModelScope.launch {
            lastDeletedFood = food
            foodRepository.delete(food)
        }
    }

    fun undoDeleteFoodEntry() {
        viewModelScope.launch {
            lastDeletedFood?.let {
                foodRepository.insert(it)
                lastDeletedFood = null
            }
        }
    }

    fun deleteExerciseEntry(exercise: ExerciseEntry) {
        viewModelScope.launch {
            lastDeletedExercise = exercise
            exerciseRepository.delete(exercise)
        }
    }

    fun undoDeleteExerciseEntry() {
        viewModelScope.launch {
            lastDeletedExercise?.let {
                exerciseRepository.insert(it)
                lastDeletedExercise = null
            }
        }
    }

    fun copyYesterdayMeals() {
        viewModelScope.launch {
            val today = _selectedDate.value
            val yesterday = DateUtils.getPreviousDay(today)
            val yesterdayFoods = foodRepository.getFoodEntriesSync(yesterday)
            yesterdayFoods.forEach { food ->
                foodRepository.insert(food.copy(id = 0, date = today))
            }
        }
    }
}

