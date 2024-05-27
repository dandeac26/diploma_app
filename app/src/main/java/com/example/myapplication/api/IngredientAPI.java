package com.example.myapplication.api;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface IngredientAPI {
    @Multipart
    @POST("ingredients/import")
    Call<Void> uploadIngredientsFile(@Part MultipartBody.Part file);
}