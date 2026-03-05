package com.simats.smartpcas

data class PacsStudy(
    val id: String,
    val patientName: String,
    val modality: String, // "X-Ray", "CT Scan", "MRI"
    val date: String,
    val imageUrl: String? = null // Placeholder for image resource or URL
)
