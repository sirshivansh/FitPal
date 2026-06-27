package com.example.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.UserProfile
import com.example.data.local.entity.WeightLog
import com.example.data.repository.ProgressRepository
import com.example.data.repository.UserRepository
import com.example.util.Calculations
import com.example.util.DateUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepository: UserRepository,
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val _cloudUserEmail = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val cloudUserEmail = _cloudUserEmail.asStateFlow()

    private val _lastSyncTime = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val lastSyncTime = _lastSyncTime.asStateFlow()

    private val _isSyncing = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private fun getAccountsFile(context: android.content.Context): java.io.File {
        return java.io.File(context.filesDir, "simulated_cloud_accounts.json")
    }

    private fun loadAccounts(context: android.content.Context): org.json.JSONObject {
        val file = getAccountsFile(context)
        if (!file.exists()) {
            return org.json.JSONObject().apply { put("accounts", org.json.JSONObject()) }
        }
        return try {
            org.json.JSONObject(file.readText())
        } catch (e: Exception) {
            org.json.JSONObject().apply { put("accounts", org.json.JSONObject()) }
        }
    }

    private fun saveAccounts(context: android.content.Context, json: org.json.JSONObject) {
        try {
            getAccountsFile(context).writeText(json.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun registerWithEmail(
        context: android.content.Context,
        email: String,
        pass: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val db = loadAccounts(context)
                val accounts = db.getJSONObject("accounts")
                if (accounts.has(email)) {
                    onError("Account with this email already exists.")
                    return@launch
                }
                
                // Export current local database as initial backup
                val backupJson = com.example.util.BackupRestoreHelper.exportDatabaseToJson(context)
                
                val userObj = org.json.JSONObject().apply {
                    put("password", pass)
                    put("backup", backupJson)
                }
                accounts.put(email, userObj)
                saveAccounts(context, db)
                
                _cloudUserEmail.value = email
                _lastSyncTime.value = "Synced just now"
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Registration failed.")
            }
        }
    }

    fun loginWithEmail(
        context: android.content.Context,
        email: String,
        pass: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val db = loadAccounts(context)
                val accounts = db.getJSONObject("accounts")
                if (!accounts.has(email)) {
                    onError("Account does not exist.")
                    return@launch
                }
                val userObj = accounts.getJSONObject(email)
                val savedPass = userObj.getString("password")
                if (savedPass != pass) {
                    onError("Incorrect password.")
                    return@launch
                }
                
                // Success! Restore database
                val backupJson = userObj.optString("backup", "")
                if (backupJson.isNotBlank()) {
                    _isSyncing.value = true
                    val restored = com.example.util.BackupRestoreHelper.importDatabaseFromJson(context, backupJson)
                    _isSyncing.value = false
                    if (restored) {
                        _cloudUserEmail.value = email
                        _lastSyncTime.value = "Restored from cloud"
                        onSuccess()
                    } else {
                        onError("Failed to restore backup from cloud.")
                    }
                } else {
                    _cloudUserEmail.value = email
                    _lastSyncTime.value = "No backup on cloud"
                    onSuccess()
                }
            } catch (e: Exception) {
                onError(e.message ?: "Login failed.")
            }
        }
    }

    fun loginWithGoogle(
        context: android.content.Context,
        email: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val db = loadAccounts(context)
            val accounts = db.getJSONObject("accounts")
            if (!accounts.has(email)) {
                // Auto-register google user
                val backupJson = com.example.util.BackupRestoreHelper.exportDatabaseToJson(context)
                val userObj = org.json.JSONObject().apply {
                    put("password", "google_oauth_bypass")
                    put("backup", backupJson)
                }
                accounts.put(email, userObj)
                saveAccounts(context, db)
                _cloudUserEmail.value = email
                _lastSyncTime.value = "Synced just now"
            } else {
                // Auto-login google user and restore
                val userObj = accounts.getJSONObject(email)
                val backupJson = userObj.optString("backup", "")
                if (backupJson.isNotBlank()) {
                    _isSyncing.value = true
                    com.example.util.BackupRestoreHelper.importDatabaseFromJson(context, backupJson)
                    _isSyncing.value = false
                    _lastSyncTime.value = "Restored from cloud"
                } else {
                    _lastSyncTime.value = "No backup on cloud"
                }
                _cloudUserEmail.value = email
            }
            onSuccess()
        }
    }

    fun triggerSync(
        context: android.content.Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val email = _cloudUserEmail.value
        if (email == null) {
            onError("You must be logged in to sync.")
            return
        }
        viewModelScope.launch {
            try {
                _isSyncing.value = true
                val db = loadAccounts(context)
                val accounts = db.getJSONObject("accounts")
                if (accounts.has(email)) {
                    val userObj = accounts.getJSONObject(email)
                    val backupJson = com.example.util.BackupRestoreHelper.exportDatabaseToJson(context)
                    userObj.put("backup", backupJson)
                    saveAccounts(context, db)
                    _lastSyncTime.value = "Synced just now"
                    _isSyncing.value = false
                    onSuccess()
                } else {
                    _isSyncing.value = false
                    onError("Account session is invalid.")
                }
            } catch (e: Exception) {
                _isSyncing.value = false
                onError(e.message ?: "Sync failed.")
            }
        }
    }

    fun logout() {
        _cloudUserEmail.value = null
        _lastSyncTime.value = null
    }

    val userProfile: StateFlow<UserProfile?> = userRepository.userProfile
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

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
        maintenanceCalories: Int?
    ) {
        viewModelScope.launch {
            val bmr = Calculations.calculateBmr(currentWeightKg, heightCm, age, gender)
            val calorieGoal = if (maintenanceCalories != null && maintenanceCalories > 0) {
                Calculations.calculateTargetCalorieGoal(maintenanceCalories.toFloat(), weightGoalType, calorieAdjustment)
            } else {
                0
            }
            val (carbs, protein, fat) = if (calorieGoal > 0) {
                Calculations.calculateMacrosCustom(calorieGoal, currentWeightKg, proteinMultiplier, fatMultiplier)
            } else {
                Triple(0, 0, 0)
            }

            val profile = UserProfile(
                name = name,
                age = age,
                gender = gender,
                heightCm = heightCm,
                currentWeightKg = currentWeightKg,
                goalWeightKg = goalWeightKg,
                bmr = bmr,
                dailyCalorieGoal = calorieGoal,
                proteinGoalGrams = protein,
                carbsGoalGrams = carbs,
                fatGoalGrams = fat,
                weightGoalType = weightGoalType,
                calorieAdjustment = calorieAdjustment,
                proteinMultiplier = proteinMultiplier,
                fatMultiplier = fatMultiplier,
                maintenanceCalories = maintenanceCalories
            )
            userRepository.saveProfile(profile)

            // Auto-log initial/updated weight to Weight history
            val todayStr = DateUtils.getTodayDateString()
            progressRepository.insertWeightLog(WeightLog(date = todayStr, weightKg = currentWeightKg))
        }
    }

    fun updateCalculations(calorieAdjustment: Int, proteinMultiplier: Float, fatMultiplier: Float) {
        viewModelScope.launch {
            val current = userRepository.getUserProfileSync() ?: return@launch
            val calorieGoal = if (current.maintenanceCalories != null && current.maintenanceCalories > 0) {
                Calculations.calculateTargetCalorieGoal(current.maintenanceCalories.toFloat(), current.weightGoalType, calorieAdjustment)
            } else {
                0
            }
            val (carbs, protein, fat) = if (calorieGoal > 0) {
                Calculations.calculateMacrosCustom(calorieGoal, current.currentWeightKg, proteinMultiplier, fatMultiplier)
            } else {
                Triple(0, 0, 0)
            }

            val updated = current.copy(
                dailyCalorieGoal = calorieGoal,
                proteinGoalGrams = protein,
                carbsGoalGrams = carbs,
                fatGoalGrams = fat,
                calorieAdjustment = calorieAdjustment,
                proteinMultiplier = proteinMultiplier,
                fatMultiplier = fatMultiplier
            )
            userRepository.saveProfile(updated)
        }
    }

    fun updateCalorieGoal(calorieGoal: Int) {
        viewModelScope.launch {
            val current = userRepository.getUserProfileSync() ?: return@launch
            val (carbs, protein, fat) = Calculations.calculateMacrosCustom(
                calorieGoal,
                current.currentWeightKg,
                current.proteinMultiplier,
                current.fatMultiplier
            )
            val updated = current.copy(
                dailyCalorieGoal = calorieGoal,
                proteinGoalGrams = protein,
                carbsGoalGrams = carbs,
                fatGoalGrams = fat
            )
            userRepository.saveProfile(updated)
        }
    }

    fun clearAllData(
        foodRepository: com.example.data.repository.FoodRepository,
        exerciseRepository: com.example.data.repository.ExerciseRepository
    ) {
        viewModelScope.launch {
            userRepository.clearProfile()
            foodRepository.clearAllFoodData()
            exerciseRepository.clearAllExerciseData()
            progressRepository.clearAllProgressData()
            logout()
        }
    }
}
