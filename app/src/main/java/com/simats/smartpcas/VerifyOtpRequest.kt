package com.simats.smartpcas

data class VerifyOtpRequest(
    val email: String,
    val otp: String
)
