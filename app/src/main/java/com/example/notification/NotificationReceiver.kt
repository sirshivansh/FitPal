package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationHelper.createNotificationChannel(context)
        NotificationHelper.showTestNotification(context)
        // Reschedule for the next day
        NotificationHelper.scheduleDailyReminder(context, 20, 0) // Default 8 PM
    }
}
