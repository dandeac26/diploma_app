package com.example.myapplication.dtos

data class ClientDTO (
    val firmName: String,
    val contactPerson: String,
    val phoneNumber: String,
    val location: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val type: String
)
