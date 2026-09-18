package com.example.fitpal.util

import com.example.fitpal.data.local.entity.WeightLog
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class CalculationsTest {

    // --- MIFFLIN-ST JEOR BMR TESTS ---

    @Test
    fun testMifflinStJeorMale() {
        // Male: 80 kg, 180 cm, 30 years old
        // BMR = (10 * 80) + (6.25 * 180) - (5 * 30) + 5
        //     = 800 + 1125 - 150 + 5 = 1780 kcal
        val bmr = Calculations.calculateBmr(
            weightKg = 80f,
            heightCm = 180f,
            age = 30,
            gender = "Male"
        )
        assertEquals(1780f, bmr, 0.01f)
    }

    @Test
    fun testMifflinStJeorFemale() {
        // Female: 60 kg, 165 cm, 28 years old
        // BMR = (10 * 60) + (6.25 * 165) - (5 * 28) - 161
        //     = 600 + 1031.25 - 140 - 161 = 1330.25 kcal
        val bmr = Calculations.calculateBmr(
            weightKg = 60f,
            heightCm = 165f,
            age = 28,
            gender = "Female"
        )
        assertEquals(1330.25f, bmr, 0.01f)
    }

    // --- UNIT CONVERSIONS TESTS ---

    @Test
    fun testUnitConversions() {
        // 100 lbs to kg
        val kg = Calculations.lbsToKg(100f)
        assertEquals(45.359237f, kg, 0.001f)

        // Reverse kg to lbs
        val lbs = Calculations.kgToLbs(kg)
        assertEquals(100f, lbs, 0.01f)

        // 70 inches to cm = 177.8 cm
        val cm = Calculations.inchesToCm(70f)
        assertEquals(177.8f, cm, 0.01f)

        // 5 feet 10 inches to cm = 70 inches = 177.8 cm
        val feetInchesCm = Calculations.feetInchesToCm(5, 10f)
        assertEquals(177.8f, feetInchesCm, 0.01f)

        // cm back to feet & inches
        val (feet, inches) = Calculations.cmToFeetInches(177.8f)
        assertEquals(5, feet)
        assertEquals(10.0f, inches, 0.1f)
    }

    // --- INPUT VALIDATION TESTS ---

    @Test
    fun testInputValidation() {
        // Valid profile
        val valid = Calculations.validateProfileInputs(
            age = 25,
            heightCm = 175f,
            weightKg = 70f,
            bodyFat = 15f
        )
        assertTrue(valid.isValid)
        assertTrue(valid.errors.isEmpty())

        // Invalid age
        val invalidAge = Calculations.validateProfileInputs(
            age = 5,
            heightCm = 175f,
            weightKg = 70f
        )
        assertFalse(invalidAge.isValid)
        assertTrue(invalidAge.errors.any { it.contains("age", ignoreCase = true) })

        // Out of range height & weight
        val invalidMetrics = Calculations.validateProfileInputs(
            age = 25,
            heightCm = 40f,
            weightKg = 12f
        )
        assertFalse(invalidMetrics.isValid)
        assertEquals(2, invalidMetrics.errors.size)
    }

    // --- ACTIVITY MULTIPLIERS & BASELINE TDEE TESTS ---

    @Test
    fun testActivityMultipliers() {
        val bmr = 1500f
        assertEquals(1800f, Calculations.calculateBaseTDEE(bmr, "Sedentary"), 0.1f)
        assertEquals(2062.5f, Calculations.calculateBaseTDEE(bmr, "Light"), 0.1f)
        assertEquals(2325f, Calculations.calculateBaseTDEE(bmr, "Moderate"), 0.1f)
        assertEquals(2587.5f, Calculations.calculateBaseTDEE(bmr, "Heavy"), 0.1f)
        assertEquals(2850f, Calculations.calculateBaseTDEE(bmr, "Athlete"), 0.1f)
    }

    // --- SAFETY FLOORS & DEFICIT BOUNDARIES TESTS ---

    @Test
    fun testSafetyFloorClampingFemale() {
        // Female with low maintenance TDEE: 1300 kcal
        // An aggressive 20% deficit would yield 1040 kcal, which is dangerously low (< 1200 kcal floor)
        val evaluation = Calculations.evaluateCalorieTarget(
            bmr = 1000f,
            activityLevel = "Sedentary", // TDEE = 1200
            goalType = "Lose",
            gender = "Female",
            deficitLevel = DeficitLevel.AGGRESSIVE // 20% deficit -> 960 kcal
        )

        assertTrue(evaluation.isClampedToFloor)
        assertEquals(Calculations.MIN_FEMALE_CALORIE_FLOOR, evaluation.finalTargetCalories)
        assertNotNull(evaluation.safetyWarning)
        assertTrue(evaluation.safetyWarning!!.contains("safety floor"))
    }

    @Test
    fun testSafetyFloorClampingMale() {
        // Male minimum floor is 1500 kcal
        val evaluation = Calculations.evaluateCalorieTarget(
            bmr = 1400f,
            activityLevel = "Sedentary", // TDEE = 1680
            goalType = "Lose",
            gender = "Male",
            deficitLevel = DeficitLevel.AGGRESSIVE // 20% -> 1344 kcal
        )

        assertTrue(evaluation.isClampedToFloor)
        assertEquals(Calculations.MIN_MALE_CALORIE_FLOOR, evaluation.finalTargetCalories)
        assertNotNull(evaluation.safetyWarning)
    }

    @Test
    fun testSafeDeficitDoesNotClamp() {
        // High TDEE male: 2800 kcal, Moderate 15% deficit -> 2380 kcal (well above 1500)
        val evaluation = Calculations.evaluateCalorieTarget(
            bmr = 1800f,
            activityLevel = "Moderate", // TDEE = 2790
            goalType = "Lose",
            gender = "Male",
            deficitLevel = DeficitLevel.MODERATE
        )

        assertFalse(evaluation.isClampedToFloor)
        assertTrue(evaluation.finalTargetCalories > 2000)
    }

    // --- WEIGHT LOSS RATE RECOMMENDATIONS TESTS ---

    @Test
    fun testWeightLossRateRecommendations() {
        // 96 kg individual (per prompt specification)
        // 0.5% = 0.48 kg/week
        // 1.0% = 0.96 kg/week
        val info = Calculations.calculateWeightLossRate(96f)
        assertEquals(0.48f, info.minWeeklyLossKg, 0.01f)
        assertEquals(0.96f, info.maxWeeklyLossKg, 0.01f)
        assertTrue(info.minDailyDeficitKcal in 500..600)
        assertTrue(info.maxDailyDeficitKcal in 1000..1100)
        assertTrue(info.fluctuationDisclaimer.contains("glycogen"))
    }

    // --- PROTEIN RECOMMENDATIONS (1.6 - 2.2 g/kg) TESTS ---

    @Test
    fun testProteinRecommendations() {
        // 75 kg individual
        // Min: 75 * 1.6 = 120g
        // Recommended: 75 * 1.8 = 135g
        // Upper: 75 * 2.2 = 165g
        val protein = Calculations.calculateProteinRecommendations(75f)
        assertEquals(120, protein.minimumGrams)
        assertEquals(135, protein.recommendedGrams)
        assertEquals(165, protein.upperGrams)
        assertEquals(60, protein.generalHealthRdaGrams)
    }

    // --- BALANCED MACRONUTRIENT ARITHMETIC (4 / 4 / 9 kcal/g) TESTS ---

    @Test
    fun testBalancedMacrosAddUpExactly() {
        val targets = listOf(1500, 1800, 2000, 2350, 2700, 3100)

        for (target in targets) {
            val macros = Calculations.calculateBalancedMacros(
                targetCalories = target,
                weightKg = 80f,
                proteinMultiplier = 1.8f,
                fatMultiplier = 0.8f
            )

            val calculatedSum = (macros.proteinGrams * 4) + (macros.carbsGrams * 4) + (macros.fatGrams * 9)
            // Verify within rounding tolerance of max 4 kcal
            assertTrue(
                "Discrepancy too high for target $target: got $calculatedSum vs $target",
                abs(calculatedSum - target) <= 4
            )
            assertTrue(macros.proteinGrams > 0)
            assertTrue(macros.fatGrams > 0)
            assertTrue(macros.carbsGrams >= 0)
        }
    }

    // --- STEP CALORIES ESTIMATION TESTS ---

    @Test
    fun testStepCaloriesEstimate() {
        // 10,000 steps for 70 kg person
        // 10000 * 0.0005 * 70 = 350 kcal
        val stepsKcal = Calculations.estimateStepCalories(10000, 70f)
        assertEquals(350f, stepsKcal, 0.1f)
    }

    // --- WEIGHT TREND & ROLLING AVERAGE TESTS ---

    @Test
    fun testWeightTrendSevenDayAverage() {
        val logs = listOf(
            WeightLog(1, 80.0f, "2026-03-01"),
            WeightLog(2, 80.5f, "2026-03-02"), // water spike
            WeightLog(3, 79.8f, "2026-03-03"),
            WeightLog(4, 80.2f, "2026-03-04"),
            WeightLog(5, 79.5f, "2026-03-05"),
            WeightLog(6, 79.7f, "2026-03-06"),
            WeightLog(7, 79.2f, "2026-03-07")
        )

        val report = Calculations.calculateWeightTrend(logs)
        assertNotNull(report.currentSevenDayAverageKg)
        assertEquals(7, report.trendPoints.size)

        // Average of the 7 days: (80 + 80.5 + 79.8 + 80.2 + 79.5 + 79.7 + 79.2) / 7 = 558.9 / 7 = 79.84 -> 79.8 kg
        assertEquals(79.8f, report.currentSevenDayAverageKg!!, 0.1f)
    }

    // --- TRANSPARENCY EXPLANATION TEST ---

    @Test
    fun testCalculationTransparencySteps() {
        val steps = Calculations.buildCalculationTransparency(
            age = 28,
            gender = "Male",
            heightCm = 178f,
            weightKg = 75f,
            activityLevel = "Moderate",
            goalType = "Lose"
        )

        assertEquals(5, steps.size)
        assertEquals("Basal Metabolic Rate (BMR)", steps[0].title)
        assertEquals("Estimated Maintenance Calories (TDEE)", steps[1].title)
        assertTrue(steps[0].resultText.contains("kcal"))
        assertTrue(steps[3].resultText.contains("kcal"))
    }

    // --- KATCH-MCARDLE BMR TEST ---

    @Test
    fun testKatchMcArdleBmr() {
        // 80 kg individual with 15% body fat
        // LBM = 80 * (1 - 0.15) = 68 kg
        // BMR = 370 + (21.6 * 68) = 370 + 1468.8 = 1838.8 kcal
        val bmr = Calculations.calculateBmrKatchMcArdle(80f, 15f)
        assertEquals(1838.8f, bmr, 0.1f)
    }

    // --- CONSERVATIVE BASELINE EXPENDITURE TEST ---

    @Test
    fun testConservativeBaselineExpenditure() {
        // BMR 1780 -> Baseline (BMR * 1.20) = 2136 kcal
        val baseline = Calculations.calculateConservativeBaselineExpenditure(1780f)
        assertEquals(2136f, baseline, 0.1f)
    }

    // --- WORKOUT MET CALORIE ENGINE TESTS ---

    @Test
    fun testWorkoutCalorieEstimation() {
        // 80 kg individual doing 60 min strength training (moderate MET 3.8)
        // Calories = 3.8 * 3.5 * 80 / 200 * 60 = 319.2 -> rounded to nearest 5 = 320 kcal
        val estimate = Calculations.estimateWorkoutCalories(
            workoutType = "Strength Training",
            intensity = "Moderate",
            durationMinutes = 60,
            weightKg = 80f
        )
        assertEquals(320, estimate.estimatedCalories)
        assertEquals("Medium", estimate.confidence) // Intermittent rest in lifting

        // 70 kg individual running 45 min moderate (MET 8.5)
        // Calories = 8.5 * 3.5 * 70 / 200 * 45 = 468.56 -> ~470 kcal
        val runEstimate = Calculations.estimateWorkoutCalories(
            workoutType = "Running",
            intensity = "Moderate",
            durationMinutes = 45,
            weightKg = 70f
        )
        assertEquals(470, runEstimate.estimatedCalories)
        assertEquals("High", runEstimate.confidence)
    }

    // --- NET STEPS & DOUBLE COUNTING PREVENTION TESTS ---

    @Test
    fun testNetStepCaloriesPrevention() {
        // 80 kg person with 10,000 steps, no walking workout
        // Baseline covers 3,000 steps
        // Active steps above baseline: 7,000 steps
        // Burn: 7000 * 0.00045 * 80 = 252 kcal
        val stepsResult = Calculations.calculateNetStepCalories(
            totalSteps = 10000,
            weightKg = 80f,
            alreadyLoggedWalkingMinutes = 0
        )
        assertEquals(7000, stepsResult.activeStepsAboveBaseline)
        assertEquals(252, stepsResult.estimatedCalories)
        assertEquals(0, stepsResult.deductedWalkingWorkoutSteps)

        // Same person with a 40 min walking workout already logged (~4,000 steps)
        // Steps remaining before baseline: 10,000 - 4,000 = 6,000 steps
        // Above 3,000 baseline: 3,000 steps
        // Burn: 3000 * 0.00045 * 80 = 108 kcal (preventing double counting!)
        val preventedResult = Calculations.calculateNetStepCalories(
            totalSteps = 10000,
            weightKg = 80f,
            alreadyLoggedWalkingMinutes = 40
        )
        assertEquals(4000, preventedResult.deductedWalkingWorkoutSteps)
        assertEquals(3000, preventedResult.activeStepsAboveBaseline)
        assertEquals(108, preventedResult.estimatedCalories)
    }

    // --- DYNAMIC DAILY EXPENDITURE ENGINE TEST ---

    @Test
    fun testDynamicDailyExpenditureEngine() {
        // Male, 30y, 180cm, 80kg (BMR = 1780)
        // Baseline = 1780 * 1.20 = 2136 kcal
        // Logged workout = 320 kcal
        // Logged steps (10k steps) = 252 kcal
        // Total Estimated Expenditure = 2136 + 320 + 252 = 2708 kcal
        val result = Calculations.evaluateDynamicDailyExpenditure(
            age = 30,
            gender = "Male",
            heightCm = 180f,
            currentWeightKg = 80f,
            goalType = "Lose",
            deficitLevel = DeficitLevel.MODERATE, // 15% deficit on 2708 = 406 kcal
            loggedWorkoutCalories = 320,
            totalSteps = 10000,
            consumedCalories = 2000
        )

        assertEquals(1780, result.bmr)
        assertEquals(2136, result.baselineExpenditure)
        assertEquals(320, result.loggedWorkoutCalories)
        assertEquals(252, result.loggedStepCalories)
        assertEquals(2708, result.totalEstimatedExpenditure)
        // Target: 2708 - 406 = 2302 kcal
        assertEquals(2302, result.dynamicCalorieTarget)
        assertFalse(result.isClampedToFloor)
        // Remaining: 2302 - 2000 = 302 kcal
        assertEquals(302, result.remainingCalories)
    }

    // --- OBSERVED REAL-WORLD MAINTENANCE TEST ---

    @Test
    fun testObservedRealWorldMaintenance() {
        // Build 14 days of weight and food entries
        val weightLogs = (1..14).map { day ->
            val dateStr = "2026-03-%02d".format(day)
            // Weight decreases from 80.0 to 79.0 kg over 14 days (lost 1 kg in 14 days = -0.5 kg/week)
            val weight = 80.0f - ((day - 1) * (1.0f / 13f))
            WeightLog(id = day, weightKg = weight, date = dateStr)
        }

        val foodEntries = (1..14).map { day ->
            val dateStr = "2026-03-%02d".format(day)
            // Consistently eating 2000 kcal/day
            com.example.fitpal.data.local.entity.FoodEntry(
                id = day,
                foodName = "Meals",
                mealType = "Lunch",
                caloriesConsumed = 2000.0,
                proteinGrams = 140.0,
                carbsGrams = 200.0,
                fatGrams = 60.0,
                gramsConsumed = 400.0,
                date = dateStr
            )
        }

        val observed = Calculations.calculateObservedMaintenance(weightLogs, foodEntries)
        assertTrue(observed.isReady)
        assertEquals(14, observed.daysLogged)
        assertNotNull(observed.observedMaintenanceKcal)
        // Since losing 1 kg in 14 days (~0.5 kg/week = ~550 kcal/day deficit)
        // Expected maintenance is ~2000 + 550 = ~2550 kcal
        assertTrue(observed.observedMaintenanceKcal!! in 2450..2650)
    }
}
