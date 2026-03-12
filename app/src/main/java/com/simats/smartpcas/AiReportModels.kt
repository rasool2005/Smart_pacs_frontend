package com.simats.smartpcas

import java.io.Serializable

data class SaveReportRequest(
    val user_id: Int,
    val examination_type: String,
    val confidence_score: Double,
    val confidence_level: String,
    val finding_name: String,
    val location: String,
    val observation: String,
    val severity: String,
    val impression: String,
    val image_uri: String? = null
)

data class AiReport(
    val id: Int,
    val user_id: Int,
    val examination_type: String,
    val confidence_score: Double,
    val confidence_level: String,
    val finding_name: String,
    val location: String,
    val observation: String,
    val severity: String,
    val impression: String,
    val created_at: String,
    val image_uri: String? = null
) : Serializable

data class SendEmailRequest(
    val report_id: Int,
    val patient_email: String
)

data class SimpleResponse(
    val status: String,
    val message: String?
)

data class AiReportsResponse(
    val status: String,
    val count: Int?,
    val reports: List<AiReport>?
)
