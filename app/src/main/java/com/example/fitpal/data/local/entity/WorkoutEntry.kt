package com.example.fitpal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

data class WorkoutExercise(
    val name: String,
    val sets: Int? = null,
    val reps: Int? = null,
    val weightKg: Float? = null,
    val notes: String? = null
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("name", name)
        sets?.let { obj.put("sets", it) }
        reps?.let { obj.put("reps", it) }
        weightKg?.let { obj.put("weightKg", it.toDouble()) }
        notes?.let { obj.put("notes", it) }
        return obj
    }

    companion object {
        fun fromJson(obj: JSONObject): WorkoutExercise {
            return WorkoutExercise(
                name = obj.optString("name", ""),
                sets = if (obj.has("sets")) obj.getInt("sets") else null,
                reps = if (obj.has("reps")) obj.getInt("reps") else null,
                weightKg = if (obj.has("weightKg")) obj.getDouble("weightKg").toFloat() else null,
                notes = if (obj.has("notes")) obj.getString("notes") else null
            )
        }
    }
}

@Entity(tableName = "workout_entries")
data class WorkoutEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // "yyyy-MM-dd"
    val time: String? = null, // "HH:mm"
    val title: String, // e.g. "Chest Day", "Morning Jog"
    val workoutType: String = "Strength Training", // "Strength Training", "Running", "Walking", "Cycling", "Swimming", "HIIT", "Sports", "Yoga", "Cardio", "Other"
    val durationMinutes: Int = 30,
    val intensity: String = "Moderate", // "Light", "Moderate", "Vigorous"
    val estimatedCalories: Int = 0,
    val userWeightKgAtTime: Float = 70f, // Preserved historical weight at the time of logging (Requirement 15)
    val confidence: String = "Medium", // "Low", "Medium", "High"
    val source: String = "AI", // "AI", "Manual"
    val distanceKm: Float? = null,
    val exercisesJson: String = "[]",
    val notes: String = ""
) {
    fun getExercises(): List<WorkoutExercise> {
        val list = mutableListOf<WorkoutExercise>()
        try {
            val array = JSONArray(exercisesJson)
            for (i in 0 until array.length()) {
                list.add(WorkoutExercise.fromJson(array.getJSONObject(i)))
            }
        } catch (e: Exception) {
            // Return empty on parse error
        }
        return list
    }

    companion object {
        fun createWithExercises(
            date: String,
            title: String,
            workoutType: String,
            durationMinutes: Int,
            intensity: String,
            estimatedCalories: Int,
            userWeightKgAtTime: Float,
            confidence: String = "Medium",
            source: String = "AI",
            distanceKm: Float? = null,
            exercises: List<WorkoutExercise> = emptyList(),
            notes: String = "",
            time: String? = null
        ): WorkoutEntry {
            val jsonArray = JSONArray()
            exercises.forEach { jsonArray.put(it.toJson()) }
            return WorkoutEntry(
                date = date,
                time = time,
                title = title,
                workoutType = workoutType,
                durationMinutes = durationMinutes,
                intensity = intensity,
                estimatedCalories = estimatedCalories,
                userWeightKgAtTime = userWeightKgAtTime,
                confidence = confidence,
                source = source,
                distanceKm = distanceKm,
                exercisesJson = jsonArray.toString(),
                notes = notes
            )
        }
    }
}
