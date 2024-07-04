package com.example.myapplication.config

import android.content.Context
import android.content.SharedPreferences

class ConfigManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("config", Context.MODE_PRIVATE)

    var baseUrlHome: String
        get() = sharedPreferences.getString("baseUrlHome", "192.168.68.56") ?: ""
        set(value) = sharedPreferences.edit().putString("baseUrlHome", value).apply()

    var baseUrlMobile: String
        get() = sharedPreferences.getString("baseUrlMobile", "192.168.197.62") ?: ""
        set(value) = sharedPreferences.edit().putString("baseUrlMobile", value).apply()

    var useHomeUrl: Boolean
        get() = sharedPreferences.getBoolean("useHomeUrl", true)
        set(value) = sharedPreferences.edit().putBoolean("useHomeUrl", value).apply()

}