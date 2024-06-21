package com.example.myapplication.adapters

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.fragments.OrderDialogFragment
import com.example.myapplication.fragments.ProductsFragment.Product
import de.hdodenhof.circleimageview.CircleImageView

class OrderDialogProductAdapter(private val productList: List<OrderDialogFragment.LineItemProduct>) :
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

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val currentProduct = productList[position]
        holder.productName.text = currentProduct.name

        Glide.with(holder.productImage.context)
            .load(currentProduct.imageUrl)
            .into(holder.productImage)

        holder.quantityEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // No action needed here
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // No action needed here
            }

            override fun afterTextChanged(s: Editable) {
                val quantity = s.toString().toIntOrNull()
                if (quantity != null) {
                    // Update the quantity for the product
                    currentProduct.quantity = quantity
                }
            }
        })
    }

    override fun getItemCount() = productList.size
}