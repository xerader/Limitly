package com.example.myapplication

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.Calendar
import android.content.ComponentName
import android.provider.Settings
import android.accessibilityservice.AccessibilityService

class MyAccessibilityService() : AccessibilityService(), Parcelable {
    private var peakList: List<PeakEntry> = emptyList()
    private var goalList: List<String> = emptyList()

    constructor(parcel: Parcel) : this() {

    }

    data class PeakEntry(val packageName: String, val startTime: Double, val endTime: Double)

    override fun onServiceConnected() {
        super.onServiceConnected()

        // Configure service info
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
        }
        serviceInfo = info

        Log.d("AccessibilityService", "Service connected and ready.")
        createNotificationChannel()
        // Load peak data from file
        peakList = loadPeakData()
        goalList = loadGoalData()
        Log.d("AccessibilityService", "TRYING: $peakList")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            if (it.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                val packageName = it.packageName.toString()
                Log.d("ForegroundApp", "Current app in foreground: $packageName")

                // Get the current time
                val calendar = Calendar.getInstance()
                val currentTime =
                    calendar.get(Calendar.HOUR_OF_DAY)  + calendar.get(Calendar.MINUTE)/60.0
                Log.d("AccessibilityService", "Current time: $currentTime")

                // if currentapp is in peakList, put log
            val matchingPeaks = peakList.filter { it.packageName == packageName }
            if (matchingPeaks.isNotEmpty()) {
                Log.d("AccessibilityService", "Current app $packageName is in peakList!")
                matchingPeaks.forEach { peak ->
                    val startTime = peak.startTime
                    val endTime = peak.endTime

                    // make startTime and endTime to be doubles


                    println("Start time: $startTime")
                    println("End time: $endTime")
                    if (currentTime in startTime..endTime) {
                        // make the message a random line in GoalList
                        val message = goalList.random()
                    sendNotification(
                        "App Usage Alert",
                        "$packageName is open! Why don't you try $message?"
                    )
                    }
                }
            }

            }
        }
    }

    override fun onInterrupt() {
        Log.d("AccessibilityService", "Service interrupted.")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d("AccessibilityService", "Service unbound.")
        return super.onUnbind(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Alert Channel"
            val descriptionText = "Channel for usage alerts"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("alert_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(title: String, message: String) {
        val notificationManagerCompat = NotificationManagerCompat.from(this)
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, "alert_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request notification permission if not granted
            return
        }
        notificationManagerCompat.notify(1, builder.build())
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {

    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<MyAccessibilityService> {
        override fun createFromParcel(parcel: Parcel): MyAccessibilityService {
            return MyAccessibilityService(parcel)
        }

        override fun newArray(size: Int): Array<MyAccessibilityService?> {
            return arrayOfNulls(size)
        }

        @SuppressLint("ServiceCast")
        fun isServiceEnabled(context: Context, serviceClass: Class<out AccessibilityService>): Boolean {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            // Retrieve the comma-separated list of enabled services
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""
            // Construct the component name of the service we are checking for
            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServices)

            // Iterate through the list of enabled services and check against your service
            while (colonSplitter.hasNext()) {
                val componentName = colonSplitter.next()
                if (componentName.equals(ComponentName(context, serviceClass).flattenToString(), ignoreCase = true)) {
                    return true
                }
            }
            return false
        }
    }

    private fun loadGoalData(): List<String>{
        val goals = goalList.toMutableList()
        try {
            val filePath = "/storage/emulated/0/Documents/Limitly/goals.txt"
            println(filePath)
            val file = File(filePath)
            if (file.exists()) {
                println("GOALS file exists")
                val reader = BufferedReader(InputStreamReader(FileInputStream(file)))
                reader.forEachLine { line ->
                    val parts = line.split(",")
                    if (parts.size == 1) {
                        try {
                            val goal = parts[0].trim()
                            goals.add(goal)
                        } catch (e: Exception) {
                            Log.e("AccessibilityService", "Error parsing goal data: ${e.message}")
                        }
                    }
                }
                reader.close()
            } else {
                Log.e("AccessibilityService", "File not found: $filePath")
            }
        } catch (e: Exception) {
            Log.e("AccessibilityService", "Error loading goal data: ${e.message}")
        }
        // make sure goals is not empty
        if (goals.isEmpty()) {
            goals.add("Goal 1")
            goals.add("Goal 2")
            goals.add("Goal 3")
            goals.add("Goal 4")
            goals.add("Goal 5")
        }
        return goals
    }
    private fun loadPeakData(): List<PeakEntry> {
        val peaks = peakList.toMutableList()
        try {
            val filePath = "/storage/emulated/0/Documents/Limitly/peaks.txt"
            println(filePath)
            val file = File(filePath)
            if (file.exists()) {
                println("PEAKS file exists")
                val reader = BufferedReader(InputStreamReader(FileInputStream(file)))
                reader.forEachLine { line ->
                    val parts = line.split(",")
                    if (parts.size == 3) {
                try {
                    // print tru for this
                    println("TRUE FOR THIS")
                    val packageName = parts[0].trim()
                    val startTime = parts[1].trim().toDouble()  // Assuming time format as HH
                    val endTime = parts[2].trim().toDouble()
                    peaks.add(PeakEntry(packageName, startTime, endTime))
                } catch (e: Exception) {
                    Log.e("AccessibilityService", "Error parsing peak data: ${e.message}")
                }
                    }
                }
                reader.close()
            } else {
                Log.e("AccessibilityService", "File not found: $filePath")
            }
        } catch (e: Exception) {
            Log.e("AccessibilityService", "Error loading peak data: ${e.message}")
        }
        // make sure peaks is not empty
        if (peaks.isEmpty()) {
            peaks.add(PeakEntry("com.google.android.youtube", 1800.1, 2200.4))
            peaks.add(PeakEntry("com.google.android.youtube", 1800.1, 2200.3))
        }
        return peaks
    }
}