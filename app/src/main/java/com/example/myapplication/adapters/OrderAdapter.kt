package com.example.myapplication.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.TextView
import android.widget.ToggleButton
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.api.OrderAPI
import com.example.myapplication.fragments.DailyOrderFragment
import com.example.myapplication.fragments.OrdersFragment
import com.example.myapplication.views.SharedViewModel


class OrderAdapter (private val orders: MutableList<OrdersFragment.Order>, private val orderAPI: OrderAPI,
                    private val fragment: DailyOrderFragment, private val sharedViewModel: SharedViewModel) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    inner class OrderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val orderClientNameTextView : TextView = view.findViewById(R.id.orderClientNameTextView)
        val orderTotalTextView : TextView = view.findViewById(R.id.orderTotalTextView)
        val completeOrderButton : ToggleButton = view.findViewById(R.id.completeOrderButton)
        val totalPriceLabel : TextView = view.findViewById(R.id.totalPriceLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.order_item, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]
        holder.orderClientNameTextView.text = order.clientName
        holder.orderTotalTextView.text = order.price.toString()
        holder.totalPriceLabel.text = "lei"

        holder.itemView.alpha = if (order.completed) 0.5f else 1.0f

        // Remove the OnCheckedChangeListener before setting the checked state
        holder.completeOrderButton.setOnCheckedChangeListener(null)
        holder.completeOrderButton.isChecked = order.completed

        // Re-attach the OnCheckedChangeListener after setting the checked state
        holder.completeOrderButton.setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed) {
                if (isChecked) {
                    // Perform action X when the ToggleButton is checked
                    fragment.updateOrderCompleted(order, true)
                    holder.itemView.alpha = 0.5f
                } else {
                    // Perform action Y when the ToggleButton is unchecked
                    fragment.updateOrderCompleted(order, false)
                    holder.itemView.alpha = 1.0f
                }
            }
        }
    }

    override fun getItemCount() = orders.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateOrdersAfterSearch(displayedOrders: MutableList<OrdersFragment.Order>) {
        orders.clear()
        orders.addAll(displayedOrders)
        notifyDataSetChanged()
    }
}