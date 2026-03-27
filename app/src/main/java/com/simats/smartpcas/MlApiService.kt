package com.simats.smartpcas

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface MlApiService {
    
    // Python AI Prediction Endpoint
    @Multipart
    @POST("predict")
    suspend fun predict(
        @Part file: MultipartBody.Part,
        @Part("scan_type") scanType: RequestBody
    ): Response<PredictionResponse>

    @Multipart
    @POST("analyze")
    suspend fun analyze(
        @Part file: MultipartBody.Part,
        @Part("scan_type") scanType: RequestBody
    ): Response<PredictionResponse>

    // AI Chat on Flask Server
    @GET("ai-chat")
    suspend fun aiChat(
        @Query("query") query: String
    ): Response<AiChatResponse>
}
