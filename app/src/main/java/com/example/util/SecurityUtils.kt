package com.example.util

import android.content.Context
import java.security.MessageDigest

object SecurityUtils {
    private const val PIN_SALT = "FitPalPasscodeSalt_55123_Secure"

    /**
     * Hashes a 4-digit PIN with a secure cryptographic salt.
     */
    fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest((pin + PIN_SALT).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Checks if passcode lock is enabled.
     */
    fun isPasscodeEnabled(context: Context): Boolean {
        val sp = context.getSharedPreferences("fitpal_settings", Context.MODE_PRIVATE)
        return sp.getBoolean("passcode_enabled", false)
    }

    /**
     * Verifies if the entered PIN matches the stored hash.
     */
    fun verifyPin(context: Context, enteredPin: String): Boolean {
        val sp = context.getSharedPreferences("fitpal_settings", Context.MODE_PRIVATE)
        val storedHash = sp.getString("passcode_hash", "") ?: ""
        return hashPin(enteredPin) == storedHash
    }

    /**
     * Enables passcode lock with a new PIN.
     */
    fun enablePasscode(context: Context, pin: String) {
        val sp = context.getSharedPreferences("fitpal_settings", Context.MODE_PRIVATE)
        sp.edit()
            .putBoolean("passcode_enabled", true)
            .putString("passcode_hash", hashPin(pin))
            .apply()
    }

    /**
     * Disables passcode lock.
     */
    fun disablePasscode(context: Context) {
        val sp = context.getSharedPreferences("fitpal_settings", Context.MODE_PRIVATE)
        sp.edit()
            .putBoolean("passcode_enabled", false)
            .putString("passcode_hash", "")
            .apply()
    }
}
