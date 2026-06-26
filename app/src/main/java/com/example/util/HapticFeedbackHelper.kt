package com.example.util

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

object HapticFeedbackHelper {
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
