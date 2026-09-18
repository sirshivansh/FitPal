package com.example.fitpal.data.api

import com.example.fitpal.BuildConfig
import com.example.fitpal.data.local.entity.WorkoutExercise
import com.example.fitpal.util.Calculations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.max

data class ParsedWorkoutItem(
    val title: String,
    val workoutType: String,
    val durationMinutes: Int,
    val intensity: String,
    val distanceKm: Float?,
    val exercises: List<WorkoutExercise>,
    val notes: String,
    val estimatedCalories: Int,
    val confidence: String,
    val calorieFormula: String,
    val calculationExplanation: String,
    val steps: Int? = null
)

data class UnifiedNotebookParseResult(
    val foods: List<ParsedNotepadFood>,
    val workouts: List<ParsedWorkoutItem>
)

object GeminiWorkoutService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private const val PRIMARY_MODEL = "gemini-3.8-flash"
    private const val FALLBACK_MODEL = "gemini-3.5-flash"

    private fun buildRequestJson(prompt: String): JSONObject {
        return JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.2)
                put("maxOutputTokens", 1024)
                put("responseMimeType", "application/json")
                put("thinkingConfig", JSONObject().apply {
                    put("thinkingLevel", "LOW")
                })
            })
        }
    }

    private fun executeGeminiCall(model: String, prompt: String, apiKey: String): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        val jsonRequest = buildRequestJson(prompt)
        val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val err = response.body?.string() ?: ""
                throw Exception("Model $model returned HTTP ${response.code}: $err")
            }
            val body = response.body?.string() ?: throw Exception("Empty response body")
            val json = JSONObject(body)
            val candidates = json.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val rawText = parts?.optJSONObject(0)?.optString("text") ?: ""
            return cleanJson(rawText)
        }
    }

    private fun callWithFallback(prompt: String, apiKey: String): String {
        return try {
            executeGeminiCall(PRIMARY_MODEL, prompt, apiKey)
        } catch (_: Exception) {
            executeGeminiCall(FALLBACK_MODEL, prompt, apiKey)
        }
    }

    suspend fun parseWorkoutText(
        text: String,
        userWeightKg: Float
    ): List<ParsedWorkoutItem> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext fallbackWorkoutParse(text, userWeightKg)
        }

        val prompt = """
            Parse fitness activities/workouts from text: "$text". User weight: ${userWeightKg.toInt()} kg.
            If steps are mentioned (e.g. 4000 steps), set "steps" to that number, "workoutType" to "Walking", and realistic duration.
            Return JSON array of objects:
            - "title": short title (e.g. "Walking (4,000 steps)", "Morning Run")
            - "workoutType": "Strength Training"|"Running"|"Walking"|"Cycling"|"Swimming"|"HIIT"|"Yoga"|"Cardio"
            - "durationMinutes": integer
            - "intensity": "Light"|"Moderate"|"Vigorous"
            - "distanceKm": Float or null
            - "steps": Integer or null
            - "exercises": [{"name":"Name","sets":3,"reps":10,"weightKg":50.0,"notes":null}]
            - "notes": string
            JSON array only.
        """.trimIndent()

        try {
            val cleanedJson = callWithFallback(prompt, apiKey)
            val jsonArray = JSONArray(cleanedJson)
            val results = mutableListOf<ParsedWorkoutItem>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val title = obj.optString("title", "Workout")
                val workoutType = obj.optString("workoutType", "Strength Training")
                val duration = obj.optInt("durationMinutes", 30).coerceIn(1, 360)
                val intensity = obj.optString("intensity", "Moderate")
                val distanceKm = if (obj.has("distanceKm") && !obj.isNull("distanceKm")) obj.getDouble("distanceKm").toFloat() else null
                val steps = if (obj.has("steps") && !obj.isNull("steps")) obj.getInt("steps") else null
                val notes = obj.optString("notes", "")

                val exercisesList = mutableListOf<WorkoutExercise>()
                val exArray = obj.optJSONArray("exercises")
                if (exArray != null) {
                    for (j in 0 until exArray.length()) {
                        val exObj = exArray.getJSONObject(j)
                        exercisesList.add(
                            WorkoutExercise(
                                name = exObj.optString("name", "Exercise"),
                                sets = if (exObj.has("sets") && !exObj.isNull("sets")) exObj.getInt("sets") else null,
                                reps = if (exObj.has("reps") && !exObj.isNull("reps")) exObj.getInt("reps") else null,
                                weightKg = if (exObj.has("weightKg") && !exObj.isNull("weightKg")) exObj.getDouble("weightKg").toFloat() else null,
                                notes = if (exObj.has("notes") && !exObj.isNull("notes")) exObj.getString("notes") else null
                            )
                        )
                    }
                }

                val calEstimate = Calculations.estimateWorkoutCalories(
                    workoutType = workoutType,
                    intensity = intensity,
                    durationMinutes = duration,
                    weightKg = userWeightKg
                )

                // If steps are present, calculate step calories using standard factor
                val finalCalories = if (steps != null && steps > 0) {
                    val stepCal = (steps * Calculations.calculateStepCalorieFactor(userWeightKg)).toInt()
                    max(calEstimate.estimatedCalories, stepCal)
                } else {
                    calEstimate.estimatedCalories
                }

                results.add(
                    ParsedWorkoutItem(
                        title = title,
                        workoutType = workoutType,
                        durationMinutes = duration,
                        intensity = intensity,
                        distanceKm = distanceKm,
                        exercises = exercisesList,
                        notes = notes,
                        estimatedCalories = finalCalories,
                        confidence = calEstimate.confidence,
                        calorieFormula = calEstimate.formulaUsed,
                        calculationExplanation = calEstimate.explanation,
                        steps = steps
                    )
                )
            }
            if (results.isEmpty()) fallbackWorkoutParse(text, userWeightKg) else results
        } catch (_: Exception) {
            fallbackWorkoutParse(text, userWeightKg)
        }
    }

    suspend fun parseUnifiedNotebook(
        text: String,
        userWeightKg: Float
    ): UnifiedNotebookParseResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            val foods = try { GeminiFoodService.parseNotepadEntries(text) } catch (_: Exception) { emptyList() }
            val workouts = fallbackWorkoutParse(text, userWeightKg)
            return@withContext UnifiedNotebookParseResult(foods = foods, workouts = workouts)
        }

        val prompt = """
            Parse both food entries AND workout/physical activities from: "$text". User weight: ${userWeightKg.toInt()} kg.
            Return strictly a JSON object:
            {
              "foods": [{"foodName":"Name","caloriesConsumed":0.0,"proteinGrams":0.0,"carbsGrams":0.0,"fatGrams":0.0,"gramsConsumed":100.0}],
              "workouts": [{"title":"Name","workoutType":"Walking|Running|Strength Training","durationMinutes":30,"intensity":"Moderate","distanceKm":null,"steps":null,"exercises":[],"notes":""}]
            }
            JSON only.
        """.trimIndent()

        try {
            val cleanedJson = callWithFallback(prompt, apiKey)
            val root = JSONObject(cleanedJson)

            val foodsList = mutableListOf<ParsedNotepadFood>()
            val fArray = root.optJSONArray("foods")
            if (fArray != null) {
                for (i in 0 until fArray.length()) {
                    val fObj = fArray.getJSONObject(i)
                    foodsList.add(
                        ParsedNotepadFood(
                            foodName = fObj.optString("foodName", "Food"),
                            calories = fObj.optDouble("caloriesConsumed", 0.0),
                            protein = fObj.optDouble("proteinGrams", 0.0),
                            carbs = fObj.optDouble("carbsGrams", 0.0),
                            fat = fObj.optDouble("fatGrams", 0.0),
                            gramsConsumed = fObj.optDouble("gramsConsumed", 100.0)
                        )
                    )
                }
            }

            val workoutsList = mutableListOf<ParsedWorkoutItem>()
            val wArray = root.optJSONArray("workouts")
            if (wArray != null) {
                for (i in 0 until wArray.length()) {
                    val wObj = wArray.getJSONObject(i)
                    val title = wObj.optString("title", "Workout")
                    val workoutType = wObj.optString("workoutType", "Strength Training")
                    val duration = wObj.optInt("durationMinutes", 30).coerceIn(1, 360)
                    val intensity = wObj.optString("intensity", "Moderate")
                    val distanceKm = if (wObj.has("distanceKm") && !wObj.isNull("distanceKm")) wObj.getDouble("distanceKm").toFloat() else null
                    val steps = if (wObj.has("steps") && !wObj.isNull("steps")) wObj.getInt("steps") else null
                    val notes = wObj.optString("notes", "")

                    val exercisesList = mutableListOf<WorkoutExercise>()
                    val exArray = wObj.optJSONArray("exercises")
                    if (exArray != null) {
                        for (j in 0 until exArray.length()) {
                            val exObj = exArray.getJSONObject(j)
                            exercisesList.add(
                                WorkoutExercise(
                                    name = exObj.optString("name", "Exercise"),
                                    sets = if (exObj.has("sets") && !exObj.isNull("sets")) exObj.getInt("sets") else null,
                                    reps = if (exObj.has("reps") && !exObj.isNull("reps")) exObj.getInt("reps") else null,
                                    weightKg = if (exObj.has("weightKg") && !exObj.isNull("weightKg")) exObj.getDouble("weightKg").toFloat() else null,
                                    notes = if (exObj.has("notes") && !exObj.isNull("notes")) exObj.getString("notes") else null
                                )
                            )
                        }
                    }

                    val calEstimate = Calculations.estimateWorkoutCalories(
                        workoutType = workoutType,
                        intensity = intensity,
                        durationMinutes = duration,
                        weightKg = userWeightKg
                    )

                    val finalCalories = if (steps != null && steps > 0) {
                        val stepCal = (steps * Calculations.calculateStepCalorieFactor(userWeightKg)).toInt()
                        max(calEstimate.estimatedCalories, stepCal)
                    } else {
                        calEstimate.estimatedCalories
                    }

                    workoutsList.add(
                        ParsedWorkoutItem(
                            title = title,
                            workoutType = workoutType,
                            durationMinutes = duration,
                            intensity = intensity,
                            distanceKm = distanceKm,
                            exercises = exercisesList,
                            notes = notes,
                            estimatedCalories = finalCalories,
                            confidence = calEstimate.confidence,
                            calorieFormula = calEstimate.formulaUsed,
                            calculationExplanation = calEstimate.explanation,
                            steps = steps
                        )
                    )
                }
            }

            UnifiedNotebookParseResult(foods = foodsList, workouts = workoutsList)
        } catch (_: Exception) {
            val foods = try { GeminiFoodService.parseNotepadEntries(text) } catch (_: Exception) { emptyList() }
            UnifiedNotebookParseResult(foods = foods, workouts = fallbackWorkoutParse(text, userWeightKg))
        }
    }

    fun fallbackWorkoutParse(text: String, userWeightKg: Float): List<ParsedWorkoutItem> {
        val lower = text.lowercase()
        val stepMatch = Regex("([0-9]+(?:,[0-9]+)?)\\s*steps?", RegexOption.IGNORE_CASE).find(lower)
        val extractedSteps = stepMatch?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull()

        val isWalking = lower.contains("walk") || extractedSteps != null
        val isRunning = lower.contains("run") || lower.contains("jog")

        val workoutType = when {
            isRunning -> "Running"
            isWalking -> "Walking"
            lower.contains("cycl") || lower.contains("bike") -> "Cycling"
            lower.contains("swim") -> "Swimming"
            lower.contains("hiit") -> "HIIT"
            lower.contains("yoga") -> "Yoga"
            else -> "Strength Training"
        }

        val durationMatch = Regex("(\\d+)\\s*(?:min|minute|m\\b)").find(lower)
        val hourMatch = Regex("(\\d+(?:\\.\\d+)?)\\s*(?:hr|hour|h\\b)").find(lower)
        val duration = when {
            durationMatch != null -> durationMatch.groupValues[1].toIntOrNull() ?: 30
            hourMatch != null -> ((hourMatch.groupValues[1].toFloatOrNull() ?: 1.0f) * 60).toInt()
            extractedSteps != null -> (extractedSteps / 100).coerceAtLeast(15)
            else -> 35
        }

        val distanceMatch = Regex("(\\d+(?:\\.\\d+)?)\\s*(?:k|km|kilometer)").find(lower)
        val milesMatch = Regex("(\\d+(?:\\.\\d+)?)\\s*(?:mi|mile)").find(lower)
        val distanceKm = when {
            distanceMatch != null -> distanceMatch.groupValues[1].toFloatOrNull()
            milesMatch != null -> (milesMatch.groupValues[1].toFloatOrNull() ?: 0f) * 1.609f
            extractedSteps != null -> extractedSteps * 0.00075f
            else -> null
        }

        val exercises = mutableListOf<WorkoutExercise>()
        if (workoutType == "Strength Training") {
            text.lines().forEach { line ->
                val trimmed = line.trim().trimStart('-', '*', '•', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.', ' ')
                if (trimmed.length > 3) {
                    val setsRepsMatch = Regex("(\\d+)\\s*[xX*]\\s*(\\d+)").find(trimmed)
                    val weightMatch = Regex("(\\d+(?:\\.\\d+)?)\\s*(?:kg|kilos|lbs|lb|pounds)").find(trimmed)
                    val sets = setsRepsMatch?.groupValues?.get(1)?.toIntOrNull()
                    val reps = setsRepsMatch?.groupValues?.get(2)?.toIntOrNull()
                    val weight = weightMatch?.groupValues?.get(1)?.toFloatOrNull()
                    val exName = trimmed.split(Regex("(?i)\\b(\\d+x|\\d+\\s*sets|@|at|\\d+kg)\\b")).first().trim()

                    exercises.add(
                        WorkoutExercise(
                            name = if (exName.isNotBlank()) exName else trimmed,
                            sets = sets,
                            reps = reps,
                            weightKg = weight
                        )
                    )
                }
            }
        }

        val estimate = Calculations.estimateWorkoutCalories(
            workoutType = workoutType,
            intensity = "Moderate",
            durationMinutes = duration,
            weightKg = userWeightKg
        )

        val finalCalories = if (extractedSteps != null && extractedSteps > 0) {
            val stepCal = (extractedSteps * Calculations.calculateStepCalorieFactor(userWeightKg)).toInt()
            max(estimate.estimatedCalories, stepCal)
        } else {
            estimate.estimatedCalories
        }

        val title = when {
            extractedSteps != null -> "Walking ($extractedSteps steps)"
            workoutType == "Running" && distanceKm != null -> "${distanceKm}km Run"
            else -> "$workoutType Session"
        }

        return listOf(
            ParsedWorkoutItem(
                title = title,
                workoutType = workoutType,
                durationMinutes = duration,
                intensity = "Moderate",
                distanceKm = distanceKm,
                exercises = exercises,
                notes = text.take(120),
                estimatedCalories = finalCalories,
                confidence = estimate.confidence,
                calorieFormula = estimate.formulaUsed,
                calculationExplanation = estimate.explanation,
                steps = extractedSteps
            )
        )
    }

    private fun cleanJson(raw: String): String {
        var cleaned = raw.trim()
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.substringAfter("```")
            if (cleaned.startsWith("json")) {
                cleaned = cleaned.substringAfter("json")
            }
            cleaned = cleaned.substringBeforeLast("```")
        }
        return cleaned.trim()
    }
}
