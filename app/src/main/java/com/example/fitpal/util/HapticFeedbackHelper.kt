package com.example.fitpal.util

import android.view.View
import android.view.HapticFeedbackConstants

object HapticFeedbackHelper {
    fun triggerLightTap(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun triggerConfirm(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    fun triggerRejectOrDelete(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }
}
