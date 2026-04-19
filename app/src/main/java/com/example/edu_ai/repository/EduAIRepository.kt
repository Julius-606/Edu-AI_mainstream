// IDENTITY: repository/EduAIRepository.kt
package com.example.edu_ai.repository

import com.example.edu_ai.data.local.EduAIDao
import com.example.edu_ai.data.local.UserEntity
import com.example.edu_ai.data.local.UnitEntity
import com.example.edu_ai.data.local.QuizHistoryEntity
import com.example.edu_ai.data.local.ChatMessageEntity
import com.example.edu_ai.data.local.ChatSessionEntity
import com.example.edu_ai.data.remote.EduAIApi
import com.example.edu_ai.data.remote.TeacherDashboardResponse
import com.example.edu_ai.data.remote.ClassReportResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class EduAIRepository(
    private val api: EduAIApi,
    private val dao: EduAIDao
) {

    fun getDashboardData(userId: String): Flow<UserEntity?> = flow {
        try {
            // 1. Try to fetch from remote
            val response = api.getDashboard(userId)
            
            // Clear all local data to ensure a fresh state
            dao.clearUsers()
            dao.deleteAllUnits()
            dao.clearAllChatSessions()
            dao.clearAllChatHistory()
            dao.clearAllQuizHistory()

            // Safely map response to UserEntity with defaults to prevent crashes
            val userEntity = UserEntity(
                id = userId,
                username = response.username ?: userId,
                role = response.role ?: "Student",
                sensoryMode = response.sensoryMode ?: "Visual",
                semesterStatus = response.semesterStatus ?: "Active",
                aiPersona = response.aiPersona ?: "Socratic Mentor"
            )
            dao.insertUser(userEntity)
            
            // Handle potentially null unit list
            val unitEntities = response.activeUnits?.map { unitName ->
                UnitEntity(unitName = unitName, isActive = true)
            } ?: emptyList()
            dao.insertUnits(unitEntities)

            // 2. Re-populate Quiz History safely
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

            // 3. Re-populate Chat History safely
            val chatHistory = response.chatHistory
            if (!chatHistory.isNullOrEmpty()) {
                val sessionId = dao.insertChatSession(
                    ChatSessionEntity(userId = userId, title = "Previous Chat", isArchived = true)
                ).toInt()
                
                for (c in chatHistory) {
                    dao.insertChatMessage(
                        ChatMessageEntity(
                            sessionId = sessionId,
                            userId = userId,
                            role = c.role ?: "model",
                            content = c.content ?: "",
                            timestamp = c.timestamp?.toLongOrNull() ?: System.currentTimeMillis()
                        )
                    )
                }
            }

            emit(userEntity)
        } catch (e: Exception) {
            // 4. Fallback: Check Local DB if offline or API fails
            val cachedUser = dao.getUserById(userId)
            if (cachedUser != null) {
                emit(cachedUser)
            } else {
                // 5. Dev Fallback / First-time login failure
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

    // --- Teacher Portal Methods ---

    suspend fun getTeacherDashboard(): TeacherDashboardResponse {
        return api.getTeacherDashboard()
    }

    suspend fun generateClassReport(): ClassReportResponse {
        return api.generateClassReport()
    }

    suspend fun updateStudentProfile(userId: String, updates: Map<String, Any?>) {
        api.updateStudentProfile(userId, updates)
    }
}
