// IDENTITY: data/remote/RetrofitClient.kt
// VERSION: 1.1.0
// ⚙️ GEAR 1.1: The Network Broker (Retrofit)
// This handles the communication with our remote server.

package com.example.edu_ai.data.remote

import com.example.edu_ai.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private val BASE_URL = BuildConfig.BACKEND_BASE_URL

    val instance: EduAIApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EduAIApi::class.java)
    }
}
