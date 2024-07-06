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
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TableLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.api.ProviderAPI
import com.example.myapplication.config.RetrofitInstance
import com.example.myapplication.entity.ProviderDTO
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProvidersFragment : Fragment() {

    private lateinit var providersTable: TableLayout
    private lateinit var providersAPI: ProviderAPI
    private lateinit var deleteAllButton : ImageButton
    private lateinit var addProviderButton: ImageButton
    private lateinit var updateProviderButton: ImageButton
    private lateinit var backButton : ImageButton

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_providers, container, false)

        providersAPI = RetrofitInstance.getInstance("http://", requireContext(), 8080).create(ProviderAPI::class.java)

        providersTable = view.findViewById(R.id.providersTable)
        deleteAllButton = view.findViewById(R.id.deleteAllProvidersButton)
        addProviderButton = view.findViewById(R.id.addProviderButton)
        updateProviderButton = view.findViewById(R.id.refreshButton)
        backButton = view.findViewById(R.id.backButton)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val deleteAllButton = view.findViewById<ImageButton>(R.id.deleteAllProvidersButton)
        deleteAllButton.setOnClickListener {
            val alphaAnimation = AlphaAnimation(1.0f, 0.5f)
            alphaAnimation.duration = 200
            alphaAnimation.repeatCount = 1
            alphaAnimation.repeatMode = Animation.REVERSE

            it.startAnimation(alphaAnimation)
            deleteAllProviders()
        }

        val addProviderButton = view.findViewById<ImageButton>(R.id.addProviderButton)
        addProviderButton.setOnClickListener {
            val alphaAnimation = AlphaAnimation(1.0f, 0.5f)
            alphaAnimation.duration = 200
            alphaAnimation.repeatCount = 1
            alphaAnimation.repeatMode = Animation.REVERSE


            it.startAnimation(alphaAnimation)
            updateProviderDialog(null)
        }

        val updateProviderButton = view.findViewById<ImageButton>(R.id.refreshButton)
        updateProviderButton.setOnClickListener {
            val alphaAnimation = AlphaAnimation(1.0f, 0.5f)
            alphaAnimation.duration = 200
            alphaAnimation.repeatCount = 1
            alphaAnimation.repeatMode = Animation.REVERSE


            it.startAnimation(alphaAnimation)
            fetchProviders()
            Toast.makeText(context, "Providers refreshed", Toast.LENGTH_SHORT).show()
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
        fetchProviders()

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
                        deleteAllProviders()
                        true
                    }
                    R.id.clearAll -> {
                        true
                    }
                    R.id.clearSelected -> {
                        true
                    }
                    R.id.ingredients -> {
                        val ingredientsFragment = IngredientsFragment()
                        (activity as MainActivity).switchFragment(ingredientsFragment)
                        true
                    }
                    R.id.providers -> {
                        val stocksFragment = StocksFragment()
                        (activity as MainActivity).switchFragment(stocksFragment)
                        true
                    }
                    R.id.predictions -> {
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }
    }

    private fun fetchProviders() {
        val call = providersAPI.getProviders()
        call.enqueue(object : Callback<List<StocksFragment.Provider>> {
            @SuppressLint("MissingInflatedId", "UseCompatLoadingForDrawables", "SetTextI18n",
                "InflateParams"
            )
            override fun onResponse(call: Call<List<StocksFragment.Provider>>, response: Response<List<StocksFragment.Provider>>) {
                if (response.isSuccessful) {
                    val providers = response.body()
                    if (providers != null) {
                        providersTable.removeAllViews()
                        val header = layoutInflater.inflate(R.layout.provider_row, null)
                        header.findViewById<TextView>(R.id.providerName).text = "Provider"
                        header.findViewById<TextView>(R.id.phoneNumber).text = "Phone"
                        header.background = resources.getDrawable(R.drawable.table_header_bg)

                        providersTable.addView(header)
                        for (provider in providers.reversed()) {
                            val row = layoutInflater.inflate(R.layout.provider_row, null)
                            row.findViewById<TextView>(R.id.providerName).text = provider.name
                            row.findViewById<TextView>(R.id.phoneNumber).text = provider.phoneNumber

                            row.setOnClickListener{
                                updateProviderDialog(provider)
                            }

                            row.setOnLongClickListener {
                                deleteProvider(provider)
                                true
                            }
                            providersTable.addView(row)
                        }
                    }
                }
            }

            override fun onFailure(call: Call<List<StocksFragment.Provider>>, t: Throwable) {
                // Handle error
            }
        })
    }

    @SuppressLint("MissingInflatedId", "CutPasteId")
    private fun updateProviderDialog(provider: StocksFragment.Provider?) {
        if (!isNetworkAvailable()) {
            Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show()
            return
        }
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_provider, null)
        val builder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle("Update Provider")

        val alertDialog = builder.show()

        if(provider != null) {
            dialogView.findViewById<TextView>(R.id.dialogProviderNameInput).text = provider.name
            dialogView.findViewById<EditText>(R.id.dialogProviderPhoneInput).setText(provider.phoneNumber)
        }
        else{
            alertDialog.setTitle("Add new Provider")
        }

        dialogView.findViewById<Button>(R.id.saveButton).setOnClickListener {

            val providerName = dialogView.findViewById<TextView>(R.id.dialogProviderNameInput).text.toString()
            val providerPhoneNumber = dialogView.findViewById<EditText>(R.id.dialogProviderPhoneInput).text.toString()

            if(provider != null){
                val newProvider = ProviderDTO(providerName, providerPhoneNumber)

                updateProvider(provider.providerId, newProvider) { errorMessage ->
                    if (errorMessage != null) {
                        val errorMessageTextView =
                            dialogView.findViewById<TextView>(R.id.errorMessage)
                        errorMessageTextView.text = errorMessage
                        errorMessageTextView.visibility = View.VISIBLE
                    } else {
                        fetchProviders()
                        alertDialog.dismiss()
                    }
                }
            }else{
                val newProvider = ProviderDTO(providerName, providerPhoneNumber)
                val call = providersAPI.addProvider(newProvider)
                call.enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            fetchProviders()
                            Toast.makeText(context, "Provider added", Toast.LENGTH_SHORT).show()
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

    private fun updateProvider(providerId: String, updatedProvider: ProviderDTO, callback: (String?) -> Unit) {
        val call = providersAPI.updateProvider(providerId, updatedProvider)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    callback(null)
                    fetchProviders()
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

    fun deleteProvider(provider: StocksFragment.Provider) {
        AlertDialog.Builder(requireContext()).apply {
            setTitle("Delete Provider?")
            setMessage("Are you sure you want to delete this provider?")

            setPositiveButton("Yes") { dialog, _ ->
                val call = providersAPI.deleteProvider(provider.providerId)
                call.enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            fetchProviders()
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

    private fun deleteAllProviders() {
        AlertDialog.Builder(requireContext()).apply {
            setTitle("Delete ALL Providers?")
            setMessage("Are you sure you want to delete all providers?")

            setPositiveButton("Yes") { dialog, _ ->
                val call = providersAPI.deleteAllProviders()
                call.enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            fetchProviders()
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