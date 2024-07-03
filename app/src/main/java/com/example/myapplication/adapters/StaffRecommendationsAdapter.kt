package com.example.myapplication.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.fragments.HomeFragment

class StaffRecommendationsAdapter(private val recommendations: List<HomeFragment.StaffRecommendation>) :
    RecyclerView.Adapter<StaffRecommendationsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val roleTextView: TextView = view.findViewById(R.id.roleTextView)
        val seniorityTextView: TextView = view.findViewById(R.id.seniorityTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.staff_recommendation_item, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recommendation = recommendations[position]

        when (recommendation.role) {
            HomeFragment.Role.HEADER_MORNING -> {
                holder.roleTextView.text = "Morning Shift:"
                holder.seniorityTextView.text = ""
                holder.itemView.setBackgroundColor(holder.itemView.resources.getColor(R.color.home_employee_header))
            }
            HomeFragment.Role.HEADER_CURRENT_SHIFT -> {
                holder.roleTextView.text = "Current Shift:"
                holder.seniorityTextView.text = ""
                holder.itemView.setBackgroundColor(holder.itemView.resources.getColor(R.color.home_employee_header))
            }
            else -> {
                holder.roleTextView.text = recommendation.role.toString()
                holder.seniorityTextView.text = recommendation.seniority.toString()
            }
        }
    }

    override fun getItemCount() = recommendations.size
}