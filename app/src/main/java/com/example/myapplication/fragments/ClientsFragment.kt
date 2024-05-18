package com.example.myapplication.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.adapters.ClientsAdapter

class ClientsFragment : Fragment() {

    private lateinit var clientsRecyclerView: RecyclerView
    private lateinit var clientsAdapter: ClientsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_clients, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        clientsRecyclerView = view.findViewById(R.id.clientsRecyclerView)
        clientsRecyclerView.layoutManager = LinearLayoutManager(context)
        clientsAdapter = ClientsAdapter(listOf()) // Initialize with empty list or fetch clients
        clientsRecyclerView.adapter = clientsAdapter


//        val btnOpenWaze = view.findViewById<Button>(R.id.btnOpenWaze)
//        btnOpenWaze.setOnClickListener {
//            openWaze(46.765311906771785, 23.546572902000218)
//        }
    }


    fun openWaze(latitude: Double, longitude: Double) {
        val uri = Uri.parse("https://waze.com/ul?ll=$latitude,$longitude&navigate=yes")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.waze")
        try {
            startActivity(intent)
        } catch (e: android.content.ActivityNotFoundException) {
            // Fallback to opening the URL in a web browser
            val browserIntent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(browserIntent)
        }
    }
    data class Client(
        val clientId: String,
        val firmName: String,
        val contactPerson: String,
        val phoneNumber: String,
        val location: String,
        val latitude: Double,
        val longitude: Double
    )
}