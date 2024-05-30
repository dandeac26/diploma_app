package com.example.myapplication.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.adapters.ClientAdapter
import com.example.myapplication.adapters.ProductAdapter
import com.example.myapplication.api.BakeryAPI
import com.example.myapplication.config.RetrofitInstance
import com.example.myapplication.entity.ProductDTO
import com.example.myapplication.fragments.ProductsFragment.Product
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProductDetailsFragment : Fragment() {

    private lateinit var productImage: ImageView
    private lateinit var productName: TextView
    private lateinit var productPrice: TextView
    private lateinit var recipeTable: TableLayout
    private lateinit var bakeryAPI: BakeryAPI

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_product_details, container, false)

        productImage = view.findViewById(R.id.productImage)
        productName = view.findViewById(R.id.productName)
        productPrice = view.findViewById(R.id.productPrice)
        recipeTable = view.findViewById(R.id.recipeTable)

        return view
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val product = arguments?.getSerializable("product") as? Product
        if (product != null) {
            setProductDetails(product)
        }

        val backButton = view.findViewById<View>(R.id.backButton)
        backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }

        bakeryAPI = RetrofitInstance.getInstance(requireContext()).create(BakeryAPI::class.java)

        val editButton = view.findViewById<View>(R.id.editButton)
        editButton.setOnClickListener {
            openAddProductDialog(product!!)
        }

        val deleteButton = view.findViewById<View>(R.id.deleteButton)
        deleteButton.setOnClickListener {
            deleteProduct(product!!) 
        }
    }
    fun setProductDetails(product: Product) {
        Glide.with(productImage.context)
            .load(product.imageUrl)
            .into(productImage)

        productName.text = product.name
        productPrice.text = product.price.toString()

        // Populate the recipeTable with the recipe information
        // This depends on the structure of your Product and Recipe classes
    }

    @SuppressLint("InflateParams", "MissingInflatedId")
    fun openAddProductDialog(product: Product) {
        if (!isNetworkAvailable()) {
            Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show()
            return
        }
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_product, null)
        val builder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle("Edit Product")

        val alertDialog = builder.show()

        dialogView.findViewById<EditText>(R.id.nameInput).setText(product.name)
        dialogView.findViewById<EditText>(R.id.priceInput).setText(product.price.toString())
        dialogView.findViewById<EditText>(R.id.imageUrlInput).setText(product.imageUrl)

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


            updateProduct(product.productId, newProduct) { errorMessage ->
                if (errorMessage != null) {
                    val errorMessageTextView =
                        dialogView.findViewById<TextView>(R.id.errorMessage)
                    errorMessageTextView.text = errorMessage
                    errorMessageTextView.visibility = View.VISIBLE
                } else {
                    productName.text = name
                    productPrice.text = price.toString()
                    Glide.with(productImage.context)
                        .load(imageUrl)
                        .into(productImage)

                    product.name = name
                    product.price = price
                    product.imageUrl = imageUrl

                    alertDialog.dismiss()
                }
            }
        }
        dialogView.findViewById<Button>(R.id.cancelButton).setOnClickListener {
            alertDialog.dismiss()
        }
    }

    private fun updateProduct(productId: String, updatedProduct: ProductDTO, callback: (String?) -> Unit) {
        val call = bakeryAPI.updateProduct(productId, updatedProduct)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    callback(null)
//                    requireActivity().onBackPressed()
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


    private fun deleteProduct(product: Product){
        AlertDialog.Builder(requireContext()).apply {
            setTitle("Delete Product")
            setMessage("Are you sure you want to delete this product?")

            setPositiveButton("Yes") { dialog, _ ->
                val call = bakeryAPI.deleteProduct(product.productId)
                call.enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            requireActivity().onBackPressed()
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

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || networkCapabilities.hasTransport(
            NetworkCapabilities.TRANSPORT_CELLULAR)
    }
}