package com.example.myapplication.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.fragments.OrdersFragment
import com.example.myapplication.shared.SharedViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DateItemAdapter(private val dates: List<OrdersFragment.DateItem>, private val fragment: OrdersFragment, private val sharedViewModel: SharedViewModel) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class DateItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dayTextView: TextView = view.findViewById(R.id.dayTextView)
        val dateTextView: TextView = view.findViewById(R.id.dateTextView)
    }
    inner class LabelViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val labelTextView: TextView = view.findViewById(R.id.labelTextView)
    }

    companion object {
        private const val TYPE_DATE = 0
        private const val TYPE_LABEL = 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_DATE) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.date_item, parent, false)
            DateItemViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.label_item, parent, false)
            LabelViewHolder(view)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holderView: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == TYPE_DATE) {
            val dateItem = dates[position]
            val holder = holderView as DateItemViewHolder

            val today = SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(Calendar.getInstance().time)
            if (dateItem.date == today) {
                holder.dayTextView.text = "${dateItem.day} - TODAY"
            } else {
                holder.dayTextView.text = dateItem.day
            }

            holder.dateTextView.text = dateItem.date

            holder.itemView.setOnClickListener {
                sharedViewModel.selectedDate.value = dateItem.day + " " + dateItem.date
                fragment.switchToDailyOrderFragment(dateItem.day + " " + dateItem.date)
            }
        } else {
            (holderView as LabelViewHolder).labelTextView.text = "Next Week:"
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (dates[position].day == "NextWeek" && position != dates.size - 1) {
            TYPE_LABEL
        } else if (position != 0 && dates[position - 1].day == "NextWeek" && position != dates.size - 1) {
            TYPE_DATE
        } else {
            TYPE_DATE
        }
    }


    override fun getItemCount() = dates.size
}