package com.example.myapplication.views

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myapplication.fragments.ClientsFragment
import com.example.myapplication.fragments.OrdersFragment
import com.example.myapplication.fragments.ProductsFragment

class SharedViewModel : ViewModel() {





    val refreshOrderDetailsTrigger: MutableLiveData<Boolean> = MutableLiveData()
    val refreshClientsTrigger = MutableLiveData<Boolean>()
    val refreshProductsTrigger = MutableLiveData<Boolean>()
    val refreshOrdersTrigger = MutableLiveData<Boolean>()
    val refreshStocksTrigger = MutableLiveData<Boolean>()
    val refreshDailyOrdersTrigger = MutableLiveData<Boolean>()
    val refreshHomeTrigger = MutableLiveData<Boolean>()

    val selectedDate: MutableLiveData<String> = MutableLiveData()
    val selectedOrder = MutableLiveData<OrdersFragment.Order>()





    private val _selectedProduct = MutableLiveData<ProductsFragment.Product>()
    val selectedProduct: LiveData<ProductsFragment.Product> get() = _selectedProduct

    fun selectProduct(product: ProductsFragment.Product) {
        _selectedProduct.value = product
    }

    private val _selectedClient = MutableLiveData<ClientsFragment.Client>()
    val selectedClient: LiveData<ClientsFragment.Client> get() = _selectedClient

    fun selectClient(client: ClientsFragment.Client) {
        _selectedClient.value = client
    }

    val isClientSelectionListenerActive = MutableLiveData<Boolean>()
    val isProductSelectionListenerActive = MutableLiveData<Boolean>()


    val onBackPressed = MutableLiveData<Boolean>()

    fun handleBackPress() {
        onBackPressed.value = true
    }

    fun saveOrder(orderProductAndQuantities: Map<String, Int>) {

    }
}