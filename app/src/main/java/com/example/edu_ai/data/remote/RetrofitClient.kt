// IDENTITY: data/remote/RetrofitClient.kt
package com.example.edu_ai.data.remote

import com.example.edu_ai.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // The BASE_URL is now dynamically injected from build.gradle.kts / gradle.properties
    private const val BASE_URL = BuildConfig.BACKEND_BASE_URL

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS) // Increased for multi-key processing
        .readTimeout(120, TimeUnit.SECONDS)    // Increased for multi-key processing
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    val instance: EduAIApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EduAIApi::class.java)
    }
}
