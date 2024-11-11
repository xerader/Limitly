package com.example.myapplication

import DailyTaskWorker
import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.myapplication.ui.theme.MyApplicationTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit


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



fun getPeaks() {
    val py = Python.getInstance()
    val pyf = py.getModule("main")
    val res = pyf.callAttr("main")

    val pyf2 = py.getModule("get_peaks")
    val res2 = pyf2.callAttr("get_peaks")
    println("HIIERERAARARA")
    println(res2)

    println(res)
}

fun getSystemTime(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date())
}


class MainActivity : ComponentActivity() {
//get the calendar date
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scheduleDailyWork()
        setContent {
            MyApplicationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                 Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { getPeaks() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Schedule Daily Peaks")
                        }
                        Button(
                            onClick = { getOldData(this@MainActivity) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Get Old Data")
                        }
                    }

                    if (!Python.isStarted()) {
                        Python.start(AndroidPlatform(this))
                    }

                    if (!readConfigFile(this)) {
                        initializeFiles(this)
                    }
                    // get data for today

//                    ShowButton { getPeaks() }

                    }
                }
            }
        }
    private fun scheduleDailyWork() {
        // Calculate the initial delay time
        val currentTimeMillis = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 11) // set desired hour
            set(Calendar.MINUTE, 59) // set desired minute
            set(Calendar.SECOND, 0)
            if (timeInMillis <= currentTimeMillis) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val initialDelay = calendar.timeInMillis - currentTimeMillis

        // Define the request
        val dailyWorkRequest = PeriodicWorkRequest.Builder(
            DailyTaskWorker::class.java,
            24,
            TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        // Schedule the work
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "DailyPeaksWork",
            ExistingPeriodicWorkPolicy.REPLACE,
            dailyWorkRequest
        )
    }
}
@Composable
fun ShowButton(onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()){
        Button(onClick = onClick,
            modifier = Modifier
                .weight(2f)
                .fillMaxWidth()
        ) {
            Text("Get Peaks") // Button label
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
