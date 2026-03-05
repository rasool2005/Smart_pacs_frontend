package com.simats.smartpcas

data class AddPatientResponse(
    val status: String,
    val message: String,
    val patient_id: Int? = null
)
