package com.example.myapplication.api

import com.example.myapplication.dtos.OrderDTO
import com.example.myapplication.fragments.OrdersFragment
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface OrderAPI {
    @GET("orders")
    fun getOrders(): Call<List<OrdersFragment.Order>>

    @GET("orders/byDate/{completionDate}")
    fun getOrdersByDate(@Path("completionDate") completionDate: String): Call<List<OrdersFragment.Order>>

    @DELETE("orders/{orderId}")
    fun deleteOrder(@Path("orderId") orderId: String): Call<Void>

    @DELETE("orders")
    fun deleteAllOrders(): Call<Void>

    @POST("orders")
    fun addOrder(@Body newOrder: OrderDTO): Call<OrdersFragment.OrderReq>

    @POST("orders/{orderId}/details")
    fun addOrderDetails(@Path("orderId") orderId: String, @Body orderDetails: OrdersFragment.OrderDetailProduct): Call<Void>

    @PUT("orders/{orderId}")
    fun updateOrder(@Path("orderId") orderId: String, @Body updatedOrder: OrderDTO): Call<Void>
}
