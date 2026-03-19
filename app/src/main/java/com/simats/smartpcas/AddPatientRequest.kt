package com.simats.smartpcas

data class AddPatientRequest(
    val user_id: Int,
    val patient_name: String,
    val dob: String,
    val phone_number: String,
    val address: String,
    val email: String,
    val blood_type: String,
    val allergies: String
)
