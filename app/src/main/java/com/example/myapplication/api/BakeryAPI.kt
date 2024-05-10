package com.example.myapplication.api

import com.example.myapplication.MainActivity
import com.example.myapplication.activities.ProductActivity
import com.example.myapplication.entity.ProductDTO
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface BakeryAPI {
    @GET("product")
    fun getProducts(): Call<List<ProductActivity.Product>>

    @DELETE("product/{productId}")
    fun deleteProduct(@Path("productId") productId: String): Call<Void>

    @POST("product")
    fun addProduct(@Body newProduct: ProductDTO): Call<Void>

    @PUT("product/{productId}")
    fun updateProduct(@Path("productId") productId: String, @Body updatedProduct: ProductDTO): Call<Void>
}