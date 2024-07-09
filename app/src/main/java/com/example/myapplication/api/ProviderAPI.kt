package com.example.myapplication.api

import com.example.myapplication.dtos.ProviderDTO
import com.example.myapplication.fragments.StocksFragment
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ProviderAPI {
    @GET("providers")
    fun getProviders(): Call<List<StocksFragment.Provider>>

    @DELETE("providers/{providerId}")
    fun deleteProvider(@Path("providerId") providersId: String): Call<Void>

    @DELETE("providers")
    fun deleteAllProviders(): Call<Void>

    @POST("providers")
    fun addProvider(@Body newProvider: ProviderDTO): Call<Void>

    @PUT("providers/{providerId}")
    fun updateProvider(@Path("providerId")providersId: String, @Body updatedProvider: ProviderDTO): Call<Void>
}