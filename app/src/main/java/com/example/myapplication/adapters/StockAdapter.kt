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
import com.example.myapplication.api.RecipeAPI
import com.example.myapplication.fragments.StocksFragment
import com.example.myapplication.api.StockAPI
import com.example.myapplication.fragments.OrdersFragment
import com.example.myapplication.fragments.ProductDetailsFragment.Recipe
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StockAdapter(private val stocks: MutableList<StocksFragment.Stock>,
                   private val stockAPI: StockAPI, private val fragment: StocksFragment,
                   private val predictionMode: MutableLiveData<Boolean>,
                   private val allShiftProducts :List<Pair<OrdersFragment.Product, Int>>,
                   private val recipeAPI: RecipeAPI) : RecyclerView.Adapter<StockAdapter.StockViewHolder>() {
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

                calculatePrediction(stock) { prediction ->
                    // If in prediction mode, change the progress bar color and value
                    Log.d("StockAdapter", "PREDICTION: $prediction")
                    holder.stockProgressBar.progressDrawable = fragment.resources.getDrawable(R.drawable.custom_prediction_progressbar, null)
                    holder.stockProgressBar.max = stock.quantity
                    holder.stockProgressBar.progress = prediction // CHANGE THIS
                    holder.quantityTextView.text = prediction.toString() // CHANGE THIS
                    holder.maxQuantityTextView.text = stock.quantity.toString()
                    holder.maxQuantityLabel.text = "current quantity:"
                }

            } else {
                // If not in prediction mode, revert to the original color and value
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

    interface RecipeCallback {
        fun onResult(recipes: List<Pair<String, Double>>)
    }


    private fun calculatePrediction(stock: StocksFragment.Stock, onResult: (Int) -> Unit) {
        val stockMaxQuantity = stock.maxQuantity
        val stockIngredientId = stock.ingredientId

        for (shiftProduct in allShiftProducts) {
            val product = shiftProduct.first
            val quantity = shiftProduct.second

            getRecipeOfProduct(product, object : RecipeCallback {
                override fun onResult(recipes: List<Pair<String, Double>>) {
                    var result = 0.0
                    for (ingredient in recipes) {
                        if (ingredient.first == stockIngredientId) {
                            result += ingredient.second * quantity
                        }
                    }
                    result /= stock.quantityPerPackage
                    val resultInt = result.toInt()
                    val finalResult = when {
                        resultInt < 0 -> 0
                        resultInt > stockMaxQuantity -> stockMaxQuantity
                        else -> resultInt
                    }
                    // Call the onResult function with the final result
                    onResult(finalResult)
                }
            })
        }
    }

    private fun getRecipeOfProduct(product: OrdersFragment.Product, callback: RecipeCallback) {
        val result = mutableListOf<Pair<String, Double>>()
        val call = recipeAPI.getRecipeOfProduct(product.productId)
        call.enqueue(object : Callback<List<Recipe>> {
            override fun onResponse(call: Call<List<Recipe>>, response: Response<List<Recipe>>) {
                if (response.isSuccessful) {
                    val recipes = response.body()
                    if (!recipes.isNullOrEmpty()) {
                        for (recipe in recipes) {
                            result.add(Pair(recipe.ingredientId, recipe.quantity))
                        }
                    }
                    callback.onResult(result)
                }
            }
            override fun onFailure(call: Call<List<Recipe>>, t: Throwable) {
                // Handle the error
            }
        })
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