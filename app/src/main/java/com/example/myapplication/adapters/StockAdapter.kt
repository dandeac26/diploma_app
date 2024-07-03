package com.example.myapplication.adapters

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.fragments.StocksFragment
import com.example.myapplication.api.StockAPI
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StockAdapter(private val stocks: MutableList<StocksFragment.Stock>,
                   private val stockAPI: StockAPI, private val fragment: StocksFragment,
                   private val predictionMode: MutableLiveData<Boolean>,
                   private val ingredientQuantities: Map<String, Int>
) : RecyclerView.Adapter<StockAdapter.StockViewHolder>() {
    private var highlightedPosition = -1

    inner class StockViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ingredientNameTextView: TextView = itemView.findViewById(R.id.ingredientNameTextView)
        val providerNameTextView: TextView = itemView.findViewById(R.id.providerNameTextView)
        val quantityTextView: TextView = itemView.findViewById(R.id.quantityTextView)
        val priceTextView: TextView = itemView.findViewById(R.id.priceTextView)
        val stockProgressBar: ProgressBar = itemView.findViewById(R.id.stockProgressBar)
        val maxQuantityTextView: TextView = itemView.findViewById(R.id.maxQuantityTextView)
        val packagingProgressLabel: TextView = itemView.findViewById(R.id.packagingProgressLabel)
        val maxQuantityLabel: TextView = itemView.findViewById(R.id.maxQuantityLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.stock_item, parent, false)
        return StockViewHolder(itemView)
    }

    override fun getItemCount() = stocks.size

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onBindViewHolder(holder: StockViewHolder, position: Int) {
        val stock = stocks[position]
        holder.ingredientNameTextView.text = stock.ingredientName
        holder.providerNameTextView.text = stock.providerName
        holder.priceTextView.text = stock.price
        holder.packagingProgressLabel.text = stock.packaging

        predictionMode.observe(fragment.viewLifecycleOwner) { isPredictionMode ->
            if (isPredictionMode) {
                val predictedQuantity = ingredientQuantities[stock.ingredientId]
                if (predictedQuantity != null) {
                    val prediction = predictedQuantity/stock.quantityPerPackage
                    holder.stockProgressBar.progressDrawable = fragment.resources.getDrawable(R.drawable.custom_prediction_progressbar, null)
                    holder.stockProgressBar.max = stock.quantity
                    holder.stockProgressBar.progress = prediction
                    holder.quantityTextView.text = prediction.toString()
                    holder.maxQuantityTextView.text = stock.quantity.toString()
                    holder.maxQuantityLabel.text = "current quantity:"
                }else{
                    holder.stockProgressBar.progressDrawable = fragment.resources.getDrawable(R.drawable.custom_prediction_progressbar, null)
                    holder.stockProgressBar.max = stock.quantity
                    holder.stockProgressBar.progress = 0
                    holder.quantityTextView.text = "0"
                    holder.maxQuantityTextView.text = stock.quantity.toString()
                    holder.maxQuantityLabel.text = "current quantity:"
                }
            } else {
                holder.stockProgressBar.progressDrawable = fragment.resources.getDrawable(R.drawable.custom_progressbar, null)
                holder.stockProgressBar.max = stock.maxQuantity
                holder.stockProgressBar.progress = stock.quantity
                holder.quantityTextView.text = stock.quantity.toString()
                holder.maxQuantityTextView.text = stock.maxQuantity.toString()
                holder.maxQuantityLabel.text = "max:"
            }
        }

        holder.itemView.setOnLongClickListener { v ->
            showPopupMenu(v, holder.adapterPosition)
            true
        }

        holder.itemView.setOnClickListener{
            fragment.openAddStockDialog(stock, position)
        }

        if (position == highlightedPosition) {
            val colorAnimation = ObjectAnimator.ofObject(
                holder.itemView,
                "backgroundColor",
                ArgbEvaluator(),
                Color.parseColor("#333333"),
                Color.TRANSPARENT
            )
            colorAnimation.duration = 1500
            colorAnimation.start()
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    private fun showPopupMenu(view: View, position: Int) {
        val popup = PopupMenu(view.context, view)
        val inflater: MenuInflater = popup.menuInflater
        inflater.inflate(R.menu.stock_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_delete -> {
                    deleteStock(position)
                    true
                }
                R.id.action_update -> {
                    updateStock(position)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun deleteStock(position: Int) {
        val stock = stocks[position]
        AlertDialog.Builder(fragment.requireContext())
            .setTitle("Delete Stock")
            .setMessage("Are you sure you want to delete this stock?")
            .setPositiveButton("Yes") { _, _ ->
                val call = stockAPI.deleteStock(stock.ingredientId, stock.providerId)
                call.enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            stocks.removeAt(position)
                            notifyItemRemoved(position)

                            fragment.removeStockFromSearchLists(stock.ingredientId, stock.providerId)
                        } else {
                            // handle the error
                        }
                    }
                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        // handle the error
                    }
                })
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun updateStock(position: Int) {
        val stock = stocks[position]
        fragment.openAddStockDialog(stock, position)
    }
    @SuppressLint("NotifyDataSetChanged")
    fun updateStocksAfterSearch(newStocks: List<StocksFragment.Stock>) {
        stocks.clear()
        stocks.addAll(newStocks)
        notifyDataSetChanged()
    }

    fun highlightItem(position: Int) {
        if (position >= 0 && position < stocks.size) {
            highlightedPosition = position
            notifyItemChanged(position)
        } else {
            highlightedPosition = -1
        }
    }
}