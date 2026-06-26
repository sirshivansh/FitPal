package com.example.data.repository

import com.example.data.local.dao.ExerciseDao
import com.example.data.local.entity.CustomExercise
import com.example.data.local.entity.ExerciseDatabase
import com.example.data.local.entity.ExerciseEntry
import com.example.data.local.entity.PrepopulatedData
import kotlinx.coroutines.flow.Flow

class ExerciseRepository(private val exerciseDao: ExerciseDao) {

    val allExerciseEntries: Flow<List<ExerciseEntry>> = exerciseDao.getAllExerciseEntries()

    fun getExerciseEntriesForDate(date: String): Flow<List<ExerciseEntry>> {
        return exerciseDao.getExerciseEntriesForDate(date)
    }

    suspend fun insertExerciseEntry(entry: ExerciseEntry) {
        exerciseDao.insertExerciseEntry(entry)
    }

    suspend fun updateExerciseEntry(entry: ExerciseEntry) {
        exerciseDao.updateExerciseEntry(entry)
    }

    suspend fun deleteExerciseEntry(entry: ExerciseEntry) {
        exerciseDao.deleteExerciseEntry(entry)
    }

    // Consolidated exercise search
    suspend fun searchExercises(query: String): List<SearchResultExercise> {
        val seeded = exerciseDao.searchExerciseDatabase(query)
        val custom = exerciseDao.searchCustomExercises(query)

        val results = mutableListOf<SearchResultExercise>()

        // Add custom exercises first
        custom.forEach { e ->
            results.add(
                SearchResultExercise(
                    id = e.id,
                    name = e.name,
                    category = e.category,
                    metValueLight = e.metValue,
                    metValueModerate = e.metValue,
                    metValueVigorous = e.metValue,
                    isCustom = true
                )
            )
        }

        // Add seeded exercises
        seeded.forEach { e ->
            results.add(
                SearchResultExercise(
                    id = e.id.toLong(),
                    name = e.name,
                    category = e.category,
                    metValueLight = e.metValueLight,
                    metValueModerate = e.metValueModerate,
                    metValueVigorous = e.metValueVigorous,
                    isCustom = false
                )
            )
        }

        return results
    }

    suspend fun insertCustomExercise(exercise: CustomExercise) {
        exerciseDao.insertCustomExercise(exercise)
    }

    // Seeding
    suspend fun seedExerciseDatabaseIfNeeded() {
        if (exerciseDao.getExerciseDatabaseCount() == 0) {
            val list = PrepopulatedData.getPrepopulatedExercises()
            exerciseDao.insertExerciseDatabaseList(list)
        }
    }

    suspend fun clearAllExerciseData() {
        exerciseDao.clearExerciseEntries()
        exerciseDao.clearCustomExercises()
    }
}

data class SearchResultExercise(
    val id: Long,
    val name: String,
    val category: String,
    val metValueLight: Float,
    val metValueModerate: Float,
    val metValueVigorous: Float,
    val isCustom: Boolean
)
