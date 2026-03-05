package com.simats.smartpcas.network

import com.google.gson.annotations.SerializedName

// --- Authentication ---

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class RegisterRequest(
    @SerializedName("full_name") val fullName: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("password") val password: String
)

data class AuthResponse(
    @SerializedName("token") val token: String,
    @SerializedName("user") val user: User
)

data class User(
    @SerializedName("id") val id: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("email") val email: String,
    @SerializedName("role") val role: String
)

// --- Patient & Studies ---

data class Patient(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("mrn") val mrn: String,
    @SerializedName("age") val age: Int,
    @SerializedName("gender") val gender: String,
    @SerializedName("dob") val dob: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("email") val email: String,
    @SerializedName("address") val address: String,
    @SerializedName("blood_type") val bloodType: String,
    @SerializedName("allergies") val allergies: String
)

data class Study(
    @SerializedName("id") val id: String,
    @SerializedName("patient_id") val patientId: String,
    @SerializedName("type") val type: String,
    @SerializedName("date") val date: String,
    @SerializedName("status") val status: String,
    @SerializedName("priority") val priority: String,
    @SerializedName("ai_result") val aiResult: String?,
    @SerializedName("confidence") val confidence: Int?
)

// --- Notifications ---

data class Notification(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("message") val message: String,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("is_read") val isRead: Boolean,
    @SerializedName("type") val type: String // e.g., "critical", "update"
)

// --- Generic Response ---

data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: T?
)
