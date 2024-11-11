package com.example.myapplication

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.widget.Toast


fun initializeFiles(context: Context) {
    // check if permission is granted
    if (!context.hasUsageStatsPermission()) {
        context.requestUsageStatsPermission()
        return
    } else{
        println("Permission granted")
    }


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