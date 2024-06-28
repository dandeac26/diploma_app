package com.example.myapplication.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.adapters.SensorAdapter
import com.example.myapplication.api.SensorAPI
import com.example.myapplication.config.RetrofitInstance
import java.io.Serializable


class SensorFragment : Fragment() {
    private lateinit var sensorAPI: SensorAPI

    private val sensors = listOf(
        Sensor("9920ccbf-d43e-4713-bc6d-5460375f6e81", "Sensor 1"),
        Sensor("1920ccbf-d43e-4713-bc6d-5460375f6e82", "Sensor 2"),
        // Add more sensors as needed
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sensor, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sensorAPI = RetrofitInstance.getInstance(requireContext(), 8001).create(SensorAPI::class.java)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = SensorAdapter(sensors, sensorAPI)

//        fetchSensorData()
    }

//    fun fetchSensorData(){
//        sensorAPI.getSensorData().enqueue(object : Callback<List<SensorData>> {
//            override fun onResponse(call: Call<List<SensorData>>, response: Response<List<SensorData>>) {
//                if (response.isSuccessful) {
//                    val sensorData = response.body()
//                    Log.d("SensorFragment", "Response: $sensorData")
//                } else {
//                    Log.e("SensorFragment", "Request failed with response code: ${response.code()}")
//                }
//            }
//
//            override fun onFailure(call: Call<List<SensorData>>, t: Throwable) {
//                Log.e("SensorFragment", "Request failed with error: ${t.message}")
//            }
//        })
//    }


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