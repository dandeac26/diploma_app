package com.example.myapplication.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.myapplication.BaseActivity
import com.example.myapplication.R
import com.example.myapplication.adapters.ProductAdapter
import com.example.myapplication.api.BakeryAPI
import com.example.myapplication.entity.ProductDTO
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class ProductActivity : BaseActivity() {

    private lateinit var productAdapter: ProductAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private var products = mutableListOf<Product>()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product)

        val selectedItemId = intent.getIntExtra("selectedItemId", R.id.nav_products)
        setupBottomNavigation(selectedItemId)

        productAdapter = ProductAdapter(products, bakeryAPI, this)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = productAdapter

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            fetchProducts()
        }

        fetchProducts()

        val addProductButton = findViewById<Button>(R.id.addProductButton)
        addProductButton.setOnClickListener {
            openAddProductDialog()
        }
    }

    data class Product(
        val productId: String,
        val name: String,
        val price: Double,
        val imageUrl: String
    )

    val retrofit = Retrofit.Builder()
        .baseUrl("http://192.168.68.56:8080/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val bakeryAPI = retrofit.create(BakeryAPI::class.java)

    // call method on the API interface to make request
    fun fetchProducts() {
        val call = bakeryAPI.getProducts()
        call.enqueue(object : Callback<List<Product>> {
            override fun onResponse(call: Call<List<Product>>, response: Response<List<Product>>) {
                if (response.isSuccessful) {
                    val productsResponse = response.body()
                    if (productsResponse != null) {
                        // Update the products list and notify the adapter
                        products.clear()
                        products.addAll(productsResponse)
                        productAdapter.notifyDataSetChanged()
                    }
                }
                swipeRefreshLayout.isRefreshing = false
            }

            override fun onFailure(call: Call<List<Product>>, t: Throwable) {
                Log.e("Error", t.message.toString())
                swipeRefreshLayout.isRefreshing = false
            }
        })
    }

    fun addProduct(newProduct: ProductDTO) {
        val call = bakeryAPI.addProduct(newProduct)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    // The product was created successfully
                    // You might want to fetch the products again here to include the new one
                    fetchProducts()
                } else {
                    // Handle the error
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                // Handle the error
            }
        })
    }

    @SuppressLint("InflateParams", "MissingInflatedId")
    fun openAddProductDialog(product: Product? = null) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_product, null)
        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle(if (product == null) "Add Product" else "Update Product")

        val alertDialog = builder.show()

        // If a product is passed, fill the form with its data
        if (product != null) {
            dialogView.findViewById<EditText>(R.id.nameInput).setText(product.name)
            dialogView.findViewById<EditText>(R.id.priceInput).setText(product.price.toString())
            dialogView.findViewById<EditText>(R.id.imageUrlInput).setText(product.imageUrl)
        }

        dialogView.findViewById<Button>(R.id.saveButton).setOnClickListener {
            val name = dialogView.findViewById<EditText>(R.id.nameInput).text.toString()
            val price = dialogView.findViewById<EditText>(R.id.priceInput).text.toString().toDouble()
            val imageUrl = dialogView.findViewById<EditText>(R.id.imageUrlInput).text.toString()
            val newProduct = ProductDTO(name, price, imageUrl)

            if (product == null) {
                // If no product was passed, add a new product
                addProduct(newProduct)
            } else {
                // If a product was passed, update the existing product
                updateProduct(product.productId, newProduct)
            }
            alertDialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.cancelButton).setOnClickListener {
            alertDialog.dismiss()
        }
    }

    fun updateProduct(productId: String, updatedProduct: ProductDTO) {
        val call = bakeryAPI.updateProduct(productId, updatedProduct)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    // The product was updated successfully
                    // You might want to fetch the products again here to get the updated one
                    fetchProducts()
                } else {
                    // Handle the error
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                // Handle the error
            }
        })
    }

}