
// IDENTITY: data/remote/EduAIApi.kt
package com.example.edu_ai.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface EduAIApi {

    @GET("api/users/{user_id}/dashboard")
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

    @GET("api/ai/recommendations/{user_id}")
    suspend fun getRecommendations(@Path("user_id") userId: String): RecommendationResponse

    @GET("api/users/{user_id}/timetable")
    suspend fun getTimetable(@Path("user_id") userId: String): ApiTimetableResponse

    @POST("api/auth/login")
    suspend fun login(@Body request: com.example.edu_ai.schemas.LoginRequest): com.example.edu_ai.schemas.TokenResponse

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

    @POST("api/teacher/send-report/{student_id}")
    suspend fun sendProgressReport(@Path("student_id") studentId: String): Map<String, Any?>

    // --- Parent Portal Endpoints ---

    @GET("api/parent/dashboard/{student_id}")
    suspend fun getParentDashboard(@Path("student_id") studentId: String): ParentDashboardResponse

    // --- Ingestion / Library Endpoints ---

    @GET("api/units/library")
    suspend fun getLibraryUnits(): List<LibraryUnit>

    @POST("api/units/library/add/{unit_id}")
    suspend fun addUnitToUser(
        @Path("unit_id") unitId: Int,
        @Query("user_id") userId: String
    ): Map<String, String>

    // --- Objective-Driven AI Generation & Persistence ---
    @POST("api/ai/course-objective")
    suspend fun getAiGeneratedContent(@Body request: CourseObjectiveRequest): AiGeneratedContentResponse

    @POST("api/sync")
    suspend fun sync(@Body request: SyncRequest): SyncResponse

    @POST("api/sync/content-completion")
    suspend fun syncContentCompletion(@Body request: ContentCompletionSyncRequest): ContentCompletionSyncResponse

    @GET("api/connections/{user_id}/messages/{with_user_id}")
    suspend fun getConnectionMessages(
        @Path("user_id") userId: String,
        @Path("with_user_id") withUserId: Int
    ): List<ConnectionMessage>

    @POST("api/connections/{user_id}/messages")
    suspend fun sendConnectionMessage(
        @Path("user_id") userId: String,
        @Body request: ConnectionMessageRequest
    ): Map<String, Any?>

    @GET("api/learning/session/{subtopic_id}")
    suspend fun getLearningSession(
        @Path("subtopic_id") subtopicId: Int,
        @Query("user_id") userId: String
    ): LearningSessionResponse

    @POST("api/learning/session/{subtopic_id}/next")
    suspend fun nextObjective(
        @Path("subtopic_id") subtopicId: Int,
        @Query("user_id") userId: String,
        @Query("user_message") userMessage: String? = null
    ): Map<String, Any?>

    @POST("api/learning/session/{subtopic_id}/prev")
    suspend fun previousObjective(
        @Path("subtopic_id") subtopicId: Int,
        @Query("user_id") userId: String
    ): Map<String, Any?>

    @POST("api/learning/content")
    suspend fun saveLearningContent(@Body request: LearningContentRequest): Map<String, Any?>

    @retrofit2.http.DELETE("api/users/{user_id}/units/{unit_id}")
    suspend fun deleteUserUnit(
        @Path("user_id") userId: String,
        @Path("unit_id") unitId: Long
    ): Map<String, Any?>

    @GET("api/users/{user_id}/restore")
    suspend fun restoreUserData(@Path("user_id") userId: String): Map<String, Any?>
}



