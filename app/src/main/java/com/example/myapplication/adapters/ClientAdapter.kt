package com.example.myapplication.adapters

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.getSystemService
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
        val contactLabel: TextView = view.findViewById(R.id.contactLabel)
        val contactPersonTextView: TextView = view.findViewById(R.id.contactPersonTextView)
        val phoneNumberTextView: TextView = view.findViewById(R.id.phoneNumberTextView)
        val locationLabel: TextView = view.findViewById(R.id.addressLabel)
        val locationTextView: TextView = view.findViewById(R.id.locationTextView)
        val btnOpenWaze: ImageButton = view.findViewById(R.id.btnOpenWaze)
        val phoneNumberContainer: View = view.findViewById(R.id.phoneNumberContainer)
        val dividerLine: View = view.findViewById(R.id.dividerLine)
        val addressManualTextView: TextView = view.findViewById(R.id.addressManualTextView)
        val addressManualLabel: TextView = view.findViewById(R.id.addressManualLabel)
        val detailsContainer: View = view.findViewById(R.id.detailsContainer)
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
        val (locationName, _) = fragment.extractLocationAndUrl(client.location)
        holder.locationTextView.text = locationName
        holder.btnOpenWaze.setOnClickListener {
            fragment.openWazeLocation(client.location)
        }
        holder.addressManualTextView.text = client.address
        holder.itemView.setOnLongClickListener { v ->
            showPopupMenu(v, holder.adapterPosition)
            true
        }
        holder.itemView.setOnClickListener {
            fragment.openAddClientDialog(client)
        }
        // Inside your Activity or Fragment, or ViewHolder class if using RecyclerView
        holder.phoneNumberContainer.setOnClickListener {
            // Get the phone number text from phoneNumberTextView
           fragment.copyToClipboard(holder.phoneNumberTextView.text.toString())
        }

        if (holder.locationTextView.text.toString().trim().isEmpty()) {
            holder.locationTextView.visibility = View.GONE
            holder.locationLabel.visibility = View.GONE
            holder.btnOpenWaze.visibility = View.GONE
        } else {
            holder.locationTextView.visibility = View.VISIBLE
            holder.locationLabel.visibility = View.VISIBLE
            holder.btnOpenWaze.visibility = View.VISIBLE
        }

        // Hide contactPersonTextView if empty
        if (holder.contactPersonTextView.text.toString().trim().isEmpty()) {
            holder.contactPersonTextView.visibility = View.GONE
            holder.contactLabel.visibility = View.GONE
        } else {
            holder.contactPersonTextView.visibility = View.VISIBLE
            holder.contactLabel.visibility = View.VISIBLE
        }

        if(holder.addressManualTextView.text.toString().trim().isEmpty()) {
            holder.addressManualTextView.visibility = View.GONE
            holder.addressManualLabel.visibility = View.GONE
        }
        else {
            holder.addressManualTextView.visibility = View.VISIBLE
            holder.addressManualLabel.visibility = View.VISIBLE
        }

        if(holder.contactPersonTextView.text.toString().trim().isEmpty() && holder.locationTextView.text.toString().trim().isEmpty() && holder.addressManualTextView.text.toString().trim().isEmpty()) {
            holder.dividerLine.visibility = View.GONE
            holder.detailsContainer.visibility = View.GONE
        }
        else {
            holder.dividerLine.visibility = View.VISIBLE
            holder.detailsContainer.visibility = View.VISIBLE
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
