package com.example.myapplication.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.adapters.OrderDetailsAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.MainActivity
import com.example.myapplication.api.OrderAPI
import com.example.myapplication.config.RetrofitInstance
import com.example.myapplication.views.SharedViewModel
import com.example.myapplication.views.SharedViewModelFactory
import com.facebook.shimmer.ShimmerFrameLayout
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OrderDetailsFragment : Fragment() {

    private lateinit var orderDetailsRecyclerView: RecyclerView
    private lateinit var orderDetailsAdapter: OrderDetailsAdapter

    private lateinit var sharedViewModel: SharedViewModel

    private val REQUEST_READ_CONTACTS = 1

    private lateinit var orderAPI: OrderAPI
    private var webSocket: WebSocket? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_order_details, container, false)
    }

    @SuppressLint("CutPasteId", "SetTextI18n", "DefaultLocale")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val factory = SharedViewModelFactory()
        sharedViewModel = ViewModelProvider(requireActivity(), factory).get(SharedViewModel::class.java)

        sharedViewModel.selectedOrder.observe(viewLifecycleOwner) { order ->

            orderAPI = RetrofitInstance.getInstance(requireContext(), 8000).create(OrderAPI::class.java)
            connectWebSocket()
            /// HEADER
            view.findViewById<TextView>(R.id.clientName).text = order.clientName
            view.findViewById<TextView>(R.id.orderClientPhoneNumberTextView).text = order.clientPhoneNumber

            val phoneNumberButton: TextView = view.findViewById(R.id.orderClientPhoneNumberTextView)
            phoneNumberButton.setOnClickListener {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.READ_CONTACTS), REQUEST_READ_CONTACTS)
                } else {
                    openContact(order.clientPhoneNumber)
                }
            }

            /// BODY
            orderDetailsAdapter = OrderDetailsAdapter(order.orderDetails)
            orderDetailsRecyclerView = view.findViewById(R.id.orderDetailsRecyclerView)
            orderDetailsRecyclerView.layoutManager = LinearLayoutManager(context)
            orderDetailsRecyclerView.adapter = orderDetailsAdapter

            /// FOOTER
            val totalPriceTextView: TextView = view.findViewById(R.id.totalPriceTextView)
            totalPriceTextView.text = "TOTAL: ${String.format("%.2f", order.price)} lei"

            val clientLocationAndDeliveryTextView: TextView = view.findViewById(R.id.clientLocationAndDeliveryTextView)
            if (order.deliveryNeeded) {
                clientLocationAndDeliveryTextView.text = "Delivery Needed to ${order.clientLocation}"
            } else {
                clientLocationAndDeliveryTextView.text = ""
            }

            val discardButton: Button = view.findViewById(R.id.discardButton)
            discardButton.setOnClickListener {
                AlertDialog.Builder(context)
                    .setTitle("Delete Order")
                    .setMessage("Are you sure you want to delete this order?")
                    .setPositiveButton("Yes") { _, _ ->
                        deleteOrder(order.orderId) { error ->
                            if (error == null) {
                                requireActivity().onBackPressed()
                            } else {
                                Toast.makeText(context, "Error deleting order: $error", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .setNegativeButton("No", null)
                    .show()
            }

            val shareButton: Button = view.findViewById(R.id.shareButton)
            shareButton.setOnClickListener {
                // Handle share action
            }

            val completeButton: Button = view.findViewById(R.id.completeButton)
            completeButton.setOnClickListener {
                sharedViewModel.selectedOrder.value?.let { order ->
                    (activity as MainActivity).supportFragmentManager.fragments.lastOrNull { it is DailyOrderFragment }?.let { fragment ->
                        (fragment as DailyOrderFragment).updateOrderCompleted(order, true)
                    }
                    requireActivity().onBackPressed()
                }
            }
        }

        sharedViewModel.selectedDate.observe(viewLifecycleOwner) { date ->
            view.findViewById<TextView>(R.id.dayTitle).text = date
        }

        val backButton = view.findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }

    }

    private fun notifyOrderDeleted() {
        webSocket?.send("Order deleted")
    }


    private fun connectWebSocket() {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("ws://192.168.68.56:8000/ws")
            .build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                Log.i("WebSocket", "Connection opened")
                this@OrderDetailsFragment.webSocket = webSocket
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


    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sharedViewModel.selectedOrder.value?.let { openContact(it.clientPhoneNumber) }
            } else {
                Toast.makeText(context, "Permission to read contacts was denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("Range")
    private fun openContact(phoneNumber: String) {
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
        val projection = arrayOf(ContactsContract.PhoneLookup._ID)
        val cursor = requireContext().contentResolver.query(uri, projection, null, null, null)
        if (cursor != null && cursor.moveToFirst()) {
            val contactId = cursor.getLong(cursor.getColumnIndex(ContactsContract.PhoneLookup._ID))
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId.toString())
            startActivity(intent)
            cursor.close()
        } else {
            val clipboard = getSystemService(requireContext(), ClipboardManager::class.java)
            val clip = ClipData.newPlainText("phone number", phoneNumber)
            clipboard?.setPrimaryClip(clip)
            Toast.makeText(context, "Contact doesn't exist. Phone number copied to clipboard.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteOrder(orderId: String, callback: (String?) -> Unit) {
        val call = orderAPI.deleteOrder(orderId)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    callback(null)
                    Log.d("OrderDialogFragment", "Order deleted successfully")
                } else {
                    // Handle the error
                    Log.d("OrderDialogFragment", "Error deleting order: ${response.errorBody()?.string()}")
                    callback(response.errorBody()?.string())
                }
            }
            override fun onFailure(call: Call<Void>, t: Throwable) {
                // Handle the error
                callback(t.message)
            }
        })
    }
}