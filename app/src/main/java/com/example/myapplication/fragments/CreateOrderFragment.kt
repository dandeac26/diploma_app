package com.example.myapplication.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.R
import com.example.myapplication.views.SharedViewModel
import com.example.myapplication.views.SharedViewModelFactory

class CreateOrderFragment : Fragment() {
    private lateinit var sharedViewModel: SharedViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_create_order, container, false)
    }

    @SuppressLint("CutPasteId")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val factory = SharedViewModelFactory()
        sharedViewModel = ViewModelProvider(requireActivity(), factory).get(SharedViewModel::class.java)

        val clientNameTextView = view.findViewById<TextView>(R.id.clientNameTextView)

        sharedViewModel.selectedClient.observe(viewLifecycleOwner) { client ->
            if (client != null) {
                clientNameTextView.text = client.firmName
            }
        }

        val productsTable = view.findViewById<TableLayout>(R.id.orderTable)

        val productNames = listOf("BRANZOICI", "BATON CU MAC", "COVRIG POLONEZ", "COVRIG CU SARE" , "COVRIG CU SUSAN", "TRIGON CU BRANZA", "STRUDEL CU BRANZA", "STRUDEL CU MAR", "STRUDEL CU VANILIE", "STRUDEL CU VISINE", "STRUDEL CU CAISE", "STRUDEL CU CIOCO", "STRUDEL CU CARAMEL", "FOIETAJ SUNCA&CAS", "PATEU CU CIUPERCI", "CUIB CU NUCA","CUIB CU CIOCO", "CROISSANT VANILIE", "CROISSANT VAN&CIOCO", "CROISSANT CAS", "PATEU CU TELEMEA", "PATEU PIZZA" )

        for ((index, productName) in productNames.withIndex()) {
            val inflater = LayoutInflater.from(context)
            val tableRow = inflater.inflate(R.layout.create_order_row, productsTable, false)

            val productNameTextView = tableRow.findViewById<TextView>(R.id.createOrderProductName)
            productNameTextView.text = productName

            val productQuantityEditText = tableRow.findViewById<EditText>(R.id.createOrderQuantity)
            productQuantityEditText.inputType = InputType.TYPE_CLASS_NUMBER
            if (index % 2 == 0) {
                tableRow.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.order_details_header))
            } else {
                tableRow.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.transparent))
            }
            productsTable.addView(tableRow)
        }

        val saveButton = view.findViewById<TextView>(R.id.submitOrderButton)
        saveButton.setOnClickListener {
//            sharedViewModel.saveOrder(getOrderProductAndQuantities())
        }
    }

    fun getOrderProductAndQuantities(): Map<String, Int> {
        val productsTable = view?.findViewById<TableLayout>(R.id.orderTable)
        val productAndQuantities = mutableMapOf<String, Int>()

        for (i in 0 until productsTable?.childCount!!) {
            val tableRow = productsTable.getChildAt(i) as TableRow
            val productName = (tableRow.getChildAt(0) as TextView).text.toString()
            val productQuantity = (tableRow.getChildAt(1) as EditText).text.toString().toIntOrNull() ?: 0

            if (productQuantity > 0) {
                productAndQuantities[productName] = productQuantity
            }
        }

        return productAndQuantities
    }
}