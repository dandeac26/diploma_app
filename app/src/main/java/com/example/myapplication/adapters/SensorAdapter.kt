package com.example.myapplication.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.api.SensorAPI
import com.example.myapplication.fragments.SensorFragment
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries

class SensorAdapter(
    private val sensors: List<SensorFragment.Sensor>,
    private val sensorAPI: SensorAPI
) : RecyclerView.Adapter<SensorAdapter.SensorViewHolder>() {

    inner class SensorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sensorName: TextView = itemView.findViewById(R.id.sensorName)
        val temperatureGraph: GraphView = itemView.findViewById(R.id.temperatureGraph)
        val humidityGraph: GraphView = itemView.findViewById(R.id.humidityGraph)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SensorViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.sensor_card, parent, false)
        return SensorViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: SensorViewHolder, position: Int) {
        val currentSensor = sensors[position]
        holder.sensorName.text = currentSensor.name

        // Make the API call to get sensor data
        Log.d("SensorAdapter", "Making API call with sensorId: ${currentSensor.sensorId}")

        sensorAPI.getSensorDataByIdLastDay(currentSensor.sensorId).enqueue(object : Callback<List<SensorFragment.SensorData>> {
            override fun onResponse(call: Call<List<SensorFragment.SensorData>>, response: Response<List<SensorFragment.SensorData>>) {
                if (response.isSuccessful) {
                    val sensorData = response.body()
                    // log response body
                    if (sensorData != null) {
                        Log.d("SensorAdapter", "entered sensor data {$sensorData}")
                        // Create a series of data points for the humidity graph
                        val humSeries = LineGraphSeries<DataPoint>().apply {
                            for ((index, data) in sensorData.withIndex()) {
                                this.appendData(DataPoint(index.toDouble(), data.humidity), true, sensorData.size)
                            }
                        }

                        val tempSeries = LineGraphSeries<DataPoint>().apply {
                            for ((index, data) in sensorData.withIndex()) {
                                this.appendData(DataPoint(index.toDouble(), data.temperature), true, sensorData.size)
                            }
                        }

                        with(holder.temperatureGraph) {
                            removeAllSeries()
                            addSeries(tempSeries)
                            viewport.isScalable = true
                            viewport.setScalableY(true)

                            // Set the bounds of the viewport
                            viewport.setMinX(0.0)
                            viewport.setMaxX(sensorData.size.toDouble()) // Assuming X-axis represents the index
                            viewport.setMinY(0.0)
                            viewport.setMaxY(100.0) // Assuming Y-axis represents humidity in percentage
                        }
                        // Configure the humidity graph
                        // Configure the humidity graph
                        with(holder.humidityGraph) {
                            removeAllSeries()
                            addSeries(humSeries)
                            viewport.isScalable = true
                            viewport.setScalableY(true)

                            // Set the bounds of the viewport
                            viewport.setMinX(0.0)
                            viewport.setMaxX(sensorData.size.toDouble()) // Assuming X-axis represents the index
                            viewport.setMinY(0.0)
                            viewport.setMaxY(100.0) // Assuming Y-axis represents humidity in percentage
                        }
                    }
                    else{
                        Log.e("SensorAdapter", "sensordata is null")
                    }
                }
                else{
                    // Handle the error here

                    Log.e("SensorAdapter", "Failed to get sensor data")
                }
            }

            override fun onFailure(call: Call<List<SensorFragment.SensorData>>, t: Throwable) {
                // Handle the error here
                Log.e("SensorAdapter", "API call failed", t)
            }
        })
    }

    override fun getItemCount() = sensors.size
}