package com.example.ui.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.FoodEntry
import com.example.data.local.entity.ExerciseEntry
import com.example.data.local.entity.WeightLog
import com.example.data.local.entity.WaterLog
import com.example.data.repository.ExerciseRepository
import com.example.data.repository.FoodRepository
import com.example.data.repository.ProgressRepository
import com.example.data.repository.UserRepository
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class AchievementViewModel(
    private val userRepository: UserRepository,
    private val foodRepository: FoodRepository,
    private val exerciseRepository: ExerciseRepository,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    val achievements: StateFlow<List<Achievement>> = combine(
        userRepository.userProfile,
        foodRepository.allFoodEntries,
        exerciseRepository.allExerciseEntries,
        progressRepository.allWeightLogs,
        progressRepository.allWaterLogs
    ) { profile, foodEntries, exerciseEntries, weightLogs, waterLogs ->
        calculateAchievements(profile, foodEntries, exerciseEntries, weightLogs, waterLogs)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private fun calculateAchievements(
        profile: com.example.data.local.entity.UserProfile?,
        foodEntries: List<FoodEntry>,
        exerciseEntries: List<ExerciseEntry>,
        weightLogs: List<WeightLog>,
        waterLogs: List<WaterLog>
    ): List<Achievement> {
        val list = mutableListOf<Achievement>()

        // 1. STREAKS
        val currentStreak = calculateStreak(foodEntries)
        list.add(
            Achievement(
                id = "streak_3",
                title = "Consistency Champ",
                description = "Log your meals for 3 consecutive days.",
                category = AchievementCategory.STREAKS,
                currentProgress = currentStreak.toFloat(),
                maxProgress = 3f,
                isUnlocked = currentStreak >= 3,
                rewardText = "+50 Consistency XP"
            )
        )
        list.add(
            Achievement(
                id = "streak_7",
                title = "Unstoppable Force",
                description = "Log your meals for 7 consecutive days.",
                category = AchievementCategory.STREAKS,
                currentProgress = currentStreak.toFloat(),
                maxProgress = 7f,
                isUnlocked = currentStreak >= 7,
                rewardText = "Unstoppable Badge & Crown Icon"
            )
        )

        // 2. WEIGHT GOALS
        val loggedWeightsCount = weightLogs.size
        list.add(
            Achievement(
                id = "weight_logs_3",
                title = "Scale Companion",
                description = "Log your weight on 3 separate days.",
                category = AchievementCategory.WEIGHT,
                currentProgress = loggedWeightsCount.toFloat(),
                maxProgress = 3f,
                isUnlocked = loggedWeightsCount >= 3,
                rewardText = "Mindful Scale Badge"
            )
        )

        // Goal Getter: weight goal completion progress
        val goalGetterUnlocked: Boolean
        val goalGetterProgress: Float
        if (profile != null) {
            val startWeight = weightLogs.firstOrNull()?.weightKg ?: profile.currentWeightKg
            val currentWeight = profile.currentWeightKg
            val goalWeight = profile.goalWeightKg
            val totalDistance = abs(startWeight - goalWeight)
            val currentDistance = abs(currentWeight - goalWeight)

            goalGetterUnlocked = currentDistance <= 0.5f
            goalGetterProgress = if (goalGetterUnlocked) {
                1f
            } else if (totalDistance > 0f) {
                ((totalDistance - currentDistance) / totalDistance).coerceIn(0f, 1f)
            } else {
                0f
            }
        } else {
            goalGetterUnlocked = false
            goalGetterProgress = 0f
        }

        list.add(
            Achievement(
                id = "weight_goal_met",
                title = "Goal Getter",
                description = "Reach within 0.5 kg of your target weight goal.",
                category = AchievementCategory.WEIGHT,
                currentProgress = goalGetterProgress * 100f,
                maxProgress = 100f,
                isUnlocked = goalGetterUnlocked,
                rewardText = "Golden Goal Medal"
            )
        )

        // 3. NUTRITION
        // Hydration Hero
        val maxWaterGlasses = waterLogs.maxOfOrNull { it.glassesCount } ?: 0
        list.add(
            Achievement(
                id = "water_8",
                title = "Hydration Hero",
                description = "Drink 8 glasses of water (2 liters) in a single day.",
                category = AchievementCategory.NUTRITION,
                currentProgress = maxWaterGlasses.toFloat(),
                maxProgress = 8f,
                isUnlocked = maxWaterGlasses >= 8,
                rewardText = "Aqua Warrior Badge"
            )
        )

        // Calorie Controller (Under Calorie Goal)
        val foodByDate = foodEntries.groupBy { it.date }
        var loggedUnderCalorieDays = 0
        if (profile != null) {
            val calorieGoal = profile.dailyCalorieGoal
            loggedUnderCalorieDays = foodByDate.filter { (_, entries) ->
                val totalCalories = entries.sumOf { it.caloriesConsumed.toDouble() }.toFloat()
                totalCalories > 0 && totalCalories <= calorieGoal
            }.size
        }
        list.add(
            Achievement(
                id = "calorie_control",
                title = "Calorie Controller",
                description = "Log a full day of eating within your calorie limit.",
                category = AchievementCategory.NUTRITION,
                currentProgress = if (loggedUnderCalorieDays > 0) 1f else 0f,
                maxProgress = 1f,
                isUnlocked = loggedUnderCalorieDays > 0,
                rewardText = "Calorie Safe Badge"
            )
        )

        // Macro Master (Protein, carbs, and fat all within 15% of daily goals)
        var hasMacroMastered = false
        if (profile != null) {
            val pGoal = profile.proteinGoalGrams
            val cGoal = profile.carbsGoalGrams
            val fGoal = profile.fatGoalGrams

            if (pGoal > 0 && cGoal > 0 && fGoal > 0) {
                for ((_, entries) in foodByDate) {
                    val dayP = entries.sumOf { it.proteinGrams.toDouble() }.toFloat()
                    val dayC = entries.sumOf { it.carbsGrams.toDouble() }.toFloat()
                    val dayF = entries.sumOf { it.fatGrams.toDouble() }.toFloat()

                    val pMatch = abs(dayP - pGoal) <= (pGoal * 0.15f)
                    val cMatch = abs(dayC - cGoal) <= (cGoal * 0.15f)
                    val fMatch = abs(dayF - fGoal) <= (fGoal * 0.15f)

                    if (pMatch && cMatch && fMatch) {
                        hasMacroMastered = true
                        break
                    }
                }
            }
        }
        list.add(
            Achievement(
                id = "macro_master",
                title = "Macro Precision",
                description = "Hit your protein, carb, and fat goals within 15% on any day.",
                category = AchievementCategory.NUTRITION,
                currentProgress = if (hasMacroMastered) 1f else 0f,
                maxProgress = 1f,
                isUnlocked = hasMacroMastered,
                rewardText = "Macro Master Badge"
            )
        )

        // 4. EXERCISE
        // Workout count
        val totalWorkouts = exerciseEntries.size
        list.add(
            Achievement(
                id = "exercise_workouts_3",
                title = "Active Week Starter",
                description = "Log at least 3 custom or database workouts.",
                category = AchievementCategory.EXERCISE,
                currentProgress = totalWorkouts.toFloat(),
                maxProgress = 3f,
                isUnlocked = totalWorkouts >= 3,
                rewardText = "Active Ribbon"
            )
        )

        // Total minutes duration
        val totalMinutes = exerciseEntries.sumOf { it.durationMinutes }
        list.add(
            Achievement(
                id = "exercise_warrior_150",
                title = "Workout Warrior",
                description = "Accumulate 150 minutes of logged physical activity.",
                category = AchievementCategory.EXERCISE,
                currentProgress = totalMinutes.toFloat(),
                maxProgress = 150f,
                isUnlocked = totalMinutes >= 150,
                rewardText = "Cardio Master Medal"
            )
        )

        // Calorie Burner (500 kcal burned from exercise in a single day)
        val exerciseByDate = exerciseEntries.groupBy { it.date }
        val maxExerciseBurnedInADay = exerciseByDate.values.maxOfOrNull { dayEntries ->
            dayEntries.sumOf { it.caloriesBurned.toDouble() }.toFloat()
        } ?: 0f
        list.add(
            Achievement(
                id = "exercise_burner_500",
                title = "Calorie Incinerator",
                description = "Burn 500 kcal or more through exercises in a single day.",
                category = AchievementCategory.EXERCISE,
                currentProgress = maxExerciseBurnedInADay,
                maxProgress = 500f,
                isUnlocked = maxExerciseBurnedInADay >= 500f,
                rewardText = "Fire Badge & Red Ribbon"
            )
        )

        return list
    }

    private fun calculateStreak(allEntries: List<FoodEntry>): Int {
        if (allEntries.isEmpty()) return 0
        val loggedDates = allEntries.map { it.date }.toSet().sorted()
        if (loggedDates.isEmpty()) return 0

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        var currentStreak = 0
        var bestStreak = 0

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
                if (currentStreak > bestStreak) {
                    bestStreak = currentStreak
                }
                currentStreak = 1
            }
        }

        return maxOf(currentStreak, bestStreak)
    }
}
