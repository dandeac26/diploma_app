package com.example.myapplication.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.fragments.ClientsFragment

class ClientsAdapter(private val clients: List<ClientsFragment.Client>) :
    RecyclerView.Adapter<ClientsAdapter.ClientViewHolder>() {

    class ClientViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val firmNameTextView: TextView = view.findViewById(R.id.firmNameTextView)
        val contactPersonTextView: TextView = view.findViewById(R.id.contactPersonTextView)
        val phoneNumberTextView: TextView = view.findViewById(R.id.phoneNumberTextView)
        val locationTextView: TextView = view.findViewById(R.id.locationTextView)
        val btnOpenWaze: Button = view.findViewById(R.id.btnOpenWaze)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClientViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.client_item, parent, false)
        return ClientViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClientViewHolder, position: Int) {
        val client = clients[position]
        holder.firmNameTextView.text = client.firmName
        holder.contactPersonTextView.text = client.contactPerson
        holder.phoneNumberTextView.text = client.phoneNumber
        holder.locationTextView.text = client.location
        holder.btnOpenWaze.setOnClickListener {
            // Open Waze with the client's location
            client.latitude?.let { latitude ->
                client.longitude?.let { longitude ->
                    (holder.itemView.context as ClientsFragment).openWaze(latitude, longitude)
                }
            }
        }
    }

    override fun getItemCount() = clients.size
}