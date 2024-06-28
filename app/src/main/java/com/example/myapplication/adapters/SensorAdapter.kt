package com.example.myapplication.adapters

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
        sensorAPI.getSensorDataById(currentSensor.sensorId).enqueue(object : Callback<List<SensorFragment.SensorData>> {
            override fun onResponse(call: Call<List<SensorFragment.SensorData>>, response: Response<List<SensorFragment.SensorData>>) {
                if (response.isSuccessful) {
                    val sensorData = response.body()
                    if (sensorData != null) {
                        // Create a series of data points for the humidity graph
                        val series = LineGraphSeries<DataPoint>().apply {
                            for ((index, data) in sensorData.withIndex()) {
                                this.appendData(DataPoint(index.toDouble(), data.humidity), true, sensorData.size)
                            }
                        }

                        // Configure the humidity graph
                        with(holder.humidityGraph) {
                            removeAllSeries()
                            addSeries(series)
                            viewport.isScalable = true
                            viewport.setScalableY(true)
                        }
                    }
                }
            }

            override fun onFailure(call: Call<List<SensorFragment.SensorData>>, t: Throwable) {
                // Handle the error here
            }
        })
    }

    override fun getItemCount() = sensors.size
}