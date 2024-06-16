package com.example.myapplication.fragments

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.adapters.DateItemAdapter
import com.example.myapplication.api.OrderAPI
import com.example.myapplication.config.RetrofitInstance
import com.example.myapplication.entity.OrderDTO
import com.example.myapplication.views.SharedViewModel
import com.example.myapplication.views.SharedViewModelFactory
import com.facebook.shimmer.ShimmerFrameLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class OrdersFragment : Fragment() {
    private lateinit var sharedViewModel: SharedViewModel
    private val orders = mutableListOf<Order>()
    private lateinit var orderAPI: OrderAPI
    private var webSocket: WebSocket? = null
    private lateinit var button : Button
    private lateinit var textOrders: TextView

    private lateinit var recyclerView: RecyclerView
    private val dates = mutableListOf<DateItem>()
    private lateinit var emptyView : ViewStub
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var dateItemAdapter: DateItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_orders, container, false)
    }

    @SuppressLint("DefaultLocale")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        orderAPI = RetrofitInstance.getInstance(requireContext(), 8000).create(OrderAPI::class.java)
        connectWebSocket()

        val factory = SharedViewModelFactory()
        sharedViewModel = ViewModelProvider(requireActivity(), factory).get(SharedViewModel::class.java)

        dateItemAdapter = DateItemAdapter(dates, this, sharedViewModel)

        recyclerView = view.findViewById(R.id.datesRecyclerView)
        emptyView = view.findViewById(R.id.emptyView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = dateItemAdapter


        dates.addAll(generateDateItems())

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            fetchOrders()
            swipeRefreshLayout.isRefreshing = false
        }


        sharedViewModel.refreshProductsTrigger.observe(viewLifecycleOwner) { shouldRefresh ->
            if (shouldRefresh) {
                fetchOrders()
            }
        }
        sharedViewModel.onBackPressed.observe(viewLifecycleOwner) {

        }

        val menuButton = view.findViewById<ImageButton>(R.id.menuButton)
        menuButton.setOnClickListener {
            val alphaAnimation = AlphaAnimation(1.0f, 0.5f)
            alphaAnimation.duration = 200
            alphaAnimation.repeatCount = 1
            alphaAnimation.repeatMode = Animation.REVERSE


            it.startAnimation(alphaAnimation)

            val popupMenu = PopupMenu(requireContext(), it)
            popupMenu.menuInflater.inflate(R.menu.orders_action_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.showCompleted -> {
                        // Navigate to StocksFragment
//                        (activity as MainActivity).switchFragment(StocksFragment())
                        true
                    }
                    R.id.history -> {
                        // Navigate to PredictionsFragment
                        true
                    }
                    R.id.centralizator -> {
                        // Navigate to CentralizatorFragment
                        true
                    }

                    else -> false
                }
            }
            popupMenu.show()
        }

        val button = view.findViewById<Button>(R.id.dateButton)
        button.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
                val formattedMonth = String.format("%02d", selectedMonth + 1)
                val lastTwoDigits = selectedYear.toString().takeLast(2)
                val selectedDate = "$selectedDay.$formattedMonth.$lastTwoDigits"
                Log.d("OrdersFragment", "Selected date: $selectedDate")

                // Create a Calendar instance with the selected date
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(selectedYear, selectedMonth, selectedDay)

                // Get the day of the week
                val selectedDayOfWeek = selectedCalendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())

                // Update the selectedDate in the shared ViewModel
                sharedViewModel.selectedDate.value = "$selectedDayOfWeek $selectedDate"

                // Switch to DailyOrderFragment
                switchToDailyOrderFragment(selectedDate)
            }, year, month, day)

            datePickerDialog.show()
        }
    }

     fun switchToDailyOrderFragment(selectedDate : String) {
        val dailyOrderFragment = DailyOrderFragment()
        val bundle = Bundle()
        bundle.putString("selectedDate", selectedDate)
        dailyOrderFragment.arguments = bundle
        (activity as MainActivity).switchFragment(dailyOrderFragment)
    }


    data class DateItem(
        val day: String,
        val date: String
    )
    private fun generateDateItems(): List<DateItem> {
        val dates = mutableListOf<DateItem>()
        val calendar = Calendar.getInstance()

        for (i in 0 until 14) {
            val dayOfWeek = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())
            val date = SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(calendar.time)
            dayOfWeek?.let { DateItem(it, date) }?.let { dates.add(it) }
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        return dates
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
        val price: Double,
        var completed: Boolean,
        val clientName: String,
        val clientLocation: String,
        val clientPhoneNumber: String,
        val orderDetails: List<OrderDetail>
    ) : Serializable

    data class OrderDetail(
        val orderId: String,
        val productId: String,
        val quantity: Int,
        val product: Product
    )

    data class Product(
        val productId: String,
        val name: String,
        val price: Double,
        val imageUrl: String
    )

    private fun fetchOrders() {
        activity?.runOnUiThread {
//            Log.d("OrdersFragment", "Fetching orders")
//            textOrders.text = "fetched orders"
//            textOrders.postDelayed({
//                textOrders.text = "Orders"
//            }, 1000)
        }
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