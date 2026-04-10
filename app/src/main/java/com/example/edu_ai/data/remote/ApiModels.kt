// IDENTITY: data/remote/ApiModels.kt
// VERSION: 1.1.0
// ⚙️ GEAR 1.1: The Network Broker (Retrofit)
// This handles the communication with our remote server.

package com.example.edu_ai.data.remote

import com.google.gson.annotations.SerializedName

data class DashboardResponse(
    @SerializedName("username") val username: String,
    @SerializedName("role") val role: String,
    @SerializedName("sensory_mode") val sensoryMode: String,
    @SerializedName("semester_status") val semesterStatus: String,
    @SerializedName("difficulty") val difficulty: String,
    @SerializedName("ai_persona") val aiPersona: String,
    @SerializedName("active_units") val activeUnits: List<String>,
    @SerializedName("average_pnl") val averagePnl: Double,
    @SerializedName("total_quizzes") val totalQuizzes: Int
)

data class ChaosRequest(
    @SerializedName("unit") val unit: String,
    @SerializedName("focus_area") val focusArea: String?,
    @SerializedName("difficulty") val difficulty: String,
    @SerializedName("student_id") val studentId: String
)

data class ChaosResponse(
    @SerializedName("case_study") val caseStudy: String
)
