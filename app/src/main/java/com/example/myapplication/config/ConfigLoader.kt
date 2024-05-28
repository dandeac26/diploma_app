package com.example.myapplication.config

import android.content.Context
import com.example.myapplication.R
import com.google.gson.Gson
import java.io.InputStreamReader

object ConfigLoader {
    fun load(context: Context): Config {
        val inputStream = context.resources.openRawResource(R.raw.config)
        val reader = InputStreamReader(inputStream)
        return Gson().fromJson(reader, Config::class.java)
    }
}