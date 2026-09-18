package com.example.fitpal.data.local.dao

import androidx.room.*
import com.example.fitpal.data.local.entity.ExerciseEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercise_entries WHERE date = :date")
    fun getExerciseEntries(date: String): Flow<List<ExerciseEntry>>

    @Query("SELECT * FROM exercise_entries")
    suspend fun getAllExercisesSync(): List<ExerciseEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<ExerciseEntry>)

    @Delete
    suspend fun deleteExercise(exercise: ExerciseEntry)

    @Query("DELETE FROM exercise_entries")
    suspend fun clearAll()
}
