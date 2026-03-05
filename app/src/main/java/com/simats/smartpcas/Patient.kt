package com.simats.smartpcas

import java.io.Serializable

data class Patient(
    val patient_id: Int,
    val patient_name: String,
    val dob: String,
    val phone_number: String,
    val address: String,
    val email: String,
    val blood_type: String,
    val allergies: String
) : Serializable

data class PatientsResponse(
    val status: String,
    val count: Int,
    val patients: List<Patient>
)
