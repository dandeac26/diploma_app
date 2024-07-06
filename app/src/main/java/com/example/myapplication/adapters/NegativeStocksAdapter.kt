package com.example.myapplication.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.fragments.HomeFragment

class NegativeStocksAdapter(private var negativeStocks: List<HomeFragment.NegativeStock>) : RecyclerView.Adapter<NegativeStocksAdapter.NegativeStockViewHolder>() {

    class NegativeStockViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ingredientIdTextView: TextView = itemView.findViewById(R.id.ingredientIdTextView)
        val remainingQuantityTextView: TextView = itemView.findViewById(R.id.remainingQuantityTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NegativeStockViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.negative_stock_item, parent, false)
        return NegativeStockViewHolder(itemView)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: NegativeStockViewHolder, position: Int) {
        val negativeStock = negativeStocks[position]
        holder.ingredientIdTextView.text = negativeStock.ingredientName
        holder.remainingQuantityTextView.text = "missing: ${kotlin.math.abs(negativeStock.remainingQuantity)} ${negativeStock.packaging}"
    }

    override fun getItemCount() = negativeStocks.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newNegativeStocks: List<HomeFragment.NegativeStock>) {
        negativeStocks = newNegativeStocks
        notifyDataSetChanged()
    }
}