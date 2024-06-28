package com.example.myapplication.api

import com.example.myapplication.fragments.SensorFragment
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface SensorAPI {
    @GET("sensor-data")
    fun getSensorData(): Call<List<SensorFragment.SensorData>>

    @GET("sensor-data/last-hour")
    fun getSensorDataLastHour(): Call<List<SensorFragment.SensorData>>

    @GET("sensor-data/last-day")
    fun getSensorDataLastDay(): Call<List<SensorFragment.SensorData>>

    @GET("sensor-data/{sensor_id}")
    fun getSensorDataById(@Path("sensor_id") sensorId: String): Call<List<SensorFragment.SensorData>>

    @GET("sensor-data/{sensor_id}/last-day")
    fun getSensorDataByIdLastDay(@Path("sensor_id") sensorId: String): Call<List<SensorFragment.SensorData>>

    @GET("sensor-data/{sensor_id}/last-hour")
    fun getSensorDataByIdLastHour(@Path("sensor_id") sensorId: String): Call<List<SensorFragment.SensorData>>

}