package com.example.watermonitoring

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val navController = rememberNavController()
            NavigationComponent(navController)
        }
    }
}

@Composable
fun NavigationComponent(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "alerts") {
        composable("alerts") {
            AlertsScreen(navController)
        }
        composable("alert_logs") {
            AlertLogsScreen()
        }
    }
}

@Composable
fun AlertsScreen(navController: NavHostController) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Water Quality Monitoring", fontSize = 24.sp)
        Spacer(modifier = Modifier.height(16.dp))

        // Only 1 Station (Example)
        Text(
            text = "Station 1",
            fontSize = 20.sp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    navController.navigate("alert_logs")
                }
                .padding(8.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun AlertLogsScreen() {
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var isLatestFirst by remember { mutableStateOf(true) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Alert Logs - Station 1", fontSize = 24.sp)
        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
            },
            label = { Text("Search Alerts") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        // Filter Button
        Button(
            onClick = { isLatestFirst = !isLatestFirst },
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text(text = if (isLatestFirst) "Show Oldest First" else "Show Latest First")
        }

        // Sample Alert Logs with Detailed Format (Replace with real data)
        val alertLogs = listOf(
            "Station - 1 | E. coli - Present | Coliform - Present | 2025-02-25 14:30",
            "Station - 1 | E. coli - Absent | Coliform - Present | 2025-02-24 10:20",
            "Station - 1 | E. coli - Present | Coliform - Absent | 2025-02-23 09:15",
            "Station - 1 | E. coli - Absent | Coliform - Absent | 2025-02-22 11:45",
            "Station - 1 | E. coli - Present | Coliform - Present | 2025-02-21 08:05"
        )

        // Parse dates and create Pair<Alert, Date>
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val parsedLogs = alertLogs.mapNotNull { log ->
            val datePart = log.substringAfterLast(" | ")
            val date = dateFormat.parse(datePart)
            if (date != null) Pair(log, date) else null
        }

        // Sort Logs by Date
        val sortedLogs = if (isLatestFirst) {
            parsedLogs.sortedByDescending { it.second }
        } else {
            parsedLogs.sortedBy { it.second }
        }.map { it.first }

        // Filtered List based on Search Query
        val filteredLogs = sortedLogs.filter {
            it.contains(searchQuery.text, ignoreCase = true)
        }

        // Display Filtered and Sorted Results
        filteredLogs.forEach { log ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = log.split(" | ")[0], fontSize = 20.sp) // Station Info
                    Text(text = log.split(" | ")[1], fontSize = 18.sp) // E. coli Status
                    Text(text = log.split(" | ")[2], fontSize = 18.sp) // Coliform Status
                    Text(
                        text = "Timestamp: ${log.split(" | ")[3]}",
                        fontSize = 14.sp
                    ) // Timestamp
                }
            }
        }
    }
}
