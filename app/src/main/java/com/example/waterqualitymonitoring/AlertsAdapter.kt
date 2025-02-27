package com.example.waterqualitymonitoring

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.watermonitoring.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.watermonitoring.R.id

class AlertsAdapter(private var alerts: List<WaterAlert>) :
    RecyclerView.Adapter<AlertsAdapter.AlertViewHolder>() {

    inner class AlertViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val station = itemView.findViewById<TextView>(R.id.tvStation)
        val eColi = itemView.findViewById<TextView>(R.id.tvEColi)
        val coliform = itemView.findViewById<TextView>(R.id.tvColiform)
        val message = itemView.findViewById<TextView>(R.id.tvMessage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_alert, parent, false)
        return AlertViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        val alert = alerts[position]
        holder.station.text = "Station: ${alert.stationNumber}"
        holder.eColi.text = "E. coli: ${alert.eColi}"
        holder.coliform.text = "Coliform: ${alert.coliform}"
        holder.message.text = alert.rawMessage
    }

    override fun getItemCount(): Int = alerts.size

    fun updateData(newAlerts: List<WaterAlert>) {
        alerts = newAlerts
        notifyDataSetChanged()
    }
}
