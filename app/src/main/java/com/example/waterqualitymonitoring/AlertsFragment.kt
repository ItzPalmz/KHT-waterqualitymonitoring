package com.example.waterqualitymonitoring

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.watermonitoring.R
import com.example.watermonitoring.databinding.FragmentAlertsBinding
import androidx.appcompat.widget.SearchView
import android.util.Log

class AlertsFragment : Fragment() {

    private var _binding: FragmentAlertsBinding? = null
    private val binding get() = _binding!!

    private lateinit var alertsAdapter: AlertsAdapter
    private var alertList = listOf<WaterAlert>()
    private var filteredAlerts = listOf<WaterAlert>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("AlertsFragment", "onCreateView called")
        _binding = FragmentAlertsBinding.inflate(inflater, container, false)

        alertList = getMockAlerts() // Load mock data or real data from source
        filteredAlerts = alertList
        alertsAdapter = AlertsAdapter(filteredAlerts)

        binding.recyclerViewAlerts.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = alertsAdapter
        }

        // Set up SearchView to filter alerts
        binding.searchViewAlerts.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterAlerts(newText)
                return true
            }
        })

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
        alertsAdapter.updateData(filteredAlerts)
    }

    private fun getMockAlerts(): List<WaterAlert> {
        return listOf(
            WaterAlert(1, 101, "Present", "Absent", 1672531200000, "High E. coli detected."),
            WaterAlert(2, 102, "Absent", "Present", 1672617600000, "Coliform level safe."),
            WaterAlert(3, 103, "Present", "Present", 1672704000000, "E. coli and Coliform detected."),
            WaterAlert(4, 104, "Absent", "Absent", 1672790400000, "All clear."),
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
