package com.example.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

object HapticFeedbackHelper {

    private fun getVibrator(context: Context): Vibrator? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Triggers a highly premium double pulse designed specifically for logging events like meals.
     * Feels like a crisp, tactile "done" click-click.
     */
    fun triggerMealLogged(context: Context, view: View? = null) {
        val vibrator = getVibrator(context)
        if (vibrator != null && vibrator.hasVibrator()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Double click: 15ms on, 50ms off, 30ms on
                    val timings = longArrayOf(0, 15, 50, 30)
                    val amplitudes = intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE)
                    val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 15, 50, 30), -1)
                }
                return
            } catch (e: Exception) {
                // fall through to view fallback
            }
        }
        // Fallback
        view?.let { triggerConfirm(it) }
    }

    /**
     * Triggers a rich, triumphant three-pulse pattern designed for completing habits or goals.
     * Feels like a small celebratory heartbeat: "thump-thump-THUMP"
     */
    fun triggerHabitCompleted(context: Context, view: View? = null) {
        val vibrator = getVibrator(context)
        if (vibrator != null && vibrator.hasVibrator()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Celebrate pattern: 15ms on, 60ms off, 20ms on, 60ms off, 45ms on
                    // Amplitudes go up to give a crescendo/triumphant effect
                    val timings = longArrayOf(0, 15, 60, 20, 60, 45)
                    val amplitudes = intArrayOf(0, 150, 0, 180, 0, 255)
                    val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 15, 60, 20, 60, 45), -1)
                }
                return
            } catch (e: Exception) {
                // fall through to view fallback
            }
        }
        // Fallback
        view?.let { triggerConfirm(it) }
    }

    /**
     * Light tap sensation for standard actions like increments, tabs, or small clicks.
     */
    fun triggerLightTap(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    /**
     * Heavy tap/sensation for confirmation, saves, and logging entries.
     */
    fun triggerConfirm(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    /**
     * Sensation for deletions or undo/reject actions.
     */
    fun triggerRejectOrDelete(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }
}
