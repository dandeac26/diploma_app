package com.example.myapplication.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
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
        val completeOrderButton : RadioButton = view.findViewById(R.id.completeOrderButton)
        val totalPriceLabel : TextView = view.findViewById(R.id.totalPriceLabel)
        val orderTimeTextView : TextView = view.findViewById(R.id.orderTimeTextView)
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
        holder.orderTimeTextView.text = order.completionTime
        holder.itemView.alpha = if (order.completed) 0.5f else 1.0f
        holder.completeOrderButton.isChecked = order.completed

        holder.completeOrderButton.setOnClickListener {
            if (holder.itemView.alpha == 0.5f) {
                fragment.updateOrderCompleted(order, false)
                holder.completeOrderButton.isChecked = false;
                holder.itemView.alpha = 1.0f
            } else {
                fragment.updateOrderCompleted(order, true)
                holder.completeOrderButton.isChecked = true;
                holder.itemView.alpha = 0.5f
            }
        }
        holder.itemView.setOnClickListener {
            fragment.switchToOrderDetailsFragment(order, sharedViewModel.selectedDate.value.toString())
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