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
import java.text.SimpleDateFormat
import java.util.Locale
import android.app.usage.UsageStats
import android.content.pm.PackageManager
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import java.util.Date
import java.io.File
import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

fun ensureFolderExists(folderPath: String): Boolean {
    val folder = File(folderPath)
    if (!folder.exists()) {
        return folder.mkdirs() // Creates the folder and returns true if successful
    }
    return true // Folder already exists
}

fun writeTextFileToDocuments(context: Context, fileName: String, fileContent: String) {
    val resolver = context.contentResolver

    // Set up content values for the file's metadata
    val contentValues = ContentValues().apply {
        put(MediaStore.Files.FileColumns.DISPLAY_NAME, fileName)  // File name
        put(MediaStore.Files.FileColumns.MIME_TYPE, "text/plain")  // MIME type
        put(MediaStore.Files.FileColumns.RELATIVE_PATH, "Documents/Limitly") // Save to Limitly folder in Documents
    }

    // Check if the file already exists and get its URI if so
    val existingUri = resolver.query(
        MediaStore.Files.getContentUri("external"),
        arrayOf(MediaStore.Files.FileColumns._ID),
        "${MediaStore.Files.FileColumns.RELATIVE_PATH}=? AND ${MediaStore.Files.FileColumns.DISPLAY_NAME}=?",
        arrayOf("Documents/Limitly", fileName),
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID))
            MediaStore.Files.getContentUri("external", id)
        } else null
    }

    // If file exists, delete it to avoid duplicates (for overwriting)
    existingUri?.let {
        resolver.delete(it, null, null)
    }

    // Insert the new file and get its URI
    val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)

    if (uri != null) {
        // Open an output stream and write to the file
        resolver.openOutputStream(uri, "w").use { outputStream ->
            if (outputStream != null) {
                outputStream.write(fileContent.toByteArray())
                outputStream.flush()
                Toast.makeText(context, "File written successfully to Limitly folder in Documents!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Failed to create output stream.", Toast.LENGTH_LONG).show()
            }
        }
    } else {
        Toast.makeText(context, "Failed to insert file into MediaStore.", Toast.LENGTH_LONG).show()
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
    while (events.hasNextEvent()) {
        val event = UsageEvents.Event()
        events.getNextEvent(event)
        if (event.packageName == packageName) {
            eventList.add(event.timeStamp)
        }
    }
    return eventList
}

// get all the time epochs for a given app in the last day
fun getUsageEvents(context: Context, packageName: String): List<Long> {
  val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
val calStart = Calendar.getInstance().apply {
    add(Calendar.DAY_OF_YEAR, -1) // Set to 24 hours ago
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}

val calEnd = Calendar.getInstance().apply {
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


fun printEventList(context: Context, packageNames: ArrayList<String>, printState: Boolean, oldState: Boolean): ArrayList<String> {
    val newData = ArrayList<String>()

    newData.add("Package Name, Start Time, End Time, Duration")
    for (packageName in packageNames) {
        var eventList = List<Long>(0) { 0 }
        if (oldState){
            eventList = getOldUsageEvents(context, packageName)
        }
        else{
            eventList = getUsageEvents(context, packageName)
        }
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
        for (data in newData) {
            println(data)
        }
    }
    return newData
}

fun getOldData(context: Context) {
    val topApps = topUsedApps(context)
    val oldData = printEventList(context, topApps, false, true)
    writeTextFileToDocuments(context, "oldData", oldData.joinToString("\n"))
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
                    GetOldDataButton()
                    // if the config file does not exist, create it
//                    generateConfigFile(this)

//                    val resList = runContext(this)
//                    // whatever the date is in the first entry in reslist
//                    val title = resList[0].split(",")[2]



                    // write to config file
//                    writeTextFileToDocuments(this, "config", "Old Data: True\n")

                    // title is today's date
                    val title = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    // get the event list for the top apps
                    val todaysDat= printEventList(this, topUsedApps(this), true, false)
                    writeTextFileToDocuments(this, title, todaysDat.joinToString("\n"))

                }
                }
            }
        }
    }


@Composable
fun GetOldDataButton() {
    val context = LocalContext.current

    Button(
        onClick = {
            getOldData(context)
        },
        modifier = Modifier.size(20.dp, 40.dp) // Adjust the size as needed

    ) {
        Text("Get Old Data")
    }
}
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}
