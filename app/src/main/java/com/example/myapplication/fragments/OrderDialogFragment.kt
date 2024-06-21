package com.example.myapplication.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
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

    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var productLineItemRecycleView : RecyclerView

    private lateinit var selectedProductsAdapter: OrderDialogProductAdapter
    private val selectedProducts = mutableListOf<LineItemProduct>()



    @SuppressLint("MissingInflatedId")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_order, container, false)

        mainInitializeViewModelAndAdapter(view)

        mainInterfaceActions(view)

        mainActivateWatchers()

        return view
    }


    private fun mainActivateWatchers(){
        /////////// WATCH IF SELECTED PRODUCT CHANGED //////////////////
        sharedViewModel.selectedProduct.observe(viewLifecycleOwner) { product ->
            val lineItemProduct =
                LineItemProduct(product.productId, product.name, 0, product.imageUrl)

            if (selectedProducts.contains(lineItemProduct)) {
                return@observe
            }
            selectedProducts.add(lineItemProduct)
            selectedProductsAdapter.notifyItemInserted(selectedProducts.size - 1)
        }
        ////////////////////// END WATCH //////////////////////
    }



    private fun mainInitializeViewModelAndAdapter(view: View){
        ///////////// INIT SHARED VIEW MODEL //////////////////
        sharedViewModel = ViewModelProvider(requireActivity(), SharedViewModelFactory()).get(SharedViewModel::class.java)



        //////////// INIT ADAPTER AND RECYCLE VIEW //////////////////
        selectedProductsAdapter = OrderDialogProductAdapter(selectedProducts)
        productLineItemRecycleView = view.findViewById<RecyclerView>(R.id.productLineItemRecycleView)

        productLineItemRecycleView.layoutManager = LinearLayoutManager(context)

        productLineItemRecycleView.adapter = selectedProductsAdapter
        //////////////////////
    }


    @SuppressLint("SetTextI18n")
    private fun mainInterfaceActions(view: View){
        //////////// ADD CLIENT BUTTON //////////////////
        val addClientButton = view.findViewById<Button>(R.id.selectClientButton)
        addClientButton.setOnClickListener {
            val clientsFragment = ClientsFragment().apply {
                setClientSelectionListener(object : ClientsFragment.ClientSelectionListener {
                    override fun onClientSelected(client: ClientsFragment.Client) {
                        selectedClient = client
                        sharedViewModel.selectClient(client)
                    }
                })
            }
            (activity as MainActivity).switchFragment(clientsFragment)

            sharedViewModel.isClientSelectionListenerActive.value = true
        }
        //////////// END ADD CLIENT BUTTON //////////////////


        //// CHANGE CLIENT TEXTVIEW ////
        val selectedClientTextView = view.findViewById<TextView>(R.id.selectedClientTextView)
        sharedViewModel.selectedClient.observe(viewLifecycleOwner, Observer { client ->
            selectedClientTextView.text = client.firmName
        })
        //// END CHANGE CLIENT TEXTVIEW ////


        //////////// ADD BUTTON //////////////////
        val addProductsButton = view.findViewById<ImageButton>(R.id.addProductsButton)
        addProductsButton.setOnClickListener {
            val productsFragment = ProductsFragment().apply {
                setProductSelectionListener(object : ProductsFragment.ProductsSelectionListener {
                    override fun onProductSelected(product: ProductsFragment.Product) {
                        sharedViewModel.selectProduct(product)
                        isProductSelectionListenerActive = false
                    }
                })
            }
            (activity as MainActivity).switchFragment(productsFragment)

            // Set the flag to indicate that the ProductsFragment is in selection mode
            sharedViewModel.isProductSelectionListenerActive.value = true
        }
        ////////////////////// END ADD BUTTON //////////////////////
    }
    override fun onDestroyView() {
        super.onDestroyView()
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