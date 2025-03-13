package com.example.watermonitoring

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ArrowForward
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation.NavType
import androidx.navigation.navArgument


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
            AlertLogsScreen(navController)
        }
        composable(
            "station_info/{villageName}/{villageNameTh}/{location}",
            arguments = listOf(
                navArgument("villageName") { type = NavType.StringType },
                navArgument("villageNameTh") { type = NavType.StringType },
                navArgument("location") { type = NavType.StringType; defaultValue = "Unknown" }
            )
        ) { backStackEntry ->
            val villageName = backStackEntry.arguments?.getString("villageName") ?: "Unknown"
            val villageNameTh = backStackEntry.arguments?.getString("villageNameTh") ?: "Unknown"
            val location = backStackEntry.arguments?.getString("location") ?: "Unknown"
            StationInfoScreen(villageName, villageNameTh, location)
        }
    }
}

// Extended data class to store village information
data class Village(
    val name: String,
    val name_th: String?,
    val location: String = "Unknown"
)

fun readVillageData(context: Context): List<Village> {
    val villageList = mutableListOf<Village>()
    try {
        val inputStream = context.assets.open("village_info.txt")
        inputStream.bufferedReader().useLines { lines ->
            lines.drop(1).forEach { line ->
                val columns = line.split("|").map { it.trim() }
                if (columns.size >= 7) { // Ensure we have enough columns including st_astext
                    val nameTh = columns[2] // Thai name
                    val nameEn = columns[3] // English name
                    // Extract location from st_astext (column 6)
                    val stAsText = columns[6]
                    // Extract coordinates from POINT format if available
                    val location = if (stAsText.startsWith("POINT")) {
                        stAsText.replace("POINT(", "").replace(")", "")
                    } else {
                        "Unknown"
                    }

                    if (nameEn.isNotEmpty()) {
                        villageList.add(Village(nameEn, nameTh, location))
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return villageList
}

@Composable
fun AlertsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val villageList = remember { mutableStateOf(readVillageData(context)) }
    val textFieldValueSaver = listSaver<TextFieldValue, Any>(
        save = { listOf(it.text, it.selection.start, it.selection.end) },
        restore = { TextFieldValue(it[0] as String) }
    )
    var searchQuery by rememberSaveable(stateSaver = textFieldValueSaver) {
        mutableStateOf(TextFieldValue(""))
    }
    // Filter villages based on search query
    val filteredVillages = villageList.value.filter { village ->
        village.name.contains(searchQuery.text, ignoreCase = true) ||
                (village.name_th?.contains(searchQuery.text, ignoreCase = true) ?: false)
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Search Village", fontSize = 24.sp)
        Spacer(modifier = Modifier.height(8.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Enter village name (English or Thai)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(filteredVillages) { village ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Village info section (clickable to go to logs)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    navController.navigate("alert_logs")
                                }
                        ) {
                            Text(text = village.name_th ?: "", fontSize = 20.sp) // Thai Name
                            Text(text = village.name, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary) // English Name
                        }

                        // Detail button - using Text button instead of IconButton with unavailable icon
                        Button(
                            onClick = {
                                // Encode parameters for navigation
                                val encodedName = java.net.URLEncoder.encode(village.name, "UTF-8")
                                val encodedNameTh = java.net.URLEncoder.encode(village.name_th ?: "", "UTF-8")
                                val encodedLocation = java.net.URLEncoder.encode(village.location, "UTF-8")

                                // Navigate to station info with village details
                                navController.navigate("station_info/$encodedName/$encodedNameTh/$encodedLocation")
                            }
                        ) {
                            Text("Details")
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "See village details",
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlertLogsScreen(navController: NavHostController) {
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var isLatestFirst by remember { mutableStateOf(true) }
    val context = LocalContext.current

    Column(modifier = Modifier.padding(16.dp)) {
        // Row for the "Alert Logs - Station 1" text and info button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "Alert Logs - Baan Mae Hat", fontSize = 24.sp, modifier = Modifier.weight(1f))

            // Info Button - Navigates to Station Info Screen
            IconButton(onClick = {
                navController.navigate(
                    "station_info/${java.net.URLEncoder.encode("Baan Mae Hat", "UTF-8")}/${java.net.URLEncoder.encode("บ้านแม่หาด", "UTF-8")}/98.0329864 18.792253"
                )
            }) {
                Icon(imageVector = Icons.Default.Info, contentDescription = "Info")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search Alerts") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )

        // Row for Sort Button and Export Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp) // Add space between buttons
        ) {
            // Sort Button
            Button(
                onClick = { isLatestFirst = !isLatestFirst },
                modifier = Modifier.weight(1f) // Allow buttons to fill available space
            ) {
                Text(text = if (isLatestFirst) "Show Oldest First" else "Show Latest First")
            }

            // Export Data Button
            Button(
                onClick = {
                    val data = generateAlertCSV()
                    shareData(context, data)
                },
                modifier = Modifier.weight(1f) // Allow buttons to fill available space
            ) {
                Text(text = "Export Data")
            }
        }

        // Sample Alerts
        val alertLogs = listOf(
            "Baan Mae Hat | E. coli - Present | Coliform - Present | 2025-02-25 14:30",
            "Baan Mae Hat | E. coli - Absent | Coliform - Present | 2025-02-24 10:20",
            "Baan Mae Hat | E. coli - Present | Coliform - Absent | 2025-02-23 09:15",
            "Baan Mae Hat | E. coli - Absent | Coliform - Absent | 2025-02-22 11:45",
            "Baan Mae Hat | E. coli - Present | Coliform - Present | 2025-02-21 08:05"
        )

        // Sort Alerts
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val parsedLogs = alertLogs.mapNotNull { log ->
            val datePart = log.substringAfterLast(" | ")
            val date = dateFormat.parse(datePart)
            if (date != null) Pair(log, date) else null
        }
        val sortedLogs = if (isLatestFirst) {
            parsedLogs.sortedByDescending { it.second }
        } else {
            parsedLogs.sortedBy { it.second }
        }.map { it.first }

        // Filtered List
        val filteredLogs = sortedLogs.filter { it.contains(searchQuery.text, ignoreCase = true) }

        // Display Alerts
        filteredLogs.forEach { log ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = log.split(" | ")[0], fontSize = 20.sp) // Station
                    Text(text = log.split(" | ")[1], fontSize = 18.sp) // E. coli Status
                    Text(text = log.split(" | ")[2], fontSize = 18.sp) // Coliform Status
                    Text(text = "Timestamp: ${log.split(" | ")[3]}", fontSize = 14.sp) // Time
                }
            }
        }
    }
}

@Composable
fun StationInfoScreen(villageName: String, villageNameTh: String, location: String) {
    // Decode the URL-encoded parameters
    val decodedVillageName = java.net.URLDecoder.decode(villageName, "UTF-8")
    val decodedVillageNameTh = java.net.URLDecoder.decode(villageNameTh, "UTF-8")
    val decodedLocation = java.net.URLDecoder.decode(location, "UTF-8")

    // Generate the QR code with the decoded village name
    val qrCodeImage = generateQRCode(decodedVillageName)

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Village Information", fontSize = 24.sp)
        Spacer(modifier = Modifier.height(16.dp))

        // Display decoded village information
        Text(text = "Village Name: $decodedVillageName", fontSize = 20.sp)
        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Thai Name: $decodedVillageNameTh", fontSize = 20.sp)
        Spacer(modifier = Modifier.height(8.dp))

        // Format the location coordinates more nicely
        Text(text = "Coordinates: $decodedLocation", fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "Description: ", fontSize = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))

        // Display QR Code Image at the bottom and center it
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp), // Space from the bottom
            contentAlignment = Alignment.BottomCenter
        ) {
            qrCodeImage?.let {
                Image(bitmap = it, contentDescription = "QR Code for $decodedVillageName")
            }
        }
    }
}

// Function to Generate CSV Data
fun generateAlertCSV(): String {
    val alerts = listOf(
        "Baan Mae Hat, E. coli - Present, Coliform - Present, 2025-02-25 14:30",
        "Baan Mae Hat, E. coli - Absent, Coliform - Present, 2025-02-24 10:20",
        "Baan Mae Hat, E. coli - Present, Coliform - Absent, 2025-02-23 09:15",
        "Baan Mae Hat, E. coli - Absent, Coliform - Absent, 2025-02-22 11:45",
        "Baan Mae Hat, E. coli - Present, Coliform - Present, 2025-02-21 08:05"
    )

    val header = "Station, E. coli Status, Coliform Status, Timestamp"
    return listOf(header).plus(alerts).joinToString("\n")
}

// Function to Share Data via Email or Other Apps
fun shareData(context: Context, data: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_SUBJECT, "Water Monitoring Alert Data")
        putExtra(Intent.EXTRA_TEXT, "Here is the exported data:\n\n$data")
    }
    context.startActivity(Intent.createChooser(intent, "Export Data"))
}

// Function to Generate QR Code
fun generateQRCode(data: String): ImageBitmap {
    val writer = MultiFormatWriter()
    val bitMatrix: BitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, 700, 700) // Adjust size if needed
    val width = bitMatrix.width
    val height = bitMatrix.height
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

    for (x in 0 until width) {
        for (y in 0 until height) {
            // Use AndroidColor for the pixel color values
            val color = if (bitMatrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE
            bitmap.setPixel(x, y, color)
        }
    }

    return bitmap.asImageBitmap()
}