package com.example.myapplication.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.fragments.OrdersFragment

class OrderDetailsAdapter(private val orderDetails: List<OrdersFragment.OrderDetail>) : RecyclerView.Adapter<OrderDetailsAdapter.OrderDetailViewHolder>() {

    inner class OrderDetailViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val productNameTextView: TextView = view.findViewById(R.id.orderDetailsProductNameTextView)
        val productQuantityTextView: TextView = view.findViewById(R.id.orderDetailsProductQuantityTextView)
        val productPriceTextView: TextView = view.findViewById(R.id.orderDetailsProductPriceTextView)
        val productTotalPriceTextView: TextView = view.findViewById(R.id.orderDetailsProductTotalPriceTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderDetailViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.order_detail_item, parent, false)
        return OrderDetailViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderDetailViewHolder, position: Int) {
        val orderDetail = orderDetails[position]
        holder.productNameTextView.text = orderDetail.product.name
        holder.productQuantityTextView.text = orderDetail.quantity.toString()
        holder.productPriceTextView.text = orderDetail.product.price.toString()
        holder.productTotalPriceTextView.text = (orderDetail.product.price * orderDetail.quantity).toString()
    }

    override fun getItemCount() = orderDetails.size
}