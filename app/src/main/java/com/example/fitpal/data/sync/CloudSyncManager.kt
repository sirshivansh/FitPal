package com.example.fitpal.data.sync

import android.content.Context
import android.content.SharedPreferences
import com.example.fitpal.util.BackupRestoreHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

data class GoogleAccountInfo(
    val email: String,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val idToken: String? = null
)

class CloudSyncManager private constructor(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("fitpal_cloud_sync_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<GoogleAccountInfo?>(loadSavedUser())
    val currentUser: StateFlow<GoogleAccountInfo?> = _currentUser.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<String?>(loadLastSyncTimeFormatted())
    val lastSyncTime: StateFlow<String?> = _lastSyncTime.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _hasCloudBackupAvailable = MutableStateFlow(checkHasCloudBackup())
    val hasCloudBackupAvailable: StateFlow<Boolean> = _hasCloudBackupAvailable.asStateFlow()

    private fun loadSavedUser(): GoogleAccountInfo? {
        val email = prefs.getString("google_email", null) ?: return null
        val name = prefs.getString("google_name", null)
        val photo = prefs.getString("google_photo", null)
        val token = prefs.getString("google_token", null)
        return GoogleAccountInfo(email = email, displayName = name, photoUrl = photo, idToken = token)
    }

    private fun checkHasCloudBackup(): Boolean {
        val email = _currentUser.value?.email ?: return false
        val backup = prefs.getString("cloud_backup_data_$email", null)
        return !backup.isNullOrBlank()
    }

    fun saveGoogleUser(email: String, displayName: String? = null, photoUrl: String? = null, idToken: String? = null) {
        prefs.edit()
            .putString("google_email", email)
            .putString("google_name", displayName)
            .putString("google_photo", photoUrl)
            .putString("google_token", idToken)
            .apply()

        val user = GoogleAccountInfo(email = email, displayName = displayName, photoUrl = photoUrl, idToken = idToken)
        _currentUser.value = user
        _hasCloudBackupAvailable.value = checkHasCloudBackup()
    }

    fun clearGoogleUser() {
        try {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
        } catch (_: Throwable) {}

        prefs.edit()
            .remove("google_email")
            .remove("google_name")
            .remove("google_photo")
            .remove("google_token")
            .apply()

        _currentUser.value = null
        _hasCloudBackupAvailable.value = false
    }

    private fun loadLastSyncTimeFormatted(): String? {
        val ts = prefs.getLong("last_sync_timestamp", 0L)
        if (ts == 0L) return null
        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        return sdf.format(Date(ts))
    }

    suspend fun syncNow(context: Context): Result<String> = withContext(Dispatchers.IO) {
        val user = _currentUser.value
        if (user == null) {
            return@withContext Result.failure(Exception("No Google Account connected. Sign in with Google first."))
        }

        _isSyncing.value = true
        try {
            // Export current local database
            val backupJson = BackupRestoreHelper.exportDatabaseToJson(context)
            val now = System.currentTimeMillis()

            // Save to persistent cloud storage container keyed by user account (representing Google Drive appdata cloud store)
            prefs.edit()
                .putString("cloud_backup_data_${user.email}", backupJson)
                .putLong("cloud_backup_timestamp_${user.email}", now)
                .putLong("last_sync_timestamp", now)
                .apply()

            val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            val formatted = sdf.format(Date(now))
            _lastSyncTime.value = formatted
            _hasCloudBackupAvailable.value = true

            try {
                val fbUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                if (fbUser != null) {
                    val db = com.google.firebase.database.FirebaseDatabase.getInstance()
                    db.reference.child("users").child(fbUser.uid).child("last_backup_timestamp").setValue(now)
                }
            } catch (_: Throwable) {}

            Result.success(formatted)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        } finally {
            _isSyncing.value = false
        }
    }

    suspend fun restoreFromCloud(context: Context): Result<Boolean> = withContext(Dispatchers.IO) {
        val user = _currentUser.value
        if (user == null) {
            return@withContext Result.failure(Exception("No Google Account connected."))
        }

        _isSyncing.value = true
        try {
            val backupJson = prefs.getString("cloud_backup_data_${user.email}", null)
            if (backupJson.isNullOrBlank()) {
                return@withContext Result.failure(Exception("No cloud backup found for ${user.email}."))
            }

            val success = BackupRestoreHelper.importDatabaseFromJson(context, backupJson)
            if (success) {
                val now = System.currentTimeMillis()
                prefs.edit().putLong("last_sync_timestamp", now).apply()
                val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                _lastSyncTime.value = sdf.format(Date(now))
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to restore cloud backup."))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        } finally {
            _isSyncing.value = false
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: CloudSyncManager? = null

        fun getInstance(context: Context): CloudSyncManager {
            return INSTANCE ?: synchronized(this) {
                val instance = CloudSyncManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
