package com.example.myapplication.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.fragments.ProductsFragment.Product
import com.example.myapplication.fragments.ProductsFragment
import com.example.myapplication.api.BakeryAPI
import com.example.myapplication.dialog.ImagePreviewDialog
import com.example.myapplication.R
import com.example.myapplication.fragments.ClientsFragment
import de.hdodenhof.circleimageview.CircleImageView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProductAdapter(private val products: MutableList<Product>, private val bakeryAPI: BakeryAPI, private val fragment: ProductsFragment, private val onProductClickListener: OnProductClickListener) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productName: TextView = itemView.findViewById(R.id.productName)
        val productPrice: TextView = itemView.findViewById(R.id.productPrice)
        val productImage: CircleImageView = itemView.findViewById(R.id.productImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.product_item, parent, false)
        return ProductViewHolder(itemView)
    }

    interface OnProductClickListener {
        fun onProductClick(product: Product)
    }

    override fun getItemCount() = products.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        holder.productName.text = product.name
        holder.productPrice.text = product.price.toString() + " lei"

        Glide.with(holder.productImage.context)
            .load(product.imageUrl)
            .placeholder(R.drawable.placeholder_50)
            .into(holder.productImage)

        holder.itemView.setOnLongClickListener { v ->
            showPopupMenu(v, holder.adapterPosition)
            true
        }
        holder.productImage.setOnClickListener {
            val dialog = ImagePreviewDialog.newInstance(product.imageUrl ?: "")
            dialog.show(fragment.parentFragmentManager, "ImagePreviewDialog")
        }

        holder.itemView.setOnClickListener {
//            fragment.openAddProductDialog(product)
            onProductClickListener.onProductClick(product)
        }
    }

    private fun showPopupMenu(view: View, position: Int) {
        val popup = PopupMenu(view.context, view)
        val inflater: MenuInflater = popup.menuInflater
        inflater.inflate(R.menu.product_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_delete -> {
                    deleteProduct(position)
                    true
                }
                R.id.action_update -> {
                    updateProduct(position)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun deleteProduct(position: Int) {
        val product = products[position]
        AlertDialog.Builder(fragment.requireContext())
            .setTitle("Delete Product")
            .setMessage("Are you sure you want to delete this product?")
            .setPositiveButton("Yes") { _, _ ->
                val call = bakeryAPI.deleteProduct(product.productId)
                call.enqueue(object : Callback<Void> {
                    override fun onResponse(call: Call<Void>, response: Response<Void>) {
                        if (response.isSuccessful) {
                            products.removeAt(position)
                            notifyItemRemoved(position)

                            fragment.removeProductFromSearchLists(product.productId)
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

    fun updateProduct(position: Int) {
        val product = products[position]
        fragment.openAddProductDialog(product)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateProductsAfterSearch(newProducts: List<Product>) {
        products.clear()
        products.addAll(newProducts)
        notifyDataSetChanged()
    }
}