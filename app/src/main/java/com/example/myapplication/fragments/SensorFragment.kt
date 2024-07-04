package com.example.myapplication.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.myapplication.R
import com.example.myapplication.adapters.SensorAdapter
import com.example.myapplication.api.SensorAPI
import com.example.myapplication.config.RetrofitInstance
import java.io.Serializable


class SensorFragment : Fragment() {
    private lateinit var sensorAPI: SensorAPI

    private val sensors = listOf(
        Sensor("9920ccbf-d43e-4713-bc6d-5460375f6e81", "Warehouse1 Sensor"),
        Sensor("1920ccbf-d43e-4713-bc6d-5460375f6e82", "Kitchen1 Sensor"),
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sensor, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val loadingProgressBar: ProgressBar = view.findViewById(R.id.loadingProgressBar)
        val swipeRefreshLayout: SwipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)

        sensorAPI = RetrofitInstance.getInstance("http://", requireContext(), 8001).create(SensorAPI::class.java)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = SensorAdapter(sensors, sensorAPI, loadingProgressBar)

        swipeRefreshLayout.setOnRefreshListener {
            // Refresh the sensor readings here
            recyclerView.adapter = SensorAdapter(sensors, sensorAPI, loadingProgressBar)
            swipeRefreshLayout.isRefreshing = false
        }
    }

    data class SensorData(
        val sensorId: String,
        val timestamp: String,
        val temperature: Double,
        val humidity: Double
    ) : Serializable

    data class Sensor(
        val sensorId: String,
        val name: String
    )
}