package com.example.myapplication.api

import com.example.myapplication.entity.IngredientDTO
import com.example.myapplication.fragments.StocksFragment
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface IngredientsAPI {
    @GET("ingredients")
    fun getIngredients(): Call<List<StocksFragment.Ingredient>>

    @DELETE("ingredients/{ingredientId}")
    fun deleteIngredient(@Path("ingredientId") ingredientsId: String): Call<Void>

    @DELETE("ingredients")
    fun deleteAllIngredients(): Call<Void>

    @POST("ingredients")
    fun addIngredient(@Body newIngredient: IngredientDTO): Call<Void>

    @PUT("ingredients/{ingredientId}")
    fun updateIngredient(@Path("ingredientId")ingredientsId: String, @Body updatedIngredient: IngredientDTO): Call<Void>
}