package com.example.fitpal.util

import android.content.Context

object SecurityUtils {
    private const val PREFS_NAME = "fitpal_security_prefs"
    private const val KEY_PIN = "security_pin"

    fun isPinSet(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.contains(KEY_PIN)
    }

    fun verifyPin(context: Context, pin: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_PIN, null)
        return saved == pin
    }

    fun savePin(context: Context, pin: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PIN, pin).apply()
    }

    fun enablePasscode(context: Context, pin: String) {
        savePin(context, pin)
    }

    fun disablePasscode(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_PIN).apply()
    }
}
