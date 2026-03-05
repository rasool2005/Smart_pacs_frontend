package com.simats.smartpcas

data class AppointmentRequest(
    val user_id: Int,
    val patient_name: String,
    val study_type: String,
    val appointment_date: String,
    val appointment_time: String,
    val notes: String?
)
