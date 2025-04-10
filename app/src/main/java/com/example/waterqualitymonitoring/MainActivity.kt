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
import android.Manifest
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
class MainActivity : ComponentActivity() {
    private lateinit var smsReceiver: SmsReceiver
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Request SMS permission
        requestSmsPermission()

        setContent {
            val navController = rememberNavController()
            NavigationComponent(navController)
        }
    }


    private fun requestSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECEIVE_SMS), 1)
        }
    }
}

// Data class for alert logs
data class AlertLog(
    val villageName: String,
    val eColiStatus: String,
    val coliformStatus: String,
    val timestamp: String
)

// Global object to store alert logs for each village
object AlertLogStorage {
    private val alertLogsByVillage = mutableMapOf<String, MutableList<AlertLog>>()

    // Initialize with sample data for multiple villages
    init {
        // Sample data for Baan Mae Hat
        val banMaeHatLogs = mutableListOf(
            AlertLog("Ban Mae Hat", "E. coli - Present", "Coliform - Present", "2025-03-12 14:30"),
            AlertLog("Ban Mae Hat", "E. coli - Absent", "Coliform - Present", "2025-03-10 10:20"),
            AlertLog("Ban Mae Hat", "E. coli - Present", "Coliform - Absent", "2025-03-07 09:15"),
            AlertLog("Ban Mae Hat", "E. coli - Absent", "Coliform - Absent", "2025-03-02 11:45"),
            AlertLog("Ban Mae Hat", "E. coli - Present", "Coliform - Present", "2025-02-26 08:05")
        )
        alertLogsByVillage["Ban Mae Hat"] = banMaeHatLogs

    }

    // Get logs for a specific village
    fun getLogsForVillage(villageName: String): List<AlertLog> {
        return alertLogsByVillage[villageName] ?: emptyList()
    }

    // Get all logs
    fun getAllLogs(): List<AlertLog> {
        return alertLogsByVillage.values.flatten()
    }

    // Add a new log
    fun addLog(log: AlertLog) {
        val logsForVillage = alertLogsByVillage.getOrPut(log.villageName) { mutableListOf() }
        logsForVillage.add(log)
    }
}

@Composable
fun NavigationComponent(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "alerts") {
        composable("alerts") {
            AlertsScreen(navController)
        }
        composable(
            "alert_logs/{villageName}",
            arguments = listOf(
                navArgument("villageName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val villageName = backStackEntry.arguments?.getString("villageName") ?: "Unknown"
            AlertLogsScreen(navController, villageName)
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
        // **App Logo and Name**
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Load and display the logo
            Image(
                painter = painterResource(id = R.drawable.logo), // Make sure your logo is in res/drawable
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(100.dp) // Adjust the size as needed
                    .padding(bottom = 8.dp)
            )
            // App Name
            Text(
                text = "KHT - Water Quality Monitoring",
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search village name (English or Thai)") },
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
                                    // Encode the village name for navigation
                                    val encodedVillageName = java.net.URLEncoder.encode(village.name, "UTF-8")
                                    navController.navigate("alert_logs/$encodedVillageName")
                                }
                        ) {
                            Text(text = village.name_th ?: "", fontSize = 20.sp) // Thai Name
                            Text(text = village.name, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary) // English Name
                        }

                        // Detail button
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
fun AlertLogsScreen(navController: NavHostController, villageName: String) {
    // Decode the village name
    val decodedVillageName = java.net.URLDecoder.decode(villageName, "UTF-8")

    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var isLatestFirst by remember { mutableStateOf(true) }
    val context = LocalContext.current

    // Get logs for the specific village
    val alertLogs = remember { AlertLogStorage.getLogsForVillage(decodedVillageName) }

    // Get Thai name for the village if available
    val thaiName = remember {
        val villageList = readVillageData(context)
        villageList.find { it.name == decodedVillageName }?.name_th ?: ""
    }

    Column(modifier = Modifier.padding(16.dp)) {
        // Row for the "Alert Logs - Station 1" text and info button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Water Logs - $decodedVillageName",
                fontSize = 24.sp,
                modifier = Modifier.weight(1f)
            )
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
                    val data = generateAlertCSV(decodedVillageName)
                    shareData(context, data, decodedVillageName)
                },
                modifier = Modifier.weight(1f) // Allow buttons to fill available space
            ) {
                Text(text = "Export Data")
            }
        }

        // Sort Alerts
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val parsedLogs = alertLogs.mapNotNull { log ->
            val date = dateFormat.parse(log.timestamp)
            if (date != null) Pair(log, date) else null
        }

        val sortedLogs = if (isLatestFirst) {
            parsedLogs.sortedByDescending { it.second }
        } else {
            parsedLogs.sortedBy { it.second }
        }.map { it.first }

        // Filtered List
        val filteredLogs = sortedLogs.filter { log ->
            log.eColiStatus.contains(searchQuery.text, ignoreCase = true) ||
                    log.coliformStatus.contains(searchQuery.text, ignoreCase = true) ||
                    log.timestamp.contains(searchQuery.text, ignoreCase = true)
        }

        // Display Alerts
        if (filteredLogs.isEmpty()) {
            // Show a message if no logs are available
            Text(
                text = "No alert logs available for this village",
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 32.dp).align(Alignment.CenterHorizontally)
            )
        } else {
            LazyColumn {
                items(filteredLogs) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = log.villageName, fontSize = 20.sp) // Station
                            Text(text = log.eColiStatus, fontSize = 18.sp) // E. coli Status
                            Text(text = log.coliformStatus, fontSize = 18.sp) // Coliform Status
                            Text(text = "Timestamp: ${log.timestamp}", fontSize = 14.sp) // Time
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StationInfoScreen(villageName: String, villageNameTh: String, location: String) {
    val decodedVillageName = java.net.URLDecoder.decode(villageName, "UTF-8")
    val decodedVillageNameTh = java.net.URLDecoder.decode(villageNameTh, "UTF-8")
    val decodedLocation = java.net.URLDecoder.decode(location, "UTF-8")

    var isFiltered by remember { mutableStateOf(true) } // Toggle between filtered and unfiltered QR codes

    val qrCodeImage = if (isFiltered) {
        generateQRCode("$decodedVillageName - filtered")
    } else {
        generateQRCode("$decodedVillageName - unfiltered")
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Village Information",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Village Name: $decodedVillageName", fontSize = 20.sp)
        Text(text = "Thai Name: $decodedVillageNameTh", fontSize = 20.sp)
        Text(text = "Coordinates: $decodedLocation", fontSize = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))

        // Center the buttons by using fillMaxWidth and Arrangement.Center
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { isFiltered = true }) {
                Text("Filtered")
            }
            Spacer(modifier = Modifier.width(10.dp)) // Add space between buttons
            Button(onClick = { isFiltered = false }) {
                Text("Unfiltered")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            qrCodeImage?.let {
                Image(bitmap = it, contentDescription = "QR Code for $decodedVillageName")
            }
        }
    }
}


// Updated Function to Generate CSV Data for a specific village
fun generateAlertCSV(villageName: String): String {
    val logs = AlertLogStorage.getLogsForVillage(villageName)

    // Create CSV header
    val header = "Village Name, E. coli Status, Coliform Status, Timestamp"

    // Create CSV rows for each log
    val rows = logs.map { log ->
        "${log.villageName}, ${log.eColiStatus}, ${log.coliformStatus}, ${log.timestamp}"
    }

    // Combine header and rows into a CSV string
    return listOf(header).plus(rows).joinToString("\n")
}

// Updated Function to Share Data via Email or Other Apps
fun shareData(context: Context, data: String, villageName: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_SUBJECT, "Water Monitoring Alert Data - $villageName")
        putExtra(Intent.EXTRA_TEXT, "Here is the exported data for $villageName:\n\n$data")
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

