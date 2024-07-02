package com.example.myapplication.adapters

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.fragments.OrderDialogFragment
import de.hdodenhof.circleimageview.CircleImageView
import kotlin.math.min

class OrderDialogProductAdapter(private val productList: MutableList<OrderDialogFragment.LineItemProduct>) :
    RecyclerView.Adapter<OrderDialogProductAdapter.ProductViewHolder>() {

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productName: TextView = itemView.findViewById(R.id.productName)
        val quantityEditText: EditText = itemView.findViewById(R.id.quantityEditText)
        val productImage: CircleImageView = itemView.findViewById(R.id.productImage)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.order_product_line_item, parent, false)
        return ProductViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val currentProduct = productList[position]
        holder.productName.text = currentProduct.name
        Glide.with(holder.productImage.context)
            .load(currentProduct.imageUrl)
            .into(holder.productImage)

        // Create a new TextWatcher
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // No action needed here
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // No action needed here
            }

            override fun afterTextChanged(s: Editable) {
                val quantity = s.toString().toIntOrNull()
                if (quantity != null) {
                    currentProduct.quantity = quantity
                }
            }
        }

        // Remove the TextWatcher before setting the text
        holder.quantityEditText.removeTextChangedListener(textWatcher)
        // If the quantity is zero, set the text to an empty string. Otherwise, set it to the quantity.
        holder.quantityEditText.setText(if (currentProduct.quantity == 0) "" else currentProduct.quantity.toString())
        // Add the TextWatcher back after setting the text
        holder.quantityEditText.addTextChangedListener(textWatcher)

        holder.itemView.setOnLongClickListener {
            showDeleteConfirmationDialog(holder.itemView.context, position)
            true
        }
    }


    private fun showDeleteConfirmationDialog(context: Context, position: Int) {
        AlertDialog.Builder(context).apply {
            setTitle("Delete Product")
            setMessage("Are you sure you want to remove this product from the order?")
            setPositiveButton("Yes") { _, _ ->
                productList.removeAt(position)
                notifyItemRemoved(position)
            }
            setNegativeButton("No", null)
        }.show()
    }

    override fun getItemCount() = min(7, productList.size)
}