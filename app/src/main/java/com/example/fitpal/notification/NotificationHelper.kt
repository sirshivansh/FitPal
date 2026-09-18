package com.example.fitpal.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.fitpal.R

object NotificationHelper {
    private const val CHANNEL_ID = "fitpal_reminders"
    private const val CHANNEL_NAME = "FitPal Reminders"

    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
        // Mock schedule function
    }

    fun cancelDailyReminder(context: Context) {
        // Mock cancel function
    }

    fun showTestNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("FitPal Reminder")
            .setContentText("Don't forget to log your meals and workouts today! Stay on track! 🔥")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notificationManager.notify(1, builder.build())
    }
}
