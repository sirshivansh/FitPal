package com.example.fitpal.data.local.dao

import androidx.room.*
import com.example.fitpal.data.local.entity.ProgressPhoto
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Query("SELECT * FROM progress_photos ORDER BY date DESC")
    fun getAllPhotos(): Flow<List<ProgressPhoto>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: ProgressPhoto)

    @Delete
    suspend fun deletePhoto(photo: ProgressPhoto)

    @Query("DELETE FROM progress_photos")
    suspend fun clearAll()
}
