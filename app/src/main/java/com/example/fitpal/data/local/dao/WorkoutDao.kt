package com.example.fitpal.data.local.dao

import androidx.room.*
import com.example.fitpal.data.local.entity.WorkoutEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workout_entries WHERE date = :date ORDER BY id DESC")
    fun getWorkoutsForDate(date: String): Flow<List<WorkoutEntry>>

    @Query("SELECT * FROM workout_entries ORDER BY date DESC, id DESC")
    fun getAllWorkouts(): Flow<List<WorkoutEntry>>

    @Query("SELECT * FROM workout_entries ORDER BY date DESC, id DESC")
    suspend fun getAllWorkoutsSync(): List<WorkoutEntry>

    @Query("SELECT * FROM workout_entries WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC, id DESC")
    fun getWorkoutsBetweenDates(startDate: String, endDate: String): Flow<List<WorkoutEntry>>

    @Query("SELECT * FROM workout_entries WHERE id = :id LIMIT 1")
    fun getWorkoutById(id: Int): Flow<WorkoutEntry?>

    @Query("SELECT * FROM workout_entries WHERE date = :date")
    suspend fun getWorkoutsForDateSync(date: String): List<WorkoutEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(workouts: List<WorkoutEntry>)

    @Update
    suspend fun updateWorkout(workout: WorkoutEntry)

    @Delete
    suspend fun deleteWorkout(workout: WorkoutEntry)

    @Query("DELETE FROM workout_entries WHERE id = :id")
    suspend fun deleteWorkoutById(id: Int)

    @Query("DELETE FROM workout_entries")
    suspend fun clearAll()
}
