package com.example.myapplication

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader

class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val _peakData: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())
    val peakData: StateFlow<List<String>> get() = _peakData

    init {
        viewModelScope.launch {
            val peaks = loadPeakData(application.applicationContext)
            _peakData.value = peaks.map { peakEntry ->
                val startHours = peakEntry.startTime.toInt()
                val startMinutes = ((peakEntry.startTime - startHours) * 60).toInt()
                val formattedStartTime = String.format("%d:%02d", startHours, startMinutes)

                val endHours = peakEntry.endTime.toInt()
                val endMinutes = ((peakEntry.endTime - endHours) * 60).toInt()
                val formattedEndTime = String.format("%d:%02d", endHours, endMinutes)

                "${peakEntry.packageName}: $formattedStartTime - $formattedEndTime"
            }
        }
    }

    private suspend fun loadPeakData(context: Context): List<MyAccessibilityService.PeakEntry> =
        withContext(Dispatchers.IO) {
            val peaks = mutableListOf<MyAccessibilityService.PeakEntry>()
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
                                println("TRUE FOR THIS")
                                val packageName = parts[0].trim()
                                val startTime = parts[1].trim().toDouble()
                                val endTime = parts[2].trim().toDouble()


                                peaks.add(
                                    MyAccessibilityService.PeakEntry(
                                        packageName,
                                        startTime,
                                        endTime
                                    )
                                )
                            } catch (e: Exception) {
                                Log.e(
                                    "AccessibilityService",
                                    "Error parsing peak data: ${e.message}"
                                )
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

            if (peaks.isEmpty()) {
                peaks.add(
                    MyAccessibilityService.PeakEntry(
                        "com.google.android.bob",
                        1800.1,
                        2200.4
                    )
                )
                peaks.add(
                    MyAccessibilityService.PeakEntry(
                        "com.google.android.youtube",
                        1800.1,
                        2200.3
                    )
                )
            }
            peaks
        }

    fun loadAppIcon(context: Context, packageName: String): Drawable? {
        return try {
            val pm: PackageManager = context.packageManager
            val ai = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            val icon = pm.getApplicationIcon(ai)
            Log.d("StatsViewModel", "Icon for $packageName loaded successfully.")
            icon
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("StatsViewModel", "Icon for $packageName not found.")
            null
        }
    }
}