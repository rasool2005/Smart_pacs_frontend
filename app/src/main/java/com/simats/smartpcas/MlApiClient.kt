package com.simats.smartpcas

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object MlApiClient {

    // ✅ Updated to match your current network IP and Flask port
    private const val BASE_URL = "http://192.168.137.134:5000/"

    private val logger = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // Body logging helps debug connection issues
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logger)
        .connectTimeout(60, TimeUnit.SECONDS) // AI analysis can take time
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    val apiService: MlApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(MlApiService::class.java)
    }
}
