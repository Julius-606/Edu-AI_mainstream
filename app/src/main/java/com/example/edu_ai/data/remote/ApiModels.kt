// IDENTITY: data/remote/ApiModels.kt
package com.example.edu_ai.data.remote

import com.google.gson.annotations.SerializedName

data class DashboardResponse(
    @SerializedName("username") val username: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("sensory_mode") val sensoryMode: String? = null,
    @SerializedName("semester_status") val semesterStatus: String? = null,
    @SerializedName("difficulty") val difficulty: String? = null,
    @SerializedName("ai_persona") val aiPersona: String? = null,
    @SerializedName("active_units") val activeUnits: List<String>? = null,
    @SerializedName("average_pnl") val averagePnl: Double? = null,
    @SerializedName("total_quizzes") val totalQuizzes: Int? = null,
    @SerializedName("quiz_history") val quizHistory: List<ApiQuizHistory>? = null,
    @SerializedName("chat_history") val chatHistory: List<ApiChatHistory>? = null
)

data class ApiQuizHistory(
    @SerializedName("unit_name") val unitName: String? = null,
    @SerializedName("pnl") val pnl: Double? = null,
    @SerializedName("timestamp") val timestamp: String? = null
)

data class ApiChatHistory(
    @SerializedName("role") val role: String? = null,
    @SerializedName("content") val content: String? = null,
    @SerializedName("timestamp") val timestamp: String? = null
)

// --- TEACHER PORTAL MODELS ---

data class StudentSummary(
    @SerializedName("id") val id: Int,
    @SerializedName("username") val username: String,
    @SerializedName("average_pnl") val averagePnl: Double,
    @SerializedName("total_quizzes") val totalQuizzes: Int,
    @SerializedName("semester_status") val semesterStatus: String,
    @SerializedName("active_units") val activeUnits: List<String>,
    @SerializedName("is_at_risk") val isAtRisk: Boolean = false,
    @SerializedName("risk_reason") val riskReason: String? = null
)

data class TeacherDashboardResponse(
    @SerializedName("action_required_queue") val actionRequiredQueue: List<StudentSummary>,
    @SerializedName("total_active_students") val totalActiveStudents: Int,
    @SerializedName("class_health_score") val classHealthScore: Double
)

data class ClassReportResponse(
    @SerializedName("report") val report: String
)

// --- PARENT PORTAL MODELS ---

data class ParentDashboardResponse(
    @SerializedName("student_name") val studentName: String,
    @SerializedName("academic_status") val academicStatus: String,
    @SerializedName("current_study_path") val currentStudyPath: List<String>,
    @SerializedName("ai_progress_review") val aiProgressReview: String,
    @SerializedName("teacher_remarks") val teacherRemarks: String?,
    @SerializedName("recent_grades") val recentGrades: List<ApiQuizHistory>
)

// --- TIMETABLE MODELS ---

data class ApiTimetableSlot(
    @SerializedName("day") val day: String,
    @SerializedName("time") val time: String,
    @SerializedName("activity") val activity: String,
    @SerializedName("unit") val unit: String?,
    @SerializedName("type") val type: String
)

data class ApiTimetableResponse(
    @SerializedName("weekly_plan") val weeklyPlan: List<ApiTimetableSlot>,
    @SerializedName("ai_brief") val aiBrief: String
)

// --- AI Models ---

data class ChatMessage(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String
)

data class ChatRequest(
    @SerializedName("prompt") val prompt: String,
    @SerializedName("user_id") val user_id: String,
    @SerializedName("history") val history: List<ChatMessage> = emptyList()
)

data class ChatResponse(
    @SerializedName("response") val response: String
)

data class QuizRequest(
    @SerializedName("unit_name") val unit_name: String,
    @SerializedName("user_id") val user_id: String
)

data class ApiQuizQuestion(
    @SerializedName("question_text") val question_text: String,
    @SerializedName("options") val options: List<String>,
    @SerializedName("correct_option_index") val correct_option_index: Int,
    @SerializedName("explanation") val explanation: String
)

data class ApiQuizResponse(
    @SerializedName("quiz_title") val quiz_title: String,
    @SerializedName("questions") val questions: List<ApiQuizQuestion>
)

data class QuizRecordRequest(
    @SerializedName("unit_name") val unit_name: String,
    @SerializedName("score") val score: Int,
    @SerializedName("total") val total: Int,
    @SerializedName("user_id") val user_id: String,
    @SerializedName("timestamp") val timestamp: Long
)

data class RecommendationResponse(
    @SerializedName("recommendation") val recommendation: String
)
