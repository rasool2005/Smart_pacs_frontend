package com.simats.smartpcas

data class LoginResponse(
    val status: String,
    val message: String,
    val user: UserData? = null
)

data class UserData(
    val user_id: Int,
    val name: String,
    val email: String,
    val hospital_id: String,
    val role: String
)
