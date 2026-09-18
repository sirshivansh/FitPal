package com.example.fitpal.data.local.dao

import androidx.room.*
import com.example.fitpal.data.local.entity.WeightLog
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {
    @Query("SELECT * FROM weight_logs ORDER BY date DESC")
    fun getAllWeights(): Flow<List<WeightLog>>

    @Query("SELECT * FROM weight_logs ORDER BY date DESC")
    suspend fun getAllWeightsSync(): List<WeightLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeight(weight: WeightLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(weights: List<WeightLog>)

    @Delete
    suspend fun deleteWeight(weight: WeightLog)

    @Query("DELETE FROM weight_logs WHERE id = :id")
    suspend fun deleteWeightById(id: Int)

    @Query("DELETE FROM weight_logs")
    suspend fun clearAll()
}
