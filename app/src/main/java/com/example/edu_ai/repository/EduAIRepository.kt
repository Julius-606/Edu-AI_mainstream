// IDENTITY: repository/EduAIRepository.kt
package com.example.edu_ai.repository

import com.example.edu_ai.data.local.EduAIDao
import com.example.edu_ai.data.local.UserEntity
import com.example.edu_ai.data.local.UnitEntity
import com.example.edu_ai.data.local.QuizHistoryEntity
import com.example.edu_ai.data.local.ChatMessageEntity
import com.example.edu_ai.data.remote.EduAIApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.firstOrNull

class EduAIRepository(
    private val api: EduAIApi,
    private val dao: EduAIDao
) {

    fun getDashboardData(userId: String): Flow<UserEntity?> = flow {
        try {
            // 1. Try to fetch from remote
            val response = api.getDashboard(userId)
            
            // Clear all local data to ensure "not locally saving" persistently and preventing leaks
            dao.clearUsers()
            dao.deleteAllUnits()
            dao.clearAllChatHistory()
            dao.clearAllQuizHistory()

            val userEntity = UserEntity(
                id = userId,
                username = response.username,
                role = response.role,
                sensoryMode = response.sensoryMode,
                semesterStatus = response.semesterStatus,
                aiPersona = response.aiPersona
            )
            dao.insertUser(userEntity)
            
            val unitEntities = response.activeUnits.map { unitName ->
                UnitEntity(unitName = unitName, isActive = true)
            }
            dao.insertUnits(unitEntities)

            // 2. Re-populate from remote response
            for (q in response.quizHistory) {
                dao.insertQuizHistory(
                    QuizHistoryEntity(
                        userId = userId,
                        unitName = q.unitName,
                        pnlScore = q.pnl,
                        timestamp = q.timestamp.toLongOrNull() ?: System.currentTimeMillis()
                    )
                )
            }

            for (c in response.chatHistory) {
                dao.insertChatMessage(
                    ChatMessageEntity(
                        userId = userId,
                        role = c.role,
                        content = c.content,
                        timestamp = c.timestamp.toLongOrNull() ?: System.currentTimeMillis()
                    )
                )
            }

            emit(userEntity)
        } catch (e: Exception) {
            // 3. Fallback: Check Local DB (Still keep for offline, but it'll only have current user's last session)
            val cachedUser = dao.getUserById(userId)
            if (cachedUser != null) {
                emit(cachedUser)
            } else {
                // 4. Dev Fallback
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
}
