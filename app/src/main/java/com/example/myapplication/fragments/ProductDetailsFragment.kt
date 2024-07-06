package com.example.myapplication.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TableLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.api.BakeryAPI
import com.example.myapplication.api.IngredientsAPI
import com.example.myapplication.api.RecipeAPI
import com.example.myapplication.config.RetrofitInstance
import com.example.myapplication.entity.ProductDTO
import com.example.myapplication.entity.RecipeDTO
import com.example.myapplication.fragments.ProductsFragment.Product
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProductDetailsFragment : Fragment() {

    private lateinit var productImage: ImageView
    private lateinit var productName: TextView
    private lateinit var productPrice: TextView
    private lateinit var recipeTable: TableLayout
    private lateinit var addRecipeButton: ImageButton
    private lateinit var bakeryAPI: BakeryAPI
    private lateinit var recipeAPI: RecipeAPI
    private lateinit var ingredientsAPI: IngredientsAPI
    private lateinit var product: Product

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_product_details, container, false)

        bakeryAPI = RetrofitInstance.getInstance("http://", requireContext(), 8080).create(BakeryAPI::class.java)
        recipeAPI = RetrofitInstance.getInstance("http://", requireContext(), 8080).create(RecipeAPI::class.java)
        ingredientsAPI = RetrofitInstance.getInstance("http://", requireContext(), 8080).create(IngredientsAPI::class.java)

        productImage = view.findViewById(R.id.productImage)
        productName = view.findViewById(R.id.productName)
        productPrice = view.findViewById(R.id.productPrice)
        recipeTable = view.findViewById(R.id.recipeTable)
        addRecipeButton = view.findViewById(R.id.addRecipeButton)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        product = (arguments?.getSerializable("product") as? Product)!!
        setProductDetails()

        val backButton = view.findViewById<View>(R.id.backButton)
        backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }

        val editButton = view.findViewById<View>(R.id.editButton)
        editButton.setOnClickListener {
            openAddProductDialog(product)
        }

        val deleteButton = view.findViewById<View>(R.id.deleteButton)
        deleteButton.setOnClickListener {
            deleteProduct(product)
        }

        val addRecipeButton = view.findViewById<ImageButton>(R.id.addRecipeButton)
        addRecipeButton.setOnClickListener {
            updateRecipeDialog(null)
        }
    }

    data class Recipe (
        val productId: String,
        val ingredientId: String,
        val ingredientName: String,
        val quantity: Double,
        val ingredientMeasurementUnit: String
    )

    private fun setProductDetails() {
        Glide.with(productImage.context)
            .load(product.imageUrl)
            .into(productImage)

        productName.text = product.name
        productPrice.text = product.price.toString()

        val call = recipeAPI.getRecipeOfProduct(product.productId)
        call.enqueue(object : Callback<List<Recipe>> {
            @SuppressLint("MissingInflatedId", "InflateParams")
            override fun onResponse(call: Call<List<Recipe>>, response: Response<List<Recipe>>) {
                if (response.isSuccessful) {
                    Log.d("API_RESPONSE", "Response Body: ${response.body()}")

                    val recipes = response.body()
                    if (!recipes.isNullOrEmpty()) {
                        for (recipe in recipes) {
                            val row = layoutInflater.inflate(R.layout.recipe_row, null)
                            row.findViewById<TextView>(R.id.ingredientName).text = recipe.ingredientName
                            row.findViewById<TextView>(R.id.quantity).text = recipe.quantity.toString()
                            row.findViewById<TextView>(R.id.measurementUnit).text = recipe.ingredientMeasurementUnit
                            row.findViewById<ImageButton>(R.id.recipeDeleteButton).setOnClickListener {
                                deleteRecipe(recipe)
                            }
                            row.setOnClickListener {
                                updateRecipeDialog(recipe)
                            }
                            recipeTable.addView(row)
                        }
                    } else {
                        val recipeTitle = view?.findViewById<TextView>(R.id.recipeTitle)
                        recipeTitle?.text = getString(R.string.no_recipe_yet)
                    }
                }
            }
            override fun onFailure(call: Call<List<Recipe>>, t: Throwable) {
                // Handle the error
            }
        })
    }

    private fun fetchRecipe() {
        val call = recipeAPI.getRecipeOfProduct(product.productId)
        call.enqueue(object : Callback<List<Recipe>> {
            @SuppressLint("MissingInflatedId", "InflateParams")
            override fun onResponse(call: Call<List<Recipe>>, response: Response<List<Recipe>>) {
                if (response.isSuccessful) {
                    Log.d("API_RESPONSE", "Response Body: ${response.body()}")

                    val recipes = response.body()

                    if (!recipes.isNullOrEmpty()) {
                        recipeTable.removeAllViews()

                        for (recipe in recipes) {
                            val row = layoutInflater.inflate(R.layout.recipe_row, null)
                            row.findViewById<TextView>(R.id.ingredientName).text = recipe.ingredientName
                            row.findViewById<TextView>(R.id.quantity).text = recipe.quantity.toString()
                            row.findViewById<TextView>(R.id.measurementUnit).text = recipe.ingredientMeasurementUnit
                            row.findViewById<ImageButton>(R.id.recipeDeleteButton).setOnClickListener {
                                deleteRecipe(recipe)
                            }
                            row.setOnClickListener {
                                updateRecipeDialog(recipe)
                            }

                            recipeTable.addView(row)
                        }
                    } else {
                        val recipeTitle = view?.findViewById<TextView>(R.id.recipeTitle)
                        recipeTitle?.text = getString(R.string.no_recipe_yet)
                    }
                }
            }

            override fun onFailure(call: Call<List<Recipe>>, t: Throwable) {
                // Handle the error
            }
        })
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

    @SuppressLint("MissingInflatedId")
    private fun updateRecipeDialog(recipe: Recipe?) {
        if (!isNetworkAvailable()) {
            Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show()
            return
        }
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_recipe, null)
        val builder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle("Update Recipe")

        val alertDialog = builder.show()

        val dialogNameInput = dialogView.findViewById<Spinner>(R.id.dialogNameInput)
        var ingredientIdsMap = mutableMapOf<String, String>()
        val ingredientCall = ingredientsAPI.getIngredients()
        ingredientCall.enqueue(object : Callback<List<StocksFragment.Ingredient>> {
            override fun onResponse(call: Call<List<StocksFragment.Ingredient>>, response: Response<List<StocksFragment.Ingredient>>) {
                if (response.isSuccessful) {
                    val ingredients = response.body()
                    if (ingredients != null) {
                        val ingredientNames = ingredients.map { it.name }
                        ingredientIdsMap = ingredients.associateBy({ it.name }, { it.ingredientId }).toMutableMap()
                        val ingredientAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, ingredientNames)
                        ingredientAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        dialogNameInput.adapter = ingredientAdapter
                        if (recipe != null) {
                            val providerPosition = ingredientNames.indexOf(recipe.ingredientName)
                            if (providerPosition != -1) {
                                dialogView.findViewById<Spinner>(R.id.dialogNameInput).setSelection(providerPosition)
                            }
                        }
                    }
                }
            }
            override fun onFailure(call: Call<List<StocksFragment.Ingredient>>, t: Throwable) {
                // Handle the error
            }
        })

        if(recipe != null) {

            dialogNameInput.isEnabled = false
            dialogView.findViewById<EditText>(R.id.dialogQuantityInput).setText(recipe.quantity.toString())
        }
        else{
            alertDialog.setTitle("Add new Ingredient")
        }

        dialogView.findViewById<Button>(R.id.saveButton).setOnClickListener {
            val quantity = dialogView.findViewById<EditText>(R.id.dialogQuantityInput).text.toString()

            if (quantity.isEmpty()) {
                Toast.makeText(context, "Quantity cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val quantityDouble = quantity.toDouble()

            if(recipe != null){
                val newRecipe = RecipeDTO(recipe.productId, recipe.ingredientId, quantityDouble)

                updateRecipe(newRecipe) { errorMessage ->
                    if (errorMessage != null) {
                        val errorMessageTextView =
                            dialogView.findViewById<TextView>(R.id.errorMessage)
                        errorMessageTextView.text = errorMessage
                        errorMessageTextView.visibility = View.VISIBLE
                    } else {
                        fetchRecipe()
                        alertDialog.dismiss()
                    }
                }
            }else{
                val ingredientId = ingredientIdsMap[dialogNameInput.selectedItem.toString()]
                if (ingredientId == null) {
                    Toast.makeText(context, "Ingredient not found", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val newRecipe = RecipeDTO(product.productId, ingredientId, quantityDouble)
                val call = recipeAPI.addRecipe(newRecipe)
                call.enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            fetchRecipe()
                            val recipeTitle = view?.findViewById<TextView>(R.id.recipeTitle)
                            recipeTitle?.text = getString(R.string.recipe)
                            alertDialog.dismiss()
                        }
                        else{
                            if (response.code() == 400) {
                                val errorMessageTextView =
                                    dialogView.findViewById<TextView>(R.id.errorMessage)
                                errorMessageTextView.text = response.errorBody()?.string()
                                errorMessageTextView.visibility = View.VISIBLE
                            }
                        }
                    }
                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        // Handle the error
                    }
                })
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

    private fun updateRecipe(recipe: RecipeDTO, callback: (String?) -> Unit) {
        val call = recipeAPI.updateRecipe(recipe.productId, recipe.ingredientId, recipe)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    callback(null)
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
                        // Handle error
                    }
                })
                dialog.dismiss()
            }
            setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
        }.create().show()
    }

    private fun deleteRecipe(recipe: Recipe){
        AlertDialog.Builder(requireContext()).apply {
            setTitle("Delete Recipe")
            setMessage("Are you sure you want to delete this recipe?")

            setPositiveButton("Yes") { dialog, _ ->
                val call = recipeAPI.deleteRecipe(recipe.productId, recipe.ingredientId)
                call.enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            fetchRecipe()
                        }
                    }
                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        // Handle error
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