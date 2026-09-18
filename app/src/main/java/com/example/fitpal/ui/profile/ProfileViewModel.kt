package com.example.fitpal.ui.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitpal.data.local.entity.UserProfile
import com.example.fitpal.data.repository.ProfileRepository
import com.example.fitpal.data.repository.FoodRepository
import com.example.fitpal.data.repository.ExerciseRepository
import com.example.fitpal.data.sync.CloudSyncManager
import com.example.fitpal.data.sync.GoogleAccountInfo
import com.example.fitpal.util.Calculations
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val cloudSyncManager: CloudSyncManager? = null
) : ViewModel() {

    val userProfile: StateFlow<UserProfile?> = profileRepository.getUserProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _fallbackUserEmail = MutableStateFlow<String?>(null)
    val cloudUserEmail: StateFlow<String?> = if (cloudSyncManager != null) {
        cloudSyncManager.currentUser.map { it?.email }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    } else {
        _fallbackUserEmail.asStateFlow()
    }

    val googleAccount: StateFlow<GoogleAccountInfo?> = if (cloudSyncManager != null) {
        cloudSyncManager.currentUser
    } else {
        MutableStateFlow(null)
    }

    private val _fallbackLastSync = MutableStateFlow<String?>(null)
    val lastSyncTime: StateFlow<String?> = if (cloudSyncManager != null) {
        cloudSyncManager.lastSyncTime
    } else {
        _fallbackLastSync.asStateFlow()
    }

    private val _fallbackIsSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = if (cloudSyncManager != null) {
        cloudSyncManager.isSyncing
    } else {
        _fallbackIsSyncing.asStateFlow()
    }

    val hasCloudBackupAvailable: StateFlow<Boolean> = if (cloudSyncManager != null) {
        cloudSyncManager.hasCloudBackupAvailable
    } else {
        MutableStateFlow(false)
    }

    fun loginFirebaseWithGoogle(context: Context, idToken: String, email: String, onComplete: () -> Unit) {
        loginWithGoogleAccount(context, email = email, displayName = email.substringBefore("@"), photoUrl = null, idToken = idToken, onComplete = onComplete)
    }

    fun loginWithGoogle(context: Context, email: String, onComplete: () -> Unit) {
        loginWithGoogleAccount(context, email = email, displayName = email.substringBefore("@"), photoUrl = null, idToken = null, onComplete = onComplete)
    }

    fun loginWithGoogleAccount(
        context: Context,
        email: String,
        displayName: String? = null,
        photoUrl: String? = null,
        idToken: String? = null,
        onComplete: () -> Unit
    ) {
        val mgr = cloudSyncManager ?: CloudSyncManager.getInstance(context)
        mgr.saveGoogleUser(email = email, displayName = displayName, photoUrl = photoUrl, idToken = idToken)
        _fallbackUserEmail.value = email
        _fallbackLastSync.value = "Just now"

        // Auto-check or auto-sync if first time
        viewModelScope.launch {
            if (mgr.hasCloudBackupAvailable.value) {
                // Cloud backup exists for this account, could be restored
            } else {
                // Sync current state to cloud
                mgr.syncNow(context)
            }
            onComplete()
        }
    }

    fun loginWithEmail(
        context: Context,
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        loginWithGoogleAccount(context, email = email, displayName = email.substringBefore("@"), photoUrl = null, idToken = null) {
            onSuccess()
        }
    }

    fun registerWithEmail(
        context: Context,
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        loginWithGoogleAccount(context, email = email, displayName = email.substringBefore("@"), photoUrl = null, idToken = null) {
            onSuccess()
        }
    }

    fun getSavedAccounts(context: Context): List<String> {
        return listOf("shivnsh01@gmail.com", "fitpal.athlete@gmail.com")
    }

    fun updateCalorieGoal(calories: Int?) {
        viewModelScope.launch {
            val current = profileRepository.getUserProfileSync()
            if (current != null) {
                profileRepository.insertProfile(current.copy(customCalorieGoal = calories))
            }
        }
    }

    fun updateCalculations(calorieAdjustment: Int, proteinMultiplier: Float, fatMultiplier: Float) {
        viewModelScope.launch {
            val current = profileRepository.getUserProfileSync()
            if (current != null) {
                profileRepository.insertProfile(
                    current.copy(
                        calorieAdjustment = calorieAdjustment,
                        proteinMultiplier = proteinMultiplier,
                        fatMultiplier = fatMultiplier
                    )
                )
            }
        }
    }

    fun triggerSync(
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val mgr = cloudSyncManager ?: CloudSyncManager.getInstance(context)
            val result = mgr.syncNow(context)
            result.fold(
                onSuccess = {
                    onSuccess()
                },
                onFailure = { err ->
                    onError(err.message ?: "Failed to sync to cloud.")
                }
            )
        }
    }

    fun restoreFromCloud(
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val mgr = cloudSyncManager ?: CloudSyncManager.getInstance(context)
            val result = mgr.restoreFromCloud(context)
            result.fold(
                onSuccess = {
                    onSuccess()
                },
                onFailure = { err ->
                    onError(err.message ?: "Failed to restore data from cloud.")
                }
            )
        }
    }

    fun logout() {
        cloudSyncManager?.clearGoogleUser()
        _fallbackUserEmail.value = null
        _fallbackLastSync.value = null
    }

    fun createOrUpdateProfile(
        name: String,
        age: Int,
        gender: String,
        heightCm: Float,
        currentWeightKg: Float,
        goalWeightKg: Float,
        weightGoalType: String,
        calorieAdjustment: Int,
        proteinMultiplier: Float,
        fatMultiplier: Float,
        maintenanceCalories: Int?,
        activityLevel: String,
        bodyFat: Float?
    ) {
        viewModelScope.launch {
            val bmr = Calculations.calculateBmr(currentWeightKg, heightCm, age, gender)
            val profile = UserProfile(
                name = name,
                age = age,
                gender = gender,
                heightCm = heightCm,
                currentWeightKg = currentWeightKg,
                goalWeightKg = goalWeightKg,
                weightGoalType = weightGoalType,
                calorieAdjustment = calorieAdjustment,
                proteinMultiplier = proteinMultiplier,
                fatMultiplier = fatMultiplier,
                maintenanceCalories = maintenanceCalories,
                bmr = bmr,
                activityLevel = activityLevel,
                bodyFat = bodyFat,
                customCalorieGoal = null
            )
            profileRepository.insertProfile(profile)
        }
    }

    fun clearAllData(foodRepository: FoodRepository, exerciseRepository: FoodRepository) {
        viewModelScope.launch {
            profileRepository.clearProfile()
            profileRepository.clearWeights()
            profileRepository.clearWater()
            profileRepository.clearActivityLogs()
            profileRepository.clearPhotos()
            foodRepository.clearAll()
        }
    }

    fun clearAllData(foodRepository: FoodRepository, exerciseRepository: ExerciseRepository) {
        viewModelScope.launch {
            profileRepository.clearProfile()
            profileRepository.clearWeights()
            profileRepository.clearWater()
            profileRepository.clearActivityLogs()
            profileRepository.clearPhotos()
            foodRepository.clearAll()
            exerciseRepository.clearAll()
        }
    }
}
