
import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.myapplication.printEventList
import com.example.myapplication.topUsedApps
import com.example.myapplication.writeTextFileToDocuments

class DailyTaskWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        return try {
            // Generate data and write to file
            val todaysData = printEventList(applicationContext, topUsedApps(applicationContext), true, false)
            writeTextFileToDocuments(applicationContext, "data.txt", todaysData.joinToString("\n"))
            println("Daily task completed")
            // Indicate success
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            // Indicate failure in the work
            Result.failure()
        }
    }
}
