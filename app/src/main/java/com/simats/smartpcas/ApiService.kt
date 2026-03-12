package com.simats.smartpcas

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("api/register/")
    suspend fun registerUser(
        @Body request: RegisterRequest
    ): Response<RegisterResponse>

    @POST("api/login/")
    suspend fun loginUser(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @FormUrlEncoded
    @POST("api/schedule-study/")
    suspend fun scheduleAppointment(
        @Field("user_id") user_id: Int,
        @Field("patient_name") patient_name: String,
        @Field("study_type") study_type: String,
        @Field("study_date") study_date: String,
        @Field("study_time") study_time: String,
        @Field("note") note: String?
    ): Response<AppointmentResponse>

    @POST("api/add-patient/")
    suspend fun addPatient(
        @Body request: AddPatientRequest
    ): Response<AddPatientResponse>

    @GET("api/patients/")
    suspend fun getPatients(): Response<PatientsResponse>

    @DELETE("api/delete-patient/{patient_id}/")
    suspend fun deletePatient(
        @Path("patient_id") patientId: Int
    ): Response<SimpleResponse>

    @FormUrlEncoded
    @POST("api/user-studies/")
    suspend fun getStudies(
        @Field("user_id") userId: Int
    ): Response<StudiesResponse>

    @DELETE("api/delete-study/{study_id}/")
    suspend fun deleteStudy(
        @Path("study_id") studyId: Int
    ): Response<SimpleResponse>

    @FormUrlEncoded
    @POST("api/save-personal-info/")
    suspend fun savePersonalInfo(
        @Field("user_id") user_id: Int,
        @Field("first_name") first_name: String,
        @Field("last_name") last_name: String,
        @Field("email") email: String,
        @Field("phone_number") phone_number: String,
        @Field("date_of_birth") date_of_birth: String,
        @Field("street_address") street_address: String,
        @Field("city") city: String,
        @Field("state") state: String,
        @Field("zip_code") zip_code: String
    ): Response<PersonalInfoResponse>

    @FormUrlEncoded
    @POST("api/update-profile/")
    suspend fun updatePersonalInfo(
        @Field("user_id") user_id: Int,
        @Field("first_name") first_name: String,
        @Field("last_name") last_name: String,
        @Field("email") email: String,
        @Field("phone_number") phone_number: String,
        @Field("date_of_birth") date_of_birth: String,
        @Field("street_address") street_address: String,
        @Field("city") city: String,
        @Field("state") state: String,
        @Field("zip_code") zip_code: String
    ): Response<PersonalInfoResponse>

    @FormUrlEncoded
    @POST("api/get-personal-info/")
    suspend fun getPersonalInfo(
        @Field("user_id") user_id: Int
    ): Response<GetPersonalInfoResponse>

    // Image Upload
    @Multipart
    @POST("api/predict_scan/")
    suspend fun predictImage(
        @Part file: MultipartBody.Part,
        @Part("scan_type") scanType: RequestBody
    ): Response<PredictionResponse>

    @POST("api/change-password/")
    suspend fun changePassword(
        @Body request: ChangePasswordRequest
    ): Response<ChangePasswordResponse>

    // AI Reports
    @POST("api/save-ai-report/")
    suspend fun saveAiReport(
        @Body request: SaveReportRequest
    ): Response<SimpleResponse>

    @GET("api/get-ai-reports/")
    suspend fun getAiReports(
        @Query("user_id") userId: Int
    ): Response<AiReportsResponse>

    @DELETE("api/delete-ai-report/{report_id}/")
    suspend fun deleteReport(
        @Path("report_id") reportId: Int
    ): Response<SimpleResponse>

    @Streaming
    @GET("api/download-report/{report_id}/")
    suspend fun downloadReport(
        @Path("report_id") reportId: Int
    ): Response<okhttp3.ResponseBody>

    @POST("api/send-report-email/")
    suspend fun sendReportEmail(
        @Body request: SendEmailRequest
    ): Response<SimpleResponse>

    // ✅ FORGOT PASSWORD

    @POST("api/send-otp/")
    suspend fun sendOtp(
        @Body request: ForgotPasswordRequest
    ): Response<ForgotPasswordResponse>

    @POST("api/verify-otp/")
    suspend fun verifyOtp(
        @Body request: VerifyOtpRequest
    ): Response<SimpleResponse>

    @POST("api/reset-password/")
    suspend fun resetPassword(
        @Body request: ResetPasswordRequest
    ): Response<SimpleResponse>
}
