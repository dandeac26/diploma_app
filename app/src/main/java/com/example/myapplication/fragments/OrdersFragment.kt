package com.example.myapplication.fragments

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.R
import com.example.myapplication.api.OrderAPI
import com.example.myapplication.config.RetrofitInstance
import com.example.myapplication.entity.OrderDTO
import com.example.myapplication.views.SharedViewModel
import com.example.myapplication.views.SharedViewModelFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class OrdersFragment : Fragment() {
    private lateinit var sharedViewModel: SharedViewModel
    private val orders = mutableListOf<Order>()
    private lateinit var orderAPI: OrderAPI
    private var webSocket: WebSocket? = null
    private lateinit var button : Button
    private lateinit var textOrders: TextView
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_orders, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        orderAPI = RetrofitInstance.getInstance(requireContext(), 8000).create(OrderAPI::class.java)
        connectWebSocket()

        val factory = SharedViewModelFactory()
        sharedViewModel = ViewModelProvider(requireActivity(), factory).get(SharedViewModel::class.java)

        sharedViewModel.refreshProductsTrigger.observe(viewLifecycleOwner) { shouldRefresh ->
            if (shouldRefresh) {
                fetchOrders()
            }
        }
        sharedViewModel.onBackPressed.observe(viewLifecycleOwner) {
            val searchBar = view.findViewById<EditText>(R.id.searchBar)
            if (searchBar.isFocused) {
                searchBar.clearFocus()
                val inputMethodManager = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                inputMethodManager?.hideSoftInputFromWindow(searchBar.windowToken, 0)
            }
        }

        button = view.findViewById(R.id.sendOrderButton)
        button.setOnClickListener() {
            val newOrder = OrderDTO(
                clientId = "498bb13c-2e3a-44ab-9a94-44cdc1c40699",
                deliveryNeeded = true,
                completionDate = "2021-12-31",
                completionTime = "12:00:00",
                price = 11.0
            )
            addOrder(newOrder) { error ->
                if (error != null) {
                    Log.e("OrdersFragment", "Error adding order: $error")
                }
            }
        }

        textOrders = view.findViewById(R.id.ordersTextView)
    }

    private fun connectWebSocket() {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("ws://192.168.68.56:8000/ws")
            .build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                // Connection opened
                Log.i("WebSocket", "Connection opened")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (text == "Refetch orders") {
                    fetchOrders()
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                // Handle binary messages
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                Log.i("WebSocket", "Connection closed")
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                Log.e("WebSocket", "Error: ${t.message}")
            }

        }

        client.newWebSocket(request, listener)
//        client.dispatcher.executorService.shutdown()
    }

    data class Order (
        val orderId: String,
        val clientId: String,
        val deliveryNeeded: Boolean,
        val completionDate: String,
        val completionTime: String,
        val price: Double
    )

    private fun fetchOrders() {
//        TODO("Not yet implemented")
        Log.d("OrdersFragment", "Fetching orders")
        textOrders.text = "fetched orders"
        // after 1 second set text back to Orders
        textOrders.postDelayed({
            textOrders.text = "Orders"
        }, 1000)
    }

    private fun addOrder(newOrder: OrderDTO, callback: (String?) -> Unit) {
        val call = orderAPI.addOrder(newOrder)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    callback(null)
                    fetchOrders()
                } else {
                    // Handle the error
                    if (response.code() == 400) {
                        callback(response.errorBody()?.string())
                    }
                }
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                // Handle the error
            }
        })
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || networkCapabilities.hasTransport(
            NetworkCapabilities.TRANSPORT_CELLULAR)
    }
}