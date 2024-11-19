package com.example.myapplication
import androidx.core.app.ActivityCompat
import DailyTaskWorker
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.myapplication.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import java.io.BufferedReader
import java.io.InputStreamReader
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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



fun getPeaks(duration: String = "2") {
    val py = Python.getInstance()
    val pyf = py.getModule("main")
    val res = pyf.callAttr("main")

    val pyf2 = py.getModule("get_peaks")
    val res2 = pyf2.callAttr("get_peaks", duration)
    println("HIIERERAARARA")
    println(res2)

    println(res)
}

fun getSystemTime(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date())
}


class MainActivity : ComponentActivity() {
    private val CHANNEL_ID = "example_channel"
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        checkNotificationPermission()
        createNotificationChannel()
        // check if permission is granted
        if (!hasUsageStatsPermission()) {
            requestUsageStatsPermission()
        }

        promptEnableAccessibilityService()
        scheduleDailyWork()
        // initialize the StatsViewModel
        val statsViewModel = StatsViewModel(application)

        // If config file does not exist, create it
        if (!readConfigFile(applicationContext)) {
            (applicationContext)
        }

//        val peakData: List<PeakEntry> = readPeaksFile()
//        sendPeaksToService(peakData)

        setContent {
            MyApplicationTheme {
                var showStats by remember { mutableStateOf(false) }
                var currentScreen by remember { mutableStateOf("goals") }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column {
                        when (currentScreen) {
                            "stats" -> StatsScreen(onBack = { currentScreen = "goals" })
                            "goals" -> GoalSettingScreen(context = this@MainActivity /* Add other required parameters */)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (currentScreen == "goals") {
                            Button(
                                onClick = { currentScreen = "stats" },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("View Stats")
                            }
                        }
                    }
                }
            }
        }
    }

    data class PeakEntry(val packageName: String, val startTime: Long, val endTime: Long)


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Example Channel"
            val descriptionText = "Channel for example notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendNotification() {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Sample Notification")
            .setContentText("This is an example notification.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(this@MainActivity)) {
            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            notify(1, builder.build())
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
            }
        }
    }


    private fun promptEnableAccessibilityService() {
        Log.d("MainActivity", "Checking if Accessibility Service is enabled.")

        // Check if the MyAccessibilityService is enabled.
        if (!MyAccessibilityService.isServiceEnabled(this, MyAccessibilityService::class.java)) {
            Log.d("MainActivity", "Accessibility Service not enabled.")

            // Prompt to enable the service by opening the accessibility settings.
            // Delay is optional if needed to ensure smooth transition to the settings screen.
            handler.postDelayed({
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }, 1000)
        } else {
            Log.d("MainActivity", "Accessibility Service is already enabled.")
        }
    }

    private fun scheduleDailyWork() {
        val currentTimeMillis = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 11)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= currentTimeMillis) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val initialDelay = calendar.timeInMillis - currentTimeMillis

        val dailyWorkRequest = PeriodicWorkRequest.Builder(
            DailyTaskWorker::class.java,
            24,
            TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "DailyPeaksWork",
            ExistingPeriodicWorkPolicy.REPLACE,
            dailyWorkRequest
        )
    }
}