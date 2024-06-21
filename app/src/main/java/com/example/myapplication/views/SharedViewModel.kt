package com.example.myapplication.views

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myapplication.fragments.ClientsFragment
import com.example.myapplication.fragments.OrdersFragment
import com.example.myapplication.fragments.ProductsFragment

class SharedViewModel : ViewModel() {



    private val _selectedProduct = MutableLiveData<ProductsFragment.Product>()
    val selectedProduct: LiveData<ProductsFragment.Product> get() = _selectedProduct

    fun selectProduct(product: ProductsFragment.Product) {
        _selectedProduct.value = product
    }


    val refreshOrderDetailsTrigger: MutableLiveData<Boolean> = MutableLiveData()
    val refreshClientsTrigger = MutableLiveData<Boolean>()
    val refreshProductsTrigger = MutableLiveData<Boolean>()
    val refreshOrdersTrigger = MutableLiveData<Boolean>()
    val refreshStocksTrigger = MutableLiveData<Boolean>()
    val refreshDailyOrdersTrigger = MutableLiveData<Boolean>()
    val refreshHomeTrigger = MutableLiveData<Boolean>()
    val selectedDate: MutableLiveData<String> = MutableLiveData()
    val selectedOrder = MutableLiveData<OrdersFragment.Order>()
    val selectedClient = MutableLiveData<ClientsFragment.Client>()
//    val selectedProduct = MutableLiveData<ProductsFragment.Product>()
    val onBackPressed = MutableLiveData<Boolean>()

    val isClientSelectionListenerActive = MutableLiveData<Boolean>()
    val isProductSelectionListenerActive = MutableLiveData<Boolean>()

    fun handleBackPress() {
        onBackPressed.value = true
    }

    fun saveOrder(orderProductAndQuantities: Map<String, Int>) {

    }
}