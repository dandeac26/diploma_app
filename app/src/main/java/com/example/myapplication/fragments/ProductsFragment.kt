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
import android.view.animation.AnimationUtils
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
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.adapters.ProductAdapter
import com.example.myapplication.api.BakeryAPI
import com.example.myapplication.config.RetrofitInstance
import com.example.myapplication.entity.ProductDTO
import com.example.myapplication.views.SharedViewModel
import com.example.myapplication.views.SharedViewModelFactory
import com.facebook.shimmer.ShimmerFrameLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.Serializable


class ProductsFragment : Fragment(), ProductAdapter.OnProductClickListener  {

    private lateinit var productAdapter: ProductAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: ViewStub
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private val products = mutableListOf<Product>()
    private lateinit var shimmerViewContainer: ShimmerFrameLayout
    private lateinit var sharedViewModel: SharedViewModel

    private lateinit var bakeryAPI: BakeryAPI

    private val allProducts = mutableListOf<Product>()
    private val displayedProducts = mutableListOf<Product>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_products, container, false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bakeryAPI = RetrofitInstance.getInstance(requireContext()).create(BakeryAPI::class.java)

        val factory = SharedViewModelFactory()
        sharedViewModel = ViewModelProvider(requireActivity(), factory).get(SharedViewModel::class.java)

        productAdapter = ProductAdapter(products, bakeryAPI, this,  this)

        recyclerView = view.findViewById(R.id.recyclerView)
        emptyView = view.findViewById(R.id.emptyView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = productAdapter

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            fetchProducts()
        }

        shimmerViewContainer = view.findViewById(R.id.shimmer_view_container)
        shimmerViewContainer.startShimmer()


        fetchProducts()

        val addProductButton = view.findViewById<Button>(R.id.addProductButton)
        addProductButton.setOnClickListener {
            openAddProductDialog()
        }

        val deleteAllButton = view.findViewById<Button>(R.id.deleteAllButton)
        deleteAllButton.setOnClickListener {
            deleteAllProducts()
        }

        sharedViewModel.refreshProductsTrigger.observe(viewLifecycleOwner) { shouldRefresh ->
            if (shouldRefresh) {
                fetchProducts()
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
                filterProducts(s.toString())
            }
        })
    }

    data class Product(
        val productId: String,
        var name: String,
        var price: Double,
        var imageUrl: String
    ):Serializable


    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    private fun filterProducts(query: String) {
        val filteredProducts = allProducts.filter { product ->
            product.name.contains(query, ignoreCase = true) ||
                    product.price.toString().contains(query, ignoreCase = true)
        }

        displayedProducts.clear()
        displayedProducts.addAll(filteredProducts)
        productAdapter.updateProductsAfterSearch(displayedProducts)
    }

    fun removeProductFromSearchLists(productId: String) {
        allProducts.removeAll { it.productId == productId }
        displayedProducts.removeAll { it.productId == productId }
    }

//    override fun onProductClick(product: Product) {
//        val productDetailsFragment = ProductDetailsFragment().apply {
//            arguments = Bundle().apply {
//                putSerializable("product", product)
//            }
//        }
//        MainActivity.switchFragment(productDetailsFragment)
//    }
    override fun onProductClick(product: Product) {
        val productDetailsFragment = ProductDetailsFragment().apply {
            arguments = Bundle().apply {
                putSerializable("product", product)
            }
        }
//        (activity as MainActivity).addFragmentToHistory(productDetailsFragment)
        (activity as MainActivity).switchFragment(productDetailsFragment)
    }

    fun fetchProducts() {
        val call = bakeryAPI.getProducts()
        call.enqueue(object : Callback<List<Product>> {
            override fun onResponse(call: Call<List<Product>>, response: Response<List<Product>>) {
                if (response.isSuccessful) {
                    val productsResponse = response.body()
                    if (productsResponse != null) {
                        allProducts.clear()
                        allProducts.addAll(productsResponse)
                        allProducts.reverse()

                        displayedProducts.clear()
                        displayedProducts.addAll(allProducts)
                        productAdapter.updateProductsAfterSearch(displayedProducts)

                        val searchBar = view?.findViewById<EditText>(R.id.searchBar)
                        searchBar?.text?.clear()
                    }
                }
                swipeRefreshLayout.isRefreshing = false

                val fadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out)

                fadeOut.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation) {}
                    @SuppressLint("NotifyDataSetChanged")
                    override fun onAnimationEnd(animation: Animation) {
                        shimmerViewContainer.visibility = View.GONE

                        productAdapter.notifyDataSetChanged()

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
            override fun onFailure(call: Call<List<Product>>, t: Throwable) {
                Log.e("Error", t.message.toString())
                swipeRefreshLayout.isRefreshing = false
            }
        })
    }

    private fun addProduct(newProduct: ProductDTO, callback: (String?) -> Unit) {
        val call = bakeryAPI.addProduct(newProduct)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    callback(null)
                    fetchProducts()
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

    @SuppressLint("InflateParams", "MissingInflatedId")
    fun openAddProductDialog(product: Product? = null) {
        if (!isNetworkAvailable()) {
            Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show()
            return
        }
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_product, null)
        val builder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle(if (product == null) "Add Product" else "Update Product")

        val alertDialog = builder.show()

        if (product != null) {
            dialogView.findViewById<EditText>(R.id.nameInput).setText(product.name)
            dialogView.findViewById<EditText>(R.id.priceInput).setText(product.price.toString())
            dialogView.findViewById<EditText>(R.id.imageUrlInput).setText(product.imageUrl)
        }

        dialogView.findViewById<Button>(R.id.saveButton).setOnClickListener {
            val name = dialogView.findViewById<EditText>(R.id.nameInput).text.toString()
            val priceString = dialogView.findViewById<EditText>(R.id.priceInput).text.toString()

            if (priceString.isEmpty()) {
                Toast.makeText(context, "Price cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val price = priceString.toDouble()
            val imageUrl = dialogView.findViewById<EditText>(R.id.imageUrlInput).text.toString()
            val newProduct = ProductDTO(name, price, imageUrl)

            if (product == null) {
                addProduct(newProduct) { errorMessage ->
                    if (errorMessage != null) {
                        val errorMessageTextView =
                            dialogView.findViewById<TextView>(R.id.errorMessage)
                        errorMessageTextView.text = errorMessage
                        errorMessageTextView.visibility = View.VISIBLE
                    } else {
                        alertDialog.dismiss()
                    }
                }
            } else {
                updateProduct(product.productId, newProduct) { errorMessage ->
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

    private fun deleteAllProducts(){
        AlertDialog.Builder(requireContext()).apply {
            setTitle("Delete All Products")
            setMessage("Are you sure you want to delete all products?")

            setPositiveButton("Yes") { dialog, _ ->
                val call = bakeryAPI.deleteAllProducts()
                call.enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            fetchProducts()
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

    private fun updateProduct(productId: String, updatedProduct: ProductDTO, callback: (String?) -> Unit) {
        val call = bakeryAPI.updateProduct(productId, updatedProduct)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    callback(null)
                    fetchProducts()
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