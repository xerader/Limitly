package com.example.myapplication

import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log

class AppUsageService : Service() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("AppUsageService", "Service started.")
        checkUsageStats() // call your method to handle usage stats
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "App Usage Channel"
            val descriptionText = "Notifications for app usage"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("app_usage_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun checkUsageStats() {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        if (usageStatsManager != null) {
            val endTime = System.currentTimeMillis()
            val beginTime = endTime - 1000 * 10 // Last 10 seconds

            val usageStatsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, beginTime, endTime
            )

            val usageStats = usageStatsList.maxByOrNull { it.lastTimeUsed }

            if (usageStats?.packageName == "com.google.android.youtube") {
                Log.d("AppUsageService", "YouTube was opened.")
                sendNotification()
            } else {
                Log.d("AppUsageService", "YouTube was NOT opened.")
            }
        } else {
            Log.e("AppUsageService", "UsageStatsManager not available.")
        }
    }

    private fun sendNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "app_usage_channel"

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("App Opened")
            .setContentText("The target app has been opened.")
            .setSmallIcon(R.drawable.ic_notification) // Ensure this icon exists
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(1, notification)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}