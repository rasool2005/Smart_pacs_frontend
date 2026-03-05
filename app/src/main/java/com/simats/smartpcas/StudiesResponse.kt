package com.simats.smartpcas

import java.io.Serializable

data class StudiesResponse(
    val status: String,
    val studies: List<Study>,
    val counts: StudyCounts? = null,
    val message: String? = null
) : Serializable

data class StudyCounts(
    val pending: Int,
    val confirmed: Int
) : Serializable

data class Study(
    val id: Int,
    val patient_name: String,
    val study_type: String,
    val study_date: String,
    val study_time: String,
    val status: String,
    val note: String?,
    val created_at: String
) : Serializable
