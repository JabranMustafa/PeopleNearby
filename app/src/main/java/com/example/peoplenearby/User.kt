package com.example.peoplenearby

data class User(
    val id: String = "",
    val name: String = "",
    val avatarUrl: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = 0
)
