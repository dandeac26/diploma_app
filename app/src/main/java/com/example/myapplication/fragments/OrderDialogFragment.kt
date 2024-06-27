package com.example.myapplication.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
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
    private lateinit var orderDetailsAPI: OrderAPI


    @SuppressLint("MissingInflatedId", "NotifyDataSetChanged")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_order, container, false)


        orderAPI = RetrofitInstance.getInstance(requireContext(), 8000).create(OrderAPI::class.java)
        connectWebSocket()
        orderDetailsAPI = RetrofitInstance.getInstance(requireContext(), 8080).create(OrderAPI::class.java)
        clientAPI = RetrofitInstance.getInstance(requireContext(), 8080).create(ClientAPI::class.java)

        mainInitializeViewModelAndAdapter(view)

        mainInterfaceActions(view)

        mainActivateWatchers()

        mainHandleOrderCreate(view)

        return view
    }


    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
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

            var selectedClientId: String? = null
            var selectedClientName: String? = null
            sharedViewModel.selectedClient.observe(viewLifecycleOwner) { client ->
                selectedClientId = client.clientId
                selectedClientName = client.firmName
            }

            val selectedClientTextView = view.findViewById<TextView>(R.id.selectedClientTextView)

            if(selectedClientTextView.text.isNotEmpty() && selectedClientTextView.text.toString() != selectedClientName) {
                val newClient = ClientDTO(
                    firmName = selectedClientTextView.text.toString(),
                    contactPerson = selectedClientTextView.text.toString(),
                    phoneNumber = "0000000000",
                    location = "Unknown",
                    latitude = 0.0,
                    longitude = 0.0,
                    address = "Unknown",
                    type = "SPECIAL"
                )

                addClient(newClient) { clientId ->
                    if (clientId != null) {
                        // Handle the error
                        Log.d("OrderDialogFragment", "Client added $clientId")

                        val order = OrderDTO(
                            clientId = clientId,
                            deliveryNeeded = false,
                            completionDate = dayDate,
                            completionTime = "10:00:00",
                            price = 0.0,
                            completed = false,
                        )

                        addOrder(order) { error, orderId ->
                            createOrderButton.isEnabled = true
                            createOrderButton.text = "Create"
                            if (error != null) {
                                // Handle the error
                                Log.e("OrderDialogFragment", "Error adding order: $error")
                            } else {

                                for (product in selectedProducts) {
                                    if(product.quantity > 0){
                                        Log.d("OrderDialogFragment", "Adding order detail for product: ${product.name}  for order $orderId")
                                        addOrderDetail(orderId!!, product.id, product.quantity) { orderDetailError ->
                                            if (orderDetailError != null) {
                                                // Handle the error
                                                Log.e("OrderDialogFragment", "Error adding order detail: $orderDetailError")
                                            } else {
                                                Log.d("OrderDialogFragment", "Order detail added successfully")
                                            }
                                        }
                                    }
                                }

                                notifyServerAboutNewOrder(order)

                                hideKeyboard(it)

                                selectedProducts.clear()
                                selectedProductsAdapter.notifyDataSetChanged()

                                selectedClientTextView.text = ""

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
                if(selectedClientId != null && selectedClientTextView.text.isNotEmpty()){
                    val order = OrderDTO(
                        clientId = selectedClientId!!,
                        deliveryNeeded = false,
                        completionDate = dayDate,
                        completionTime = "10:00:00",
                        price = 0.0,
                        completed = false,
                    )

                    addOrder(order) { error, orderId ->
                        createOrderButton.isEnabled = true
                        createOrderButton.text = "Create"
                        if (error != null) {
                            // Handle the error
                            Log.e("OrderDialogFragment", "Error adding order: $error")
                        } else {

                            for (product in selectedProducts) {
                                if(product.quantity > 0){
                                    Log.d("OrderDialogFragment", "Adding order detail for product: ${product.name}  for order $orderId")
                                    addOrderDetail(orderId!!, product.id, product.quantity) { orderDetailError ->
                                        if (orderDetailError != null) {
                                            // Handle the error
                                            Log.e("OrderDialogFragment", "Error adding order detail: $orderDetailError")
                                        } else {
                                            Log.d("OrderDialogFragment", "Order detail added successfully")
                                        }
                                    }
                                }
                            }

                            notifyServerAboutNewOrder(order)

                            hideKeyboard(it)

                            selectedProducts.clear()
                            selectedProductsAdapter.notifyDataSetChanged()

                            selectedClientTextView.text = ""

                            Toast.makeText(context, "Order added successfully", Toast.LENGTH_SHORT).show()
                            val dailyOrderFragment = DailyOrderFragment()
                            (activity as MainActivity).switchFragment(dailyOrderFragment)
                        }
                    }
//                    addOrder(order) { error, _ ->
//                        createOrderButton.isEnabled = true
//                        createOrderButton.text = "Create"
//                        if (error != null) {
//                            // Handle the error
//                            Log.e("OrderDialogFragment", "Error adding order: $error")
//                        } else {
//
//                            notifyServerAboutNewOrder(order)
//
//                            hideKeyboard(it)
//
//                            selectedProducts.clear()
//                            selectedProductsAdapter.notifyDataSetChanged()
//
//                            selectedClientTextView.text = ""
//
//                            Toast.makeText(context, "Order added successfully", Toast.LENGTH_SHORT).show()
//                            val dailyOrderFragment = DailyOrderFragment()
//                            (activity as MainActivity).switchFragment(dailyOrderFragment)
//                        }
//                    }
                }
                else
                {
                    Toast.makeText(context, "Please select a client", Toast.LENGTH_SHORT).show()
                }
                createOrderButton.isEnabled = true
                createOrderButton.text = "Create"

                return@setOnClickListener
            }
        }
    }

    private fun addOrderDetail(orderId: String, productId: String, quantity: Int, callback: (String?) -> Unit) {
        val orderDetail = OrdersFragment.OrderDetailProduct(orderId, productId, quantity)
        val call = orderAPI.addOrderDetails(orderId, orderDetail)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    callback(null)
                    Log.d("OrderDialogFragment", "Order detail added successfully")
                } else {
                    // Handle the error
                    callback(response.errorBody()?.string())
                }
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                // Handle the error
                callback(t.message)
            }
        })
    }



    private fun hideKeyboard(view: View) {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
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

    private fun addOrder(order: OrderDTO, callback: (String?, String?) -> Unit) {
        val call = orderAPI.addOrder(order)
        call.enqueue(object : Callback<OrdersFragment.OrderReq> {
            override fun onResponse(call: Call<OrdersFragment.OrderReq>, response: Response<OrdersFragment.OrderReq>) {
                if (response.isSuccessful) {
                    callback(null, response.body()?.orderId)
                    Log.d("OrderDialogFragment", "Order added successfully with id: ${response.body()}")
                } else {
                    // Handle the error
                    if (response.code() == 400) {
                        callback(response.errorBody()?.string(), null)
                    }
                }
            }
            override fun onFailure(call: Call<OrdersFragment.OrderReq>, t: Throwable) {
                // Handle the error
                callback(t.message, null)
            }
        })
    }

    private fun notifyServerAboutNewOrder(order: OrderDTO) {
        val orderJson = Gson().toJson(order)
        webSocket?.send(orderJson)
    }

//    private fun notifyServerAboutOrderDelete(orderId: String) {
//        webSocket?.send(orderId)
//    }

    private fun connectWebSocket() {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("ws://192.168.68.56:8000/ws")
            .build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
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



    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    private fun mainInitializeViewModelAndAdapter(view: View){
        ///////////// INIT SHARED VIEW MODEL //////////////////
        sharedViewModel = ViewModelProvider(requireActivity(), SharedViewModelFactory())[SharedViewModel::class.java]



        //////////// INIT ADAPTER AND RECYCLE VIEW //////////////////
        selectedProductsAdapter = OrderDialogProductAdapter(selectedProducts)
        productLineItemRecycleView = view.findViewById(R.id.productLineItemRecycleView)

        productLineItemRecycleView.layoutManager = LinearLayoutManager(context)

        productLineItemRecycleView.adapter = selectedProductsAdapter

        val newClient = ClientsFragment.Client(
            clientId = "1",
            firmName = "Client 1",
            contactPerson = "Contact Person 1",
            phoneNumber = "0000000000",
            location = "Location 1",
            latitude = 0.0,
            longitude = 0.0,
            address = "Address 1",
            type = "SPECIAL"
        )
        sharedViewModel.selectClient(newClient)

        val selectedClientTextView = view.findViewById<TextView>(R.id.selectedClientTextView)
        selectedClientTextView.text = ""

        selectedProducts.clear()
        selectedProductsAdapter.notifyDataSetChanged()

        //////////////////////
    }


    @SuppressLint("SetTextI18n")
    private fun mainInterfaceActions(view: View){
        //////////// ADD CLIENT BUTTON //////////////////
        val addClientButton = view.findViewById<Button>(R.id.selectClientButton)
        addClientButton.setOnClickListener {
            hideKeyboard(it)
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
        sharedViewModel.selectedClient.observe(viewLifecycleOwner) { client ->
            if (client.clientId != "1") {
                selectedClientTextView.text = client.firmName
            }
        }
        //// END CHANGE CLIENT TEXTVIEW ////


        //////////// ADD BUTTON //////////////////
        val addProductsButton = view.findViewById<ImageButton>(R.id.addProductsButton)
        addProductsButton.setOnClickListener {
            hideKeyboard(it)
            val productsFragment = ProductsFragment().apply {
                setProductSelectionListener(object : ProductsFragment.ProductsSelectionListener {
                    override fun onProductSelected(product: ProductsFragment.Product) {
                        sharedViewModel.selectProduct(product)
                        isProductSelectionListenerActive = false
                    }
                })
            }
            (activity as MainActivity).switchFragment(productsFragment)

            sharedViewModel.isProductSelectionListenerActive.value = true
        }
        ////////////////////// END ADD BUTTON //////////////////////
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