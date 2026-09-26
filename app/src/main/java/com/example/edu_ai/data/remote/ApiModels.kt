
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
    @SerializedName("units") val units: List<ApiUnit>? = null,
    @SerializedName("average_pnl") val averagePnl: Double? = null,
    @SerializedName("total_quizzes") val totalQuizzes: Int? = null,
    @SerializedName("quiz_history") val quizHistory: List<ApiQuizHistory>? = null,
    @SerializedName("chat_history") val chatHistory: List<ApiChatHistory>? = null
)

data class ApiUnit(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("modules") val modules: List<ApiModule> = emptyList()
)

data class ApiModule(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("topics") val topics: List<ApiTopic> = emptyList()
)

data class ApiTopic(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("subtopics") val subtopics: List<ApiSubtopic> = emptyList()
)

data class ApiSubtopic(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("is_completed") val isCompleted: Boolean,
    @SerializedName("learning_objectives") val learningObjectives: List<ApiLearningObjective> = emptyList()
)

data class ApiLearningObjective(
    @SerializedName("id") val id: Int,
    @SerializedName("description") val description: String,
    @SerializedName("is_completed") val isCompleted: Boolean
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

data class ApiUnitProgressInfo(
    @SerializedName("unit_name") val unitName: String,
    @SerializedName("completed_subtopics") val completedSubtopics: Int,
    @SerializedName("total_subtopics") val totalSubtopics: Int,
    @SerializedName("progress_percentage") val progressPercentage: Float
)

data class ApiQuizAttemptInfo(
    @SerializedName("unit_name") val unitName: String,
    @SerializedName("score") val score: Float,
    @SerializedName("timestamp") val timestamp: Long? = null
)

data class ApiStudyContextPayload(
    @SerializedName("overall_progress_percentage") val overallProgressPercentage: Float? = null,
    @SerializedName("units_progress") val unitsProgress: List<ApiUnitProgressInfo> = emptyList(),
    @SerializedName("completed_subtopic_names") val completedSubtopicNames: List<String> = emptyList(),
    @SerializedName("pending_subtopic_names") val pendingSubtopicNames: List<String> = emptyList(),
    @SerializedName("average_quiz_score") val averageQuizScore: Float? = null,
    @SerializedName("total_quizzes_taken") val totalQuizzesTaken: Int? = null,
    @SerializedName("mastered_topics") val masteredTopics: List<String> = emptyList(),
    @SerializedName("weak_topics") val weakTopics: List<String> = emptyList(),
    @SerializedName("recent_quizzes") val recentQuizzes: List<ApiQuizAttemptInfo> = emptyList()
)

data class LibraryUnit(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("category") val category: String
)

data class ApiProgressItem(
    @SerializedName("node_id") val nodeId: Int,
    @SerializedName("node_type") val nodeType: String,
    @SerializedName("status") val status: String,
    @SerializedName("last_studied_at") val lastStudiedAt: Double
)

data class ApiBookmarkItem(
    @SerializedName("id") val id: String? = null,
    @SerializedName("type") val type: String,
    @SerializedName("title") val title: String,
    @SerializedName("target") val target: String,
    @SerializedName("context") val context: String,
    @SerializedName("timestamp") val timestamp: Double,
    @SerializedName("notes") val notes: String? = null
)

data class ApiSyncResponse(
    @SerializedName("progress") val progress: List<ApiProgressItem> = emptyList(),
    @SerializedName("bookmarks") val bookmarks: List<ApiBookmarkItem> = emptyList()
)

data class ApiSyncRequest(
    @SerializedName("progress") val progress: List<ApiProgressItem> = emptyList(),
    @SerializedName("bookmarks") val bookmarks: List<ApiBookmarkItem> = emptyList()
)

data class ApiAppRelease(
    @SerializedName("id") val id: Long,
    @SerializedName("version") val version: String,
    @SerializedName("version_code") val versionCode: Int = 1,
    @SerializedName("artifact_type") val artifactType: String = "Trace Android APK",
    @SerializedName("download_url") val downloadUrl: String,
    @SerializedName("release_notes") val releaseNotes: String? = null,
    @SerializedName("is_current") val isCurrent: Boolean = false,
    @SerializedName("is_mandatory") val isMandatory: Boolean = false,
    @SerializedName("min_supported_version_code") val minSupportedVersionCode: Int = 1,
    @SerializedName("file_size") val fileSize: String = "14.8 MB",
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis()
)

data class ApiReleasesResponse(
    @SerializedName("latest_version") val latestVersion: String? = null,
    @SerializedName("latest_version_code") val latestVersionCode: Int? = null,
    @SerializedName("mandatory_update_active") val mandatoryUpdateActive: Boolean = false,
    @SerializedName("min_supported_version_code") val minSupportedVersionCode: Int = 1,
    @SerializedName("releases") val releases: List<ApiAppRelease> = emptyList()
)

data class CreateReleaseRequest(
    @SerializedName("version") val version: String,
    @SerializedName("version_code") val versionCode: Int,
    @SerializedName("artifact_type") val artifactType: String = "Trace Android APK",
    @SerializedName("download_url") val downloadUrl: String,
    @SerializedName("release_notes") val releaseNotes: String,
    @SerializedName("is_mandatory") val isMandatory: Boolean = false,
    @SerializedName("file_size") val fileSize: String = "14.8 MB",
    @SerializedName("is_current") val isCurrent: Boolean = true
)


 