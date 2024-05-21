package com.example.myapplication.entity

data class ClientDTO (
    val firmName: String,
    val contactPerson: String,
    val phoneNumber: String,
    val location: String,
    val latitude: Double,
    val longitude: Double,
    val address: String
)
