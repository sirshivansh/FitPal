package com.example.fitpal.util

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import com.example.fitpal.data.local.entity.WeightLog
import com.example.fitpal.data.local.entity.FoodEntry

data class MacroDetails(
    val grams: Int,
    val calories: Int
)

data class ConsumedDetails(
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int
)

data class RemainingDetails(
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int
)

data class ProgressDetails(
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float
)

data class NutritionEngineResult(
    val bmr: Int,
    val baseTDEE: Int,
    val exerciseCalories: Int,
    val stepCalories: Int,
    val goalAdjustment: Int,
    val dailyCalorieTarget: Int,
    val protein: MacroDetails,
    val fat: MacroDetails,
    val carbs: MacroDetails,
    val consumed: ConsumedDetails,
    val remaining: RemainingDetails,
    val progress: ProgressDetails
)

/**
 * Supported measurement unit systems.
 */
enum class UnitSystem(val label: String) {
    METRIC("Metric (kg, cm)"),
    IMPERIAL("Imperial (lbs, ft/in)")
}

/**
 * Standard scientifically validated Physical Activity Levels (PAL).
 */
enum class ActivityLevel(
    val key: String,
    val label: String,
    val multiplier: Float,
    val description: String
) {
    SEDENTARY("Sedentary", "Sedentary", 1.20f, "Desk job, little or no structured exercise"),
    LIGHT("Light", "Lightly Active", 1.375f, "Light exercise or sports 1–3 days/week"),
    MODERATE("Moderate", "Moderately Active", 1.55f, "Moderate exercise or sports 3–5 days/week"),
    HEAVY("Heavy", "Very Active", 1.725f, "Hard exercise or sports 6–7 days/week"),
    ATHLETE("Athlete", "Extremely Active", 1.90f, "Very hard training, competitive athlete or physical labor job");

    companion object {
        fun fromKey(key: String): ActivityLevel {
            return entries.find { it.key.equals(key, ignoreCase = true) } ?: MODERATE
        }
    }
}

/**
 * Scientifically structured calorie deficit levels for fat loss.
 */
enum class DeficitLevel(
    val key: String,
    val label: String,
    val percentage: Float,
    val description: String
) {
    MILD("Mild", "Mild (10% Deficit)", 0.10f, "Gentle fat loss, maximum muscle preservation & adherence"),
    MODERATE("Moderate", "Moderate (15% Deficit)", 0.15f, "Recommended & sustainable sweet spot for steady progress"),
    AGGRESSIVE("Aggressive", "Aggressive (20% Deficit)", 0.20f, "Faster progress; best for shorter phases or higher body fat");

    companion object {
        fun fromKey(key: String): DeficitLevel {
            return entries.find { it.key.equals(key, ignoreCase = true) } ?: MODERATE
        }
    }
}

/**
 * Input validation result with descriptive user-friendly guidance.
 */
data class InputValidationResult(
    val isValid: Boolean,
    val errors: List<String>
)

/**
 * Calorie target evaluation with physiological safety floor protections.
 */
data class CalorieTargetEvaluation(
    val bmr: Int,
    val baseTdee: Int,
    val rawTargetCalories: Int,
    val finalTargetCalories: Int,
    val isClampedToFloor: Boolean,
    val safetyWarning: String?,
    val recommendedPaceNote: String
)

/**
 * Scientifically backed weight loss rate guidance (0.5% - 1.0% body weight/week).
 */
data class WeightLossRateInfo(
    val currentWeightKg: Float,
    val minWeeklyLossKg: Float,
    val maxWeeklyLossKg: Float,
    val minWeeklyLossLbs: Float,
    val maxWeeklyLossLbs: Float,
    val minDailyDeficitKcal: Int,
    val maxDailyDeficitKcal: Int,
    val fluctuationDisclaimer: String
)

/**
 * Evidence-based protein intake targets (1.6 - 2.2 g/kg/day).
 */
data class ProteinRecommendation(
    val weightKg: Float,
    val minimumGrams: Int,        // 1.6 g/kg
    val recommendedGrams: Int,    // 1.8 - 2.0 g/kg
    val upperGrams: Int,          // 2.2 g/kg
    val generalHealthRdaGrams: Int, // 0.8 g/kg
    val explanation: String
)

/**
 * Macronutrient breakdown with perfect calorie balancing.
 */
data class BalancedMacros(
    val carbsGrams: Int,
    val proteinGrams: Int,
    val fatGrams: Int,
    val carbsCalories: Int,
    val proteinCalories: Int,
    val fatCalories: Int,
    val totalCalories: Int
) {
    val proteinPercentage: Int
        get() = if (totalCalories > 0) kotlin.math.round((proteinCalories.toFloat() / totalCalories) * 100).toInt() else 0

    val carbsPercentage: Int
        get() = if (totalCalories > 0) kotlin.math.round((carbsCalories.toFloat() / totalCalories) * 100).toInt() else 0

    val fatPercentage: Int
        get() = if (totalCalories > 0) kotlin.math.round((fatCalories.toFloat() / totalCalories) * 100).toInt() else 0
}

/**
 * Rolling average data point for weight tracking.
 */
data class WeightTrendPoint(
    val date: String,
    val rawWeightKg: Float,
    val rollingAverageKg: Float
)

/**
 * Comprehensive analysis of weight trend to prevent daily fluctuation stress.
 */
data class WeightTrendAnalysis(
    val latestRawWeightKg: Float?,
    val currentSevenDayAverageKg: Float?,
    val previousSevenDayAverageKg: Float?,
    val weeklyChangeKg: Float?,
    val totalChangeKg: Float?,
    val trendSummary: String,
    val trendPoints: List<WeightTrendPoint>
)

/**
 * Step-by-step transparency breakdown explaining how the target was derived.
 */
data class CalculationTransparencyStep(
    val stepNumber: Int,
    val title: String,
    val formula: String,
    val inputValues: String,
    val resultText: String,
    val note: String
)

/**
 * MET-based workout calorie estimation result with explicit confidence level.
 */
data class WorkoutCalorieEstimate(
    val estimatedCalories: Int,
    val met: Float,
    val confidence: String, // "High", "Medium", "Low"
    val formulaUsed: String,
    val explanation: String
)

/**
 * Step expenditure calculation isolating active steps above baseline and preventing double counting.
 */
data class StepExpenditureResult(
    val totalSteps: Int,
    val baselineIncidentalSteps: Int = 3000,
    val activeStepsAboveBaseline: Int,
    val estimatedCalories: Int,
    val deductedWalkingWorkoutSteps: Int,
    val explanation: String
)

/**
 * Full Dynamic Daily Energy Expenditure breakdown.
 * Formula: Total Expenditure = Baseline Non-Exercise Expenditure + Logged Workouts + Relevant Net Steps.
 */
data class DynamicExpenditureResult(
    val bmr: Int,
    val isKatchMcArdle: Boolean,
    val baselineExpenditure: Int,
    val loggedWorkoutCalories: Int,
    val loggedStepCalories: Int,
    val totalEstimatedExpenditure: Int,
    val goalAdjustment: Int,
    val rawTargetCalories: Int,
    val dynamicCalorieTarget: Int,
    val isClampedToFloor: Boolean,
    val safetyFloor: Int,
    val netEnergyBalance: Int, // consumed - totalEstimatedExpenditure
    val remainingCalories: Int, // dynamicCalorieTarget - consumed
    val explanation: String,
    val stepExpenditure: StepExpenditureResult = StepExpenditureResult(0, 0, 0, 0, 0, ""),
    val totalActiveBurn: Int = loggedWorkoutCalories + loggedStepCalories,
    val dailyCaloriesBurned: Int = bmr + loggedWorkoutCalories + loggedStepCalories
)

/**
 * Personalized Real-World Maintenance estimated from actual logged intake vs weight change rate.
 */
data class ObservedMaintenanceResult(
    val daysLogged: Int,
    val minDaysRequired: Int = 14,
    val isReady: Boolean,
    val observedMaintenanceKcal: Int?,
    val averageDailyIntakeKcal: Int,
    val totalWeightChangeKg: Float,
    val weeklyChangeRateKg: Float,
    val explanation: String
)

/**
 * Centralized, mathematically and scientifically sound nutrition and metabolic engine.
 */
object Calculations {

    // --- UNIT CONVERSIONS ---

    const val LBS_TO_KG_FACTOR = 0.45359237f
    const val INCHES_TO_CM_FACTOR = 2.54f

    fun lbsToKg(lbs: Float): Float = lbs * LBS_TO_KG_FACTOR

    fun kgToLbs(kg: Float): Float = kg / LBS_TO_KG_FACTOR

    fun inchesToCm(inches: Float): Float = inches * INCHES_TO_CM_FACTOR

    fun cmToInches(cm: Float): Float = cm / INCHES_TO_CM_FACTOR

    fun feetInchesToCm(feet: Int, inches: Float): Float {
        val totalInches = (feet * 12) + inches
        return inchesToCm(totalInches)
    }

    fun cmToFeetInches(cm: Float): Pair<Int, Float> {
        val totalInches = cmToInches(cm)
        val feet = (totalInches / 12f).toInt()
        val remainingInches = totalInches - (feet * 12)
        return Pair(feet, (remainingInches * 10f).roundToInt() / 10f)
    }

    // --- INPUT VALIDATION ---

    fun validateProfileInputs(
        age: Int?,
        heightCm: Float?,
        weightKg: Float?,
        bodyFat: Float? = null
    ): InputValidationResult {
        val errors = mutableListOf<String>()

        if (age == null || age !in 10..120) {
            errors.add("Please enter a valid age between 10 and 120 years.")
        }
        if (heightCm == null || heightCm !in 50f..280f) {
            errors.add("Please enter a realistic height between 50 cm and 280 cm.")
        }
        if (weightKg == null || weightKg !in 20f..400f) {
            errors.add("Please enter a realistic weight between 20 kg and 400 kg.")
        }
        if (bodyFat != null && bodyFat !in 3f..65f) {
            errors.add("Body fat percentage must be between 3% and 65%.")
        }

        return InputValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }

    // --- BMR: MIFFLIN-ST JEOR & KATCH-MCARDLE FORMULAS ---

    /**
     * Calculates Basal Metabolic Rate using the Mifflin-St Jeor equation.
     * All calculations are strictly performed in metric units.
     *
     * Male: (10 × weight_kg) + (6.25 × height_cm) - (5 × age) + 5
     * Female: (10 × weight_kg) + (6.25 × height_cm) - (5 × age) - 161
     */
    fun calculateBmr(weightKg: Float, heightCm: Float, age: Int, gender: String): Float {
        val safeWeight = weightKg.coerceIn(20f, 400f)
        val safeHeight = heightCm.coerceIn(50f, 280f)
        val safeAge = age.coerceIn(10, 120)

        val baseFormula = (10f * safeWeight) + (6.25f * safeHeight) - (5f * safeAge)
        val isMale = gender.trim().lowercase() in listOf("male", "m", "man")

        return if (isMale) {
            baseFormula + 5f
        } else {
            baseFormula - 161f
        }
    }

    /**
     * Katch-McArdle Formula (optional, when body fat percentage is known).
     * Calculates BMR based on Lean Body Mass (LBM):
     * LBM = weight_kg × (1 - body_fat% / 100)
     * BMR = 370 + (21.6 × LBM)
     */
    fun calculateBmrKatchMcArdle(weightKg: Float, bodyFatPercentage: Float): Float {
        val safeWeight = weightKg.coerceIn(20f, 400f)
        val safeFat = bodyFatPercentage.coerceIn(3f, 65f)
        val leanBodyMassKg = safeWeight * (1f - (safeFat / 100f))
        return 370f + (21.6f * leanBodyMassKg)
    }

    /**
     * Conservative Baseline Non-Exercise Expenditure.
     * FitPal does NOT assume arbitrary activity levels.
     * Baseline represents resting BMR + non-exercise daily living activities & TEF (BMR × 1.20).
     * All workouts, sports, and additional steps are dynamically added on top.
     */
    fun calculateConservativeBaselineExpenditure(bmr: Float): Float {
        return bmr * 1.20f
    }

    // --- ACTIVITY MULTIPLIERS & BASELINE TDEE ---

    /**
     * Calculates Baseline Total Daily Energy Expenditure (TDEE).
     * Multipliers:
     * - Sedentary: 1.2
     * - Lightly active: 1.375
     * - Moderately active: 1.55
     * - Very active / Heavy: 1.725
     * - Extremely active / Athlete: 1.9
     */
    fun calculateBaseTDEE(bmr: Float, activityLevel: String): Float {
        val multiplier = when (activityLevel.trim().lowercase()) {
            "sedentary" -> 1.20f
            "light", "lightly active" -> 1.375f
            "moderate", "moderately active" -> 1.55f
            "heavy", "very active" -> 1.725f
            "athlete", "extremely active" -> 1.90f
            else -> 1.20f
        }
        return bmr * multiplier
    }

    // --- SAFETY FLOORS & CALORIE TARGETS ---

    const val MIN_FEMALE_CALORIE_FLOOR = 1200
    const val MIN_MALE_CALORIE_FLOOR = 1500

    fun getSafeFloor(gender: String): Int {
        val isMale = gender.trim().lowercase() in listOf("male", "m", "man")
        return if (isMale) MIN_MALE_CALORIE_FLOOR else MIN_FEMALE_CALORIE_FLOOR
    }

    /**
     * Evaluates daily calorie target based on goal, deficit/surplus, and enforces safety boundaries.
     */
    fun evaluateCalorieTarget(
        bmr: Float,
        activityLevel: String,
        goalType: String,
        gender: String,
        deficitLevel: DeficitLevel = DeficitLevel.MODERATE,
        customDeficitCalories: Int? = null,
        customSurplusCalories: Int? = null
    ): CalorieTargetEvaluation {
        val bmrInt = bmr.roundToInt()
        val baseTdee = calculateBaseTDEE(bmr, activityLevel)
        val tdeeInt = baseTdee.roundToInt()
        val safeFloor = getSafeFloor(gender)

        val rawTarget: Float = when (goalType.trim().lowercase()) {
            "lose", "lose weight", "lose fat" -> {
                if (customDeficitCalories != null && customDeficitCalories > 0) {
                    baseTdee - customDeficitCalories
                } else {
                    baseTdee * (1f - deficitLevel.percentage)
                }
            }
            "gain", "gain weight", "gain muscle" -> {
                if (customSurplusCalories != null && customSurplusCalories > 0) {
                    baseTdee + customSurplusCalories
                } else {
                    // Default modest lean surplus: +10% (~200-300 kcal)
                    baseTdee * 1.10f
                }
            }
            else -> baseTdee // Maintain weight
        }

        val rawTargetInt = rawTarget.roundToInt()
        val isClamped = rawTargetInt < safeFloor
        val finalTarget = max(safeFloor, rawTargetInt)

        var warningMessage: String? = null
        if (isClamped) {
            val genderWord = if (gender.trim().lowercase() in listOf("male", "m", "man")) "men" else "women"
            warningMessage = "Your calculated calorie deficit would result in ${rawTargetInt} kcal, which is below the recognized physiological safety floor for $genderWord ($safeFloor kcal/day). FitPal has automatically adjusted your target to $safeFloor kcal to protect metabolic health and hormone function. Large deficits below this floor require direct clinical supervision."
        } else if (goalType.trim().lowercase().startsWith("lose") && (baseTdee - rawTarget) > (baseTdee * 0.25f)) {
            warningMessage = "A deficit exceeding 25% of your maintenance calories is aggressive and increases risk of muscle loss, fatigue, and rebound. A moderate deficit of 15% is recommended for sustainable fat loss."
        }

        val paceNote = when (goalType.trim().lowercase()) {
            "lose", "lose weight", "lose fat" -> {
                if (deficitLevel == DeficitLevel.AGGRESSIVE) {
                    "Aggressive pace: Requires disciplined macro adherence and adequate protein to spare lean muscle."
                } else if (deficitLevel == DeficitLevel.MILD) {
                    "Mild pace: Optimal for hunger management, strength maintenance, and long-term sustainability."
                } else {
                    "Moderate pace: The scientifically recommended balance between noticeable progress and sustainability."
                }
            }
            "gain", "gain weight", "gain muscle" -> "Lean surplus pace: Aiming for ~0.25% to 0.5% body weight gain per month to maximize lean mass while minimizing fat gain."
            else -> "Maintenance pace: Designed to keep your current scale weight and body composition stable."
        }

        return CalorieTargetEvaluation(
            bmr = bmrInt,
            baseTdee = tdeeInt,
            rawTargetCalories = rawTargetInt,
            finalTargetCalories = finalTarget,
            isClampedToFloor = isClamped,
            safetyWarning = warningMessage,
            recommendedPaceNote = paceNote
        )
    }

    // --- WEIGHT LOSS RATE GUIDANCE (0.5% - 1.0% / WEEK) ---

    fun calculateWeightLossRate(weightKg: Float): WeightLossRateInfo {
        val safeWeight = weightKg.coerceIn(20f, 400f)
        val minLossKg = (safeWeight * 0.005f * 100f).roundToInt() / 100f
        val maxLossKg = (safeWeight * 0.010f * 100f).roundToInt() / 100f

        val minLossLbs = (kgToLbs(minLossKg) * 10f).roundToInt() / 10f
        val maxLossLbs = (kgToLbs(maxLossKg) * 10f).roundToInt() / 10f

        // ~7700 kcal deficit per 1 kg of fat tissue
        val minDailyDeficit = ((minLossKg * 7700f) / 7f).roundToInt()
        val maxDailyDeficit = ((maxLossKg * 7700f) / 7f).roundToInt()

        return WeightLossRateInfo(
            currentWeightKg = safeWeight,
            minWeeklyLossKg = minLossKg,
            maxWeeklyLossKg = maxLossKg,
            minWeeklyLossLbs = minLossLbs,
            maxWeeklyLossLbs = maxLossLbs,
            minDailyDeficitKcal = minDailyDeficit,
            maxDailyDeficitKcal = maxDailyDeficit,
            fluctuationDisclaimer = "Daily scale weight fluctuates continuously due to changes in water retention, glycogen storage (1g glycogen binds ~3-4g water), sodium intake, gut food volume, cortisol/stress, and hormonal cycles. These recommended rates represent average fat loss over several weeks, not daily linear changes."
        )
    }

    // --- PROTEIN RECOMMENDATIONS (1.6 - 2.2 g/kg/day) ---

    fun calculateProteinRecommendations(weightKg: Float): ProteinRecommendation {
        val safeWeight = weightKg.coerceIn(20f, 400f)
        val minGrams = (safeWeight * 1.6f).roundToInt()
        val recGrams = (safeWeight * 1.8f).roundToInt()
        val upperGrams = (safeWeight * 2.2f).roundToInt()
        val rdaGrams = (safeWeight * 0.8f).roundToInt()

        return ProteinRecommendation(
            weightKg = safeWeight,
            minimumGrams = minGrams,
            recommendedGrams = recGrams,
            upperGrams = upperGrams,
            generalHealthRdaGrams = rdaGrams,
            explanation = "For active individuals and resistance trainers, research strongly supports 1.6 to 2.2 g protein per kg of body weight daily to maximize muscle protein synthesis and prevent muscle loss during a calorie deficit. Hitting an exact number each day is not necessary; staying within the 1.6–2.2 g/kg target range is optimal."
        )
    }

    // --- BALANCED MACRONUTRIENTS (4 / 4 / 9 kcal/g) ---

    fun calculateBalancedMacros(
        targetCalories: Int,
        weightKg: Float,
        proteinMultiplier: Float = 1.8f,
        fatMultiplier: Float = 0.8f
    ): BalancedMacros {
        val safeCalories = max(1200, targetCalories)
        val safeWeight = weightKg.coerceIn(20f, 400f)

        // 1. Protein: 4 kcal/g
        val targetProteinGrams = (safeWeight * proteinMultiplier.coerceIn(1.2f, 2.5f)).roundToInt()
        var proteinCal = targetProteinGrams * 4

        // 2. Fat: 9 kcal/g (ensure at least 20% of total calories or 0.6g/kg for endocrine health)
        val idealFatGrams = (safeWeight * fatMultiplier.coerceIn(0.5f, 1.5f)).roundToInt()
        val minFatGrams = max((safeWeight * 0.6f).roundToInt(), ((safeCalories * 0.20f) / 9f).roundToInt())
        var fatGrams = max(minFatGrams, idealFatGrams)
        var fatCal = fatGrams * 9

        // Safety check if protein + fat exceed total calories
        if (proteinCal + fatCal > safeCalories) {
            fatGrams = minFatGrams
            fatCal = fatGrams * 9
            val remainingForProtein = max(0, safeCalories - fatCal)
            val adjustedProtein = remainingForProtein / 4
            proteinCal = adjustedProtein * 4
        }

        // 3. Carbs: 4 kcal/g (remainder)
        val remainingCaloriesForCarbs = max(0, safeCalories - proteinCal - fatCal)
        var carbsGrams = remainingCaloriesForCarbs / 4
        var carbsCal = carbsGrams * 4

        // 4. Exact calorie alignment: adjust carbs by ±1-2 grams to prevent UI rounding discrepancies
        var currentSum = proteinCal + fatCal + carbsCal
        val discrepancy = safeCalories - currentSum
        if (discrepancy != 0) {
            val carbAdjustment = discrepancy / 4
            carbsGrams = max(0, carbsGrams + carbAdjustment)
            carbsCal = carbsGrams * 4
            currentSum = proteinCal + fatCal + carbsCal
        }

        return BalancedMacros(
            carbsGrams = carbsGrams,
            proteinGrams = (proteinCal / 4),
            fatGrams = fatGrams,
            carbsCalories = carbsCal,
            proteinCalories = proteinCal,
            fatCalories = fatCal,
            totalCalories = currentSum
        )
    }

    // --- WORKOUT CALORIE ENGINE (MET BASED) ---

    /**
     * Standard scientifically recognized MET (Metabolic Equivalent of Task) values.
     * 1 MET = 1 kcal / kg / hour (energy expended sitting quietly).
     * Calories = MET × 3.5 × weight_kg / 200 × duration_minutes
     */
    fun getMetForActivity(workoutType: String, intensity: String = "Moderate"): Float {
        val type = workoutType.trim().lowercase()
        val inten = intensity.trim().lowercase()
        val isVigorous = inten in listOf("high", "vigorous", "hard", "heavy")
        val isLight = inten in listOf("low", "light", "gentle")

        return when {
            type.contains("run") || type.contains("jog") -> {
                if (isVigorous) 10.5f else if (isLight) 7.0f else 8.5f
            }
            type.contains("walk") -> {
                if (isVigorous) 4.5f else if (isLight) 2.8f else 3.5f
            }
            type.contains("cycl") || type.contains("bike") -> {
                if (isVigorous) 8.5f else if (isLight) 5.0f else 6.8f
            }
            type.contains("swim") -> {
                if (isVigorous) 9.0f else if (isLight) 5.5f else 7.0f
            }
            type.contains("strength") || type.contains("weight") || type.contains("gym") ||
            type.contains("lift") || type.contains("chest") || type.contains("back") ||
            type.contains("leg") || type.contains("arm") || type.contains("shoulder") -> {
                // Strength training MET: moderate ~3.8, vigorous/circuit ~5.5, light ~3.0
                if (isVigorous) 5.2f else if (isLight) 3.0f else 3.8f
            }
            type.contains("hiit") || type.contains("circuit") -> {
                if (isVigorous) 9.5f else if (isLight) 6.5f else 8.0f
            }
            type.contains("yoga") || type.contains("stretch") || type.contains("pilates") -> {
                if (isVigorous) 3.8f else if (isLight) 2.2f else 2.8f
            }
            type.contains("football") || type.contains("soccer") || type.contains("basketball") ||
            type.contains("tennis") || type.contains("badminton") || type.contains("sport") -> {
                if (isVigorous) 8.5f else if (isLight) 5.5f else 7.0f
            }
            type.contains("cardio") || type.contains("aerobic") || type.contains("dance") -> {
                if (isVigorous) 7.5f else if (isLight) 4.5f else 6.0f
            }
            else -> {
                if (isVigorous) 6.0f else if (isLight) 3.0f else 4.0f
            }
        }
    }

    /**
     * Estimates workout calories burned without fake precision.
     * Strength training is explicitly modeled based on duration, body weight, and intensity,
     * not arbitrary per-exercise guesses.
     */
    fun estimateWorkoutCalories(
        workoutType: String,
        intensity: String,
        durationMinutes: Int,
        weightKg: Float
    ): WorkoutCalorieEstimate {
        val safeDuration = durationMinutes.coerceIn(1, 360)
        val safeWeight = weightKg.coerceIn(20f, 400f)
        val met = getMetForActivity(workoutType, intensity)

        // Standard exercise physiology formula: Calories = MET × 3.5 × weight_kg / 200 × minutes
        val rawCalories = (met * 3.5f * safeWeight / 200f) * safeDuration
        val roundedCalories = (rawCalories / 5f).roundToInt() * 5 // Round to nearest 5 kcal to avoid false precision

        val isStrength = workoutType.lowercase().let {
            it.contains("strength") || it.contains("weight") || it.contains("gym") || it.contains("lift")
        }

        val confidence = when {
            safeDuration > 0 && safeWeight > 0 && isStrength -> "Medium" // Strength has intermittent rest intervals
            safeDuration > 0 && safeWeight > 0 -> "High"
            else -> "Low"
        }

        val explanation = if (isStrength) {
            "Estimated for ${safeDuration}m of $intensity intensity strength training at ${safeWeight.roundToInt()}kg (MET ~$met). Strength training energy expenditure accounts for working sets, rest intervals, and elevated post-exercise recovery."
        } else {
            "Calculated via standard exercise metabolic equivalent (MET $met × 3.5 × ${safeWeight.roundToInt()}kg / 200 × ${safeDuration}m)."
        }

        return WorkoutCalorieEstimate(
            estimatedCalories = roundedCalories,
            met = met,
            confidence = confidence,
            formulaUsed = "MET ($met) × 3.5 × Weight (${safeWeight.roundToInt()}kg) / 200 × Duration (${safeDuration}m)",
            explanation = explanation
        )
    }

    // --- STEP CALORIES & DOUBLE COUNTING PREVENTION ---

    /**
     * Standard sports science step calorie factor: 0.00045 kcal per kg per step.
     */
    fun calculateStepCalorieFactor(weightKg: Float): Float {
        val safeWeight = weightKg.coerceIn(20f, 400f)
        return 0.00045f * safeWeight
    }

    /**
     * Calculates active step calories isolating active steps above baseline and preventing double counting:
     * activeBurn = loggedSteps * stepCalorieFactor
     */
    fun calculateNetStepCalories(
        totalSteps: Int,
        weightKg: Float,
        alreadyLoggedWalkingMinutes: Int = 0
    ): StepExpenditureResult {
        val safeSteps = max(0, totalSteps)
        val safeWeight = weightKg.coerceIn(20f, 400f)

        // Deduct steps attributed to already logged dedicated walking/running workouts (~100 steps/min)
        val deductedWorkoutSteps = min(safeSteps, alreadyLoggedWalkingMinutes * 100)
        val stepsAfterWorkoutDeduction = safeSteps - deductedWorkoutSteps

        // Baseline expenditure inherently accounts for sedentary lifestyle and first 3,000 incidental steps
        val baselineIncidentalSteps = 3000
        val activeStepsAboveBaseline = max(0, stepsAfterWorkoutDeduction - baselineIncidentalSteps)

        // Standard formula: 0.00045 kcal per kg per step
        val netKcal = (activeStepsAboveBaseline * 0.00045f * safeWeight).roundToInt()

        val explanation = when {
            safeSteps == 0 -> "No steps logged yet today."
            activeStepsAboveBaseline == 0 ->
                "$safeSteps steps logged. Incidental daily steps (first 3,000) are already accounted for in your baseline metabolism to prevent double counting."
            deductedWorkoutSteps > 0 ->
                "$activeStepsAboveBaseline active steps above baseline burned ~$netKcal kcal ($deductedWorkoutSteps steps excluded to avoid double-counting logged walking workout)."
            else ->
                "$activeStepsAboveBaseline active steps above baseline burned ~$netKcal kcal."
        }

        return StepExpenditureResult(
            totalSteps = safeSteps,
            baselineIncidentalSteps = baselineIncidentalSteps,
            activeStepsAboveBaseline = activeStepsAboveBaseline,
            estimatedCalories = netKcal,
            deductedWalkingWorkoutSteps = deductedWorkoutSteps,
            explanation = explanation
        )
    }

    /**
     * Legacy estimate for backward compatibility with older components.
     * Standard formula: steps * 0.0005 * weightKg
     */
    fun estimateStepCalories(steps: Int, weightKg: Float): Float {
        val safeSteps = max(0, steps)
        val safeWeight = weightKg.coerceIn(20f, 400f)
        return safeSteps * 0.0005f * safeWeight
    }

    // --- DYNAMIC DAILY ENERGY EXPENDITURE ENGINE ---

    /**
     * Evaluates dynamic daily energy expenditure and calorie targets with zero double counting.
     * Profile -> Baseline -> Actual logged activity -> Dynamic expenditure -> Calorie target.
     */
    fun evaluateDynamicDailyExpenditure(
        age: Int,
        gender: String,
        heightCm: Float,
        currentWeightKg: Float,
        bodyFatPercent: Float? = null,
        goalType: String = "maintain",
        customAdjustmentKcal: Int? = null,
        deficitLevel: DeficitLevel = DeficitLevel.MODERATE,
        loggedWorkoutCalories: Int = 0,
        totalSteps: Int = 0,
        alreadyLoggedWalkingMinutes: Int = 0,
        consumedCalories: Int = 0
    ): DynamicExpenditureResult {
        val safeWeight = currentWeightKg.coerceIn(20f, 400f)
        val safeHeight = heightCm.coerceIn(50f, 280f)
        val safeAge = age.coerceIn(10, 120)

        // 1. BMR calculation: Mifflin-St Jeor (or Katch-McArdle if body fat is provided)
        val (bmrVal, isKatch) = if (bodyFatPercent != null && bodyFatPercent in 5f..60f) {
            Pair(calculateBmrKatchMcArdle(safeWeight, bodyFatPercent), true)
        } else {
            Pair(calculateBmr(safeWeight, safeHeight, safeAge, gender), false)
        }
        val bmrInt = bmrVal.roundToInt()

        // 2. Conservative baseline expenditure: BMR × 1.20 (resting + minimal sedentary living & TEF)
        val baselineInt = calculateConservativeBaselineExpenditure(bmrVal).roundToInt()

        // 3. Logged workout expenditure
        val workoutInt = max(0, loggedWorkoutCalories)

        // 4. Net step expenditure (additive formula: loggedSteps * stepCalorieFactor)
        val stepResult = calculateNetStepCalories(totalSteps, safeWeight, alreadyLoggedWalkingMinutes)
        val stepInt = stepResult.estimatedCalories

        // 5. Total active burn and daily calories burned:
        // dailyCaloriesBurned = baseMetabolicRate + totalActiveBurn
        val totalActiveBurn = workoutInt + stepInt
        val dailyCaloriesBurned = bmrInt + totalActiveBurn
        val totalExpenditure = baselineInt + workoutInt + stepInt

        // 6. Goal Adjustment (Deficit or Surplus applied to dynamic expenditure)
        val goalLower = goalType.trim().lowercase()
        val goalAdj = when {
            goalLower.startsWith("lose") -> {
                if (customAdjustmentKcal != null && customAdjustmentKcal > 0) {
                    -customAdjustmentKcal
                } else {
                    -(totalExpenditure * deficitLevel.percentage).roundToInt()
                }
            }
            goalLower.startsWith("gain") -> {
                if (customAdjustmentKcal != null && customAdjustmentKcal > 0) {
                    customAdjustmentKcal
                } else {
                    // Modest 10% lean surplus
                    (totalExpenditure * 0.10f).roundToInt()
                }
            }
            else -> 0
        }

        val rawTarget = totalExpenditure + goalAdj
        val safeFloor = getSafeFloor(gender)
        val isClamped = rawTarget < safeFloor
        val finalTarget = max(safeFloor, rawTarget)

        val netBalance = consumedCalories - totalExpenditure
        val remaining = finalTarget - consumedCalories

        val explanation = StringBuilder().apply {
            append("BMR: $bmrInt kcal + Active: $totalActiveBurn kcal (Workouts: $workoutInt + Steps: $stepInt)")
            append(" = Total Daily Burn: $dailyCaloriesBurned kcal.")
            if (goalAdj != 0) {
                val sign = if (goalAdj > 0) "+" else ""
                append(" Goal Adjustment: $sign$goalAdj kcal -> Target $finalTarget kcal.")
            }
            if (isClamped) {
                append(" Clamped to $safeFloor kcal physiological safety floor.")
            }
        }.toString()

        return DynamicExpenditureResult(
            bmr = bmrInt,
            isKatchMcArdle = isKatch,
            baselineExpenditure = baselineInt,
            loggedWorkoutCalories = workoutInt,
            loggedStepCalories = stepInt,
            totalEstimatedExpenditure = totalExpenditure,
            goalAdjustment = goalAdj,
            rawTargetCalories = rawTarget,
            dynamicCalorieTarget = finalTarget,
            isClampedToFloor = isClamped,
            safetyFloor = safeFloor,
            netEnergyBalance = netBalance,
            remainingCalories = remaining,
            explanation = explanation,
            stepExpenditure = stepResult,
            totalActiveBurn = totalActiveBurn,
            dailyCaloriesBurned = dailyCaloriesBurned
        )
    }

    // --- PERSONALIZED OBSERVED REAL-WORLD MAINTENANCE (REQUIREMENT 18) ---

    /**
     * Calculates personalized observed maintenance once sufficient historical data exists (14+ days).
     * Relies on the scientific principle that 1 kg of body mass change ≈ 7,700 kcal energy imbalance.
     * Observed Maintenance = Average Daily Intake - (Daily Weight Change Rate in kg × 7,700)
     */
    fun calculateObservedMaintenance(
        weightLogs: List<WeightLog>,
        foodEntries: List<FoodEntry>
    ): ObservedMaintenanceResult {
        if (weightLogs.size < 2 || foodEntries.isEmpty()) {
            return ObservedMaintenanceResult(
                daysLogged = weightLogs.map { it.date }.distinct().size,
                minDaysRequired = 14,
                isReady = false,
                observedMaintenanceKcal = null,
                averageDailyIntakeKcal = 0,
                totalWeightChangeKg = 0f,
                weeklyChangeRateKg = 0f,
                explanation = "Log your weight and meals across at least 14 days to unlock your personalized observed metabolic maintenance."
            )
        }

        val sortedWeights = weightLogs.sortedBy { it.date }
        val distinctWeightDays = sortedWeights.map { it.date }.distinct()
        val daysLogged = distinctWeightDays.size

        if (daysLogged < 14) {
            return ObservedMaintenanceResult(
                daysLogged = daysLogged,
                minDaysRequired = 14,
                isReady = false,
                observedMaintenanceKcal = null,
                averageDailyIntakeKcal = 0,
                totalWeightChangeKg = 0f,
                weeklyChangeRateKg = 0f,
                explanation = "$daysLogged of 14 days logged. FitPal requires 14+ days of consistent weight and food logging to eliminate water noise and establish an accurate observed maintenance."
            )
        }

        val firstWeight = sortedWeights.first().weightKg
        val lastWeight = sortedWeights.last().weightKg
        val totalDeltaKg = lastWeight - firstWeight

        // Group food entries by date
        val dailyIntake = foodEntries.groupBy { it.date }.mapValues { (_, entries) ->
            entries.sumOf { it.caloriesConsumed.toDouble() }.toInt()
        }

        val avgDailyIntake = dailyIntake.values.average().roundToInt()

        // Days between first and last weight log
        val totalDaysBetween = max(1, daysLogged)
        val dailyWeightChangeRateKg = totalDeltaKg / totalDaysBetween.toFloat()
        val weeklyChangeRateKg = ((dailyWeightChangeRateKg * 7f) * 100f).roundToInt() / 100f

        // Energy delta: 1 kg body mass ≈ 7,700 kcal
        val dailyEnergyImbalanceKcal = (dailyWeightChangeRateKg * 7700f).roundToInt()
        val calculatedMaintenance = avgDailyIntake - dailyEnergyImbalanceKcal
        val safeObservedMaintenance = calculatedMaintenance.coerceIn(1200, 5000)

        val direction = when {
            weeklyChangeRateKg < -0.05f -> "losing approximately ${-weeklyChangeRateKg} kg/week"
            weeklyChangeRateKg > 0.05f -> "gaining approximately ${weeklyChangeRateKg} kg/week"
            else -> "maintaining stable weight"
        }

        val explanation = "Based on $daysLogged days of logged food (averaging $avgDailyIntake kcal/day) and $direction, your actual real-world maintenance is approximately ~$safeObservedMaintenance kcal/day."

        return ObservedMaintenanceResult(
            daysLogged = daysLogged,
            minDaysRequired = 14,
            isReady = true,
            observedMaintenanceKcal = safeObservedMaintenance,
            averageDailyIntakeKcal = avgDailyIntake,
            totalWeightChangeKg = ((totalDeltaKg * 10f).roundToInt() / 10f),
            weeklyChangeRateKg = weeklyChangeRateKg,
            explanation = explanation
        )
    }

    // --- WEIGHT TREND & 7-DAY ROLLING AVERAGE ---

    /**
     * Calculates rolling 7-day average for daily weight logs to eliminate noise from water retention.
     */
    fun calculateWeightTrend(weightLogs: List<WeightLog>): WeightTrendAnalysis {
        if (weightLogs.isEmpty()) {
            return WeightTrendAnalysis(
                latestRawWeightKg = null,
                currentSevenDayAverageKg = null,
                previousSevenDayAverageKg = null,
                weeklyChangeKg = null,
                totalChangeKg = null,
                trendSummary = "Log your weight consistently across multiple days to view your true 7-day smoothed trend.",
                trendPoints = emptyList()
            )
        }

        // Sort ascending by date for chronological calculation
        val sorted = weightLogs.sortedBy { it.date }
        val points = mutableListOf<WeightTrendPoint>()

        for (i in sorted.indices) {
            val windowStart = max(0, i - 6)
            val window = sorted.subList(windowStart, i + 1)
            val avg = window.map { it.weightKg }.average().toFloat()
            val roundedAvg = (avg * 10f).roundToInt() / 10f
            points.add(
                WeightTrendPoint(
                    date = sorted[i].date,
                    rawWeightKg = sorted[i].weightKg,
                    rollingAverageKg = roundedAvg
                )
            )
        }

        val latestPoint = points.last()
        val latestAvg = latestPoint.rollingAverageKg

        // Compare current 7-day window with previous 7-day window if available
        var prevAvg: Float? = null
        var weeklyChange: Float? = null
        if (points.size >= 8) {
            val prevPoint = points[points.size - 8]
            prevAvg = prevPoint.rollingAverageKg
            weeklyChange = ((latestAvg - prevAvg) * 10f).roundToInt() / 10f
        }

        val firstPoint = points.first()
        val totalChange = ((latestPoint.rawWeightKg - firstPoint.rawWeightKg) * 10f).roundToInt() / 10f

        val summary = when {
            weeklyChange != null && weeklyChange < -0.1f -> {
                "7-Day Trend: Down ${-weeklyChange} kg this week. Moving steadily in a safe fat-loss trajectory."
            }
            weeklyChange != null && weeklyChange > 0.1f -> {
                "7-Day Trend: Up +${weeklyChange} kg this week. Note: short-term increases are usually water, glycogen, or sodium."
            }
            weeklyChange != null -> {
                "7-Day Trend: Weight is stable (change < 0.1 kg). Great maintenance or steady compositional equilibrium."
            }
            else -> {
                "Latest 7-day rolling average: $latestAvg kg. Continue logging daily to unlock weekly trend rate comparison."
            }
        }

        return WeightTrendAnalysis(
            latestRawWeightKg = latestPoint.rawWeightKg,
            currentSevenDayAverageKg = latestAvg,
            previousSevenDayAverageKg = prevAvg,
            weeklyChangeKg = weeklyChange,
            totalChangeKg = totalChange,
            trendSummary = summary,
            trendPoints = points
        )
    }

    // --- CALCULATION TRANSPARENCY EXPLANATION BUILDER ---

    fun buildCalculationTransparency(
        age: Int,
        gender: String,
        heightCm: Float,
        weightKg: Float,
        activityLevel: String,
        goalType: String,
        deficitLevel: DeficitLevel = DeficitLevel.MODERATE,
        customAdjustment: Int? = null
    ): List<CalculationTransparencyStep> {
        val bmr = calculateBmr(weightKg, heightCm, age, gender)
        val activity = ActivityLevel.fromKey(activityLevel)
        val tdee = calculateBaseTDEE(bmr, activityLevel)
        val evaluation = evaluateCalorieTarget(
            bmr = bmr,
            activityLevel = activityLevel,
            goalType = goalType,
            gender = gender,
            deficitLevel = deficitLevel,
            customDeficitCalories = if (goalType.trim().lowercase().startsWith("lose")) customAdjustment else null,
            customSurplusCalories = if (goalType.trim().lowercase().startsWith("gain")) customAdjustment else null
        )
        val macros = calculateBalancedMacros(
            targetCalories = evaluation.finalTargetCalories,
            weightKg = weightKg
        )

        val isMale = gender.trim().lowercase() in listOf("male", "m", "man")
        val bmrConst = if (isMale) "+5" else "-161"

        return listOf(
            CalculationTransparencyStep(
                stepNumber = 1,
                title = "Basal Metabolic Rate (BMR)",
                formula = "Mifflin-St Jeor: (10 × kg) + (6.25 × cm) - (5 × age) $bmrConst",
                inputValues = "Weight: ${weightKg}kg | Height: ${heightCm.toInt()}cm | Age: ${age}y | Sex: $gender",
                resultText = "${bmr.roundToInt()} kcal/day",
                note = "This is the minimum energy your body burns at complete rest for vital cellular and organ functions."
            ),
            CalculationTransparencyStep(
                stepNumber = 2,
                title = "Estimated Maintenance Calories (TDEE)",
                formula = "TDEE = BMR × Activity Multiplier (${activity.multiplier})",
                inputValues = "Activity Level: ${activity.label} (${activity.description})",
                resultText = "${tdee.roundToInt()} kcal/day",
                note = "TDEE is an estimate representing your baseline total energy expenditure including daily lifestyle activity."
            ),
            CalculationTransparencyStep(
                stepNumber = 3,
                title = "Goal Adjustment (${goalType.replaceFirstChar { it.uppercase() }})",
                formula = when (goalType.trim().lowercase()) {
                    "lose", "lose weight", "lose fat" -> "Deficit: -${(deficitLevel.percentage * 100).toInt()}% (-${(tdee * deficitLevel.percentage).roundToInt()} kcal)"
                    "gain", "gain weight", "gain muscle" -> "Surplus: +10% (+${(tdee * 0.10f).roundToInt()} kcal)"
                    else -> "Maintenance: 0 kcal adjustment"
                },
                inputValues = "Selected Strategy: ${deficitLevel.label}",
                resultText = "${evaluation.rawTargetCalories} kcal/day",
                note = evaluation.recommendedPaceNote
            ),
            CalculationTransparencyStep(
                stepNumber = 4,
                title = "Daily Target & Safety Floor Protection",
                formula = "Final Target = max(Safety Floor (${getSafeFloor(gender)} kcal), Calculated Target)",
                inputValues = "Safe physiological floor: ${getSafeFloor(gender)} kcal/day",
                resultText = "${evaluation.finalTargetCalories} kcal/day",
                note = if (evaluation.isClampedToFloor) {
                    "Safety floor active! Target clamped to ${getSafeFloor(gender)} kcal to avoid metabolic & endocrine slowdown."
                } else {
                    "Your calculated target is safely above the minimum physiological threshold."
                }
            ),
            CalculationTransparencyStep(
                stepNumber = 5,
                title = "Macronutrient Distribution",
                formula = "Protein (4 kcal/g) + Carbs (4 kcal/g) + Fat (9 kcal/g)",
                inputValues = "P: ${macros.proteinGrams}g (${macros.proteinCalories} kcal) | C: ${macros.carbsGrams}g (${macros.carbsCalories} kcal) | F: ${macros.fatGrams}g (${macros.fatCalories} kcal)",
                resultText = "${macros.totalCalories} kcal balanced total",
                note = "Protein spared at 1.8 g/kg to protect muscle; healthy fat maintained for hormone and cellular integrity."
            )
        )
    }

    // --- LEGACY COMPATIBILITY METHODS ---
    // Preserved to ensure 100% compile safety with all existing screens and view models

    fun calculateDynamicCalories(
        baseTDEE: Float,
        exerciseCalories: Float,
        stepCalories: Float,
        goalAdjustment: Float
    ): Float {
        return (baseTDEE + exerciseCalories + stepCalories + goalAdjustment).coerceAtLeast(1200f)
    }

    fun calculateProtein(goalWeightKg: Float, goalType: String, customMultiplier: Float? = null): Float {
        val multiplier = customMultiplier ?: when (goalType.lowercase()) {
            "lose", "lose fat" -> 2.0f
            "maintain" -> 1.8f
            "gain", "gain muscle" -> 2.2f
            else -> 1.8f
        }
        return goalWeightKg * multiplier
    }

    fun calculateFat(currentWeightKg: Float, customMultiplier: Float? = null): Float {
        val multiplier = customMultiplier ?: 0.7f
        return currentWeightKg * multiplier
    }

    fun calculateCarbs(dailyCalories: Float, proteinCalories: Float, fatCalories: Float): Float {
        return max(0f, (dailyCalories - proteinCalories - fatCalories) / 4f)
    }

    fun calculateMacros(
        dailyCalories: Float,
        currentWeightKg: Float,
        goalWeightKg: Float,
        goalType: String,
        customProteinMultiplier: Float? = null,
        customFatMultiplier: Float? = null
    ): Triple<Float, Float, Float> {
        val balanced = calculateBalancedMacros(
            targetCalories = dailyCalories.roundToInt(),
            weightKg = currentWeightKg,
            proteinMultiplier = customProteinMultiplier ?: 1.8f,
            fatMultiplier = customFatMultiplier ?: 0.8f
        )
        return Triple(balanced.carbsGrams.toFloat(), balanced.proteinGrams.toFloat(), balanced.fatGrams.toFloat())
    }

    fun calculateTargetCalorieGoal(
        dailyCalories: Float,
        weightGoalType: String,
        adjustmentValue: Int,
        bmr: Float,
        gender: String
    ): Int {
        val safeFloor = getSafeFloor(gender)
        val goalAdj = when (weightGoalType.lowercase()) {
            "lose", "lose fat" -> -adjustmentValue.toFloat()
            "gain", "gain muscle" -> adjustmentValue.toFloat()
            else -> 0f
        }
        val target = (dailyCalories + goalAdj).roundToInt()
        return max(safeFloor, target)
    }

    fun calculateMacrosCustom(
        targetCalories: Int,
        weightKg: Float,
        proteinMultiplier: Float,
        fatMultiplier: Float
    ): Triple<Int, Int, Int> {
        val balanced = calculateBalancedMacros(
            targetCalories = targetCalories,
            weightKg = weightKg,
            proteinMultiplier = proteinMultiplier,
            fatMultiplier = fatMultiplier
        )
        return Triple(balanced.carbsGrams, balanced.proteinGrams, balanced.fatGrams)
    }

    fun runEngine(
        age: Int,
        gender: String,
        heightCm: Float,
        currentWeightKg: Float,
        goalWeightKg: Float,
        activityLevel: String,
        goalType: String,
        calorieAdjustment: Int,
        stepsCount: Int,
        exerciseActiveCalories: Float,
        consumedCalories: Float,
        consumedProtein: Float,
        consumedCarbs: Float,
        consumedFat: Float,
        customProteinMultiplier: Float? = null,
        customFatMultiplier: Float? = null
    ): NutritionEngineResult {
        val bmrVal = calculateBmr(currentWeightKg, heightCm, age, gender)
        val baseTDEEVal = calculateBaseTDEE(bmrVal, activityLevel)
        val stepCaloriesVal = estimateStepCalories(stepsCount, currentWeightKg)
        val goalAdj = when (goalType.lowercase()) {
            "lose", "lose fat" -> -calorieAdjustment.toFloat()
            "gain", "gain muscle" -> calorieAdjustment.toFloat()
            else -> 0f
        }

        val dailyCalorieTargetVal = max(getSafeFloor(gender).toFloat(), baseTDEEVal + goalAdj)

        val balanced = calculateBalancedMacros(
            targetCalories = dailyCalorieTargetVal.roundToInt(),
            weightKg = currentWeightKg,
            proteinMultiplier = customProteinMultiplier ?: 1.8f,
            fatMultiplier = customFatMultiplier ?: 0.8f
        )

        val pGrams = balanced.proteinGrams
        val pCal = balanced.proteinCalories
        val fGrams = balanced.fatGrams
        val fCal = balanced.fatCalories
        val cGrams = balanced.carbsGrams
        val cCal = balanced.carbsCalories
        val targetCalories = balanced.totalCalories

        val remCal = max(0, targetCalories - consumedCalories.roundToInt())
        val remProt = max(0, pGrams - consumedProtein.roundToInt())
        val remCarb = max(0, cGrams - consumedCarbs.roundToInt())
        val remFat = max(0, fGrams - consumedFat.roundToInt())

        val progressCal = if (targetCalories > 0) (consumedCalories / targetCalories) * 100f else 0f
        val progressProt = if (pGrams > 0) (consumedProtein / pGrams) * 100f else 0f
        val progressCarb = if (cGrams > 0) (consumedCarbs / cGrams) * 100f else 0f
        val progressFat = if (fGrams > 0) (consumedFat / fGrams) * 100f else 0f

        return NutritionEngineResult(
            bmr = bmrVal.roundToInt(),
            baseTDEE = baseTDEEVal.roundToInt(),
            exerciseCalories = exerciseActiveCalories.roundToInt(),
            stepCalories = stepCaloriesVal.roundToInt(),
            goalAdjustment = goalAdj.roundToInt(),
            dailyCalorieTarget = targetCalories,
            protein = MacroDetails(grams = pGrams, calories = pCal),
            fat = MacroDetails(grams = fGrams, calories = fCal),
            carbs = MacroDetails(grams = cGrams, calories = cCal),
            consumed = ConsumedDetails(
                calories = consumedCalories.roundToInt(),
                protein = consumedProtein.roundToInt(),
                carbs = consumedCarbs.roundToInt(),
                fat = consumedFat.roundToInt()
            ),
            remaining = RemainingDetails(
                calories = remCal,
                protein = remProt,
                carbs = remCarb,
                fat = remFat
            ),
            progress = ProgressDetails(
                calories = progressCal,
                protein = progressProt,
                carbs = progressCarb,
                fat = progressFat
            )
        )
    }
}
