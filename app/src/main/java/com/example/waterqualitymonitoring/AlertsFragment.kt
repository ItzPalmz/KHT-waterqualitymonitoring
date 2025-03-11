package com.example.waterqualitymonitoring

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.watermonitoring.databinding.FragmentAlertsBinding
import android.util.Log

class AlertsFragment : Fragment() {

    private var _binding: FragmentAlertsBinding? = null
    private val binding get() = _binding!!

    private lateinit var alertsAdapter: AlertsAdapter
    private var alertList = listOf<WaterAlert>()
    private var filteredAlerts = listOf<WaterAlert>()

    private var selectedAlerts = mutableSetOf<WaterAlert>()
    private var selectAll = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("AlertsFragment", "onCreateView called")
        _binding = FragmentAlertsBinding.inflate(inflater, container, false)

        alertList = getMockAlerts()
        filteredAlerts = alertList

        alertsAdapter = AlertsAdapter(filteredAlerts.toMutableList(), ::onAlertSelected)

        binding.recyclerViewAlerts.apply {
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = false
                reverseLayout = false
            }
            adapter = alertsAdapter
        }

        // Search functionality
        binding.searchViewAlerts.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterAlerts(newText)
                return true
            }
        })

        // Select All Toggle
        binding.checkboxSelectAll.setOnCheckedChangeListener { _, isChecked ->
            selectAll = isChecked
            selectedAlerts = if (isChecked) filteredAlerts.toMutableSet() else mutableSetOf()
            alertsAdapter.updateSelected(selectedAlerts)
        }

        // Export Button Click
        binding.buttonExport.setOnClickListener {
            exportSelectedAlerts()
        }

        return binding.root
    }

    private fun filterAlerts(query: String?) {
        filteredAlerts = if (!TextUtils.isEmpty(query)) {
            alertList.filter {
                it.stationNumber.toString().contains(query!!, ignoreCase = true) ||
                        it.eColi.contains(query, ignoreCase = true) ||
                        it.coliform.contains(query, ignoreCase = true) ||
                        it.rawMessage.contains(query, ignoreCase = true)
            }
        } else {
            alertList
        }
        selectedAlerts.clear() // Reset selection when filtering
        alertsAdapter.updateData(filteredAlerts)
    }

    private fun onAlertSelected(alert: WaterAlert, isSelected: Boolean) {
        if (isSelected) {
            selectedAlerts.add(alert)
        } else {
            selectedAlerts.remove(alert)
        }
        binding.checkboxSelectAll.isChecked = selectedAlerts.size == filteredAlerts.size && filteredAlerts.isNotEmpty()
    }

    private fun exportSelectedAlerts() {
        if (selectedAlerts.isEmpty()) return

        val exportText = selectedAlerts.joinToString("\n") { alert ->
            "Station: ${alert.stationNumber}, E. coli: ${alert.eColi}, Coliform: ${alert.coliform}, Timestamp: ${alert.timestamp}, Message: ${alert.rawMessage}"
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Exported Water Alerts")
            putExtra(Intent.EXTRA_TEXT, exportText)
        }

        startActivity(Intent.createChooser(intent, "Export Data"))
    }

    private fun getMockAlerts(): List<WaterAlert> {
        return listOf(
            WaterAlert(1, 101, "Present", "Absent", 1672531200000, "High E. coli detected."),
            WaterAlert(2, 102, "Absent", "Present", 1672617600000, "Coliform level safe."),
            WaterAlert(3, 103, "Present", "Present", 1672704000000, "E. coli and Coliform detected."),
            WaterAlert(4, 104, "Absent", "Absent", 1672790400000, "All clear.")
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
