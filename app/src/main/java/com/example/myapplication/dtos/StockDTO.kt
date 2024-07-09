package com.example.myapplication.dtos

data class StockDTO(
    val ingredientId: String,
    val providerId: String,
    val quantity: Int,
    val price: Double,
    val maxQuantity: Int,
    val quantityPerPackage: Int
)
