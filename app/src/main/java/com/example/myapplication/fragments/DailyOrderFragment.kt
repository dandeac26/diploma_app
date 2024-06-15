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
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.adapters.OrderAdapter
import com.example.myapplication.api.ClientAPI
import com.example.myapplication.api.OrderAPI
import com.example.myapplication.config.RetrofitInstance
import com.example.myapplication.entity.OrderDTO
import com.example.myapplication.views.SharedViewModel
import com.example.myapplication.views.SharedViewModelFactory
import com.facebook.shimmer.ShimmerFrameLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale


class DailyOrderFragment : Fragment() {

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

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        orderAPI = RetrofitInstance.getInstance(requireContext(), 8080).create(OrderAPI::class.java)



        val factory = SharedViewModelFactory()
        sharedViewModel = ViewModelProvider(requireActivity(), factory).get(SharedViewModel::class.java)


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




        orderAdapter = OrderAdapter(orders, orderAPI, this, sharedViewModel)

        orderRecyclerView = view.findViewById(R.id.ordersRecyclerView)
        emptyView = view.findViewById(R.id.emptyView)
        orderRecyclerView.layoutManager = LinearLayoutManager(context)

        sharedViewModel.refreshOrdersTrigger.observe(viewLifecycleOwner, Observer {
            orderRecyclerView.adapter = orderAdapter
        })



        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)

        swipeRefreshLayout.setOnRefreshListener {
            // Refresh orders
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





        /// SEARCH BAR LOGIC

        val searchBar = view.findViewById<EditText>(R.id.searchBar)
        searchBar.setOnTouchListener { v, event ->
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
                // No action needed here
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // No action needed here
            }

            override fun afterTextChanged(s: Editable) {
                filterOrders(s.toString())
            }
        })

    }

    private fun convertDateFormat(inputDate: String): String? {
        val originalFormat = SimpleDateFormat("dd.MM.yy", Locale.US)
        val targetFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = originalFormat.parse(inputDate)
        return date?.let { targetFormat.format(it) }
    }

    private fun filterOrders(query: String) {
        val filteredOrders = allOrders.filter { order ->
            order.clientName?.contains(query, ignoreCase = true) == true ||
                    order.clientLocation?.contains(query, ignoreCase = true) == true ||
                    order.price.toString().contains(query, ignoreCase = true)
        }

        displayedOrders.clear()
        displayedOrders.addAll(filteredOrders)
        orderAdapter.updateOrdersAfterSearch(displayedOrders)
    }

    fun removeOrderFromSearchLists(orderId: String) {
        allOrders.removeAll { it.orderId == orderId }
        displayedOrders.removeAll { it.orderId == orderId }
    }

    private fun fetchDailyOrders() {
        activity?.runOnUiThread {
            // check network connection
            if (!isNetworkAvailable()) {
                swipeRefreshLayout.isRefreshing = false
                shimmerViewContainer.visibility = View.GONE
                emptyView.visibility = View.VISIBLE
                return@runOnUiThread
            }
            // fetch orders by date



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
    data class OrderListItem(val client: String, val totalPrice: String, val completed: Boolean)


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
                    // Update the completed property of the Order after the API call is successful
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