package com.example.waterqualitymonitoring

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import java.util.regex.Pattern

class SMSReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            messages.forEach { smsMessage ->
                val messageBody = smsMessage.messageBody
                parseAndSaveAlert(context, messageBody)
            }
        }
    }

    private fun parseAndSaveAlert(context: Context, message: String) {
        val stationPattern = Pattern.compile("Station #(\\d+)")
        val eColiPattern = Pattern.compile("E. coli - (\\w+)")
        val coliformPattern = Pattern.compile("Coliform - (\\w+)")

        val stationMatcher = stationPattern.matcher(message)
        val eColiMatcher = eColiPattern.matcher(message)
        val coliformMatcher = coliformPattern.matcher(message)

        if (stationMatcher.find() && eColiMatcher.find() && coliformMatcher.find()) {
            val alert = WaterAlert(
                id = 0, // Database will auto-generate
                stationNumber = stationMatcher.group(1).toInt(),
                eColi = eColiMatcher.group(1),
                coliform = coliformMatcher.group(1),
                timestamp = System.currentTimeMillis(),
                rawMessage = message
            )

            val db = DatabaseHelper(context)
            db.addAlert(alert)

            // Broadcast to update UI
            context.sendBroadcast(Intent("com.watermonitoring.NEW_ALERT"))
        }
    }
}