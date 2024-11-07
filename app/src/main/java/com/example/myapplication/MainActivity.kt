package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.myapplication.ui.theme.MyApplicationTheme
import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.content.Intent
import android.provider.Settings
import android.app.usage.UsageStatsManager
import java.util.Calendar
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Locale
import android.app.usage.UsageStats
import android.content.ContentUris
import android.content.ContentValues
import android.provider.MediaStore
import android.content.Context
import android.util.Log
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import java.util.Date
import java.io.File
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.chaquo.python.PyException

fun ensureFolderExists(folderPath: String): Boolean {
    val folder = File(folderPath)
    if (!folder.exists()) {
        return folder.mkdirs() // Creates the folder and returns true if successful
    }
    return true // Folder already exists
}


fun writeTextFileToDocuments(context: Context, fileName: String, fileContent: String) {
    val resolver = context.contentResolver
    val folderPath = "Documents/Limitly/"

    // Query if file exists
    val projection = arrayOf(MediaStore.Files.FileColumns._ID)
    val selection = "${MediaStore.Files.FileColumns.RELATIVE_PATH}='$folderPath' AND " +
            "${MediaStore.Files.FileColumns.DISPLAY_NAME}='$fileName'"

    val cursor = resolver.query(
        MediaStore.Files.getContentUri("external"),
        projection,
        selection,
        null,
        null
    )

    if (cursor?.moveToFirst() == true) {
        // File exists, get its URI and append
        val id = cursor.getLong(0)
        val existingUri = ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), id)
        cursor.close()

        try {
            resolver.openOutputStream(existingUri, "wa")?.use { outputStream ->
                outputStream.write(fileContent.toByteArray())
                outputStream.flush()
                println("Successfully appended to existing file")
            }
        } catch (e: Exception) {
            println("Error appending to file: ${e.message}")
        }
    } else {
        cursor?.close()
        // Create new file
        val contentValues = ContentValues().apply {
            put(MediaStore.Files.FileColumns.DISPLAY_NAME, fileName)
            put(MediaStore.Files.FileColumns.MIME_TYPE, "text/plain")
            put(MediaStore.Files.FileColumns.RELATIVE_PATH, folderPath)
        }

        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
        if (uri != null) {
            try {
                resolver.openOutputStream(uri, "w")?.use { outputStream ->
                    outputStream.write(fileContent.toByteArray())
                    outputStream.flush()
                    println("Successfully created and wrote to new file")
                }
            } catch (e: Exception) {
                println("Error creating new file: ${e.message}")
            }
        } else {
            println("Failed to insert file into MediaStore")
        }
    }
}
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
        emptyList()
    } catch (e: Exception) {
        Log.e("UsageStats", "Error getting usage stats: ${e.message}")
        emptyList()
    }
}

@Composable
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
    if (printState) {
        LazyColumn {
            items(newData) { event ->
                Text(text = event)
            }
        }
    }
    return newData
}

@Composable
fun getOldData(context: Context): ArrayList<String> {
    val topApps = topUsedApps(context)
    val oldData = printEventList(context, topApps, false,  true)
    writeTextFileToDocuments(context, "data.txt", oldData.joinToString("\n"))
    return oldData
}

fun initializeFiles(context: Context) {
    Toast.makeText(context, "Initializing files", Toast.LENGTH_SHORT).show()

    val firstRun = "Old Data: False"
    writeTextFileToDocuments(context, "config.txt", firstRun)

    val firstText = "Package Name, Start Time, End Time, Duration\n"
    writeTextFileToDocuments(context, "data.txt", firstText)
}

fun readConfigFile(context: Context): Boolean {
    val resolver = context.contentResolver
    val folderPath = "Documents/Limitly/"
    val fileName = "config.txt"

    // Query if file exists
    val projection = arrayOf(MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.DISPLAY_NAME)
    val selection = "${MediaStore.Files.FileColumns.RELATIVE_PATH}='$folderPath' AND " +
            "${MediaStore.Files.FileColumns.DISPLAY_NAME}='$fileName'"

    val cursor = resolver.query(
        MediaStore.Files.getContentUri("external"),
        projection,
        selection,
        null,
        null
    )

    if (cursor?.moveToFirst() == true) {
        // File exists, get its URI
        println("Config file exists")
        val id = cursor.getLong(0)
        val existingUri = ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), id)
        cursor.close()

        try {
            resolver.openInputStream(existingUri)?.use { inputStream ->
                val content = inputStream.bufferedReader().use { it.readText() }
                println("Config file content: $content")
            }
        } catch (e: Exception) {
            println("Error reading file: ${e.message}")
        }
        return true
    } else {
        cursor?.close()
        println("Config file does not exist.")
    }
    return false
}

class MainActivity : ComponentActivity() {
//get the calendar date
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MyApplicationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (!readConfigFile(this)) {
                        initializeFiles(this)
                    }

                    if (! Python.isStarted()){
                        Python.start(AndroidPlatform(this))
                    }
                    val py = Python.getInstance()
                    val pyf = py.getModule("main")
                    val res = pyf.callAttr("main")


                    println(res)

                    // get data for today
                    val todaysDat= printEventList(this, topUsedApps(this),  true, false)
                    writeTextFileToDocuments(this, "data.txt", todaysDat.joinToString("\n"))
                }
                }
            }
        }
    }

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}
