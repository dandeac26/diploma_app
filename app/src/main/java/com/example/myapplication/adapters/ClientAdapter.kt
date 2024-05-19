package com.example.myapplication.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.fragments.ClientsFragment.Client
import com.example.myapplication.fragments.ClientsFragment
import com.example.myapplication.api.ClientAPI
import com.example.myapplication.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ClientAdapter(private val clients: MutableList<ClientsFragment.Client>, private val clientAPI: ClientAPI, private val fragment: ClientsFragment) : RecyclerView.Adapter<ClientAdapter.ClientViewHolder>() {

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
            // open google maps with the client's location
            fragment.openWazeLocation(client.location)
            // Open Waze with the client's location
//            client.latitude?.let { latitude ->
//                client.longitude?.let { longitude ->
//                    fragment.openWaze(latitude, longitude)
//
//                }
//            }
        }
        holder.itemView.setOnLongClickListener { v ->
            showPopupMenu(v, holder.adapterPosition)
            true
        }
        holder.itemView.setOnClickListener {
            fragment.openAddClientDialog(client)
        }
    }

    override fun getItemCount() = clients.size

    private fun showPopupMenu(view: View, position: Int) {
        val popup = PopupMenu(view.context, view)
        val inflater: MenuInflater = popup.menuInflater
        inflater.inflate(R.menu.client_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_delete -> {
                    deleteClient(position)
                    true
                }
                R.id.action_update -> {
                    updateClient(position)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun deleteClient(position: Int) {
        val client = clients[position]
        AlertDialog.Builder(fragment.requireContext())
            .setTitle("Delete Client")
            .setMessage("Are you sure you want to delete this client?")
            .setPositiveButton("Yes") { _, _ ->
                val call = clientAPI.deleteClient(client.clientId)
                call.enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            clients.removeAt(position)
                            notifyItemRemoved(position)
                        } else {
                            // handle the error
                        }
                    }
                    override fun onFailure(call: Call<Void>, t: Throwable) {
                        // handle the error
                    }
                })
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun updateClient(position: Int) {
        val client = clients[position]
        fragment.openAddClientDialog(client)
    }
}
