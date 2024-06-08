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
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.view.animation.Animation
import android.view.animation.AnimationUtils.*
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.Spinner
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
import com.example.myapplication.api.IngredientsAPI
import com.example.myapplication.api.ProviderAPI
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
    private lateinit var ingredientsAPI: IngredientsAPI
    private lateinit var providerAPI: ProviderAPI

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
        ingredientsAPI = RetrofitInstance.getInstance(requireContext()).create(IngredientsAPI::class.java)
        providerAPI = RetrofitInstance.getInstance(requireContext()).create(ProviderAPI::class.java)

        val factory = SharedViewModelFactory()
        sharedViewModel = ViewModelProvider(requireActivity(), factory).get(SharedViewModel::class.java)

        stockAdapter = StockAdapter(stocks, stockAPI, this) // was all stocks

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
            openAddStockDialog()
        }

//        val deleteAllButton = view.findViewById<Button>(R.id.deleteAllButton)
//        deleteAllButton.setOnClickListener {
//            deleteAllStocks()
//        }
        val menuButton = view.findViewById<ImageButton>(R.id.menuButton)
        menuButton.setOnClickListener {
            val popupMenu = PopupMenu(requireContext(), it)
            popupMenu.menuInflater.inflate(R.menu.stocks_action_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.deleteAll -> {
                        deleteAllStocks()
                        true
                    }
                    R.id.clearAll -> {
                        // Implement clearAll functionality
                        true
                    }
                    R.id.clearSelected -> {
                        // Implement clearSelected functionality
                        true
                    }
                    R.id.ingredients -> {
                        // Navigate to IngredientsFragment
                        true
                    }
                    R.id.providers -> {
                        // Navigate to ProvidersFragment
                        true
                    }
                    R.id.refill -> {
                        // Implement refill functionality
                        true
                    }
                    R.id.predictions -> {
                        // Navigate to PredictionsFragment
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
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
        searchBar.setOnTouchListener { v, event ->
            val DRAWABLE_RIGHT = 2

            if (event.action == MotionEvent.ACTION_UP) {
                val drawableStart = searchBar.right - searchBar.compoundDrawables[DRAWABLE_RIGHT].bounds.width() - 50
                if (event.rawX >= drawableStart) {
                    searchBar.text.clear()
                    searchBar.clearFocus()
                    val inputMethodManager = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    inputMethodManager?.hideSoftInputFromWindow(searchBar.windowToken, 0)
                    return@setOnTouchListener true
                }
            }
            false
        }
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // No action needed here
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // No action needed here
            }

            override fun afterTextChanged(s: Editable) {
                filterStocks(s.toString())
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
        var maxQuantity: Int,
        var packaging: String
    ):Serializable

    data class Ingredient(
        val ingredientId: String,
        val name: String,
        val measurementUnit: String,
        val packaging: String
    ):Serializable

    data class Provider(
        val providerId: String,
        val name: String,
        val phoneNumber: String
    ):Serializable

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || networkCapabilities.hasTransport(
            NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    private fun filterStocks(query: String) {
        val filteredStocks = allStocks.filter { stock ->
            stock.ingredientName.contains(query, ignoreCase = true) ||
                    stock.providerName.contains(query, ignoreCase = true)
        }

        displayedStocks.clear()
        displayedStocks.addAll(filteredStocks)
        stockAdapter.updateStocksAfterSearch(displayedStocks)
    }

    fun removeStockFromSearchLists(ingredientId: String, providerId: String) {
          allStocks.removeAll { it.ingredientId == ingredientId && it.providerId == providerId }
          displayedStocks.removeAll { it.ingredientId == ingredientId && it.providerId == providerId }
    }


    fun fetchStocks(callback: (() -> Unit)? = null) {
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
                        stockAdapter.updateStocksAfterSearch(displayedStocks)

                        val searchBar = view?.findViewById<EditText>(R.id.searchBar)
                        searchBar?.text?.clear()
                    }
                }
                swipeRefreshLayout.isRefreshing = false

                recyclerView.postDelayed({
                    stockAdapter.highlightItem(-1)
                }, 500)

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
                    fetchStocks()
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
    var updatingStockPosition = -1
    @SuppressLint("InflateParams", "MissingInflatedId", "CutPasteId")
    fun openAddStockDialog(stock: Stock? = null, position: Int = -1) {
        if (!isNetworkAvailable()) {
            Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show()
            return
        }

        updatingStockPosition = position

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_stock, null)
        val builder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle(if (stock == null) "Add Stock" else "Update Stock")

        val alertDialog = builder.show()

        // populate spinner with ingredients and providers
        val ingredientSpinner = dialogView.findViewById<Spinner>(R.id.ingredientNameInput)
        val providerSpinner = dialogView.findViewById<Spinner>(R.id.providerNameInput)



        // Fetch the ingredients and set them to the ingredientSpinner
        val ingredientCall = ingredientsAPI.getIngredients()
        ingredientCall.enqueue(object : Callback<List<Ingredient>> {
            override fun onResponse(call: Call<List<Ingredient>>, response: Response<List<Ingredient>>) {
                if (response.isSuccessful) {
                    val ingredients = response.body()
                    if (ingredients != null) {
                        val ingredientNames = ingredients.map { it.name }
                        val ingredientAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, ingredientNames)
                        ingredientAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        ingredientSpinner.adapter = ingredientAdapter
                        if (stock != null) {
                            val ingredientPosition = ingredientNames.indexOf(stock.ingredientName)
                            if (ingredientPosition != -1) {
                                dialogView.findViewById<Spinner>(R.id.ingredientNameInput)
                                    .setSelection(ingredientPosition)
                            }
                        }
                    }
                }
            }
            override fun onFailure(call: Call<List<Ingredient>>, t: Throwable) {
                // Handle the error
            }
        })

        // Fetch the providers and set them to the providerSpinner
        val providerCall = providerAPI.getProviders()
        providerCall.enqueue(object : Callback<List<Provider>> {
            override fun onResponse(call: Call<List<Provider>>, response: Response<List<Provider>>) {
                if (response.isSuccessful) {
                    val providers = response.body()
                    if (providers != null) {
                        val providerNames = providers.map { it.name }
                        val providerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, providerNames)
                        providerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        providerSpinner.adapter = providerAdapter
                        if (stock != null) {
                            val providerPosition = providerNames.indexOf(stock.providerName)
                            if (providerPosition != -1) {
                                dialogView.findViewById<Spinner>(R.id.providerNameInput).setSelection(providerPosition)
                            }
                        }
                    }
                }
            }
            override fun onFailure(call: Call<List<Provider>>, t: Throwable) {
                // Handle the error
            }
        })

        if (stock != null) {
            ingredientSpinner.isEnabled = false
            providerSpinner.isEnabled = false

            dialogView.findViewById<EditText>(R.id.quantityInput).setText(stock.quantity.toString())
            dialogView.findViewById<EditText>(R.id.priceInput).setText(stock.price)
            dialogView.findViewById<EditText>(R.id.maxQuantityInput).setText(stock.maxQuantity.toString())
        }

        dialogView.findViewById<Button>(R.id.saveButton).setOnClickListener {
            val ingredientNameString = dialogView.findViewById<Spinner>(R.id.ingredientNameInput).selectedItem.toString()
            val providerNameString = dialogView.findViewById<Spinner>(R.id.providerNameInput).selectedItem.toString()
            val quantityString = dialogView.findViewById<EditText>(R.id.quantityInput).text.toString()
            val priceString = dialogView.findViewById<EditText>(R.id.priceInput).text.toString()
            val maxQuantityString = dialogView.findViewById<EditText>(R.id.maxQuantityInput).text.toString()

            if (quantityString.isEmpty()) {
                Toast.makeText(context, "Quantity cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (priceString.isEmpty()) {
                Toast.makeText(context, "Price cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (maxQuantityString.isEmpty()) {
                Toast.makeText(context, "Max quantity cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                val quantityInt = quantityString.toInt()
                val price = priceString.toDouble()
                val maxQuantity = maxQuantityString.toInt()
            } catch (e: NumberFormatException) {
                Toast.makeText(context, "Input number is too large", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val quantityInt = quantityString.toInt()
            val priceDouble = priceString.toDouble()
            val maxQuantityInt = maxQuantityString.toInt()


            val ingredientId = allStocks.find { it.ingredientName == ingredientNameString }?.ingredientId
            if (ingredientId == null) {
                Toast.makeText(context, "Ingredient not found", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val providerId = allStocks.find { it.providerName == providerNameString }?.providerId
            if (providerId == null) {
                Toast.makeText(context, "Provider not found", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newStock = StockDTO(ingredientId, providerId, quantityInt, priceDouble, maxQuantityInt)

            if (stock == null) {
                addStock(newStock) { errorMessage ->
                    if (errorMessage != null) {
                        val errorMessageTextView =
                            dialogView.findViewById<TextView>(R.id.errorMessage)
                        errorMessageTextView.text = errorMessage
                        errorMessageTextView.visibility = View.VISIBLE
                    } else {
                        Toast.makeText(context, "Stock added successfully", Toast.LENGTH_SHORT).show()
                        alertDialog.dismiss()
                    }
                }
            } else {
                val oldStock = stocks.find { it.ingredientId == ingredientId && it.providerId == providerId }

                if (oldStock != null && newStock.providerId == oldStock.providerId && oldStock.quantity == quantityInt && oldStock.price == priceDouble.toString() && oldStock.maxQuantity == maxQuantityInt) {
                    alertDialog.dismiss()
                    return@setOnClickListener
                }
                updateStock(newStock.ingredientId, newStock.providerId, newStock) { errorMessage ->
                    if (errorMessage != null) {
                        val errorMessageTextView =
                            dialogView.findViewById<TextView>(R.id.errorMessage)
                        errorMessageTextView.text = errorMessage
                        errorMessageTextView.visibility = View.VISIBLE
                    } else {
                        alertDialog.dismiss()
                    }
                }
            }
        }
        dialogView.findViewById<Button>(R.id.cancelButton).setOnClickListener {
            alertDialog.dismiss()
        }
    }

    private fun deleteAllStocks(){
        AlertDialog.Builder(requireContext()).apply {
            setTitle("Delete All Stocks")
            setMessage("Are you sure you want to delete all stocks?")

            setPositiveButton("Yes") { dialog, _ ->
                val call = stockAPI.deleteAllStocks()
                call.enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            fetchStocks()
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
                    fetchStocks()
                    // notify the user the update was successful
                    Toast.makeText(context, "Stock updated successfully", Toast.LENGTH_SHORT).show()
                } else {
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
}