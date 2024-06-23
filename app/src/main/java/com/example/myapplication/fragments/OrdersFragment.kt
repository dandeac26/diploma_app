package com.example.myapplication.fragments

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.Button
import android.widget.ImageButton
import android.widget.PopupMenu
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
import com.example.myapplication.views.SharedViewModel
import com.example.myapplication.views.SharedViewModelFactory

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
    private lateinit var orderAPI: OrderAPI

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
        sharedViewModel = ViewModelProvider(requireActivity(), factory)[SharedViewModel::class.java]

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
                        true
                    }
                    R.id.history -> {
                        true
                    }
                    R.id.centralizator -> {
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

                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(selectedYear, selectedMonth, selectedDay)

                val selectedDayOfWeek = selectedCalendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())

                sharedViewModel.selectedDate.value = "$selectedDayOfWeek $selectedDate"

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

            if (dayOfWeek == "Sunday") {
                dates.add(DateItem("NextWeek", ""))
            }

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

    data class OrderReq (
        val orderId: String,
        val clientId: String,
        val deliveryNeeded: Boolean,
        val completionDate: String,
        val completionTime: String,
        val price: Double,
        val completed: Boolean,
        val clientName: String,
        val clientLocation: String,
        val clientPhoneNumber: String,
        val orderDetails: Any?
    ) : Serializable

    data class OrderDetail(
        val orderId: String,
        val productId: String,
        val quantity: Int,
        val product: Product
    )

    data class OrderDetailProduct(
        val orderId: String,
        val productId: String,
        val quantity: Int,
    )

    data class Product(
        val productId: String,
        val name: String,
        val price: Double,
        val imageUrl: String
    )

    private fun fetchOrders() {
        activity?.runOnUiThread {

        }
    }
}