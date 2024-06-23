package com.example.myapplication.api

import com.example.myapplication.entity.ClientDTO
import com.example.myapplication.fragments.ClientsFragment

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ClientAPI {
    @GET("client")
    fun getClients(): Call<List<ClientsFragment.Client>>

    @DELETE("client/{clientId}")
    fun deleteClient(@Path("clientId") clientId: String): Call<Void>

    @DELETE("client")
    fun deleteAllClients(): Call<Void>

    @POST("client")
    fun addClient(@Body newClient:ClientDTO): Call<Void>

    @POST("client")
    fun addClientAndReturnId(@Body newClient:ClientDTO): Call<ClientsFragment.Client>

    @PUT("client/{clientId}")
    fun updateClient(@Path("clientId")clientId: String, @Body updatedClient: ClientDTO): Call<Void>
}
