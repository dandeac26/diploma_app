package com.example.myapplication.config

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.content.Context
import android.util.Log

object RetrofitInstance {
    fun getInstance(protocol: String, context: Context, port: Int): Retrofit {
        val configManager = ConfigManager(context)
        val baseUrl = if (configManager.useHomeUrl) configManager.baseUrlHome else configManager.baseUrlMobile
        val fullUrl = "$protocol$baseUrl:$port/"
        Log.d("RetrofitInstance", "baseUrl: $fullUrl")
        val retrofit = Retrofit.Builder()
            .baseUrl(fullUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit
    }
}