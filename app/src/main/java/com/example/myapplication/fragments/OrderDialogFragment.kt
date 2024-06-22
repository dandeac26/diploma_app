package com.example.myapplication.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.adapters.OrderDialogProductAdapter
import com.example.myapplication.api.ClientAPI
import com.example.myapplication.api.OrderAPI
import com.example.myapplication.config.RetrofitInstance
import com.example.myapplication.entity.ClientDTO
import com.example.myapplication.entity.OrderDTO
import com.example.myapplication.views.SharedViewModel
import com.example.myapplication.views.SharedViewModelFactory
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale

class OrderDialogFragment : DialogFragment(), ClientsFragment.ClientSelectionListener, ProductsFragment.ProductsSelectionListener {

    private lateinit var selectedClient: ClientsFragment.Client

    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var productLineItemRecycleView : RecyclerView

    private lateinit var selectedProductsAdapter: OrderDialogProductAdapter
    private val selectedProducts = mutableListOf<LineItemProduct>()

    private lateinit var orderAPI: OrderAPI
    private var webSocket: WebSocket? = null

    private lateinit var clientAPI: ClientAPI


    @SuppressLint("MissingInflatedId", "NotifyDataSetChanged")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_order, container, false)


        orderAPI = RetrofitInstance.getInstance(requireContext(), 8000).create(OrderAPI::class.java)
        connectWebSocket()

        clientAPI = RetrofitInstance.getInstance(requireContext(), 8080).create(ClientAPI::class.java)

        mainInitializeViewModelAndAdapter(view)

        val selectedClientTextView = view.findViewById<TextView>(R.id.selectedClientTextView)
        sharedViewModel.selectedClient.observe(viewLifecycleOwner, Observer { client ->
            if (client == null) {
                // Clear the selected client text view
                selectedClientTextView.text = ""
            } else {
                selectedClientTextView.text = client.firmName
            }
        })

        sharedViewModel.selectedProduct.observe(viewLifecycleOwner, Observer { product ->
            if (product == null) {
                // Clear the selected products list
                selectedProducts.clear()
                selectedProductsAdapter.notifyDataSetChanged()
            } else {
                // Add the selected product to the list
                val lineItemProduct =
                    LineItemProduct(product.productId, product.name, 0, product.imageUrl)
                selectedProducts.add(lineItemProduct)
                selectedProductsAdapter.notifyItemInserted(selectedProducts.size - 1)
            }
        })

        mainInterfaceActions(view)

        mainActivateWatchers()

        mainHandleOrderCreate(view)

        return view
    }

//    private fun mainHandleOrderCreate(view: View){
//        val createOrderButton = view.findViewById<Button>(R.id.createOrderButton)
//        createOrderButton.setOnClickListener {
//
//        }
//    }
//    private fun mainHandleOrderCreate(view: View){
//        val createOrderButton = view.findViewById<Button>(R.id.createOrderButton)
//        createOrderButton.setOnClickListener {
//            createOrderButton.isEnabled = false
//            createOrderButton.text = "Loading..."
//            // Construct the Order object from the gathered data
//            var selectedClientId: String? = null
//            sharedViewModel.selectedClient.observe(viewLifecycleOwner, Observer { client ->
//                selectedClientId = client.clientId
//            })
//
//            var dayDate = ""
//            sharedViewModel.selectedDate.observe(viewLifecycleOwner) { date ->
//                val splitDate = date.split(" ")
//                if (splitDate.size > 1) {
//                    val originalDate = splitDate[1]
//                    dayDate = convertDateFormat(originalDate).toString()
//                }
//            }
//
//            val selectedClientTextView = view.findViewById<TextView>(R.id.selectedClientTextView)
//
//            if(selectedClientTextView.text.isNotEmpty() && selectedClientId == null){
//                // create new client with the name and phone number 0000000000
//                val newClient = ClientDTO(
//                    firmName = selectedClientTextView.text.toString(),
//                    contactPerson = selectedClientTextView.text.toString(),
//                    phoneNumber = "0000000000",
//                    location = "Unknown",
//                    latitude = 0.0,
//                    longitude = 0.0,
//                    address = "Unknown",
//                )
//
//                // Send the new client to the clientAPI
//                addClient(newClient) { clientId ->
//                    if (clientId != null) {
//                        // Handle the error
//                        Log.d("OrderDialogFragment", "Client added $clientId")
//                        selectedClientId = clientId
//
//                    } else {
//                        Log.e("OrderDialogFragment", "Error adding client")
//                        Toast.makeText(context, "Error adding client", Toast.LENGTH_SHORT).show()
//                    }
//                }
//
//            }
//
//            if (selectedClientId == null) {
//                createOrderButton.isEnabled = true
//                createOrderButton.text = "Create"
//                Toast.makeText(context, "Please select a client", Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
//
//            val order = OrderDTO(
//                clientId = selectedClientId!!,
//                deliveryNeeded = false, // Replace with actual value
//                completionDate = dayDate, // Replace with actual date
//                completionTime = "10:00:00", // Replace with actual time
//                price = 0.0, // Replace with actual price
//                completed = false,
////                orderDetails = selectedProducts.map {
////                    OrdersFragment.OrderDetail(
////                        orderId = "someId", // Replace with actual ID
////                        productId = it.id,
////                        quantity = it.quantity,
////                        product = OrdersFragment.Product(
////                            productId = it.id,
////                            name = it.name,
////                            price = 0.0, // Replace with actual price
////                            imageUrl = it.imageUrl
////                        )
////                    )
////                }
//            )
//
//            // Send the order to the orderAPI
//            addOrder(order) { error ->
//                createOrderButton.isEnabled = true
//                createOrderButton.text = "Create"
//                if (error != null) {
//                    // Handle the error
//                    Log.e("OrderDialogFragment", "Error adding order: $error")
//                } else {
//                    // Notify the server about the new order
//                    notifyServerAboutNewOrder(order)
//                    // switch to DailyOrderFragment
//                    Toast.makeText(context, "Order added successfully", Toast.LENGTH_SHORT).show()
//                    val dailyOrderFragment = DailyOrderFragment()
//                    (activity as MainActivity).switchFragment(dailyOrderFragment)
//                }
//            }
//        }
//    }
    private fun mainHandleOrderCreate(view: View){
        val createOrderButton = view.findViewById<Button>(R.id.createOrderButton)
        createOrderButton.setOnClickListener {
            createOrderButton.isEnabled = false
            createOrderButton.text = "Loading..."

            var dayDate = ""
            sharedViewModel.selectedDate.observe(viewLifecycleOwner) { date ->
                val splitDate = date.split(" ")
                if (splitDate.size > 1) {
                    val originalDate = splitDate[1]
                    dayDate = convertDateFormat(originalDate).toString()
                }
            }

            val selectedClientTextView = view.findViewById<TextView>(R.id.selectedClientTextView)

            if(selectedClientTextView.text.isNotEmpty()){
                // create new client with the name and phone number 0000000000
                val newClient = ClientDTO(
                    firmName = selectedClientTextView.text.toString(),
                    contactPerson = selectedClientTextView.text.toString(),
                    phoneNumber = "0000000000",
                    location = "Unknown",
                    latitude = 0.0,
                    longitude = 0.0,
                    address = "Unknown",
                )

                // Send the new client to the clientAPI
                addClient(newClient) { clientId ->
                    if (clientId != null) {
                        // Handle the error
                        Log.d("OrderDialogFragment", "Client added $clientId")

                        val order = OrderDTO(
                            clientId = clientId,
                            deliveryNeeded = false, // Replace with actual value
                            completionDate = dayDate, // Replace with actual date
                            completionTime = "10:00:00", // Replace with actual time
                            price = 0.0, // Replace with actual price
                            completed = false,
                        )

                        // Send the order to the orderAPI
                        addOrder(order) { error ->
                            createOrderButton.isEnabled = true
                            createOrderButton.text = "Create"
                            if (error != null) {
                                // Handle the error
                                Log.e("OrderDialogFragment", "Error adding order: $error")
                            } else {
                                // Notify the server about the new order
                                notifyServerAboutNewOrder(order)
                                // switch to DailyOrderFragment
                                Toast.makeText(context, "Order added successfully", Toast.LENGTH_SHORT).show()
                                val dailyOrderFragment = DailyOrderFragment()
                                (activity as MainActivity).switchFragment(dailyOrderFragment)
                            }
                        }

                    } else {
                        createOrderButton.isEnabled = true
                        createOrderButton.text = "Create"
                        Log.e("OrderDialogFragment", "Error adding client")
                        Toast.makeText(context, "Error adding client", Toast.LENGTH_SHORT).show()
                    }
                }

            } else {
                createOrderButton.isEnabled = true
                createOrderButton.text = "Create"
                Toast.makeText(context, "Please select a client", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
        }
    }

    private fun convertDateFormat(inputDate: String): String? {
        val originalFormat = SimpleDateFormat("dd.MM.yy", Locale.US)
        val targetFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = originalFormat.parse(inputDate)
        return date?.let { targetFormat.format(it) }
    }


    private fun addClient(newClient: ClientDTO, callback: (String?) -> Unit) {
        val call = clientAPI.addClientAndReturnId(newClient)
        call.enqueue(object : Callback<ClientsFragment.Client> {
            override fun onResponse(call: Call<ClientsFragment.Client>, response: Response<ClientsFragment.Client>) {
                if (response.isSuccessful) {
                    // Extract the clientId from the response body
                    Log.d("OrderDialogFragment", "Client added ${response.body()?.clientId}")
                    val clientId = response.body()?.clientId
                    callback(clientId)
                } else {
                    // Handle the error
                    if (response.code() == 400) {
                        callback(null)
                    }
                }
            }
            override fun onFailure(call: Call<ClientsFragment.Client>, t: Throwable) {
                // Handle the error
                callback(null)
            }
        })
    }


    private fun addOrder(order: OrderDTO, callback: (String?) -> Unit) {
        val call = orderAPI.addOrder(order)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    callback(null)
                    Log.d("OrderDialogFragment", "Order added successfully")

                } else {
                    // Handle the error
                    if (response.code() == 400) {
                        callback(response.errorBody()?.string())
                    }
                }
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                // Handle the error
                callback(t.message)
            }
        })
    }

    private fun notifyServerAboutNewOrder(order: OrderDTO) {
        // Convert the order to a JSON string
        val orderJson = Gson().toJson(order)

        // Send the orderJson string to the server over the WebSocket connection
        webSocket?.send(orderJson)
    }

    private fun connectWebSocket() {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("ws://192.168.68.56:8000/ws")
            .build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                // Connection opened
                Log.i("WebSocket", "Connection opened")
                this@OrderDialogFragment.webSocket = webSocket
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                // Handle incoming messages
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                Log.i("WebSocket", "Connection closed")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                Log.e("WebSocket", "Error: ${t.message}")
            }
        }

        client.newWebSocket(request, listener)
    }

    private fun mainActivateWatchers(){
        /////////// WATCH IF SELECTED PRODUCT CHANGED //////////////////
        sharedViewModel.selectedProduct.observe(viewLifecycleOwner) { product ->
            val lineItemProduct =
                LineItemProduct(product.productId, product.name, 0, product.imageUrl)

            if (selectedProducts.contains(lineItemProduct)) {
                return@observe
            }
            selectedProducts.add(lineItemProduct)
            selectedProductsAdapter.notifyItemInserted(selectedProducts.size - 1)
        }
        ////////////////////// END WATCH //////////////////////
    }



    private fun mainInitializeViewModelAndAdapter(view: View){
        ///////////// INIT SHARED VIEW MODEL //////////////////
        sharedViewModel = ViewModelProvider(requireActivity(), SharedViewModelFactory()).get(SharedViewModel::class.java)



        //////////// INIT ADAPTER AND RECYCLE VIEW //////////////////
        selectedProductsAdapter = OrderDialogProductAdapter(selectedProducts)
        productLineItemRecycleView = view.findViewById<RecyclerView>(R.id.productLineItemRecycleView)

        productLineItemRecycleView.layoutManager = LinearLayoutManager(context)

        productLineItemRecycleView.adapter = selectedProductsAdapter
        //////////////////////
    }


    @SuppressLint("SetTextI18n")
    private fun mainInterfaceActions(view: View){
        //////////// ADD CLIENT BUTTON //////////////////
        val addClientButton = view.findViewById<Button>(R.id.selectClientButton)
        addClientButton.setOnClickListener {
            val clientsFragment = ClientsFragment().apply {
                setClientSelectionListener(object : ClientsFragment.ClientSelectionListener {
                    override fun onClientSelected(client: ClientsFragment.Client) {
                        selectedClient = client
                        sharedViewModel.selectClient(client)
                    }
                })
            }
            (activity as MainActivity).switchFragment(clientsFragment)

            sharedViewModel.isClientSelectionListenerActive.value = true
        }
        //////////// END ADD CLIENT BUTTON //////////////////


        //// CHANGE CLIENT TEXTVIEW ////
        val selectedClientTextView = view.findViewById<TextView>(R.id.selectedClientTextView)
        sharedViewModel.selectedClient.observe(viewLifecycleOwner, Observer { client ->
            selectedClientTextView.text = client.firmName
        })
        //// END CHANGE CLIENT TEXTVIEW ////


        //////////// ADD BUTTON //////////////////
        val addProductsButton = view.findViewById<ImageButton>(R.id.addProductsButton)
        addProductsButton.setOnClickListener {
            val productsFragment = ProductsFragment().apply {
                setProductSelectionListener(object : ProductsFragment.ProductsSelectionListener {
                    override fun onProductSelected(product: ProductsFragment.Product) {
                        sharedViewModel.selectProduct(product)
                        isProductSelectionListenerActive = false
                    }
                })
            }
            (activity as MainActivity).switchFragment(productsFragment)

            // Set the flag to indicate that the ProductsFragment is in selection mode
            sharedViewModel.isProductSelectionListenerActive.value = true
        }
        ////////////////////// END ADD BUTTON //////////////////////
    }
    override fun onDestroyView() {
        super.onDestroyView()
    }

    data class LineItemProduct(
        var id: String,
        var name: String,
        var quantity: Int = 0,
        var imageUrl: String
    )

    override fun onClientSelected(client: ClientsFragment.Client) {

    }

    override fun onProductSelected(product: ProductsFragment.Product) {

    }
}