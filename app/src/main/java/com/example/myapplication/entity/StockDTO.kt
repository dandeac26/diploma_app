package com.example.myapplication.entity

data class StockDTO(
    val ingredientId: String,
    val providerId: String,
    val quantity: Int,
    val price: Double,
    val maxQuantity: Int
)
