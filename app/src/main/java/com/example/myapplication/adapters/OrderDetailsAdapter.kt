package com.example.myapplication.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.fragments.OrdersFragment

class OrderDetailsAdapter(private val orderDetails: List<OrdersFragment.OrderDetail>) : RecyclerView.Adapter<OrderDetailsAdapter.OrderDetailViewHolder>() {

    inner class OrderDetailViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val orderDetailsProductNameTextView: TextView = view.findViewById(R.id.orderDetailsProductNameTextView)
        val orderDetailsQuantityTextView: TextView = view.findViewById(R.id.orderDetailsQuantityTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderDetailViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.order_detail_item, parent, false)
        return OrderDetailViewHolder(view)
    }

    @SuppressLint("SetTextI18n", "DefaultLocale")
    override fun onBindViewHolder(holder: OrderDetailViewHolder, position: Int) {
        val orderDetail = orderDetails[position]
        holder.orderDetailsProductNameTextView.text = orderDetail.product.name
        holder.orderDetailsQuantityTextView.text = orderDetail.quantity.toString()
    }

    override fun getItemCount() = orderDetails.size
}