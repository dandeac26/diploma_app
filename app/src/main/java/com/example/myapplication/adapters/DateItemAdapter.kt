package com.example.myapplication.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.fragments.OrdersFragment
import com.example.myapplication.fragments.StocksFragment
import com.example.myapplication.views.SharedViewModel

class DateItemAdapter(private val dates: List<OrdersFragment.DateItem>, private val fragment: OrdersFragment, private val sharedViewModel: SharedViewModel) : RecyclerView.Adapter<DateItemAdapter.DateItemViewHolder>() {

    inner class DateItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dayTextView: TextView = view.findViewById(R.id.dayTextView)
        val dateTextView: TextView = view.findViewById(R.id.dateTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.date_item, parent, false)
        return DateItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: DateItemViewHolder, position: Int) {
        val dateItem = dates[position]
        holder.dayTextView.text = dateItem.day
        holder.dateTextView.text = dateItem.date

        holder.itemView.setOnClickListener {
            sharedViewModel.selectedDate.value = dateItem.day + " " + dateItem.date
            fragment.switchToDailyOrderFragment(dateItem.day + " " + dateItem.date)
        }
    }

    override fun getItemCount() = dates.size
}