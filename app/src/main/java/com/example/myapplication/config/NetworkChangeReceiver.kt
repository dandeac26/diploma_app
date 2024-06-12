package com.example.myapplication.config

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

//class NetworkChangeReceiver(private val onNetworkChange: () -> Unit) : BroadcastReceiver() {
//    override fun onReceive(context: Context, intent: Intent) {
//        if (isNetworkChanged(context)) {
//            onNetworkChange()
//        }
//    }

class NetworkChangeReceiver(private val onNetworkChange: () -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (isNetworkChanged(context)) {
            CoroutineScope(Dispatchers.IO).launch {
                onNetworkChange()
            }
        }
    }

    private fun isNetworkChanged(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            return networkInfo.type == ConnectivityManager.TYPE_WIFI || networkInfo.type == ConnectivityManager.TYPE_MOBILE
        }
    }
}