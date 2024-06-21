package com.example.myapplication.fragments

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.myapplication.R
import com.example.myapplication.adapters.ClientAdapter
import com.example.myapplication.api.ClientAPI
import com.example.myapplication.entity.ClientDTO
import com.example.myapplication.views.SharedViewModel
import com.facebook.shimmer.ShimmerFrameLayout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.MainActivity
import com.example.myapplication.adapters.ProductAdapter
import com.example.myapplication.config.RetrofitInstance
import com.example.myapplication.fragments.ProductsFragment.Product
import com.example.myapplication.views.SharedViewModelFactory


class ClientsFragment : Fragment(), ClientAdapter.OnClientClickListener {

    private lateinit var clientsRecyclerView: RecyclerView
    private lateinit var clientAdapter: ClientAdapter
    private lateinit var emptyView: ViewStub
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private val clients = mutableListOf<Client>()
    private lateinit var shimmerViewContainer: ShimmerFrameLayout
    private lateinit var sharedViewModel: SharedViewModel

    private val allClients = mutableListOf<Client>()
    private val displayedClients = mutableListOf<Client>()

    private lateinit var clientAPI: ClientAPI

    interface ClientSelectionListener {
        fun onClientSelected(client: ClientsFragment.Client)
    }

    private var clientSelectionListener: ClientSelectionListener? = null


    var isClientSelectionListenerActive: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_clients, container, false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        clientAPI = RetrofitInstance.getInstance(requireContext(), 8080).create(ClientAPI::class.java)

        val factory = SharedViewModelFactory()
        sharedViewModel = ViewModelProvider(requireActivity(), factory).get(SharedViewModel::class.java)

        sharedViewModel.refreshClientsTrigger.observe(viewLifecycleOwner) { shouldRefresh ->
            if (shouldRefresh) {
                fetchClients()
            }
        }

        clientAdapter = ClientAdapter(clients, clientAPI, this, this)

        clientsRecyclerView = view.findViewById(R.id.clientsRecyclerView)
        emptyView = view.findViewById(R.id.emptyView)
        clientsRecyclerView.layoutManager = LinearLayoutManager(context)

        sharedViewModel.refreshClientsTrigger.observe(viewLifecycleOwner, Observer {
            clientsRecyclerView.adapter = clientAdapter
        })


        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            fetchClients()
        }

        shimmerViewContainer = view.findViewById(R.id.shimmer_view_container)
        shimmerViewContainer.startShimmer()


        fetchClients()

        val addClientButton = view.findViewById<Button>(R.id.addClientButton)
        addClientButton.setOnClickListener {
            openAddClientDialog()
        }

        val deleteAllButton = view.findViewById<Button>(R.id.deleteAllButton)
        deleteAllButton.setOnClickListener {
            deleteAllClients()
        }

        sharedViewModel.refreshClientsTrigger.observe(viewLifecycleOwner) { shouldRefresh ->
            if (shouldRefresh) {
                fetchClients()
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
                val drawableStart = searchBar.right - searchBar.compoundDrawables[DRAWABLE_RIGHT].bounds.width() - 50 // Subtract 50 or any other value that works for you
                if (event.rawX >= drawableStart) {
                    searchBar.text.clear()
                    searchBar.clearFocus() // Clear focus
                    val inputMethodManager = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    inputMethodManager?.hideSoftInputFromWindow(searchBar.windowToken, 0) // Hide keyboard
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
                filterClients(s.toString())
            }
        })


        sharedViewModel.isClientSelectionListenerActive.observe(viewLifecycleOwner, Observer { isActive ->
            isClientSelectionListenerActive = isActive
        })

    }

    override fun onClientClick(client: Client) {
        if (isClientSelectionListenerActive) {
            Log.d("Client", client.toString())

            sharedViewModel.selectClient(client)

            isClientSelectionListenerActive = false

            val orderDialog = OrderDialogFragment()
            (activity as MainActivity).switchFragment(orderDialog)
        }
        else{
            openAddClientDialog(client)
        }
    }

    fun setClientSelectionListener(listener: ClientSelectionListener) {
        this.clientSelectionListener = listener
    }

    fun removeClientFromSearchLists(clientId: String) {
        allClients.removeAll { it.clientId == clientId }
        displayedClients.removeAll { it.clientId == clientId }
    }
    private fun filterClients(query: String) {
        val filteredClients = allClients.filter { client ->
            client.firmName.contains(query, ignoreCase = true) ||
            client.contactPerson.contains(query, ignoreCase = true) ||
            client.address.contains(query, ignoreCase = true)
        }

        displayedClients.clear()
        displayedClients.addAll(filteredClients)
        clientAdapter.updateClientsAfterSearch(displayedClients)
    }

    fun openWaze(latitude: Double, longitude: Double) {
        val uri = Uri.parse("https://waze.com/ul?ll=$latitude,$longitude&navigate=yes")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.waze")
        try {
            startActivity(intent)
        } catch (e: android.content.ActivityNotFoundException) {
            val browserIntent = Intent(Intent.ACTION_VIEW, uri)
            context?.startActivity(browserIntent) // Use context to start the activity
        }
    }
    fun openGoogleMaps(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.setPackage("com.google.android.apps.maps")
        try {
            startActivity(intent)
        } catch (e: android.content.ActivityNotFoundException) {
            // Fallback to opening the URL in a web browser if Google Maps is not installed
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context?.startActivity(browserIntent)
        }
    }

    fun extractLocationAndUrl(sharedText: String): Pair<String?, String?> {
        // Regex for old format
        val oldFormatRegex = "https://waze.com/ul\\S+".toRegex()
        // Regex for new format
        val newFormatRegex = "https://ul\\.waze\\.com/ul\\S+".toRegex()

        // Try to find matches for both formats
        val oldMatchResult = oldFormatRegex.find(sharedText)
        val newMatchResult = newFormatRegex.find(sharedText)

        return when {
            oldMatchResult != null -> {
                // Old format processing
                val url = oldMatchResult.value
                val prefix = "Use Waze to drive to "
                val suffix = ":"
                val locationName = oldMatchResult.range.let { range ->
                    sharedText.substring(0, range.first).trim().removePrefix(prefix).removeSuffix(suffix)
                }
                Pair(locationName, url)
            }
            newMatchResult != null -> {
                // New format processing
                val url = newMatchResult.value
                // For the new format, the location name might need to be fetched or processed differently
                // Here, we set it to null as a placeholder
                val locationName = "Check Waze for location!"
                Pair(locationName, url)
            }
            else -> Pair(null, null) // No match found
        }
    }

    fun openWazeLocation(sharedText: String) {
        val (locationName, wazeUrl) = extractLocationAndUrl(sharedText)
        if (locationName != null && wazeUrl != null) {
            val uri = Uri.parse(wazeUrl)
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.waze")
            try {
                startActivity(intent)
            } catch (e: android.content.ActivityNotFoundException) {
                val browserIntent = Intent(Intent.ACTION_VIEW, uri)
                context?.startActivity(browserIntent) // Use context to start the activity
            }
        } else {
            Toast.makeText(context, "Invalid Waze link", Toast.LENGTH_SHORT).show()
        }
    }

    data class Client(
        val clientId: String,
        val firmName: String,
        val contactPerson: String,
        val phoneNumber: String,
        val location: String,
        val latitude: Double,
        val longitude: Double,
        val address: String
    )



    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || networkCapabilities.hasTransport(
            NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    fun fadeOutAnimation(){
        val fadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out)

        fadeOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            @SuppressLint("NotifyDataSetChanged")
            override fun onAnimationEnd(animation: Animation) {
                shimmerViewContainer.visibility = View.GONE

                clientAdapter.notifyDataSetChanged()

                if (clientsRecyclerView.adapter?.itemCount == 0) {
                    clientsRecyclerView.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                } else {
                    clientsRecyclerView.visibility = View.VISIBLE
                    emptyView.visibility = View.GONE
                }
                clientsRecyclerView.visibility = View.VISIBLE
            }
            override fun onAnimationRepeat(animation: Animation) {}
        })
        shimmerViewContainer.startAnimation(fadeOut)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun fetchClients() {
        val call = clientAPI.getClients()
        call.enqueue(object : Callback<List<Client>> {
            override fun onResponse(call: Call<List<Client>>, response: Response<List<Client>>) {
                if (response.isSuccessful) {
                    val clientsResponse = response.body()
                    if (clientsResponse != null) {
                        allClients.clear()
                        allClients.addAll(clientsResponse)
                        allClients.reverse()
                        // move view at the top
                        clientsRecyclerView.scrollToPosition(0)

                        displayedClients.clear()
                        displayedClients.addAll(allClients)
                        clientAdapter.updateClientsAfterSearch(displayedClients)

                        val searchBar = view?.findViewById<EditText>(R.id.searchBar)
                        searchBar?.text?.clear()
                    }
                }
                swipeRefreshLayout.isRefreshing = false
                
                fadeOutAnimation()
            }
            override fun onFailure(call: Call<List<Client>>, t: Throwable) {
                Log.e("Error", t.message.toString())
                swipeRefreshLayout.isRefreshing = false
            }
        })
    }

    private fun addClient(newClient: ClientDTO, callback: (String?) -> Unit) {
        val call = clientAPI.addClient(newClient)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    callback(null)
                    fetchClients()
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
    @SuppressLint("InflateParams", "MissingInflatedId", "SetTextI18n")
    fun openAddClientDialog(client: Client? = null) {
        if (!isNetworkAvailable()) {
            Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show()
            return
        }
        
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_client, null)
        val builder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle(if (client == null) "Add Client" else "Client Information")

        val alertDialog = builder.show()

        if (client != null) {
            dialogView.findViewById<EditText>(R.id.firmNameInput).setText(client.firmName)
            dialogView.findViewById<EditText>(R.id.phoneNumberInput).setText(client.phoneNumber.toString())
            dialogView.findViewById<EditText>(R.id.contactPersonInput).setText(client.contactPerson)
            dialogView.findViewById<EditText>(R.id.locationInput).setText(client.location)
//            dialogView.findViewById<EditText>(R.id.coordinatesInput).setText("${client.latitude},${client.longitude}")
            dialogView.findViewById<EditText>(R.id.addressInput).setText(client.address)
        }

        dialogView.findViewById<Button>(R.id.saveButton).setOnClickListener {
            val firmName = dialogView.findViewById<EditText>(R.id.firmNameInput).text.toString()

            if(firmName.isEmpty()) {
                Toast.makeText(context, "Firm name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val phoneNumber = dialogView.findViewById<EditText>(R.id.phoneNumberInput).text.toString()

            if (phoneNumber.isEmpty()) {
                Toast.makeText(context, "Phone number cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val contactPerson = dialogView.findViewById<EditText>(R.id.contactPersonInput).text.toString()
            val location = dialogView.findViewById<EditText>(R.id.locationInput).text.toString()

//            val coordinates = dialogView.findViewById<EditText>(R.id.coordinatesInput).text.toString()
//            val coordinatesSplit = coordinates.split(",")
            val latitude: Double? = null
            val longitude: Double? = null
//            if (coordinatesSplit.size >= 2) {
//                latitude = coordinatesSplit[0].takeIf { it.isNotEmpty() }?.toDouble()
//                longitude = coordinatesSplit[1].takeIf { it.isNotEmpty() }?.toDouble()
//            }

            val address = dialogView.findViewById<EditText>(R.id.addressInput).text.toString()

            val newClient = ClientDTO(firmName, contactPerson, phoneNumber, location, latitude ?: 0.0, longitude ?: 0.0, address)

            if (client == null) {
                addClient(newClient) { errorMessage ->
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
                updateClient(client.clientId, newClient) { errorMessage ->
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

    private fun deleteAllClients(){
        AlertDialog.Builder(requireContext()).apply {
            setTitle("Delete All Clients")
            setMessage("Are you sure you want to delete all clients?")

            setPositiveButton("Yes") { dialog, _ ->
                val call = clientAPI.deleteAllClients()
                call.enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            fetchClients()
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

    private fun updateClient(clientId: String, updatedClient: ClientDTO, callback: (String?) -> Unit) {
        val call = clientAPI.updateClient(clientId, updatedClient)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    callback(null)
                    fetchClients()
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

    fun copyToClipboard(phoneNumberText: String) {
        val clip = ClipData.newPlainText("Phone Number", phoneNumberText)
        ContextCompat.getSystemService(requireContext(), ClipboardManager::class.java)
            ?.setPrimaryClip(clip)
    }


}