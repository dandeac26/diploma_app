package com.example.myapplication.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.adapters.OrderDialogProductAdapter
import com.example.myapplication.adapters.ProductAdapter
import com.example.myapplication.views.SharedViewModel
import com.example.myapplication.views.SharedViewModelFactory

class OrderDialogFragment : DialogFragment(), ClientsFragment.ClientSelectionListener, ProductsFragment.ProductsSelectionListener {

    private lateinit var selectedClient: ClientsFragment.Client
//    sharedViewModel
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var productLineItemRecycleView : RecyclerView

    private lateinit var selectedProductsAdapter: OrderDialogProductAdapter
    private val selectedProducts = mutableListOf<LineItemProduct>()

    private var selectedProductObserver: Observer<ProductsFragment.Product>? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_order, container, false)

        val factory = SharedViewModelFactory()
        sharedViewModel = ViewModelProvider(requireActivity(), factory).get(SharedViewModel::class.java)


        val selectedClientTextView = view.findViewById<TextView>(R.id.selectedClientTextView)
        sharedViewModel.selectedClient.observe(viewLifecycleOwner, Observer { client ->
            selectedClientTextView.text = client.firmName
        })

        selectedProductsAdapter = OrderDialogProductAdapter(selectedProducts)
        productLineItemRecycleView = view.findViewById<RecyclerView>(R.id.productLineItemRecycleView)

        productLineItemRecycleView.layoutManager = LinearLayoutManager(context)

        productLineItemRecycleView.adapter = selectedProductsAdapter



        val selectClientButton = view.findViewById<Button>(R.id.selectClientButton)
        selectClientButton.setOnClickListener {
            sharedViewModel.isClientSelectionListenerActive.value = true
            val clientsFragment = ClientsFragment().apply {
                isClientSelectionListenerActive = true
                setClientSelectionListener(this@OrderDialogFragment)

            }
            (activity as MainActivity).switchFragment(clientsFragment)
        }

        val addProductsButton = view.findViewById<Button>(R.id.addProductsButton)
        addProductsButton.setOnClickListener {
            sharedViewModel.isProductSelectionListenerActive.value = true
            val productsFragment = ProductsFragment().apply {
                isProductSelectionListenerActive = true
                setProductSelectionListener(this@OrderDialogFragment)

            }
            (activity as MainActivity).switchFragment(productsFragment)
        }

        val createOrderButton = view.findViewById<Button>(R.id.createOrderButton)
        createOrderButton.setOnClickListener {
            // Call createOrder function with selectedClient and selectedProducts
        }

        selectedProductObserver = Observer { product ->
            // create line item product
            val lineItemProduct = LineItemProduct(product.productId, product.name, 0, product.imageUrl)
            Log.d("OrderDialogFragment", "Selected product: $lineItemProduct")
            selectedProducts.add(lineItemProduct)
            selectedProductsAdapter.notifyItemInserted(selectedProducts.size - 1)
        }

        sharedViewModel.selectedProduct.observe(viewLifecycleOwner, selectedProductObserver!!)


        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        sharedViewModel.selectedProduct.removeObserver(selectedProductObserver!!)
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