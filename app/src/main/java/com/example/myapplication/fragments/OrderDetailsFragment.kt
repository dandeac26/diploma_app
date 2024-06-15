package com.example.myapplication.fragments

import android.content.Context
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.adapters.OrderDetailsAdapter
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.views.SharedViewModel
import com.example.myapplication.views.SharedViewModelFactory
import com.facebook.shimmer.ShimmerFrameLayout

class OrderDetailsFragment : Fragment() {

    private lateinit var orderDetailsRecyclerView: RecyclerView
    private lateinit var orderDetailsAdapter: OrderDetailsAdapter

    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var shimmerViewContainer: ShimmerFrameLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_order_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val factory = SharedViewModelFactory()
        sharedViewModel = ViewModelProvider(requireActivity(), factory).get(SharedViewModel::class.java)

        sharedViewModel.selectedOrder.observe(viewLifecycleOwner) { order ->
            view.findViewById<TextView>(R.id.clientName).text = order.clientName
            view.findViewById<TextView>(R.id.totalPrice).text = order.price.toString()

            orderDetailsAdapter = OrderDetailsAdapter(order.orderDetails)
            orderDetailsRecyclerView = view.findViewById(R.id.orderDetailsRecyclerView)
            orderDetailsRecyclerView.layoutManager = LinearLayoutManager(context)
            orderDetailsRecyclerView.adapter = orderDetailsAdapter
        }

        sharedViewModel.selectedDate.observe(viewLifecycleOwner) { date ->
            view.findViewById<TextView>(R.id.dayTitle).text = date
        }
    }
}