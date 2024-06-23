package com.example.myapplication.api

import com.example.myapplication.entity.RecipeDTO
import com.example.myapplication.fragments.ProductDetailsFragment
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface RecipeAPI {
    @GET("recipe")
    fun getRecipes(): Call<Map<String, List<ProductDetailsFragment.Recipe>>>

    @GET("recipe/{productId}")
    fun getRecipeOfProduct(@Path("productId") productId: String): Call<List<ProductDetailsFragment.Recipe>>

    @DELETE("recipe/{productId}/{ingredientId}")
    fun deleteRecipe(@Path("productId") productId: String, @Path("ingredientId") ingredientId:String): Call<Void>

    @DELETE("recipe")
    fun deleteAllRecipes(): Call<Void>

    @POST("recipe")
    fun addRecipe(@Body newRecipe: RecipeDTO): Call<Void>

    @PUT("recipe/{productId}/{ingredientId}")
    fun updateRecipe(@Path("productId")productId: String, @Path("ingredientId")ingredientId: String, @Body updatedRecipe: RecipeDTO): Call<Void>
}