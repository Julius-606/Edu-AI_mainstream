// IDENTITY: repository/EduAIRepository.kt
package com.example.edu_ai.repository

import com.example.edu_ai.data.local.*
import com.example.edu_ai.data.remote.EduAIApi
import com.example.edu_ai.data.remote.TeacherDashboardResponse
import com.example.edu_ai.data.remote.ClassReportResponse
import com.example.edu_ai.data.remote.ApiTimetableResponse
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class EduAIRepository(
    private val api: EduAIApi,
    private val dao: EduAIDao
) {
    private val gson = Gson()

    suspend fun logout() {
        dao.clearUsers()
        dao.deleteAllUnits()
        dao.clearAllChatSessions()
        dao.clearAllChatHistory()
        dao.clearAllQuizHistory()
        // Note: We might want to clear timetables too
    }

    fun getDashboardData(userId: String): Flow<UserEntity?> = flow {
        try {
            val response = api.getDashboard(userId)
            dao.clearUsers()
            dao.deleteAllUnits()

            val userEntity = UserEntity(
                id = userId,
                username = response.username ?: userId,
                role = response.role ?: "Student",
                sensoryMode = response.sensoryMode ?: "Visual",
                semesterStatus = response.semesterStatus ?: "Active",
                aiPersona = response.aiPersona ?: "Socratic Mentor"
            )
            dao.insertUser(userEntity)
            
            val unitEntities = response.activeUnits?.map { unitName ->
                UnitEntity(unitName = unitName, isActive = true)
            } ?: emptyList()
            dao.insertUnits(unitEntities)

            response.quizHistory?.forEach { q ->
                dao.insertQuizHistory(
                    QuizHistoryEntity(
                        userId = userId,
                        unitName = q.unitName ?: "General",
                        pnlScore = q.pnl ?: 0.0,
                        timestamp = q.timestamp?.toLongOrNull() ?: System.currentTimeMillis()
                    )
                )
            }

            emit(userEntity)
        } catch (e: Exception) {
            val cachedUser = dao.getUserById(userId)
            if (cachedUser != null) {
                emit(cachedUser)
            } else {
                val devUser = UserEntity(
                    id = userId,
                    username = userId,
                    role = "Student",
                    sensoryMode = "Visual",
                    semesterStatus = "Year 4 - Redemption Arc 🔥",
                    aiPersona = "Socratic Mentor"
                )
                dao.insertUser(devUser)

                val devUnits = listOf(
                    UnitEntity(unitName = "Biochemistry II", isActive = true),
                    UnitEntity(unitName = "General Surgery", isActive = true),
                    UnitEntity(unitName = "Internal Medicine", isActive = true)
                )
                dao.insertUnits(devUnits)

                emit(devUser)
            }
        }
    }

    suspend fun getWeeklyTimetable(userId: String): ApiTimetableResponse {
        val cachedTimetable = dao.getTimetableByUserId(userId)
        val oneWeekInMillis = 7 * 24 * 60 * 60 * 1000L
        
        if (cachedTimetable != null && (System.currentTimeMillis() - cachedTimetable.timestamp) < oneWeekInMillis) {
            // Return cached version
            return ApiTimetableResponse(
                weeklyPlan = gson.fromJson(cachedTimetable.weeklyPlanJson, Array<com.example.edu_ai.data.remote.ApiTimetableSlot>::class.java).toList(),
                aiBrief = cachedTimetable.aiBrief
            )
        }

        // Fetch fresh from AI
        return try {
            val freshTimetable = api.getTimetable(userId)
            // Cache it
            dao.insertTimetable(
                TimetableEntity(
                    userId = userId,
                    weeklyPlanJson = gson.toJson(freshTimetable.weeklyPlan),
                    aiBrief = freshTimetable.aiBrief,
                    timestamp = System.currentTimeMillis()
                )
            )
            freshTimetable
        } catch (e: Exception) {
            // If API fails but we have an old cache, return it anyway
            if (cachedTimetable != null) {
                ApiTimetableResponse(
                    weeklyPlan = gson.fromJson(cachedTimetable.weeklyPlanJson, Array<com.example.edu_ai.data.remote.ApiTimetableSlot>::class.java).toList(),
                    aiBrief = cachedTimetable.aiBrief
                )
            } else {
                throw e
            }
        }
    }

    // --- Teacher Portal Methods ---
    suspend fun getTeacherDashboard(): TeacherDashboardResponse = api.getTeacherDashboard()
    suspend fun generateClassReport(): ClassReportResponse = api.generateClassReport()
    suspend fun updateStudentProfile(userId: String, updates: Map<String, Any?>) = api.updateStudentProfile(userId, updates)
    suspend fun sendProgressReport(studentId: String) = api.sendProgressReport(studentId)

    // --- Parent Portal Methods ---
    suspend fun getParentDashboard(studentId: String) = api.getParentDashboard(studentId)
}
