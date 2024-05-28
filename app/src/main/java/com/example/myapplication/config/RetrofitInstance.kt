package com.example.myapplication.config

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.content.Context
import android.util.Log
import com.example.myapplication.config.ConfigLoader

//object RetrofitInstance {
//    private var retrofit: Retrofit? = null
//
//    fun getInstance(context: Context): Retrofit {
//        if (retrofit == null) {
//            val configManager = ConfigManager(context)
//            val baseUrl = if (configManager.useHomeUrl) configManager.baseUrlHome else configManager.baseUrlMobile
//            retrofit = Retrofit.Builder()
//                .baseUrl(baseUrl)
//                .addConverterFactory(GsonConverterFactory.create())
//                .build()
//        }
//        return retrofit!!
//    }
//}


//object RetrofitInstance {
//    private lateinit var retrofit: Retrofit
//
//    fun getInstance(context: Context): Retrofit {
//        val configManager = ConfigManager(context)
//        val baseUrl = if (configManager.useHomeUrl) configManager.baseUrlHome else configManager.baseUrlMobile
//        Log.d("RetrofitInstance", "baseUrl: $baseUrl")
//        retrofit = Retrofit.Builder()
//            .baseUrl(baseUrl)
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//
//        return retrofit
//    }
//}

object RetrofitInstance {
    fun getInstance(context: Context): Retrofit {
        val configManager = ConfigManager(context)
        val baseUrl = if (configManager.useHomeUrl) configManager.baseUrlHome else configManager.baseUrlMobile
        Log.d("RetrofitInstance", "baseUrl: $baseUrl")
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit
    }
}