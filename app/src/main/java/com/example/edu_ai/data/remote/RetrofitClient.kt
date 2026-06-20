// IDENTITY: data/remote/RetrofitClient.kt
package com.example.edu_ai.data.remote

import android.content.Context
import com.example.edu_ai.BuildConfig
import com.example.edu_ai.utils.PreferenceManager
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = BuildConfig.BACKEND_BASE_URL
    private const val INTERNAL_API_KEY = BuildConfig.INTERNAL_API_KEY

    private var context: Context? = null

    fun init(context: Context) {
        this.context = context.applicationContext
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
                .addHeader("X-Internal-Api-Key", INTERNAL_API_KEY)
            
            context?.let { ctx ->
                PreferenceManager.getToken(ctx)?.let { token ->
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }
            }
            
            chain.proceed(requestBuilder.build())
        }
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
