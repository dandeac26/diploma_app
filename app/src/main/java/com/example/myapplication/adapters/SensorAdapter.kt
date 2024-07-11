package com.example.myapplication.adapters

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.api.SensorAPI
import com.example.myapplication.fragments.SensorFragment
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.LabelFormatter
import com.jjoe64.graphview.Viewport
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SensorAdapter(
    private val sensors: List<SensorFragment.Sensor>,
    private val sensorAPI: SensorAPI,
    private val loadingProgressBar: ProgressBar
) : RecyclerView.Adapter<SensorAdapter.SensorViewHolder>() {

    inner class SensorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sensorName: TextView = itemView.findViewById(R.id.sensorName)
        val temperatureGraph: GraphView = itemView.findViewById(R.id.temperatureGraph)
        val humidityGraph: GraphView = itemView.findViewById(R.id.humidityGraph)
        val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)
        val temperatureTextView: TextView = itemView.findViewById(R.id.temperatureTextView)
        val humidityTextView: TextView = itemView.findViewById(R.id.humidityTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SensorViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.sensor_card, parent, false)
        return SensorViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: SensorViewHolder, position: Int) {
        val currentSensor = sensors[position]
        holder.sensorName.text = currentSensor.name

        loadingProgressBar.visibility = View.VISIBLE
        Log.d("SensorAdapter", "Making API call with sensorId: ${currentSensor.sensorId}")

        sensorAPI.getSensorDataByIdLastDay(currentSensor.sensorId).enqueue(object : Callback<List<SensorFragment.SensorData>> {
            @SuppressLint("SetTextI18n")
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onResponse(call: Call<List<SensorFragment.SensorData>>, response: Response<List<SensorFragment.SensorData>>) {

                if (response.isSuccessful) {
                    val sensorData = response.body()
                    if (!sensorData.isNullOrEmpty()) {
                        val mostRecentData = sensorData.last()

                        holder.timestampTextView.text = "Latest reading: ${mostRecentData.timestamp}"
                        holder.temperatureTextView.text = "${mostRecentData.temperature.toInt()}°C"
                        holder.humidityTextView.text = "${mostRecentData.humidity.toInt()}%"

                        val sortedSensorData = sensorData.sortedBy { data ->
                            val timestamp = LocalDateTime.parse(data.timestamp, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                            timestamp.hour * 60.0 + timestamp.minute
                        }

                        val tempSeries = LineGraphSeries<DataPoint>().apply {
                            sortedSensorData.forEach { data ->
                                val timestamp = LocalDateTime.parse(data.timestamp, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                                val minutesFromStartOfDay = timestamp.hour * 60.0 + timestamp.minute
                                this.appendData(DataPoint(minutesFromStartOfDay, data.temperature), true, sortedSensorData.size)
                            }
                            this.color = Color.GREEN
                        }

                        val humSeries = LineGraphSeries<DataPoint>().apply {
                            sortedSensorData.forEach { data ->
                                val timestamp = LocalDateTime.parse(data.timestamp, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                                val minutesFromStartOfDay = timestamp.hour * 60.0 + timestamp.minute
                                this.appendData(DataPoint(minutesFromStartOfDay, data.humidity), true, sortedSensorData.size)
                            }
                        }

                        if (sensorData.isNotEmpty()) {
                            with(holder.temperatureGraph) {
                                removeAllSeries()
                                addSeries(tempSeries)
                                viewport.isScalable = true
                                viewport.setScalableY(true)
                                gridLabelRenderer.labelFormatter = TimeAsXAxisLabelFormatter()
                                viewport.setMinX(0.0)
                                viewport.setMaxX(24 * 60.0)
                            }

                            with(holder.humidityGraph) {
                                removeAllSeries()
                                addSeries(humSeries)
                                viewport.isScalable = true
                                viewport.setScalableY(true)
                                gridLabelRenderer.labelFormatter = TimeAsXAxisLabelFormatter()
                                viewport.setMinX(0.0)
                                viewport.setMaxX(24 * 60.0)
                            }
                        }

                        if (sortedSensorData.isNotEmpty()) {
                            val minHour = sortedSensorData.minOf { data ->
                                val timestamp = LocalDateTime.parse(data.timestamp, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                                timestamp.hour
                            }

                            val maxHour = sortedSensorData.maxOf { data ->
                                val timestamp = LocalDateTime.parse(data.timestamp, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                                timestamp.hour
                            }

                            with(holder.temperatureGraph.viewport) {
                                setMinX(minHour * 60.0)
                                setMaxX(maxHour * 60.0 + 59)
                            }

                            with(holder.humidityGraph.viewport) {
                                setMinX(minHour * 60.0)
                                setMaxX(maxHour * 60.0 + 59)
                            }
                        } else {
                            with(holder.temperatureGraph.viewport) {
                                setMinX(0.0)
                                setMaxX(24 * 60.0)
                            }

                            with(holder.humidityGraph.viewport) {
                                setMinX(0.0)
                                setMaxX(24 * 60.0)
                            }
                        }

                        with(holder.temperatureGraph.gridLabelRenderer) {
                            horizontalAxisTitle = "Hours"
                            verticalAxisTitle = "Temperature (°C)"
                            padding = 32
                        }

                        with(holder.humidityGraph.gridLabelRenderer) {
                            horizontalAxisTitle = "Hours"
                            verticalAxisTitle = "Humidity (%)"
                            padding = 32
                        }
                        loadingProgressBar.visibility = View.GONE
                    }
                    else{
                        loadingProgressBar.visibility = View.GONE
                        Toast.makeText(holder.itemView.context, "No data available for sensor ${holder.sensorName.text}", Toast.LENGTH_SHORT).show()
                        Log.e("SensorAdapter", "sensordata is null")
                    }

                }
                else{
                    loadingProgressBar.visibility = View.GONE

                    Log.e("SensorAdapter", "Failed to get sensor data")
                }

            }

            override fun onFailure(call: Call<List<SensorFragment.SensorData>>, t: Throwable) {
                // Handle the error here
                loadingProgressBar.visibility = View.GONE
                Log.e("SensorAdapter", "API call failed", t)
            }


        })

    }

    override fun getItemCount() = sensors.size

    class TimeAsXAxisLabelFormatter : LabelFormatter {
        override fun setViewport(viewport: Viewport?) {
            // No-op
        }

        @SuppressLint("DefaultLocale")
        override fun formatLabel(value: Double, isValueX: Boolean): String {
            if (isValueX) {
                val hours = value.toInt() / 60
                val minutes = value.toInt() % 60
                return String.format("%02d:%02d", hours, minutes)
            } else {
                return String.format("%.2f", value)
            }
        }
    }
}

