package com.example.watermonitoring

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val messageBody = sms.messageBody
                Log.d("SmsReceiver", "Received SMS: $messageBody")

                // Process SMS content
                processReceivedSms(context, messageBody)
            }
        }
    }

    private fun processReceivedSms(context: Context?, message: String) {
        val parts = message.split(",").map { it.trim() }
        if (parts.size == 4) {
            val villageName = parts[0]
            val eColiStatus = if (parts[1] == "1") "E. coli - Present" else "E. coli - Absent"
            val coliformStatus = if (parts[2] == "1") "Coliform - Present" else "Coliform - Absent"
            val timestamp = parts[3]

            val newAlert = AlertLog(villageName, eColiStatus, coliformStatus, timestamp)
            AlertLogStorage.addLog(newAlert)

            Log.d("SmsReceiver", "Alert added: $newAlert")
        } else {
            Log.e("SmsReceiver", "Invalid SMS format: $message")
        }
    }
}
