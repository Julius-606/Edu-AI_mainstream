// IDENTITY: data/remote/EduAIApi.kt
// VERSION: 1.1.0
// ⚙️ GEAR 1.1: The Network Broker (Retrofit)
// This handles the communication with our remote server.

package com.example.edu_ai.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface EduAIApi {

    @GET("api/user/{user_id}/dashboard")
    suspend fun getDashboard(@Path("user_id") userId: String): DashboardResponse

    @POST("api/chaos/generate_case")
    suspend fun generateChaosCase(@Body request: ChaosRequest): ChaosResponse
}
