package com.example.data.repository

import com.example.data.local.dao.UserProfileDao
import com.example.data.local.entity.UserProfile
import kotlinx.coroutines.flow.Flow

class UserRepository(private val userProfileDao: UserProfileDao) {

    val userProfile: Flow<UserProfile?> = userProfileDao.getUserProfile()

    suspend fun getUserProfileSync(): UserProfile? {
        return userProfileDao.getUserProfileSync()
    }

    suspend fun saveProfile(profile: UserProfile) {
        userProfileDao.insertProfile(profile)
    }

    suspend fun clearProfile() {
        userProfileDao.clearProfile()
    }
}
