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
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.app.usage.UsageStatsManager
import java.util.Calendar
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Locale
import android.app.usage.UsageEvents
fun Context.hasUsageStatsPermission(): Boolean {
    val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
    return mode == AppOpsManager.MODE_ALLOWED
}

fun Context.requestUsageStatsPermission() {
    startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
}

fun getUsageStats(context: Context): List<UsageEvents.Event> {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -7) // Get stats for the last 7 days

    val usageEvents = usageStatsManager.queryEvents(cal.timeInMillis, System.currentTimeMillis())
    val events = mutableListOf<UsageEvents.Event>()
    val event = UsageEvents.Event()

    while (usageEvents.hasNextEvent()) {
        usageEvents.getNextEvent(event)
        val newEvent = UsageEvents.Event().apply {
        }
        events.add(newEvent)
    }

    return events
}

fun runContext(context: Context) {
    if (context.hasUsageStatsPermission()) {
        val usageEventsList = getUsageStats(context)
        Toast.makeText(context, "Usage Stats permission is obtained!", Toast.LENGTH_LONG).show()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        for (event in usageEventsList) {
            val date = dateFormat.format(event.timeStamp)
            println("Package: ${event.packageName}, Event Type: ${event.eventType}, Time: $date")

        }
    } else {
        Toast.makeText(context, "Usage Stats permission is required", Toast.LENGTH_LONG).show()
        context.requestUsageStatsPermission()
    }
}
class MainActivity : ComponentActivity() {
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
                    runContext(this)

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
