//package com.example.myapplication.fragments
//
//import android.annotation.SuppressLint
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.ImageView
//import android.widget.TextView
//import androidx.constraintlayout.widget.ConstraintLayout
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.Observer
//import androidx.lifecycle.ViewModelProvider
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//
//import com.example.myapplication.R
//import com.example.myapplication.adapters.ShiftProductsAdapter
//import com.example.myapplication.api.OrderAPI
//import com.example.myapplication.config.RetrofitInstance
//import com.example.myapplication.views.SharedViewModel
//import com.example.myapplication.views.SharedViewModelFactory
//
//import java.text.SimpleDateFormat
//import java.util.Calendar
//import java.util.Locale
//
//class HomeFragment : Fragment() {
//    private lateinit var sharedViewModel: SharedViewModel
//    private lateinit var shiftProductsAdapter: ShiftProductsAdapter
//    private lateinit var shiftRecycleView: RecyclerView
//
//    private lateinit var orderAPI: OrderAPI
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        return inflater.inflate(R.layout.fragment_home, container, false)
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        ///// INITIALIZATION /////
//        val factory = SharedViewModelFactory()
//        sharedViewModel = ViewModelProvider(requireActivity(), factory)[SharedViewModel::class.java]
//
//        orderAPI = RetrofitInstance.getInstance(requireContext(), 8080).create(OrderAPI::class.java)
//
//        sharedViewModel.refreshHomeTrigger.observe(viewLifecycleOwner, Observer {
////            recycleview.adapter = homeAdapter
//        })
//        //////////
//
//
//        //// HEADER ////
//        val shiftTitle: TextView = view.findViewById(R.id.shiftTitle)
//        val shiftImage: ImageView = view.findViewById(R.id.shiftImage)
//        val header: ConstraintLayout = view.findViewById(R.id.header)
//        val shiftDate: TextView = view.findViewById(R.id.shiftDate)
//
//        updateShift(shiftTitle, shiftImage, shiftDate)
//
//        header.setOnClickListener {
//            val date = convertDateFormat(shiftDate.text.toString())
//            val currentTitle = shiftTitle.text.toString()
//            if (currentTitle == "Noon Shift") {
//                shiftTitle.text = "Night Shift"
//                shiftImage.setImageResource(R.drawable.midnight_100)
//                date?.let {
//                    sharedViewModel.fetchOrdersByDate(orderAPI, it, "KINDERGARTEN")
//                    sharedViewModel.fetchOrdersByDate(orderAPI, it, "SPECIAL")
//                }
//
//            } else {
//                shiftTitle.text = "Noon Shift"
//                shiftImage.setImageResource(R.drawable.midday_100)
//
//                date?.let {
//                    sharedViewModel.fetchOrdersByDate(orderAPI, it, "REGULAR")
//                }
//            }
//        }
//
//        //////////
//
//
//        //// products ////
//        shiftRecycleView = view.findViewById(R.id.shiftRecycleView)
//        shiftRecycleView.layoutManager = LinearLayoutManager(context)
//
//        sharedViewModel.orders.observe(viewLifecycleOwner) { orders ->
//            val vShiftDate = convertDateFormat(shiftDate.text.toString())
//            val products = orders.filter { it.completionDate == vShiftDate }
//                .flatMap { it.orderDetails }
//                .groupBy { it.product }
//                .map { (product, orderDetails) -> Pair(product, orderDetails.sumOf { it.quantity }) }
//
//            shiftProductsAdapter = ShiftProductsAdapter(products)
//            shiftRecycleView.adapter = shiftProductsAdapter
//        }
//
//    }
////    private fun convertDateFormat(inputDate: String): String? {
////        if (inputDate.isBlank()) {
////            return null
////        }
////
////        val originalFormat = SimpleDateFormat("dd/MM/yy", Locale.US)
////        val targetFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
////        val date = originalFormat.parse(inputDate)
////        return date?.let { targetFormat.format(it) }
////    }
//
//    private fun convertDateFormat(inputDate: String): String? {
//        val originalFormat = SimpleDateFormat("dd/MM/yy", Locale.US)
//        val targetFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
//        val date = originalFormat.parse(inputDate)
//        return date?.let { targetFormat.format(it) }
//    }
//
//    @SuppressLint("SetTextI18n")
//    private fun updateShift(shiftTitle: TextView, shiftImage: ImageView, shiftDate: TextView) {
//        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
//
//        if (currentHour in 14..22) {
//            shiftTitle.text = "Noon Shift"
//            shiftImage.setImageResource(R.drawable.midday_100)
//        } else {
//            shiftTitle.text = "Night Shift"
//            shiftImage.setImageResource(R.drawable.midnight_100)
//        }
//
//        val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.US)
//        val currentDate = Calendar.getInstance().time
//
//        if(currentHour in 6..13) {
//            shiftDate.text = dateFormat.format(currentDate)
//        } else {
//            val nextDay = Calendar.getInstance()
//            nextDay.add(Calendar.DAY_OF_YEAR, 1)
//            shiftDate.text = dateFormat.format(nextDay.time)
//        }
//
//
//    }
//
////    @SuppressLint("SetTextI18n")
////    private fun updateShift(shiftTitle: TextView, shiftImage: ImageView, shiftDate: TextView) {
////        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
////
////        if (currentHour in 14..22) {
////            shiftTitle.text = "Noon Shift"
////            shiftImage.setImageResource(R.drawable.midday_100)
////            convertDateFormat(shiftDate.text.toString())?.let {
////                sharedViewModel.fetchOrdersByDate(orderAPI, it, "KINDERGARTEN")
////            }
////        } else {
////            shiftTitle.text = "Night Shift"
////            shiftImage.setImageResource(R.drawable.midnight_100)
////            convertDateFormat(shiftDate.text.toString())?.let {
////                sharedViewModel.fetchOrdersByDate(orderAPI, it, "REGULAR")
////            }
////        }
////
////        val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.US)
////        val currentDate = Calendar.getInstance().time
////
////        if(currentHour in 6..13) {
////            shiftDate.text = dateFormat.format(currentDate)
////        } else {
////            val nextDay = Calendar.getInstance()
////            nextDay.add(Calendar.DAY_OF_YEAR, 1)
////            shiftDate.text = dateFormat.format(nextDay.time)
////        }
////    }
//
//
//}

package com.example.myapplication.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.adapters.ShiftProductsAdapter
import com.example.myapplication.api.OrderAPI
import com.example.myapplication.config.RetrofitInstance
import com.example.myapplication.views.SharedViewModel
import com.example.myapplication.views.SharedViewModelFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomeFragment : Fragment() {
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var shiftProductsAdapter: ShiftProductsAdapter
    private lateinit var shiftRecycleView: RecyclerView
    private lateinit var orderAPI: OrderAPI
    private var isNoonShift = true
    private lateinit var loadingSpinner: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val factory = SharedViewModelFactory()
        sharedViewModel = ViewModelProvider(requireActivity(), factory)[SharedViewModel::class.java]

        orderAPI = RetrofitInstance.getInstance(requireContext(), 8080).create(OrderAPI::class.java)

        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        isNoonShift = currentHour in 14..22

        loadingSpinner = view.findViewById(R.id.loadingSpinner) // Add this line

        sharedViewModel.isLoadingOrders.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                loadingSpinner.visibility = View.VISIBLE
            } else {
                loadingSpinner.visibility = View.GONE
            }
        }

        val shiftTitle: TextView = view.findViewById(R.id.shiftTitle)
        val shiftImage: ImageView = view.findViewById(R.id.shiftImage)
        val header: ConstraintLayout = view.findViewById(R.id.header)
        val shiftDate: TextView = view.findViewById(R.id.shiftDate)
        val shiftIndicator: TextView = view.findViewById(R.id.shiftIndicator)

        updateShift(shiftTitle, shiftIndicator, shiftImage, shiftDate)

        header.setOnClickListener {
            if (sharedViewModel.isLoadingOrders.value == true) {
                return@setOnClickListener
            }
            updateShift(shiftTitle, shiftIndicator, shiftImage, shiftDate)
        }

        shiftRecycleView = view.findViewById(R.id.shiftRecycleView)
        shiftRecycleView.layoutManager = LinearLayoutManager(context)

        updateShiftRecycleView(shiftDate)

    }

    private fun convertDateFormat(inputDate: String): String? {
        if (inputDate.isBlank()) {
            return null
        }

        val originalFormat = SimpleDateFormat("dd/MM/yy", Locale.US)
        val targetFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = originalFormat.parse(inputDate)
        return date?.let { targetFormat.format(it) }
    }

    @SuppressLint("SetTextI18n")
    private fun updateShift(shiftTitle: TextView, shiftIndicator: TextView, shiftImage: ImageView, shiftDate: TextView) {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val currentDate = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.US)

        if(currentHour in 6..13) {
            shiftDate.text = dateFormat.format(currentDate)
            shiftIndicator.text = "Orders For Today"
        } else {
            val nextDay = Calendar.getInstance()
            nextDay.add(Calendar.DAY_OF_YEAR, 1)
            shiftDate.text = dateFormat.format(nextDay.time)
            shiftIndicator.text = "Orders For Tomorrow"
        }



        isNoonShift = !isNoonShift

        updateShiftRecycleView(shiftDate)

        if (isNoonShift) {
            shiftTitle.text = "Noon Shift"
            shiftImage.setImageResource(R.drawable.midday_100)
        } else {
            shiftTitle.text = "Night Shift"
            shiftImage.setImageResource(R.drawable.midnight_100)
        }
    }

    private fun updateShiftRecycleView(shiftDate: TextView){
        convertDateFormat(shiftDate.text.toString())?.let {
            sharedViewModel.fetchOrdersByDate(orderAPI,
                it
            )
        }

        sharedViewModel.orders.observe(viewLifecycleOwner) { orders ->
            val vShiftDate = convertDateFormat(shiftDate.text.toString())
            val filteredOrders = if (isNoonShift) {
                orders.filter { it.clientType == "SPECIAL" || it.clientType == "KINDERGARTEN" }
            } else {
                orders.filter { it.clientType == "REGULAR" }
            }
            val products = filteredOrders.filter { it.completionDate == vShiftDate }
                .flatMap { it.orderDetails }
                .groupBy { it.product }
                .map { (product, orderDetails) -> Pair(product, orderDetails.sumOf { it.quantity }) }

            shiftProductsAdapter = ShiftProductsAdapter(products)
            shiftRecycleView.adapter = shiftProductsAdapter
        }
    }
}