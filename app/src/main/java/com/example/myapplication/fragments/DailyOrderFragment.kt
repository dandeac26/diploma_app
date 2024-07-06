package com.example.myapplication.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.myapplication.MainActivity
import com.example.myapplication.MainActivity.Companion.CHANNEL_ID
import com.example.myapplication.R
import com.example.myapplication.adapters.OrderAdapter
import com.example.myapplication.api.OrderAPI
import com.example.myapplication.config.RetrofitInstance
import com.example.myapplication.entity.OrderDTO
import com.example.myapplication.views.SharedViewModel
import com.example.myapplication.views.SharedViewModelFactory
import com.facebook.shimmer.ShimmerFrameLayout
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale


class DailyOrderFragment : Fragment(), ClientsFragment.ClientSelectionListener {

    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var shimmerViewContainer: ShimmerFrameLayout
    private lateinit var orderRecyclerView: RecyclerView
    private lateinit var orderAdapter: OrderAdapter
    private lateinit var emptyView : ViewStub
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private lateinit var dayTitle : TextView
    
    private lateinit var orderAPI: OrderAPI

    private val orders = mutableListOf<OrdersFragment.Order>()
    private val allOrders = mutableListOf<OrdersFragment.Order>()
    private val displayedOrders = mutableListOf<OrdersFragment.Order>()

    private lateinit var dayDate : String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_daily_order, container, false)
    }

    @SuppressLint("ClickableViewAccessibility", "NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        orderAPI = RetrofitInstance.getInstance("http://", requireContext(), 8080).create(OrderAPI::class.java)
        connectWebSocket()

        val factory = SharedViewModelFactory()
        sharedViewModel = ViewModelProvider(requireActivity(), factory)[SharedViewModel::class.java]

        dayTitle = view.findViewById(R.id.dayTitle)

        sharedViewModel.selectedDate.observe(viewLifecycleOwner) { date ->
            dayTitle.text = date
            val splitDate = date.split(" ")
            if (splitDate.size > 1) {
                val originalDate = splitDate[1]
                dayDate = convertDateFormat(originalDate).toString()
            }

            fetchDailyOrders()
        }

        orderAdapter = OrderAdapter(orders, this, sharedViewModel)

        orderRecyclerView = view.findViewById(R.id.ordersRecyclerView)
        emptyView = view.findViewById(R.id.emptyView)
        orderRecyclerView.layoutManager = LinearLayoutManager(context)

        sharedViewModel.refreshOrdersTrigger.observe(viewLifecycleOwner, Observer {
            orderRecyclerView.adapter = orderAdapter
        })

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)

        swipeRefreshLayout.setOnRefreshListener {
            fetchDailyOrders()
        }

        shimmerViewContainer = view.findViewById(R.id.shimmer_view_container)
        shimmerViewContainer.startShimmer()

        sharedViewModel.refreshDailyOrdersTrigger.observe(viewLifecycleOwner) { shouldRefresh ->
            if (shouldRefresh) {
                fetchDailyOrders()
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

        val addDayOrderButton: ImageButton = view.findViewById(R.id.addDayOrderButton)
        addDayOrderButton.setOnClickListener {

            val orderDialogFragment = OrderDialogFragment().apply {
                arguments = Bundle().apply {
                    putString("selectedDateString", dayDate)
                }
            }
            (activity as MainActivity).switchFragment(orderDialogFragment)
        }

        /// SEARCH BAR LOGIC

        val searchBar = view.findViewById<EditText>(R.id.searchBar)
        searchBar.setOnTouchListener { _, event ->
            val DRAWABLE_RIGHT = 2

            if (event.action == MotionEvent.ACTION_UP) {
                val drawableStart = searchBar.right - searchBar.compoundDrawables[DRAWABLE_RIGHT].bounds.width() - 50
                if (event.rawX >= drawableStart) {
                    searchBar.text.clear()
                    searchBar.clearFocus()
                    val inputMethodManager = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    inputMethodManager?.hideSoftInputFromWindow(searchBar.windowToken, 0)
                    return@setOnTouchListener true
                }
            }
            false
        }
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // No action is needed here
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // No action needed here
            }

            override fun afterTextChanged(s: Editable) {
                filterOrders(s.toString())
            }
        })

        val backButton = view.findViewById<View>(R.id.backButton)
        backButton.setOnClickListener {
            (activity as MainActivity).switchFragment(OrdersFragment())
        }

        val showCompletedLabel = view.findViewById<TextView>(R.id.showCompletedLabel)
        showCompletedLabel.setOnClickListener {
            orderAdapter.showCompleted = !orderAdapter.showCompleted
            orderAdapter.updateOrdersAfterSearch(displayedOrders)
            val numStr = getNumCompletedOrders()
            showCompletedLabel.text = if (orderAdapter.showCompleted) "Hide completed" else "Show completed ($numStr)"
        }
    }

    private fun getNumCompletedOrders(): Int {
        return allOrders.count { it.completed }
    }

    private fun connectWebSocket() {
        val client = OkHttpClient()
        val retrofit = RetrofitInstance.getInstance("http://", requireContext(), 8000)
        val httpUrl = retrofit.baseUrl()

        val request = Request.Builder()
            .url("ws://${httpUrl.host}:${httpUrl.port}/ws")
            .build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                Log.i("WebSocket", "Connection opened")
            }

            @SuppressLint("MissingPermission")
            override fun onMessage(webSocket: WebSocket, text: String) {
                if (text == "Refetch orders") {
                    fetchDailyOrders()
                }
                else {
                    activity?.runOnUiThread {
                        Log.i("WebSocket", "Received message: $text")
                        val notificationId = 2
                        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                        val builder = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                            .setSmallIcon(R.drawable.midday_100)
                            .setContentTitle("My notification")
                            .setContentText(text)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

                        with(NotificationManagerCompat.from(requireContext())) {
                            notify(notificationId, builder.build())
                        }
                    }
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

    private fun convertDateFormat(inputDate: String): String? {
        val originalFormat = SimpleDateFormat("dd.MM.yy", Locale.US)
        val targetFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = originalFormat.parse(inputDate)
        return date?.let { targetFormat.format(it) }
    }

    private fun filterOrders(query: String) {
        val filteredOrders = allOrders.filter { order ->
            order.clientName.contains(query, ignoreCase = true) || order.clientLocation.contains(query, ignoreCase = true) || order.price.toString().contains(query, ignoreCase = true)
        }

        displayedOrders.clear()
        displayedOrders.addAll(filteredOrders)
        orderAdapter.updateOrdersAfterSearch(displayedOrders)
    }

    override fun onClientSelected(client: ClientsFragment.Client) {
        Log.d("Client", client.toString())

        sharedViewModel.selectClient(client)

        val createOrderFragment = CreateOrderFragment()
        (activity as MainActivity).switchFragment(createOrderFragment)
    }

    fun removeOrderFromSearchLists(orderId: String) {
        allOrders.removeAll { it.orderId == orderId }
        displayedOrders.removeAll { it.orderId == orderId }
    }

    fun fetchDailyOrders() {
        activity?.runOnUiThread {

            if (!isNetworkAvailable()) {
                swipeRefreshLayout.isRefreshing = false
                shimmerViewContainer.visibility = View.GONE
                emptyView.visibility = View.VISIBLE
                return@runOnUiThread
            }

            val call = orderAPI.getOrdersByDate(dayDate)
            call.enqueue(object : Callback<List<OrdersFragment.Order>> {
                override fun onResponse(
                    call: Call<List<OrdersFragment.Order>>,
                    response: Response<List<OrdersFragment.Order>>
                ) {
                    if (response.isSuccessful) {
                        val ordersResponse = response.body()
                        println("Response: $ordersResponse")
                        if (ordersResponse != null) {
                            allOrders.clear()
                            ordersResponse.forEach { allOrders.add(it) }

                            displayedOrders.clear()
                            displayedOrders.addAll(allOrders)
                            orderAdapter.updateOrdersAfterSearch(displayedOrders)

                            sharedViewModel.setOrders(allOrders)

                            val searchBar = view?.findViewById<EditText>(R.id.searchBar)
                            searchBar?.text?.clear()
                        }
                    }

                    swipeRefreshLayout.isRefreshing = false

                    val fadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out)

                    fadeOut.setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationStart(animation: Animation) {}

                        @SuppressLint("NotifyDataSetChanged")
                        override fun onAnimationEnd(animation: Animation) {
                            shimmerViewContainer.visibility = View.GONE

                            orderAdapter.notifyDataSetChanged()

                            if (orderRecyclerView.adapter?.itemCount == 0) {
                                orderRecyclerView.visibility = View.GONE
                                emptyView.visibility = View.VISIBLE
                            } else {
                                orderRecyclerView.visibility = View.VISIBLE
                                emptyView.visibility = View.GONE
                            }
                            orderRecyclerView.visibility = View.VISIBLE
                        }

                        override fun onAnimationRepeat(animation: Animation) {}
                    })
                    shimmerViewContainer.startAnimation(fadeOut)
                }

                override fun onFailure(
                    call: Call<List<OrdersFragment.Order>>,
                    t: Throwable
                ) {
                    Log.e("Error", t.message.toString())
                    swipeRefreshLayout.isRefreshing = false
                }
            })

        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || networkCapabilities.hasTransport(
            NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    fun updateOrderCompleted(order: OrdersFragment.Order, checked: Boolean) {
        val orderDTO = OrderDTO(
            clientId = order.clientId,
            deliveryNeeded = order.deliveryNeeded,
            completionDate = order.completionDate,
            completionTime = order.completionTime,
            price = order.price,
            completed = checked
        )
        Log.d("OrderDTO", orderDTO.toString())
        val call = orderAPI.updateOrder(order.orderId, orderDTO)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    order.completed = checked
                    fetchDailyOrders()
                } else {
                    // handle the error
                    Log.e("Error", "Error updating order: ${response.code()} with message : ${response.message()}")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                // handle the error
            }
        })
    }

    fun switchToOrderDetailsFragment(selectedOrder : OrdersFragment.Order, selectedDate : String) {
        sharedViewModel.selectedOrder.value = selectedOrder
        sharedViewModel.selectedDate.value = selectedDate
        (activity as MainActivity).switchFragment(OrderDetailsFragment())
    }
}