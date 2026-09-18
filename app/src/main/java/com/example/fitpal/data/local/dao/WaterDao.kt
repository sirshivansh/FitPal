package com.example.fitpal.data.local.dao

import androidx.room.*
import com.example.fitpal.data.local.entity.WaterLog
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterDao {
    @Query("SELECT * FROM water_logs WHERE date = :date LIMIT 1")
    fun getWaterLog(date: String): Flow<WaterLog?>

    @Query("SELECT * FROM water_logs WHERE date = :date LIMIT 1")
    suspend fun getWaterLogSync(date: String): WaterLog?

    @Query("SELECT * FROM water_logs")
    suspend fun getAllWaterLogsSync(): List<WaterLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWater(water: WaterLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(waters: List<WaterLog>)

    @Query("DELETE FROM water_logs")
    suspend fun clearAll()
}
