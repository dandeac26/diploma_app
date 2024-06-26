package com.example.myapplication.views

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myapplication.adapters.ShiftProductsAdapter
import com.example.myapplication.api.OrderAPI
import com.example.myapplication.api.RecipeAPI
import com.example.myapplication.fragments.ClientsFragment
import com.example.myapplication.fragments.OrdersFragment
import com.example.myapplication.fragments.ProductDetailsFragment
import com.example.myapplication.fragments.ProductsFragment
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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



    private val _orders = MutableLiveData<List<OrdersFragment.Order>>()
    val orders: LiveData<List<OrdersFragment.Order>> get() = _orders

    fun setOrders(orders: List<OrdersFragment.Order>) {
        val currentOrders = _orders.value?.toMutableList() ?: mutableListOf()
        val newOrdersDate = orders.firstOrNull()?.completionDate

        if (newOrdersDate != null) {
            currentOrders.removeAll { it.completionDate == newOrdersDate }
        }

        currentOrders.addAll(orders)
        _orders.value = currentOrders
    }



    val _allShiftProducts = MutableLiveData<List<Pair<OrdersFragment.Product, Int>>>()
    fun setAllShiftProducts(products: List<Pair<OrdersFragment.Product, Int>>){
        _allShiftProducts.value = products
    }



    val ingredientQuantities = MutableLiveData<Map<String, Int>>()

    fun calculateIngredientQuantities(recipeAPI: RecipeAPI) {
        val quantities = mutableMapOf<String, Int>()
        _allShiftProducts.value?.forEach { (product, productQuantity) ->
            recipeAPI.getRecipeOfProduct(product.productId).enqueue(object : Callback<List<ProductDetailsFragment.Recipe>> {
                override fun onResponse(call: Call<List<ProductDetailsFragment.Recipe>>, response: Response<List<ProductDetailsFragment.Recipe>>) {
                    if (response.isSuccessful) {
                        val recipes = response.body()
                        if (recipes != null) {
                            for (recipe in recipes) {
                                val ingredientId = recipe.ingredientId
                                val ingredientQuantity = recipe.quantity
                                quantities[ingredientId] = quantities.getOrDefault(ingredientId, 0) + (ingredientQuantity * productQuantity).toInt()
                            }
                        }
                        ingredientQuantities.value = quantities
                    }
                }

                override fun onFailure(call: Call<List<ProductDetailsFragment.Recipe>>, t: Throwable) {
                    // Handle the error
                }
            })
        }
    }

    val onBackPressed = MutableLiveData<Boolean>()

    fun handleBackPress() {
        onBackPressed.value = true
    }

    fun saveOrder(orderProductAndQuantities: Map<String, Int>) {

    }

    val isLoadingOrders = MutableLiveData<Boolean>(false)

    fun fetchOrdersByDate(orderAPI: OrderAPI, date: String) {
        isLoadingOrders.value = true
        val call = orderAPI.getOrdersByDate(date)
        call.enqueue(object : Callback<List<OrdersFragment.Order>> {
            override fun onResponse(
                call: Call<List<OrdersFragment.Order>>,
                response: Response<List<OrdersFragment.Order>>
            ) {
                if (response.isSuccessful) {
                    val ordersResponse = response.body()
                    if (ordersResponse != null) {
                        setOrders(ordersResponse)
//                        val allProducts = ordersResponse
//                            .flatMap { it.orderDetails }
//                            .groupBy { it.product }
//                            .map { (product, orderDetails) -> Pair(product, orderDetails.sumOf { it.quantity }) }
//
//                        setAllShiftProducts(allProducts)
                    }
                } else {
                    // Handle the error
                    Log.e(
                        "SharedViewModel",
                        "Error fetching orders: ${response.errorBody()?.string()}"
                    )
                }
                isLoadingOrders.value = false
            }

            override fun onFailure(call: Call<List<OrdersFragment.Order>>, t: Throwable) {
                // Handle the error
                isLoadingOrders.value = false
            }
        })
    }
}