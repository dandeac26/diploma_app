package com.example.myapplication.dtos

data class OrderDTO(
    val clientId: String,
    val deliveryNeeded: Boolean,
    val completionDate: String,
    val completionTime: String,
    val price: Double,
    var completed: Boolean,
)