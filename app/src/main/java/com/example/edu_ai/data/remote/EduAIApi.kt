// IDENTITY: data/remote/EduAIApi.kt
package com.example.edu_ai.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface EduAIApi {

    @GET("api/user/{user_id}/dashboard")
    suspend fun getDashboard(@Path("user_id") userId: String): DashboardResponse

    // --- AI Endpoints ---

    @POST("api/ai/chat")
    suspend fun aiChat(@Body request: ChatRequest): ChatResponse

    @POST("api/ai/quiz")
    suspend fun generateAiQuiz(
        @Body request: QuizRequest,
        @Query("topic") topic: String? = null
    ): ApiQuizResponse

    @POST("api/quiz/record")
    suspend fun recordQuiz(@Body request: QuizRecordRequest): Map<String, String>

    @GET("api/user/{user_id}/recommendations")
    suspend fun getRecommendations(@Path("user_id") userId: String): RecommendationResponse
}

data class RecommendationResponse(
    @SerializedName("recommendation") val recommendation: String
)
