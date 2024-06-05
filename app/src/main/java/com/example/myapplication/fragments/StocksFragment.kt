package com.example.myapplication.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.view.animation.Animation
import android.view.animation.AnimationUtils.*
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.myapplication.R
import com.example.myapplication.adapters.StockAdapter
import com.example.myapplication.api.StockAPI
import com.example.myapplication.config.RetrofitInstance
import com.example.myapplication.entity.StockDTO
import com.example.myapplication.views.SharedViewModel
import com.example.myapplication.views.SharedViewModelFactory
import com.facebook.shimmer.ShimmerFrameLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.Serializable

class StocksFragment : Fragment() {

    private lateinit var stockAdapter: StockAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: ViewStub
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private val stocks = mutableListOf<Stock>()
    private lateinit var shimmerViewContainer: ShimmerFrameLayout
    private lateinit var sharedViewModel: SharedViewModel

    private lateinit var stockAPI: StockAPI

    private val allStocks = mutableListOf<Stock>()
    private val displayedStocks = mutableListOf<Stock>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_stocks, container, false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        stockAPI = RetrofitInstance.getInstance(requireContext()).create(StockAPI::class.java)

        val factory = SharedViewModelFactory()
        sharedViewModel = ViewModelProvider(requireActivity(), factory).get(SharedViewModel::class.java)

        stockAdapter = StockAdapter(allStocks, stockAPI, this)

        recyclerView = view.findViewById(R.id.stocksRecyclerView)
        emptyView = view.findViewById(R.id.emptyView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = stockAdapter

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            fetchStocks()
        }

        shimmerViewContainer = view.findViewById(R.id.shimmer_view_container)
        shimmerViewContainer.startShimmer()

        fetchStocks()

        val addStockButton = view.findViewById<Button>(R.id.addStockButton)
        addStockButton.setOnClickListener {
//            openAddStockDialog()
        }

        val deleteAllButton = view.findViewById<Button>(R.id.deleteAllButton)
        deleteAllButton.setOnClickListener {
            deleteAllStocks()
        }

        sharedViewModel.refreshStocksTrigger.observe(viewLifecycleOwner) { shouldRefresh ->
            if (shouldRefresh) {
                fetchStocks()
            }
        }
        sharedViewModel.onBackPressed.observe(viewLifecycleOwner) {
            val searchBar = view.findViewById<EditText>(R.id.searchBar)
            if (searchBar.isFocused) {
                searchBar.clearFocus()
                val inputMethodManager = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                inputMethodManager?.hideSoftInputFromWindow(searchBar.windowToken, 0)
            }
        }

        val searchBar = view.findViewById<EditText>(R.id.searchBar)
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // No action needed here
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // No action needed here
            }

            override fun afterTextChanged(s: Editable) {
//                filterStocks(s.toString())
            }
        })
    }

    data class Stock(
        val ingredientId: String,
        val ingredientName: String,
        val providerId: String,
        val providerName: String,
        var quantity: Int,
        var price: String,
        var maxQuantity: Int
    ):Serializable

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || networkCapabilities.hasTransport(
            NetworkCapabilities.TRANSPORT_CELLULAR)
    }

//    private fun filterStocks(query: String) {
//        val filteredStocks = allStocks.filter { stock ->
//            stock.name.contains(query, ignoreCase = true) ||
//                    stock.quantity.toString().contains(query, ignoreCase = true)
//        }
//
//        displayedStocks.clear()
//        displayedStocks.addAll(filteredStocks)
//        stockAdapter.updateStocksAfterSearch(displayedStocks)
//    }

    fun removeStockFromSearchLists(ingredientId: String, providerId: String) {
//        allStocks.removeAll { it.ingredientId == stockId }
//        displayedStocks.removeAll { it.stockId == stockId }
          allStocks.removeAll { it.ingredientId == ingredientId && it.providerId == providerId }
          displayedStocks.removeAll { it.ingredientId == ingredientId && it.providerId == providerId }
    }


//    fun fetchStocks() {
//        val call = stockAPI.getStocks()
//        call.enqueue(object : Callback<Map<String, Stock>> {
//            override fun onResponse(call: Call<Map<String, Stock>>, response: Response<List<Stock>>) {
//                if (response.isSuccessful) {
//                    val stocksResponse = response.body()
//                    if (stocksResponse != null) {
//                        allStocks.clear()
//                        allStocks.addAll(stocksResponse)
//                        allStocks.reverse()
//
//                        displayedStocks.clear()
//                        displayedStocks.addAll(allStocks)
////                        stockAdapter.updateStocksAfterSearch(displayedStocks)
//
//                        val searchBar = view?.findViewById<EditText>(R.id.searchBar)
//                        searchBar?.text?.clear()
//                    }
//                }
//                swipeRefreshLayout.isRefreshing = false
//
//                val fadeOut = loadAnimation(context, R.anim.fade_out)
//
//                fadeOut.setAnimationListener(object : Animation.AnimationListener {
//                    override fun onAnimationStart(animation: Animation) {}
//                    @SuppressLint("NotifyDataSetChanged")
//                    override fun onAnimationEnd(animation: Animation) {
//                        shimmerViewContainer.visibility = View.GONE
//
//                        stockAdapter.notifyDataSetChanged()
//
//                        if (recyclerView.adapter?.itemCount == 0) {
//                            recyclerView.visibility = View.GONE
//                            emptyView.visibility = View.VISIBLE
//                        } else {
//                            recyclerView.visibility = View.VISIBLE
//                            emptyView.visibility = View.GONE
//                        }
//                        recyclerView.visibility = View.VISIBLE
//                    }
//                    override fun onAnimationRepeat(animation: Animation) {}
//                })
//                shimmerViewContainer.startAnimation(fadeOut)
//            }
//            override fun onFailure(call: Call<List<Stock>>, t: Throwable) {
//                Log.e("Error", t.message.toString())
//                swipeRefreshLayout.isRefreshing = false
//            }
//        })
//    }
fun fetchStocks() {
    val call = stockAPI.getStocks()
    call.enqueue(object : Callback<Map<String, List<Stock>>> {
        override fun onResponse(call: Call<Map<String, List<Stock>>>, response: Response<Map<String, List<Stock>>>) {
            if (response.isSuccessful) {
                val stocksResponse = response.body()
                println("Response: $stocksResponse")
                if (stocksResponse != null) {
                    allStocks.clear()
                    stocksResponse.values.flatten().forEach { allStocks.add(it) }
                    allStocks.reverse()

                    displayedStocks.clear()
                    displayedStocks.addAll(allStocks)
                    // stockAdapter.updateStocksAfterSearch(displayedStocks)

                    val searchBar = view?.findViewById<EditText>(R.id.searchBar)
                    searchBar?.text?.clear()
                }
            }
            swipeRefreshLayout.isRefreshing = false

            val fadeOut = loadAnimation(context, R.anim.fade_out)

            fadeOut.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {}
                @SuppressLint("NotifyDataSetChanged")
                override fun onAnimationEnd(animation: Animation) {
                    shimmerViewContainer.visibility = View.GONE

                    stockAdapter.notifyDataSetChanged()

                    if (recyclerView.adapter?.itemCount == 0) {
                        recyclerView.visibility = View.GONE
                        emptyView.visibility = View.VISIBLE
                    } else {
                        recyclerView.visibility = View.VISIBLE
                        emptyView.visibility = View.GONE
                    }
                    recyclerView.visibility = View.VISIBLE
                }
                override fun onAnimationRepeat(animation: Animation) {}
            })
            shimmerViewContainer.startAnimation(fadeOut)
        }
        override fun onFailure(call: Call<Map<String, List<Stock>>>, t: Throwable) {
            Log.e("Error", t.message.toString())
            swipeRefreshLayout.isRefreshing = false
        }
    })
}

    private fun addStock(newStock: StockDTO, callback: (String?) -> Unit) {
        val call = stockAPI.addStock(newStock)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    callback(null)
//                    fetchStocks()
                } else {
                    // Handle the error
                    if (response.code() == 400) {
                        callback(response.errorBody()?.string())
                    }
                }
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                // Handle the error
            }
        })
    }

//    @SuppressLint("InflateParams", "MissingInflatedId")
//    fun openAddStockDialog(stock: Stock? = null) {
//        if (!isNetworkAvailable()) {
//            Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show()
//            return
//        }
//        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_stock, null)
//        val builder = AlertDialog.Builder(requireContext())
//            .setView(dialogView)
//            .setTitle(if (stock == null) "Add Stock" else "Update Stock")
//
//        val alertDialog = builder.show()
//
//        if (stock != null) {
//            dialogView.findViewById<EditText>(R.id.nameInput).setText(stock.name)
//            dialogView.findViewById<EditText>(R.id.quantityInput).setText(stock.quantity.toString())
//            dialogView.findViewById<EditText>(R.id.priceInput).setText(stock.price)
//        }
//
//        dialogView.findViewById<Button>(R.id.saveButton).setOnClickListener {
//            val name = dialogView.findViewById<EditText>(R.id.nameInput).text.toString()
//            val quantityString = dialogView.findViewById<EditText>(R.id.quantityInput).text.toString()
//
//            if (quantityString.isEmpty()) {
//                Toast.makeText(context, "Quantity cannot be empty", Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
//
//            val quantity = quantityString.toInt()
//            val price = dialogView.findViewById<EditText>(R.id.priceInput).text.toString()
//            val newStock = StockDTO(name, quantity, price)
//
//            if (stock == null) {
//                addStock(newStock) { errorMessage ->
//                    if (errorMessage != null) {
//                        val errorMessageTextView =
//                            dialogView.findViewById<TextView>(R.id.errorMessage)
//                        errorMessageTextView.text = errorMessage
//                        errorMessageTextView.visibility = View.VISIBLE
//                    } else {
//                        alertDialog.dismiss()
//                    }
//                }
//            } else {
//                updateStock(stock.stockId, newStock) { errorMessage ->
//                    if (errorMessage != null) {
//                        val errorMessageTextView =
//                            dialogView.findViewById<TextView>(R.id.errorMessage)
//                        errorMessageTextView.text = errorMessage
//                        errorMessageTextView.visibility = View.VISIBLE
//                    } else {
//                        alertDialog.dismiss()
//                    }
//                }
//            }
//        }
//        dialogView.findViewById<Button>(R.id.cancelButton).setOnClickListener {
//            alertDialog.dismiss()
//        }
//    }

    private fun deleteAllStocks(){
        AlertDialog.Builder(requireContext()).apply {
            setTitle("Delete All Stocks")
            setMessage("Are you sure you want to delete all stocks?")

            setPositiveButton("Yes") { dialog, _ ->
                val call = stockAPI.deleteAllStocks()
                call.enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
//                            fetchStocks()
                        }
                    }
                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        // Handle the error
                    }
                })
                dialog.dismiss()
            }
            setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
        }.create().show()
    }

    private fun updateStock(ingredientId: String, providerId: String, updatedStock: StockDTO, callback: (String?) -> Unit) {
        val call = stockAPI.updateStock(ingredientId, providerId, updatedStock)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    callback(null)
//                    fetchStocks()
                } else {
                    if(response.code() == 400) {
                        callback(response.errorBody()?.string())
                    }
                }
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                // Handle the error
            }
        })
    }
}