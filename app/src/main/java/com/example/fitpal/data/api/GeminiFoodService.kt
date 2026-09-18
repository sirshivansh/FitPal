package com.example.fitpal.data.api

import android.util.LruCache
import com.example.fitpal.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class FoodSearchResult(
    val foodName: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val defaultGrams: Double = 100.0
)

data class ParsedNotepadFood(
    val foodName: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val gramsConsumed: Double
)

object GeminiFoodService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private const val PRIMARY_MODEL = "gemini-3.8-flash"
    private const val FALLBACK_MODEL = "gemini-3.5-flash"

    // Local in-memory LRU cache to prevent repetitive nutritional lookups & save tokens/quota
    private val searchCache = object : LruCache<String, List<FoodSearchResult>>(100) {}

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
                val errBody = response.body?.string() ?: ""
                throw Exception("Model $model returned HTTP ${response.code}: $errBody")
            }
            val responseBodyStr = response.body?.string() ?: throw Exception("Empty response body")
            val responseJson = JSONObject(responseBodyStr)
            val candidates = responseJson.optJSONArray("candidates")
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
            // Graceful fallback to gemini-3.5-flash if primary encounters transient issues
            executeGeminiCall(FALLBACK_MODEL, prompt, apiKey)
        }
    }

    suspend fun searchFood(query: String): List<FoodSearchResult> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()

        val cacheKey = trimmed.lowercase()
        synchronized(searchCache) {
            val cached = searchCache.get(cacheKey)
            if (cached != null) return@withContext cached
        }

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            val localResults = listOf(
                FoodSearchResult(foodName = "$trimmed (Standard Serving)", calories = 150.0, protein = 8.0, carbs = 18.0, fat = 5.0, defaultGrams = 100.0),
                FoodSearchResult(foodName = "$trimmed (Lean/Raw)", calories = 110.0, protein = 12.0, carbs = 10.0, fat = 2.0, defaultGrams = 100.0)
            )
            synchronized(searchCache) { searchCache.put(cacheKey, localResults) }
            return@withContext localResults
        }

        val prompt = """
            Return a precise JSON array of 5-8 food items matching: "$trimmed".
            Per 100g schema:
            [{"foodName":"Name","caloriesConsumed":0.0,"proteinGrams":0.0,"carbsGrams":0.0,"fatGrams":0.0,"gramsConsumed":100.0}]
            JSON only.
        """.trimIndent()

        try {
            val cleanedJson = callWithFallback(prompt, apiKey)
            val jsonArray = JSONArray(cleanedJson)
            val results = mutableListOf<FoodSearchResult>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                results.add(
                    FoodSearchResult(
                        foodName = obj.optString("foodName", trimmed),
                        calories = obj.optDouble("caloriesConsumed", 0.0),
                        protein = obj.optDouble("proteinGrams", 0.0),
                        carbs = obj.optDouble("carbsGrams", 0.0),
                        fat = obj.optDouble("fatGrams", 0.0),
                        defaultGrams = obj.optDouble("gramsConsumed", 100.0)
                    )
                )
            }
            synchronized(searchCache) { searchCache.put(cacheKey, results) }
            results
        } catch (_: Exception) {
            emptyList()
        }
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

    suspend fun parseNotepadEntries(text: String): List<ParsedNotepadFood> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext emptyList()
        }

        val prompt = """
            Parse each food item from this text: "$text".
            Return JSON array:
            [{"foodName":"Name","caloriesConsumed":0.0,"proteinGrams":0.0,"carbsGrams":0.0,"fatGrams":0.0,"gramsConsumed":100.0}]
            JSON only.
        """.trimIndent()

        try {
            val cleanedJson = callWithFallback(prompt, apiKey)
            val jsonArray = JSONArray(cleanedJson)
            val results = mutableListOf<ParsedNotepadFood>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                results.add(
                    ParsedNotepadFood(
                        foodName = obj.optString("foodName", "Food Item"),
                        calories = obj.optDouble("caloriesConsumed", 0.0),
                        protein = obj.optDouble("proteinGrams", 0.0),
                        carbs = obj.optDouble("carbsGrams", 0.0),
                        fat = obj.optDouble("fatGrams", 0.0),
                        gramsConsumed = obj.optDouble("gramsConsumed", 100.0)
                    )
                )
            }
            results
        } catch (_: Exception) {
            emptyList()
        }
    }
}
