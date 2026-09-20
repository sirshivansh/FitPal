package com.example.fitpal.data.repository

import com.example.fitpal.data.local.dao.ExerciseDao
import com.example.fitpal.data.local.dao.WorkoutDao
import com.example.fitpal.data.local.entity.ExerciseEntry
import com.example.fitpal.data.local.entity.WorkoutEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class ExerciseRepository(
    private val exerciseDao: ExerciseDao,
    private val workoutDao: WorkoutDao? = null
) {
    // Legacy ExerciseEntry support
    fun getExerciseEntries(date: String): Flow<List<ExerciseEntry>> = exerciseDao.getExerciseEntries(date)
    suspend fun getExerciseEntriesSync(date: String): List<ExerciseEntry> = exerciseDao.getExerciseEntriesSync(date)
    suspend fun insert(exercise: ExerciseEntry) = exerciseDao.insertExercise(exercise)
    suspend fun delete(exercise: ExerciseEntry) = exerciseDao.deleteExercise(exercise)
    suspend fun clearAll() {
        exerciseDao.clearAll()
        workoutDao?.clearAll()
    }

    // Rich WorkoutEntry support
    fun getWorkoutsForDate(date: String): Flow<List<WorkoutEntry>> =
        workoutDao?.getWorkoutsForDate(date) ?: flowOf(emptyList())

    fun getAllWorkouts(): Flow<List<WorkoutEntry>> =
        workoutDao?.getAllWorkouts() ?: flowOf(emptyList())

    fun getWorkoutsBetweenDates(startDate: String, endDate: String): Flow<List<WorkoutEntry>> =
        workoutDao?.getWorkoutsBetweenDates(startDate, endDate) ?: flowOf(emptyList())

    fun getWorkoutById(id: Int): Flow<WorkoutEntry?> =
        workoutDao?.getWorkoutById(id) ?: flowOf(null)

    suspend fun insertWorkout(workout: WorkoutEntry): Long {
        val id = workoutDao?.insertWorkout(workout) ?: 0L
        // Also keep ExerciseEntry in sync so legacy components and habits receive the workout
        try {
            exerciseDao.insertExercise(
                ExerciseEntry(
                    id = 0,
                    exerciseName = workout.title.ifBlank { workout.workoutType },
                    durationMinutes = workout.durationMinutes.toDouble(),
                    intensity = workout.intensity,
                    caloriesBurned = workout.estimatedCalories.toDouble(),
                    date = workout.date
                )
            )
        } catch (e: Exception) {
            // Non-blocking sync
        }
        return id
    }

    suspend fun updateWorkout(workout: WorkoutEntry) {
        workoutDao?.updateWorkout(workout)
    }

    suspend fun deleteWorkout(workout: WorkoutEntry) {
        workoutDao?.deleteWorkout(workout)
    }

    suspend fun deleteWorkoutById(id: Int) {
        workoutDao?.deleteWorkoutById(id)
    }
}

