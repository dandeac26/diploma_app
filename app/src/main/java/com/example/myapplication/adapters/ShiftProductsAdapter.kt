package com.example.myapplication.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.fragments.OrdersFragment

class ShiftProductsAdapter(private val products: List<Pair<OrdersFragment.Product, Int>>) : RecyclerView.Adapter<ShiftProductsAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val productNameTextView: TextView = view.findViewById(R.id.orderDetailsProductNameTextView)
        val productQuantityTextView: TextView = view.findViewById(R.id.orderDetailsQuantityTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.order_detail_item, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]

        holder.productNameTextView.text = product.first.name
        holder.productQuantityTextView.text = product.second.toString()
    }

    override fun getItemCount() = products.size
}