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
import android.content.ContentValues
import android.provider.MediaStore
import android.content.Context
import android.content.pm.PackageManager
import java.util.Date

fun writeTextFileToDocuments(context: Context, fileName: String, fileContent: String) {
    val resolver = context.contentResolver

    // Set up content values for the file's metadata
    val contentValues = ContentValues().apply {
        put(MediaStore.Files.FileColumns.DISPLAY_NAME, fileName)  // File name
        put(MediaStore.Files.FileColumns.MIME_TYPE, "text/plain")  // MIME type
        put(MediaStore.Files.FileColumns.RELATIVE_PATH, "Documents/") // Save to Documents folder
    }

    // Insert the file and get its URI
    val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)

    if (uri != null) {
        // Open an output stream and write to the file
        resolver.openOutputStream(uri, "wa").use { outputStream ->
            if (outputStream != null) {
                outputStream.write(fileContent.toByteArray())
                outputStream.flush()
                println("File written successfully to Documents folder!")
            } else {
                println("Failed to create output stream.")
            }
        }
    } else {
        println("Failed to insert file into MediaStore.")
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
fun topUsedApps(context: Context): List<UsageStats> {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val calStart = Calendar.getInstance().apply {
        add(Calendar.MONTH, -1) // Set to one month ago
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val calEnd = Calendar.getInstance() // Set to current time

    val usageStats = usageStatsManager.queryUsageStats(
        UsageStatsManager.INTERVAL_YEARLY,
        calStart.timeInMillis,
        calEnd.timeInMillis
    )

    // Sort the apps by total usage time in descending order and get the top 10
    return usageStats
//        .filter { !it.packageName.contains("android", ignoreCase = true) && !it.packageName.contains("frontpage", ignoreCase = true) }
        .sortedByDescending { it.totalTimeInForeground }
        .take(10)

}




fun getUsageStats(context: Context): List<UsageStats> {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val calStart = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -1) // Set to the previous day
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val calEnd = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -1) // Same day as start
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }
    return usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, calStart.timeInMillis, calEnd.timeInMillis)
}


fun runContext(context: Context, top: Boolean): ArrayList<String> {
    val resultList = ArrayList<String>()
    if (context.hasUsageStatsPermission()) {
        val usageStatsList: List<UsageStats>
        if (top){
            usageStatsList = topUsedApps(context)
        }
        else {
            usageStatsList = getUsageStats(context)
        }
        Toast.makeText(context, "Usage Stats permission is obtained!", Toast.LENGTH_LONG).show()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val packageManager = context.packageManager
        val statsBuilder = StringBuilder()
        for (usageStats in usageStatsList) {
            val appName = try {
                // Try multiple methods to get the app name
                val applicationInfo = packageManager.getApplicationInfo(
                    usageStats.packageName,
                    PackageManager.GET_META_DATA or
                            PackageManager.GET_SHARED_LIBRARY_FILES or
                            PackageManager.GET_UNINSTALLED_PACKAGES
                )

                // First try: Get label from application info
                val nameFromLabel = packageManager.getApplicationLabel(applicationInfo).toString()

                if (nameFromLabel != usageStats.packageName) {
                    nameFromLabel
                } else {
                    // Second try: Get name from package info
                    val packageInfo = packageManager.getPackageInfo(usageStats.packageName, 0)
                    val nameFromPackage = packageInfo.applicationInfo.loadLabel(packageManager).toString()

                    if (nameFromPackage != usageStats.packageName) {
                        nameFromPackage
                    } else {
                        // Fallback: Use last part of package name but make it more readable
                        usageStats.packageName.substringAfterLast('.').split("(?<=.)(?=\\p{Upper})".toRegex())
                            .joinToString(" ").capitalize()
                    }
                }
            } catch (e: Exception) {
                // If all methods fail, make the package name more readable
                usageStats.packageName.substringAfterLast('.').split("(?<=.)(?=\\p{Upper})".toRegex())
                    .joinToString(" ").capitalize()
            }
            val date = dateFormat.format(usageStats.firstTimeStamp)

            // get the usageStats output in a readable format
            val packageName = usageStats.packageName
            val totalTimeInMins = usageStats.totalTimeInForeground / (1000 * 60)


            if (totalTimeInMins > 0) {
                statsBuilder.append("Package: $packageName, Total Time: $totalTimeInMins mins, Last Time Used: $date\n")
                resultList.add("$packageName, $totalTimeInMins, $date\n")

            }
        }
    } else {
        Toast.makeText(context, "Usage Stats permission is required", Toast.LENGTH_LONG).show()
        context.requestUsageStatsPermission()

    }
    return resultList
}
// check if i have write permission



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
                    Greeting("Bob")
                    val resList = runContext(this, true)
                    // whatever the date is in the first entry in reslist
                    var title = resList[0].split(",")[2]
                    writeTextFileToDocuments(this, title, resList.joinToString("\n"))
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
