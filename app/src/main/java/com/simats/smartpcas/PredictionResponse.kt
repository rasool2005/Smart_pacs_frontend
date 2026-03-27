package com.simats.smartpcas

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class PredictionResponse(
    val status: String,
    val scan_type: String?,
    val confidence_score: Double?,
    val confidence_level: String?,
    val message: String?,
    val findings: List<AiFinding>? = null, // Dynamic list of problems
    val error: String? = null
) : Serializable

data class AiFinding(
    @SerializedName(value = "condition", alternate = ["title"])
    val title: String,
    val location: String,
    val description: String,
    val confidence: Double,
    val severity: String // "Low", "Moderate", "High"
) : Serializable
