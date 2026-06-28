package com.example.util

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import org.json.JSONArray
import org.json.JSONObject

object BackupRestoreHelper {

    private fun calculateHash(obj: JSONObject): String {
        val keys = mutableListOf<String>()
        val it = obj.keys()
        while (it.hasNext()) {
            keys.add(it.next())
        }
        keys.sort()
        val sb = StringBuilder()
        for (k in keys) {
            sb.append(k).append(":").append(obj.get(k).toString()).append("|")
        }
        val salt = "FitPalSecuritySalt_91823_TamperProof"
        val bytes = java.security.MessageDigest.getInstance("SHA-256").digest((sb.toString() + salt).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun exportDatabaseToJson(context: Context): String {
        val db = AppDatabase.getDatabase(context)
        
        val root = JSONObject()
        
        // 1. Profile
        val profile = db.userProfileDao().getUserProfileSync()
        if (profile != null) {
            val pJson = JSONObject().apply {
                put("name", profile.name)
                put("age", profile.age)
                put("gender", profile.gender)
                put("heightCm", profile.heightCm.toDouble())
                put("currentWeightKg", profile.currentWeightKg.toDouble())
                put("goalWeightKg", profile.goalWeightKg.toDouble())
                put("bmr", profile.bmr.toDouble())
                put("dailyCalorieGoal", profile.dailyCalorieGoal)
                put("proteinGoalGrams", profile.proteinGoalGrams)
                put("carbsGoalGrams", profile.carbsGoalGrams)
                put("fatGoalGrams", profile.fatGoalGrams)
                put("weightGoalType", profile.weightGoalType)
                put("calorieAdjustment", profile.calorieAdjustment)
                put("proteinMultiplier", profile.proteinMultiplier.toDouble())
                put("fatMultiplier", profile.fatMultiplier.toDouble())
                put("maintenanceCalories", profile.maintenanceCalories ?: -1)
            }
            root.put("profile", pJson)
        }
        
        // 2. Food Entries
        val foodEntries = db.foodDao().getAllFoodEntriesSync()
        val foodEntriesArray = JSONArray()
        foodEntries.forEach { f ->
            val item = JSONObject().apply {
                put("date", f.date)
                put("mealType", f.mealType)
                put("foodName", f.foodName)
                put("gramsConsumed", f.gramsConsumed.toDouble())
                put("caloriesConsumed", f.caloriesConsumed.toDouble())
                put("proteinGrams", f.proteinGrams.toDouble())
                put("carbsGrams", f.carbsGrams.toDouble())
                put("fatGrams", f.fatGrams.toDouble())
                put("isCustomFood", f.isCustomFood)
                put("timestamp", f.timestamp)
            }
            foodEntriesArray.put(item)
        }
        root.put("foodEntries", foodEntriesArray)

        // 3. Custom Foods
        val customFoods = db.foodDao().getAllCustomFoodsSync()
        val customFoodsArray = JSONArray()
        customFoods.forEach { f ->
            val item = JSONObject().apply {
                put("name", f.name)
                put("caloriesPer100g", f.caloriesPer100g.toDouble())
                put("proteinPer100g", f.proteinPer100g.toDouble())
                put("carbsPer100g", f.carbsPer100g.toDouble())
                put("fatPer100g", f.fatPer100g.toDouble())
                put("defaultServingGrams", f.defaultServingGrams.toDouble())
            }
            customFoodsArray.put(item)
        }
        root.put("customFoods", customFoodsArray)

        // 4. Exercise Entries
        val exerciseEntries = db.exerciseDao().getAllExerciseEntriesSync()
        val exerciseEntriesArray = JSONArray()
        exerciseEntries.forEach { e ->
            val item = JSONObject().apply {
                put("date", e.date)
                put("exerciseName", e.exerciseName)
                put("category", e.category)
                put("durationMinutes", e.durationMinutes)
                put("intensity", e.intensity)
                put("metValue", e.metValue.toDouble())
                put("caloriesBurned", e.caloriesBurned.toDouble())
                put("isCustomExercise", e.isCustomExercise)
                put("timestamp", e.timestamp)
            }
            exerciseEntriesArray.put(item)
        }
        root.put("exerciseEntries", exerciseEntriesArray)

        // 5. Custom Exercises
        val customExercises = db.exerciseDao().getAllCustomExercisesSync()
        val customExercisesArray = JSONArray()
        customExercises.forEach { e ->
            val item = JSONObject().apply {
                put("name", e.name)
                put("category", e.category)
                put("metValue", e.metValue.toDouble())
            }
            customExercisesArray.put(item)
        }
        root.put("customExercises", customExercisesArray)

        // 6. Weight Logs
        val weightLogs = db.weightLogDao().getAllWeightLogsSync()
        val weightLogsArray = JSONArray()
        weightLogs.forEach { w ->
            val item = JSONObject().apply {
                put("date", w.date)
                put("weightKg", w.weightKg.toDouble())
            }
            weightLogsArray.put(item)
        }
        root.put("weightLogs", weightLogsArray)

        // 7. Water Logs
        val waterLogs = db.waterLogDao().getAllWaterLogsSync()
        val waterLogsArray = JSONArray()
        waterLogs.forEach { w ->
            val item = JSONObject().apply {
                put("date", w.date)
                put("glassesCount", w.glassesCount)
            }
            waterLogsArray.put(item)
        }
        root.put("waterLogs", waterLogsArray)

        // 8. Daily Activity Calories
        val activityLogs = db.dailyActivityCalorieDao().getAllActivityCalorieLogsSync()
        val activityLogsArray = JSONArray()
        activityLogs.forEach { a ->
            val item = JSONObject().apply {
                put("date", a.date)
                put("additionalCalories", a.additionalCalories)
            }
            activityLogsArray.put(item)
        }
        root.put("activityLogs", activityLogsArray)

        // Calculate and embed integrity hash to prevent tampering
        val hash = calculateHash(root)
        root.put("integrityHash", hash)

        return root.toString(2)
    }

    suspend fun importDatabaseFromJson(context: Context, jsonStr: String): Boolean {
        return try {
            val db = AppDatabase.getDatabase(context)
            val root = JSONObject(jsonStr)

            // Validate integrity hash to protect database from tampering
            if (!root.has("integrityHash")) {
                android.util.Log.e("BackupRestoreHelper", "Import rejected: No integrity hash present (possible tampering).")
                return false
            }
            val providedHash = root.getString("integrityHash")
            root.remove("integrityHash")

            val expectedHash = calculateHash(root)
            if (providedHash != expectedHash) {
                android.util.Log.e("BackupRestoreHelper", "Import rejected: Integrity hash mismatch (content tampered).")
                return false
            }

            // Clear existing database
            db.userProfileDao().clearProfile()
            db.foodDao().clearFoodEntries()
            db.foodDao().clearCustomFoods()
            db.exerciseDao().clearExerciseEntries()
            db.exerciseDao().clearCustomExercises()
            db.weightLogDao().clearWeightLogs()
            db.waterLogDao().clearWaterLogs()
            db.dailyActivityCalorieDao().clearActivityCalorieLogs()

            // 1. Profile
            if (root.has("profile")) {
                val p = root.getJSONObject("profile")
                val mCal = p.optInt("maintenanceCalories", -1)
                val profile = UserProfile(
                    name = p.getString("name"),
                    age = p.getInt("age"),
                    gender = p.getString("gender"),
                    heightCm = p.getDouble("heightCm").toFloat(),
                    currentWeightKg = p.getDouble("currentWeightKg").toFloat(),
                    goalWeightKg = p.getDouble("goalWeightKg").toFloat(),
                    bmr = p.getDouble("bmr").toFloat(),
                    dailyCalorieGoal = p.getInt("dailyCalorieGoal"),
                    proteinGoalGrams = p.getInt("proteinGoalGrams"),
                    carbsGoalGrams = p.getInt("carbsGoalGrams"),
                    fatGoalGrams = p.getInt("fatGoalGrams"),
                    weightGoalType = p.getString("weightGoalType"),
                    calorieAdjustment = p.optInt("calorieAdjustment", 0),
                    proteinMultiplier = p.optDouble("proteinMultiplier", 1.6).toFloat(),
                    fatMultiplier = p.optDouble("fatMultiplier", 0.8).toFloat(),
                    maintenanceCalories = if (mCal == -1) null else mCal
                )
                db.userProfileDao().insertProfile(profile)
            }

            // 2. Food Entries
            if (root.has("foodEntries")) {
                val foodEntries = mutableListOf<FoodEntry>()
                val arr = root.getJSONArray("foodEntries")
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    foodEntries.add(
                        FoodEntry(
                            date = item.getString("date"),
                            mealType = item.getString("mealType"),
                            foodName = item.getString("foodName"),
                            gramsConsumed = item.getDouble("gramsConsumed").toFloat(),
                            caloriesConsumed = item.getDouble("caloriesConsumed").toFloat(),
                            proteinGrams = item.getDouble("proteinGrams").toFloat(),
                            carbsGrams = item.getDouble("carbsGrams").toFloat(),
                            fatGrams = item.getDouble("fatGrams").toFloat(),
                            isCustomFood = item.optBoolean("isCustomFood", false),
                            timestamp = item.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
                db.foodDao().insertFoodEntries(foodEntries)
            }

            // 3. Custom Foods
            if (root.has("customFoods")) {
                val customFoods = mutableListOf<CustomFood>()
                val arr = root.getJSONArray("customFoods")
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    customFoods.add(
                        CustomFood(
                            name = item.getString("name"),
                            caloriesPer100g = item.getDouble("caloriesPer100g").toFloat(),
                            proteinPer100g = item.getDouble("proteinPer100g").toFloat(),
                            carbsPer100g = item.getDouble("carbsPer100g").toFloat(),
                            fatPer100g = item.getDouble("fatPer100g").toFloat(),
                            defaultServingGrams = item.getDouble("defaultServingGrams").toFloat()
                        )
                    )
                }
                db.foodDao().insertCustomFoods(customFoods)
            }

            // 4. Exercise Entries
            if (root.has("exerciseEntries")) {
                val exerciseEntries = mutableListOf<ExerciseEntry>()
                val arr = root.getJSONArray("exerciseEntries")
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    exerciseEntries.add(
                        ExerciseEntry(
                            date = item.getString("date"),
                            exerciseName = item.getString("exerciseName"),
                            category = item.getString("category"),
                            durationMinutes = item.getInt("durationMinutes"),
                            intensity = item.getString("intensity"),
                            metValue = item.getDouble("metValue").toFloat(),
                            caloriesBurned = item.getDouble("caloriesBurned").toFloat(),
                            isCustomExercise = item.optBoolean("isCustomExercise", false),
                            timestamp = item.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
                db.exerciseDao().insertExerciseEntries(exerciseEntries)
            }

            // 5. Custom Exercises
            if (root.has("customExercises")) {
                val customExercises = mutableListOf<CustomExercise>()
                val arr = root.getJSONArray("customExercises")
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    customExercises.add(
                        CustomExercise(
                            name = item.getString("name"),
                            category = item.getString("category"),
                            metValue = item.getDouble("metValue").toFloat()
                        )
                    )
                }
                db.exerciseDao().insertCustomExercises(customExercises)
            }

            // 6. Weight Logs
            if (root.has("weightLogs")) {
                val weightLogs = mutableListOf<WeightLog>()
                val arr = root.getJSONArray("weightLogs")
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    weightLogs.add(
                        WeightLog(
                            date = item.getString("date"),
                            weightKg = item.getDouble("weightKg").toFloat()
                        )
                    )
                }
                db.weightLogDao().insertWeightLogs(weightLogs)
            }

            // 7. Water Logs
            if (root.has("waterLogs")) {
                val waterLogs = mutableListOf<WaterLog>()
                val arr = root.getJSONArray("waterLogs")
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    waterLogs.add(
                        WaterLog(
                            date = item.getString("date"),
                            glassesCount = item.getInt("glassesCount")
                        )
                    )
                }
                db.waterLogDao().insertWaterLogs(waterLogs)
            }

            // 8. Daily Activity Calories
            if (root.has("activityLogs")) {
                val activityLogs = mutableListOf<DailyActivityCalorieLog>()
                val arr = root.getJSONArray("activityLogs")
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    activityLogs.add(
                        DailyActivityCalorieLog(
                            date = item.getString("date"),
                            additionalCalories = item.getInt("additionalCalories")
                        )
                    )
                }
                db.dailyActivityCalorieDao().insertActivityCalorieLogs(activityLogs)
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
