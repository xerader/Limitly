package com.example.myapplication

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun Context.hasUsageStatsPermission(): Boolean {
    val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
    return mode == AppOpsManager.MODE_ALLOWED
}


fun Context.requestUsageStatsPermission() {
    startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
}

// get top 10 most used apps in the last month
fun topUsedApps(context: Context): ArrayList<String> {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val calStart = Calendar.getInstance().apply {
        add(Calendar.MONTH, -1) // Set to one month ago
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val calEnd = Calendar.getInstance() // Set to current time

    var usageStats = usageStatsManager.queryUsageStats(
        UsageStatsManager.INTERVAL_YEARLY,
        calStart.timeInMillis,
        calEnd.timeInMillis
    )

    // Sort the apps by total usage time in descending order and get the top 10
    usageStats = usageStats
//        .filter { !it.packageName.contains("android", ignoreCase = true) && !it.packageName.contains("frontpage", ignoreCase = true) }
        .sortedByDescending { it.totalTimeInForeground }
        .take(10)
    // get the names of the top 10 apps
    val topApps = ArrayList<String>()
    for (usageStat in usageStats) {
        topApps.add(usageStat.packageName)
    }
    return topApps
}



fun getOldUsageEvents(context: Context, packageName: String): List<Long> {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val calStart = Calendar.getInstance().apply {
        add(Calendar.MONTH, -1) // Set to one month ago
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val calEnd = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -1) // Set to one day before today
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }
    val events = usageStatsManager.queryEvents(calStart.timeInMillis, calEnd.timeInMillis)
    val eventList = mutableListOf<Long>()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    while (events.hasNextEvent()) {
        val event = UsageEvents.Event()
        events.getNextEvent(event)
        if (event.packageName == packageName) {
            val date = dateFormat.format(event.timeStamp)
            eventList.add(event.timeStamp)
        }
    }
    return eventList
}
fun getUsageEvents(context: Context, packageName: String): List<Long> {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    // Set start time to yesterday at 00:00:00
    val calStart = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -2)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    // Set end time to yesterday at 23:59:59
    val calEnd = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    return try {
        val events = usageStatsManager.queryEvents(calStart.timeInMillis, calEnd.timeInMillis)
        val eventList = mutableListOf<Long>()
        // print length of events
        println(events)

        if (events != null) {
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.packageName == packageName) {
                    // Only add timestamps that fall within our time window
                    if (event.timeStamp in calStart.timeInMillis..calEnd.timeInMillis) {
                        eventList.add(event.timeStamp)
                    }
                }
            }
        }
        else {
            Log.d("UsageStats", "No events found for $packageName")
        }

        // Log the size of the list for debugging
        Log.d("UsageStats", "Found ${eventList.size} events for $packageName")

        if (eventList.isEmpty()) {
            Log.d("UsageStats", "No usage events found for yesterday for $packageName")
        }

        eventList
    } catch (e: SecurityException) {
        Log.e("UsageStats", "Permission denied: ${e.message}")
        // request for permission
        context.requestUsageStatsPermission()
        emptyList()
    } catch (e: Exception) {
        Log.e("UsageStats", "Error getting usage stats: ${e.message}")
        emptyList()
    }
}



fun printEventList(context: Context, packageNames: ArrayList<String>, printState: Boolean, oldState: Boolean): ArrayList<String> {
    val newData = ArrayList<String>()

    for (packageName in packageNames) {
        var eventList = if (oldState){
            getOldUsageEvents(context, packageName)
        } else {
            getUsageEvents(context, packageName)
        }

        if (eventList.isEmpty()) {
            continue
        }

        println("Old State: ${oldState.toString()}")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        var newStart = eventList[0]
        for (i in 1 until eventList.size) {
            if (eventList[i] - eventList[i - 1] > 60 * 60 * 1000) {
                newData.add(
                    " ${packageName}, ${dateFormat.format(newStart)},  ${
                        dateFormat.format(
                            eventList[i - 1]
                        )
                    },  ${(eventList[i - 1] - newStart) / (1000 * 60)} mins and ${(eventList[i - 1] - newStart) / 1000 % 60} seconds"
                )
                newStart = eventList[i]

            }
        }
    }

    return newData
}

fun getOldData(context: Context): ArrayList<String> {
    val topApps = topUsedApps(context)
    val oldData = printEventList(context, topApps, false,  true)
    writeTextFileToDocuments(context, "data.txt", oldData.joinToString("\n"))
    return oldData
}