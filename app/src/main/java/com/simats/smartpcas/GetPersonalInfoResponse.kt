package com.simats.smartpcas

data class GetPersonalInfoResponse(
    val status: String,
    val data: PersonalInfoData?,
    val message: String?
)

data class PersonalInfoData(
    val first_name: String,
    val last_name: String,
    val email: String,
    val phone_number: String,
    val date_of_birth: String,
    val street_address: String,
    val city: String,
    val state: String,
    val zip_code: String
)
