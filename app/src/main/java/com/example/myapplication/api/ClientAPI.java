package com.example.myapplication.api;

import com.example.myapplication.fragments.ClientsFragment;
import com.example.myapplication.fragments.OrdersFragment;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ClientAPI {
    @GET("/clients")
    Call<List<ClientsFragment.Client>> getClients();
}
