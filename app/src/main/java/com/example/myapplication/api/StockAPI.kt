package com.example.myapplication.api

import com.example.myapplication.dtos.StockDTO
import com.example.myapplication.fragments.StocksFragment
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface StockAPI {
    @GET("stock")
    fun getStocks(): Call<Map<String, List<StocksFragment.Stock>>>

    @GET("stock/unique-stocks")
    fun getUniqueStocks(): Call<List<StocksFragment.Stock>>

    @DELETE("stock/{ingredientId}/{providerId}")
    fun deleteStock(@Path("ingredientId") ingredientId: String, @Path("providerId") providerId:String): Call<Void>

    @DELETE("stock")
    fun deleteAllStocks(): Call<Void>

    @POST("stock")
    fun addStock(@Body newStock: StockDTO): Call<Void>

    @PUT("stock/{ingredientId}/{providerId}")
    fun updateStock(@Path("ingredientId")ingredientId: String,@Path("providerId")providerId: String,  @Body updatedStock: StockDTO): Call<Void>
}