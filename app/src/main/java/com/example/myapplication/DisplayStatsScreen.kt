package com.example.myapplication

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.concurrent.Executors

@Composable
fun StatsScreen(onBack: () -> Unit, statsViewModel: StatsViewModel = viewModel()) {
    val stats = statsViewModel.peakData.collectAsState()

    var selectedPackageDetails by remember { mutableStateOf("") }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Statistics",
            style = MaterialTheme.typography.headlineMedium.copy(color = Color.Black)
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(stats.value) { stat ->
                val parts = stat.split(":")

                if (parts.isNotEmpty()) {
                    val packageName = parts[0].trim()
                    val packageIcon = loadAppIcon(context, packageName)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable { selectedPackageDetails = stat }
                    ) {
                        packageIcon?.let { icon ->
                            Image(
                                bitmap = icon,
                                contentDescription = "$packageName Icon",
                                modifier = Modifier.size(48.dp)
                            )
                        } ?: Box(modifier = Modifier.size(48.dp, 48.dp), contentAlignment = Alignment.Center) {
                            Text("Icon NA", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = packageName,
                            style = MaterialTheme.typography.bodyLarge.copy(color = Color.Black),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedPackageDetails.isNotEmpty()) {
            Text(
                text = "Selected Package Details",
                style = MaterialTheme.typography.bodyLarge.copy(color = Color.Black),
                modifier = Modifier.padding(8.dp)
            )
            Text(
                text = selectedPackageDetails,
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Black),
                modifier = Modifier.padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onBack) {
            Text("Back")
        }
    }
}

private fun loadAppIcon(context: Context, packageName: String): androidx.compose.ui.graphics.ImageBitmap? {
    return try {
        val pm: PackageManager = context.packageManager
        val ai = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        val icon: Drawable = context.packageManager.getApplicationIcon(ai)
        icon.toBitmap().asImageBitmap()
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }
}