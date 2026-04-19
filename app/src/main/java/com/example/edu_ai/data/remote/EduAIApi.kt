// IDENTITY: data/remote/EduAIApi.kt
package com.example.edu_ai.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
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

    // --- Teacher Portal Endpoints ---

    @GET("api/teacher/dashboard")
    suspend fun getTeacherDashboard(): TeacherDashboardResponse

    @POST("api/teacher/class-report")
    suspend fun generateClassReport(): ClassReportResponse

    @PUT("api/users/{user_id}")
    suspend fun updateStudentProfile(
        @Path("user_id") userId: String,
        @Body updates: Map<String, Any?>
    ): Map<String, Any?>
}
