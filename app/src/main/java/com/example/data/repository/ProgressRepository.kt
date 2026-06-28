package com.example.data.repository

import com.example.data.local.dao.DailyActivityCalorieDao
import com.example.data.local.dao.WaterLogDao
import com.example.data.local.dao.WeightLogDao
import com.example.data.local.dao.ProgressPhotoDao
import com.example.data.local.entity.DailyActivityCalorieLog
import com.example.data.local.entity.WaterLog
import com.example.data.local.entity.WeightLog
import com.example.data.local.entity.ProgressPhoto
import kotlinx.coroutines.flow.Flow

class ProgressRepository(
    private val weightLogDao: WeightLogDao,
    private val waterLogDao: WaterLogDao,
    private val dailyActivityCalorieDao: DailyActivityCalorieDao,
    private val progressPhotoDao: ProgressPhotoDao
) {

    // Progress Photos
    val allProgressPhotos: Flow<List<ProgressPhoto>> = progressPhotoDao.getAllProgressPhotos()

    suspend fun insertProgressPhoto(photo: ProgressPhoto) {
        progressPhotoDao.insertProgressPhoto(photo)
    }

    suspend fun deleteProgressPhoto(photo: ProgressPhoto) {
        progressPhotoDao.deleteProgressPhoto(photo)
    }

    // Weight Logging
    val allWeightLogs: Flow<List<WeightLog>> = weightLogDao.getAllWeightLogs()

    suspend fun insertWeightLog(weightLog: WeightLog) {
        weightLogDao.insertWeightLog(weightLog)
    }

    suspend fun deleteWeightLog(weightLog: WeightLog) {
        weightLogDao.deleteWeightLog(weightLog)
    }

    // Water Logging
    val allWaterLogs: Flow<List<WaterLog>> = waterLogDao.getAllWaterLogs()

    fun getWaterLogForDate(date: String): Flow<WaterLog?> {
        return waterLogDao.getWaterLogForDate(date)
    }

    suspend fun incrementWaterLog(date: String, amount: Int) {
        val existing = waterLogDao.getWaterLogForDateSync(date)
        if (existing != null) {
            val updated = existing.copy(glassesCount = existing.glassesCount + amount)
            waterLogDao.updateWaterLog(updated)
        } else {
            val newLog = WaterLog(date = date, glassesCount = amount)
            waterLogDao.insertWaterLog(newLog)
        }
    }

    suspend fun resetWaterLog(date: String) {
        val existing = waterLogDao.getWaterLogForDateSync(date)
        if (existing != null) {
            val updated = existing.copy(glassesCount = 0)
            waterLogDao.updateWaterLog(updated)
        }
    }

    suspend fun clearAllProgressData() {
        weightLogDao.clearWeightLogs()
        waterLogDao.clearWaterLogs()
        dailyActivityCalorieDao.clearActivityCalorieLogs()
        progressPhotoDao.clearProgressPhotos()
    }

    // Daily Activity Calories
    fun getActivityCalorieLogForDate(date: String): Flow<DailyActivityCalorieLog?> {
        return dailyActivityCalorieDao.getActivityCalorieLogForDate(date)
    }

    suspend fun saveActivityCalorieLog(date: String, calories: Int) {
        dailyActivityCalorieDao.insertActivityCalorieLog(
            DailyActivityCalorieLog(date = date, additionalCalories = calories)
        )
    }
}
