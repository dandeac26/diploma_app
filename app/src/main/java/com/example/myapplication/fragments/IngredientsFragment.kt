package com.example.myapplication.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.Spinner
import android.widget.TableLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.api.IngredientsAPI
import com.example.myapplication.config.RetrofitInstance
import com.example.myapplication.entity.IngredientDTO
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class IngredientsFragment : Fragment() {

    private lateinit var ingredientsTable: TableLayout
    private lateinit var ingredientsAPI: IngredientsAPI
    private lateinit var deleteAllButton : ImageButton
    private lateinit var addIngredientButton: ImageButton
    private lateinit var updateIngredientButton: ImageButton
    private lateinit var backButton : ImageButton
    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_ingredients, container, false)

        ingredientsAPI = RetrofitInstance.getInstance(requireContext(), 8080).create(IngredientsAPI::class.java)

        ingredientsTable = view.findViewById(R.id.ingredientsTable)
        deleteAllButton = view.findViewById(R.id.deleteAllIngredientsButton)
        addIngredientButton = view.findViewById(R.id.addIngredientButton)
        updateIngredientButton = view.findViewById(R.id.refreshButton)
        backButton = view.findViewById(R.id.backButton)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val deleteAllButton = view.findViewById<ImageButton>(R.id.deleteAllIngredientsButton)
        deleteAllButton.setOnClickListener {
            val alphaAnimation = AlphaAnimation(1.0f, 0.5f)
            alphaAnimation.duration = 200
            alphaAnimation.repeatCount = 1
            alphaAnimation.repeatMode = Animation.REVERSE


            it.startAnimation(alphaAnimation)
            deleteAllIngredients()
        }

        val addIngredientButton = view.findViewById<ImageButton>(R.id.addIngredientButton)
        addIngredientButton.setOnClickListener {
            val alphaAnimation = AlphaAnimation(1.0f, 0.5f)
            alphaAnimation.duration = 200
            alphaAnimation.repeatCount = 1
            alphaAnimation.repeatMode = Animation.REVERSE


            it.startAnimation(alphaAnimation)
            updateIngredientDialog(null)
        }

        val updateIngredientButton = view.findViewById<ImageButton>(R.id.refreshButton)
        updateIngredientButton.setOnClickListener {

            val alphaAnimation = AlphaAnimation(1.0f, 0.5f)
            alphaAnimation.duration = 200
            alphaAnimation.repeatCount = 1
            alphaAnimation.repeatMode = Animation.REVERSE


            it.startAnimation(alphaAnimation)
            fetchIngredients()
            Toast.makeText(context, "Ingredients refreshed", Toast.LENGTH_SHORT).show()
        }

        val backButton = view.findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {

            val alphaAnimation = AlphaAnimation(1.0f, 0.5f)
            alphaAnimation.duration = 200
            alphaAnimation.repeatCount = 1
            alphaAnimation.repeatMode = Animation.REVERSE


            it.startAnimation(alphaAnimation)
            val stocksFragment = StocksFragment()
            (activity as MainActivity).switchFragment(stocksFragment)
        }
        fetchIngredients()

        val menuButton = view.findViewById<ImageButton>(R.id.menuButton)
        menuButton.setOnClickListener {
            val alphaAnimation = AlphaAnimation(1.0f, 0.5f)
            alphaAnimation.duration = 200
            alphaAnimation.repeatCount = 1
            alphaAnimation.repeatMode = Animation.REVERSE


            it.startAnimation(alphaAnimation)

            val popupMenu = PopupMenu(requireContext(), it)
            popupMenu.menuInflater.inflate(R.menu.stocks_action_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.deleteAll -> {
                        deleteAllIngredients()
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
                        val stocksFragment = StocksFragment()
                        (activity as MainActivity).switchFragment(stocksFragment)
                        true
                    }
                    R.id.providers -> {
                        val providersFragment = ProvidersFragment()
                        (activity as MainActivity).switchFragment(providersFragment)
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
    }

    private fun fetchIngredients() {
        val call = ingredientsAPI.getIngredients()
        call.enqueue(object : Callback<List<StocksFragment.Ingredient>> {
            @SuppressLint("MissingInflatedId", "UseCompatLoadingForDrawables")
            override fun onResponse(call: Call<List<StocksFragment.Ingredient>>, response: Response<List<StocksFragment.Ingredient>>) {
                if (response.isSuccessful) {
                    val ingredients = response.body()
                    if (ingredients != null) {
                        ingredientsTable.removeAllViews()
                        val header = layoutInflater.inflate(R.layout.ingredient_row, null)
                        header.findViewById<TextView>(R.id.ingredientName).text = "Ingredient"
                        header.findViewById<TextView>(R.id.measurementUnit).text = "M. Unit"
                        header.findViewById<TextView>(R.id.packaging).text = "Packaging"
                        header.background = resources.getDrawable(R.drawable.table_header_bg)
                        
                        ingredientsTable.addView(header)
                        for (ingredient in ingredients.reversed()) {
                            val row = layoutInflater.inflate(R.layout.ingredient_row, null)
                            row.findViewById<TextView>(R.id.ingredientName).text = ingredient.name
                            row.findViewById<TextView>(R.id.measurementUnit).text = ingredient.measurementUnit
                            row.findViewById<TextView>(R.id.packaging).text = ingredient.packaging
                            row.setOnClickListener{
                                updateIngredientDialog(ingredient)
                            }

                            row.setOnLongClickListener {
                                deleteIngredient(ingredient)
                                true
                            }
                            ingredientsTable.addView(row)
                        }
                    }
                }
            }

            override fun onFailure(call: Call<List<StocksFragment.Ingredient>>, t: Throwable) {
                // Handle the error
            }
        })
    }

    @SuppressLint("MissingInflatedId", "CutPasteId")
    private fun updateIngredientDialog(ingredient: StocksFragment.Ingredient?) {
        if (!isNetworkAvailable()) {
            Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show()
            return
        }
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_ingredient, null)
        val builder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle("Update Ingredient")

        val alertDialog = builder.show()

        if(ingredient != null) {
            dialogView.findViewById<TextView>(R.id.dialogIngredientNameInput).text = ingredient.name
            dialogView.findViewById<EditText>(R.id.dialogIngredientMUInput).setText(ingredient.measurementUnit)
            dialogView.findViewById<EditText>(R.id.dialogIngredientPackInput).setText(ingredient.packaging)
        }
        else{
            alertDialog.setTitle("Add new Ingredient")
        }

        dialogView.findViewById<Button>(R.id.saveButton).setOnClickListener {

            val ingredientName = dialogView.findViewById<TextView>(R.id.dialogIngredientNameInput).text.toString()
            val measurementUnit = dialogView.findViewById<EditText>(R.id.dialogIngredientMUInput).text.toString()
            val packaging = dialogView.findViewById<EditText>(R.id.dialogIngredientPackInput).text.toString()

            if(ingredient != null){
                val newIngredient = IngredientDTO(ingredientName, measurementUnit, packaging)

                updateIngredient(ingredient.ingredientId, newIngredient) { errorMessage ->
                    if (errorMessage != null) {
                        val errorMessageTextView =
                            dialogView.findViewById<TextView>(R.id.errorMessage)
                        errorMessageTextView.text = errorMessage
                        errorMessageTextView.visibility = View.VISIBLE
                    } else {
                        fetchIngredients()
                        alertDialog.dismiss()
                    }
                }
            }else{
                val newIngredient = IngredientDTO(ingredientName, measurementUnit, packaging)
                val call = ingredientsAPI.addIngredient(newIngredient)
                call.enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            fetchIngredients()
                            Toast.makeText(context, "Ingredient added", Toast.LENGTH_SHORT).show()
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
    private fun updateIngredient(ingredientId: String, updatedIngredient: IngredientDTO, callback: (String?) -> Unit) {
        val call = ingredientsAPI.updateIngredient(ingredientId, updatedIngredient)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    callback(null)
                    fetchIngredients()
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

    private fun addIngredient(newIngredient: IngredientDTO, callback: (String?) -> Unit) {
        val call = ingredientsAPI.addIngredient(newIngredient)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    callback(null)
                    fetchIngredients()
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

    fun deleteIngredient(ingredient: StocksFragment.Ingredient) {
        AlertDialog.Builder(requireContext()).apply {
            setTitle("Delete Ingredient?")
            setMessage("Are you sure you want to delete this ingredient?")

            setPositiveButton("Yes") { dialog, _ ->
            val call = ingredientsAPI.deleteIngredient(ingredient.ingredientId)
            call.enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        fetchIngredients()
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

    private fun deleteAllIngredients() {
        AlertDialog.Builder(requireContext()).apply {
            setTitle("Delete ALL Ingredients?")
            setMessage("Are you sure you want to delete all ingredients?")

            setPositiveButton("Yes") { dialog, _ ->
            val call = ingredientsAPI.deleteAllIngredients()
            call.enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        fetchIngredients()
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