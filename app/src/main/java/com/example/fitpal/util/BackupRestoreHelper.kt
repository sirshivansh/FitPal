package com.example.fitpal.util

import android.content.Context
import com.example.fitpal.data.local.AppDatabase
import com.example.fitpal.data.local.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object BackupRestoreHelper {

    suspend fun exportDatabaseToJson(context: Context): String = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val root = JSONObject()
        root.put("version", 2)
        root.put("timestamp", System.currentTimeMillis())

        // 1. Profile
        val profile = db.profileDao().getUserProfileSync()
        if (profile != null) {
            val pObj = JSONObject().apply {
                put("name", profile.name)
                put("age", profile.age)
                put("gender", profile.gender)
                put("heightCm", profile.heightCm.toDouble())
                put("currentWeightKg", profile.currentWeightKg.toDouble())
                put("goalWeightKg", profile.goalWeightKg.toDouble())
                put("weightGoalType", profile.weightGoalType)
                put("calorieAdjustment", profile.calorieAdjustment)
                put("proteinMultiplier", profile.proteinMultiplier.toDouble())
                put("fatMultiplier", profile.fatMultiplier.toDouble())
                put("maintenanceCalories", profile.maintenanceCalories ?: 0)
                put("bmr", profile.bmr.toDouble())
                put("activityLevel", profile.activityLevel)
                profile.bodyFat?.let { put("bodyFat", it.toDouble()) }
                profile.customCalorieGoal?.let { put("customCalorieGoal", it) }
            }
            root.put("profile", pObj)
        }

        // 2. Food Entries
        val foods = db.foodDao().getAllFoodEntriesSync()
        val foodArray = JSONArray()
        for (f in foods) {
            val fObj = JSONObject().apply {
                put("id", f.id)
                put("foodName", f.foodName)
                put("mealType", f.mealType)
                put("caloriesConsumed", f.caloriesConsumed)
                put("proteinGrams", f.proteinGrams)
                put("carbsGrams", f.carbsGrams)
                put("fatGrams", f.fatGrams)
                put("gramsConsumed", f.gramsConsumed)
                put("date", f.date)
            }
            foodArray.put(fObj)
        }
        root.put("foodEntries", foodArray)

        // 3. Workout Entries
        val workouts = db.workoutDao().getAllWorkoutsSync()
        val workoutArray = JSONArray()
        for (w in workouts) {
            val wObj = JSONObject().apply {
                put("id", w.id)
                put("date", w.date)
                w.time?.let { put("time", it) }
                put("title", w.title)
                put("workoutType", w.workoutType)
                put("durationMinutes", w.durationMinutes)
                put("intensity", w.intensity)
                put("estimatedCalories", w.estimatedCalories)
                put("userWeightKgAtTime", w.userWeightKgAtTime.toDouble())
                put("confidence", w.confidence)
                put("source", w.source)
                w.distanceKm?.let { put("distanceKm", it.toDouble()) }
                put("exercisesJson", w.exercisesJson)
                put("notes", w.notes)
            }
            workoutArray.put(wObj)
        }
        root.put("workoutEntries", workoutArray)

        // 4. Exercise Entries
        val exercises = db.exerciseDao().getAllExercisesSync()
        val exerciseArray = JSONArray()
        for (e in exercises) {
            val eObj = JSONObject().apply {
                put("id", e.id)
                put("exerciseName", e.exerciseName)
                put("durationMinutes", e.durationMinutes)
                put("intensity", e.intensity)
                put("caloriesBurned", e.caloriesBurned)
                put("date", e.date)
            }
            exerciseArray.put(eObj)
        }
        root.put("exerciseEntries", exerciseArray)

        // 5. Weight Logs
        val weights = db.weightDao().getAllWeightsSync()
        val weightArray = JSONArray()
        for (wt in weights) {
            val wtObj = JSONObject().apply {
                put("id", wt.id)
                put("weightKg", wt.weightKg.toDouble())
                put("date", wt.date)
            }
            weightArray.put(wtObj)
        }
        root.put("weightLogs", weightArray)

        // 6. Water Logs
        val waters = db.waterDao().getAllWaterLogsSync()
        val waterArray = JSONArray()
        for (wat in waters) {
            val watObj = JSONObject().apply {
                put("id", wat.id)
                put("glassesCount", wat.glassesCount)
                put("milliliters", wat.milliliters)
                put("date", wat.date)
            }
            waterArray.put(watObj)
        }
        root.put("waterLogs", waterArray)

        // 7. Activity Logs
        val activities = db.activityCalorieDao().getAllActivityLogsSync()
        val actArray = JSONArray()
        for (act in activities) {
            val actObj = JSONObject().apply {
                put("id", act.id)
                put("additionalCalories", act.additionalCalories)
                put("steps", act.steps)
                put("date", act.date)
            }
            actArray.put(actObj)
        }
        root.put("activityLogs", actArray)

        root.toString(2)
    }

    suspend fun importDatabaseFromJson(context: Context, jsonStr: String): Boolean = withContext(Dispatchers.IO) {
        if (jsonStr.isBlank()) return@withContext false
        val db = AppDatabase.getDatabase(context)
        try {
            val root = JSONObject(jsonStr)

            // 1. Profile
            if (root.has("profile")) {
                val p = root.getJSONObject("profile")
                val profile = UserProfile(
                    name = p.optString("name", "FitPal User"),
                    age = p.optInt("age", 25),
                    gender = p.optString("gender", "Male"),
                    heightCm = p.optDouble("heightCm", 175.0).toFloat(),
                    currentWeightKg = p.optDouble("currentWeightKg", 70.0).toFloat(),
                    goalWeightKg = p.optDouble("goalWeightKg", 70.0).toFloat(),
                    weightGoalType = p.optString("weightGoalType", "Maintain"),
                    calorieAdjustment = p.optInt("calorieAdjustment", 0),
                    proteinMultiplier = p.optDouble("proteinMultiplier", 1.6).toFloat(),
                    fatMultiplier = p.optDouble("fatMultiplier", 0.8).toFloat(),
                    maintenanceCalories = if (p.has("maintenanceCalories") && p.getInt("maintenanceCalories") > 0) p.getInt("maintenanceCalories") else null,
                    bmr = p.optDouble("bmr", 1700.0).toFloat(),
                    activityLevel = p.optString("activityLevel", "Moderate"),
                    bodyFat = if (p.has("bodyFat")) p.getDouble("bodyFat").toFloat() else null,
                    customCalorieGoal = if (p.has("customCalorieGoal")) p.getInt("customCalorieGoal") else null
                )
                db.profileDao().insertProfile(profile)
            }

            // 2. Foods
            if (root.has("foodEntries")) {
                val array = root.getJSONArray("foodEntries")
                val foods = mutableListOf<FoodEntry>()
                for (i in 0 until array.length()) {
                    val f = array.getJSONObject(i)
                    foods.add(
                        FoodEntry(
                            id = f.optInt("id", 0),
                            foodName = f.optString("foodName", ""),
                            mealType = f.optString("mealType", "Snacks"),
                            caloriesConsumed = f.optDouble("caloriesConsumed", 0.0),
                            proteinGrams = f.optDouble("proteinGrams", 0.0),
                            carbsGrams = f.optDouble("carbsGrams", 0.0),
                            fatGrams = f.optDouble("fatGrams", 0.0),
                            gramsConsumed = f.optDouble("gramsConsumed", 100.0),
                            date = f.optString("date", "")
                        )
                    )
                }
                if (foods.isNotEmpty()) {
                    db.foodDao().insertAll(foods)
                }
            }

            // 3. Workouts
            if (root.has("workoutEntries")) {
                val array = root.getJSONArray("workoutEntries")
                val workouts = mutableListOf<WorkoutEntry>()
                for (i in 0 until array.length()) {
                    val w = array.getJSONObject(i)
                    workouts.add(
                        WorkoutEntry(
                            id = w.optInt("id", 0),
                            date = w.optString("date", ""),
                            time = if (w.has("time")) w.getString("time") else null,
                            title = w.optString("title", "Workout"),
                            workoutType = w.optString("workoutType", "Strength Training"),
                            durationMinutes = w.optInt("durationMinutes", 30),
                            intensity = w.optString("intensity", "Moderate"),
                            estimatedCalories = w.optInt("estimatedCalories", 0),
                            userWeightKgAtTime = w.optDouble("userWeightKgAtTime", 70.0).toFloat(),
                            confidence = w.optString("confidence", "Medium"),
                            source = w.optString("source", "Manual"),
                            distanceKm = if (w.has("distanceKm")) w.getDouble("distanceKm").toFloat() else null,
                            exercisesJson = w.optString("exercisesJson", "[]"),
                            notes = w.optString("notes", "")
                        )
                    )
                }
                if (workouts.isNotEmpty()) {
                    db.workoutDao().insertAll(workouts)
                }
            }

            // 4. Exercise Entries
            if (root.has("exerciseEntries")) {
                val array = root.getJSONArray("exerciseEntries")
                val exercises = mutableListOf<ExerciseEntry>()
                for (i in 0 until array.length()) {
                    val e = array.getJSONObject(i)
                    exercises.add(
                        ExerciseEntry(
                            id = e.optInt("id", 0),
                            exerciseName = e.optString("exerciseName", ""),
                            durationMinutes = e.optDouble("durationMinutes", 0.0),
                            intensity = e.optString("intensity", "Moderate"),
                            caloriesBurned = e.optDouble("caloriesBurned", 0.0),
                            date = e.optString("date", "")
                        )
                    )
                }
                if (exercises.isNotEmpty()) {
                    db.exerciseDao().insertAll(exercises)
                }
            }

            // 5. Weight Logs
            if (root.has("weightLogs")) {
                val array = root.getJSONArray("weightLogs")
                val weights = mutableListOf<WeightLog>()
                for (i in 0 until array.length()) {
                    val wt = array.getJSONObject(i)
                    weights.add(
                        WeightLog(
                            id = wt.optInt("id", 0),
                            weightKg = wt.optDouble("weightKg", 70.0).toFloat(),
                            date = wt.optString("date", "")
                        )
                    )
                }
                if (weights.isNotEmpty()) {
                    db.weightDao().insertAll(weights)
                }
            }

            // 6. Water Logs
            if (root.has("waterLogs")) {
                val array = root.getJSONArray("waterLogs")
                val waters = mutableListOf<WaterLog>()
                for (i in 0 until array.length()) {
                    val wat = array.getJSONObject(i)
                    waters.add(
                        WaterLog(
                            id = wat.optInt("id", 0),
                            glassesCount = wat.optInt("glassesCount", 0),
                            milliliters = wat.optInt("milliliters", 0),
                            date = wat.optString("date", "")
                        )
                    )
                }
                if (waters.isNotEmpty()) {
                    db.waterDao().insertAll(waters)
                }
            }

            // 7. Activity Logs
            if (root.has("activityLogs")) {
                val array = root.getJSONArray("activityLogs")
                val acts = mutableListOf<ActivityCalorieLog>()
                for (i in 0 until array.length()) {
                    val act = array.getJSONObject(i)
                    acts.add(
                        ActivityCalorieLog(
                            id = act.optInt("id", 0),
                            additionalCalories = act.optInt("additionalCalories", 0),
                            steps = act.optInt("steps", 0),
                            date = act.optString("date", "")
                        )
                    )
                }
                if (acts.isNotEmpty()) {
                    db.activityCalorieDao().insertAll(acts)
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
