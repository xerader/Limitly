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


fun getUsageStats(context: Context): List<UsageStats> {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, cal.timeInMillis, System.currentTimeMillis())
}

fun runContext(context: Context): ArrayList<String> {
    val resultList = ArrayList<String>()
    if (context.hasUsageStatsPermission()) {
        val usageStatsList = getUsageStats(context)
        Toast.makeText(context, "Usage Stats permission is obtained!", Toast.LENGTH_LONG).show()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val statsBuilder = StringBuilder()
        for (usageStats in usageStatsList) {
            val date = dateFormat.format(usageStats.lastTimeUsed)

            // get the usageStats ouptut in a readable format

            val totalTimeInMins = usageStats.totalTimeInForeground / (1000 * 60)
            val packageName = usageStats.packageName.substringAfterLast('.')

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

    val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
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
                    val resList = runContext(this)
                    writeTextFileToDocuments(this, todayDate, resList.joinToString("\n"))
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
