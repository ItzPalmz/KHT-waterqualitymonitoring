package com.example.waterqualitymonitoring

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.watermonitoring.R

class AlertsAdapter(
    private var alerts: MutableList<WaterAlert>,
    private val onAlertSelected: (WaterAlert, Boolean) -> Unit
) : RecyclerView.Adapter<AlertsAdapter.AlertViewHolder>() {

    private val selectedAlerts = mutableSetOf<WaterAlert>()

    inner class AlertViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val station: TextView = itemView.findViewById(R.id.tvStation)
        val eColi: TextView = itemView.findViewById(R.id.tvEColi)
        val coliform: TextView = itemView.findViewById(R.id.tvColiform)
        val message: TextView = itemView.findViewById(R.id.tvMessage)
        val checkBox: CheckBox = itemView.findViewById(R.id.cbSelect)
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

        // Handle checkbox state
        holder.checkBox.setOnCheckedChangeListener(null) // Prevents unwanted triggers
        holder.checkBox.isChecked = selectedAlerts.contains(alert)

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedAlerts.add(alert)
            } else {
                selectedAlerts.remove(alert)
            }
            onAlertSelected(alert, isChecked)
        }
    }

    override fun getItemCount(): Int = alerts.size

    fun updateData(newAlerts: List<WaterAlert>) {
        alerts.clear()
        alerts.addAll(newAlerts)
        notifyDataSetChanged()
    }

    fun updateSelected(selected: Set<WaterAlert>) {
        selectedAlerts.clear()
        selectedAlerts.addAll(selected)
        notifyDataSetChanged()
    }
}
