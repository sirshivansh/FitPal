package com.example.fitpal.data.local.dao

import androidx.room.*
import com.example.fitpal.data.local.entity.ActivityCalorieLog
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityCalorieDao {
    @Query("SELECT * FROM activity_calorie_logs WHERE date = :date LIMIT 1")
    fun getActivityCalorieLog(date: String): Flow<ActivityCalorieLog?>

    @Query("SELECT * FROM activity_calorie_logs WHERE date = :date LIMIT 1")
    suspend fun getActivityCalorieLogSync(date: String): ActivityCalorieLog?

    @Query("SELECT * FROM activity_calorie_logs")
    suspend fun getAllActivityLogsSync(): List<ActivityCalorieLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityCalorie(log: ActivityCalorieLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<ActivityCalorieLog>)

    @Query("DELETE FROM activity_calorie_logs")
    suspend fun clearAll()
}
