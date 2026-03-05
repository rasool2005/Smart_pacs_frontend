package com.simats.smartpcas

data class ChangePasswordRequest(
    val user_id: Int,
    val current_password: String,
    val new_password: String,
    val confirm_password: String
)

data class ChangePasswordResponse(
    val status: String,
    val message: String?,
    val errors: Map<String, String>? = null
)
