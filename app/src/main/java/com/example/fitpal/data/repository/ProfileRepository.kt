package com.example.fitpal.data.repository

import com.example.fitpal.data.local.dao.*
import com.example.fitpal.data.local.entity.*
import kotlinx.coroutines.flow.Flow

class ProfileRepository(
    private val profileDao: UserProfileDao,
    private val weightDao: WeightDao,
    private val waterDao: WaterDao,
    private val activityDao: ActivityCalorieDao,
    private val photoDao: PhotoDao
) {
    fun getUserProfile(): Flow<UserProfile?> = profileDao.getUserProfile()
    suspend fun getUserProfileSync(): UserProfile? = profileDao.getUserProfileSync()
    suspend fun insertProfile(profile: UserProfile) = profileDao.insertProfile(profile)
    suspend fun updateProfile(profile: UserProfile) = profileDao.updateProfile(profile)
    suspend fun clearProfile() = profileDao.clearProfile()

    fun getAllWeights(): Flow<List<WeightLog>> = weightDao.getAllWeights()
    suspend fun insertWeight(weight: WeightLog) = weightDao.insertWeight(weight)
    suspend fun deleteWeight(weight: WeightLog) = weightDao.deleteWeight(weight)
    suspend fun deleteWeightById(id: Int) = weightDao.deleteWeightById(id)
    suspend fun clearWeights() = weightDao.clearAll()

    fun getWaterLog(date: String): Flow<WaterLog?> = waterDao.getWaterLog(date)
    suspend fun getWaterLogSync(date: String): WaterLog? = waterDao.getWaterLogSync(date)
    suspend fun insertWater(water: WaterLog) = waterDao.insertWater(water)
    suspend fun clearWater() = waterDao.clearAll()

    fun getActivityLog(date: String): Flow<ActivityCalorieLog?> = activityDao.getActivityCalorieLog(date)
    suspend fun getActivityLogSync(date: String): ActivityCalorieLog? = activityDao.getActivityCalorieLogSync(date)
    suspend fun insertActivityLog(log: ActivityCalorieLog) = activityDao.insertActivityCalorie(log)
    suspend fun clearActivityLogs() = activityDao.clearAll()

    fun getAllPhotos(): Flow<List<ProgressPhoto>> = photoDao.getAllPhotos()
    suspend fun insertPhoto(photo: ProgressPhoto) = photoDao.insertPhoto(photo)
    suspend fun deletePhoto(photo: ProgressPhoto) = photoDao.deletePhoto(photo)
    suspend fun clearPhotos() = photoDao.clearAll()
}
