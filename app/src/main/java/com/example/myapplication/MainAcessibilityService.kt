package com.example.myapplication

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.Calendar

class MyAccessibilityService() : AccessibilityService(), Parcelable {
    private var peakList: List<PeakEntry> = emptyList()

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
                    sendNotification(
                        "App Usage Alert",
                        "$packageName is open during restricted hours."
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