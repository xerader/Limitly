package com.example.myapplication

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GoalSettingScreen(context: Context) {
    var goals by remember { mutableStateOf(listOf("", "", "", "", "")) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Enter Your 5 Goals:",
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
            color = Color.Black,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        goals.forEachIndexed { index, goal ->
            var goalText by remember { mutableStateOf(TextFieldValue(goal)) }
            BasicTextField(
                value = goalText,
                onValueChange = {
                    goalText = it
                    goals = goals.toMutableList().also { list ->
                        list[index] = it.text
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
                    .shadow(1.dp, RoundedCornerShape(8.dp))
                    .background(Color(0xFFBBDEFB), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                decorationBox = { innerTextField ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        if (goalText.text.isEmpty()) {
                            Text(
                                "Goal ${index + 1}",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val goalContent = goals.filter { it.isNotBlank() }.joinToString("\n")
                writeTextFileToDocuments(context, "goals.txt", goalContent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp)),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
        ) {
            Text("Save Goals", color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val actions = listOf(
                "Obtain Peak Data" to { /* getPeaks() */ },
                "Get Old Data" to { /* getOldData(context) */ },
                "Send Notification" to { (context as? MainActivity)?.sendNotification() }
            )

            actions.forEach { (label, action) ->
                Button(
                    onClick = { action() },
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp)
                        .shadow(2.dp, RoundedCornerShape(12.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Text(label, color = Color.White, textAlign = TextAlign.Center)
                }
            }
        }
    }
}