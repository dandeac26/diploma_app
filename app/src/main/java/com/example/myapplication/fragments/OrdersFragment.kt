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
import com.example.myapplication.views.SharedViewModel
import com.example.myapplication.views.SharedViewModelFactory

import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class OrdersFragment : Fragment() {

    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var recyclerView: RecyclerView
    private val dates = mutableListOf<DateItem>()
    private lateinit var emptyView : ViewStub
    private lateinit var dateItemAdapter: DateItemAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_orders, container, false)
    }

    @SuppressLint("DefaultLocale")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val factory = SharedViewModelFactory()
        sharedViewModel = ViewModelProvider(requireActivity(), factory)[SharedViewModel::class.java]

        dateItemAdapter = DateItemAdapter(dates, this, sharedViewModel)

        recyclerView = view.findViewById(R.id.datesRecyclerView)
        emptyView = view.findViewById(R.id.emptyView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = dateItemAdapter

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            dates.addAll(generateDateItems())
        }

        dates.addAll(generateDateItems())

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

    private fun generateDateItems(): List<DateItem> {
        val dates = mutableListOf<DateItem>()
        val calendar = Calendar.getInstance()

        for (i in 0 until 14) {
            val dayOfWeek = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())
            val date = SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(calendar.time)
            dayOfWeek?.let { DateItem(it, date) }?.let { dates.add(it) }

            if (dayOfWeek == "Sunday" && i<13) {
                dates.add(DateItem("NextWeek", ""))
            }

            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        swipeRefreshLayout.isRefreshing = false
        return dates
    }

    data class DateItem(
        val day: String,
        val date: String
    )

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
        val clientType: String,
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
        val clientType: String,
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
}