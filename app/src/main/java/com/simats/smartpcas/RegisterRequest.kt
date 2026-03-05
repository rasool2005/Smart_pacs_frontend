package com.simats.smartpcas

data class RegisterRequest(
    val name: String,
    val email: String,
    val hospital_id: String,
    val password: String,
    val confirm_password: String
)
