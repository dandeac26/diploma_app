package com.example.myapplication.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.provider.ContactsContract
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
import com.example.myapplication.views.SharedViewModel
import com.example.myapplication.views.SharedViewModelFactory
import com.facebook.shimmer.ShimmerFrameLayout

class OrderDetailsFragment : Fragment() {

    private lateinit var orderDetailsRecyclerView: RecyclerView
    private lateinit var orderDetailsAdapter: OrderDetailsAdapter

    private lateinit var sharedViewModel: SharedViewModel

    private val REQUEST_READ_CONTACTS = 1


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
                // Handle discard action
            }

            val shareButton: Button = view.findViewById(R.id.shareButton)
            shareButton.setOnClickListener {
                // Handle share action
            }

            val completeButton: Button = view.findViewById(R.id.completeButton)
            completeButton.setOnClickListener {
                // Handle complete action
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
}