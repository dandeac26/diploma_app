package com.example.myapplication.entity

import com.example.myapplication.fragments.OrdersFragment.OrderDetail

data class OrderDTO(
    val clientId: String,
    val deliveryNeeded: Boolean,
    val completionDate: String,
    val completionTime: String,
    val price: Double,
    var completed: Boolean,
)