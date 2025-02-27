package com.example.waterqualitymonitoring

data class WaterAlert(
    val id: Long,
    val stationNumber: Int,
    val eColi: String,
    val coliform: String,
    val timestamp: Long,
    val rawMessage: String
)