package com.example.waterqualitymonitoring

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_NAME = "WaterMonitorDB"
        private const val DATABASE_VERSION = 1
        private const val TABLE_ALERTS = "alerts"

        private const val KEY_ID = "id"
        private const val KEY_STATION = "station_number"
        private const val KEY_ECOLI = "ecoli"
        private const val KEY_COLIFORM = "coliform"
        private const val KEY_TIMESTAMP = "timestamp"
        private const val KEY_RAW_MESSAGE = "raw_message"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_ALERTS (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_STATION INTEGER,
                $KEY_ECOLI TEXT,
                $KEY_COLIFORM TEXT,
                $KEY_TIMESTAMP INTEGER,
                $KEY_RAW_MESSAGE TEXT
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ALERTS")
        onCreate(db)
    }

    fun addAlert(alert: WaterAlert): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_STATION, alert.stationNumber)
            put(KEY_ECOLI, alert.eColi)
            put(KEY_COLIFORM, alert.coliform)
            put(KEY_TIMESTAMP, alert.timestamp)
            put(KEY_RAW_MESSAGE, alert.rawMessage)
        }
        return db.insert(TABLE_ALERTS, null, values)
    }

    fun getAlerts(fromDate: Long = 0, toDate: Long = Long.MAX_VALUE, station: Int? = null): List<WaterAlert> {
        val alerts = mutableListOf<WaterAlert>()
        var selection = "$KEY_TIMESTAMP BETWEEN ? AND ?"
        val selectionArgs = mutableListOf(fromDate.toString(), toDate.toString())

        if (station != null) {
            selection += " AND $KEY_STATION = ?"
            selectionArgs.add(station.toString())
        }

        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_ALERTS,
            null,
            selection,
            selectionArgs.toTypedArray(),
            null,
            null,
            "$KEY_TIMESTAMP DESC"
        )

        with(cursor) {
            while (moveToNext()) {
                alerts.add(
                    WaterAlert(
                        getLong(getColumnIndexOrThrow(KEY_ID)),
                        getInt(getColumnIndexOrThrow(KEY_STATION)),
                        getString(getColumnIndexOrThrow(KEY_ECOLI)),
                        getString(getColumnIndexOrThrow(KEY_COLIFORM)),
                        getLong(getColumnIndexOrThrow(KEY_TIMESTAMP)),
                        getString(getColumnIndexOrThrow(KEY_RAW_MESSAGE))
                    )
                )
            }
        }
        cursor.close()
        return alerts
    }
}