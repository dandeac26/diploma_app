//package com.example.myapplication.adapters
//
//import android.os.Parcel
//import android.os.Parcelable
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.ProgressBar
//import android.widget.TextView
//import androidx.recyclerview.widget.RecyclerView
//import com.example.myapplication.R
//import com.example.myapplication.fragments.StocksFragment
//
//class StockAdapter(private var stocks: List<StocksFragment.Stock>) : RecyclerView.Adapter<StockAdapter.StockViewHolder>() {
//
//    inner class StockViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        val ingredientNameTextView: TextView = itemView.findViewById(R.id.ingredientNameTextView)
//        val remainingQuantityTextView: TextView = itemView.findViewById(R.id.remainingQuantityTextView)
//        val stockProgressBar: ProgressBar = itemView.findViewById(R.id.stockProgressBar)
//        val estimatedTimeTextView: TextView = itemView.findViewById(R.id.estimatedTimeTextView)
//
//        fun bind(stock: StocksFragment.Stock) {
//            ingredientNameTextView.text = stock.ingredientId
//            remainingQuantityTextView.text = "Remaining Quantity: ${stock.quantity}"
//            stockProgressBar.progress = stock.quantity // You might need to calculate the percentage based on your maximum stock quantity
//            estimatedTimeTextView.text = "Estimated Time to Depletion: TODO" // You'll need to calculate this based on your data
//        }
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockViewHolder {
//        val view = LayoutInflater.from(parent.context).inflate(R.layout.stock_item, parent, false)
//        return StockViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: StockViewHolder, position: Int) {
//        holder.bind(stocks[position])
//    }
//
//    override fun getItemCount() = stocks.size
//}

package com.example.myapplication.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.fragments.StocksFragment
import com.example.myapplication.api.StockAPI
import com.example.myapplication.entity.StockDTO

class StockAdapter(private val stocks: MutableList<StocksFragment.Stock>, private val stockAPI: StockAPI, private val fragment: StocksFragment) : RecyclerView.Adapter<StockAdapter.StockViewHolder>() {

    inner class StockViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ingredientNameTextView: TextView = itemView.findViewById(R.id.ingredientNameTextView)
        val providerNameTextView: TextView = itemView.findViewById(R.id.providerNameTextView)
        val quantityTextView: TextView = itemView.findViewById(R.id.quantityTextView)
        val priceTextView: TextView = itemView.findViewById(R.id.priceTextView)
        val stockProgressBar: ProgressBar = itemView.findViewById(R.id.stockProgressBar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.stock_item, parent, false)
        return StockViewHolder(itemView)
    }

    override fun getItemCount() = stocks.size

    override fun onBindViewHolder(holder: StockViewHolder, position: Int) {
        val stock = stocks[position]
        holder.ingredientNameTextView.text = stock.ingredientName
        holder.providerNameTextView.text = stock.providerName
        holder.quantityTextView.text = stock.quantity.toString()
        holder.priceTextView.text = stock.price

        holder.stockProgressBar.max = stock.maxQuantity
        holder.stockProgressBar.progress = stock.quantity
    }
}