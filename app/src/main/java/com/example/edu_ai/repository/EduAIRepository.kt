// IDENTITY: repository/EduAIRepository.kt
package com.example.edu_ai.repository

import com.example.edu_ai.data.local.*
import com.example.edu_ai.data.remote.EduAIApi
import com.example.edu_ai.data.remote.TeacherDashboardResponse
import com.example.edu_ai.data.remote.ClassReportResponse
import com.example.edu_ai.data.remote.ApiTimetableResponse
import com.example.edu_ai.utils.PreferenceManager
import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

class EduAIRepository(
    private val api: EduAIApi,
    private val dao: EduAIDao
) {
    private val gson = Gson()

    suspend fun logout(context: Context) {
        PreferenceManager.clearToken(context)
        PreferenceManager.clearLastUserId(context)
        // We no longer clear all data here to support multi-user offline persistence.
        // dao.clearUsers() 
        // dao.deleteAllUnits()
        // ...
    }

    suspend fun offlineLogin(email: String, password: String): UserEntity? {
        val user = dao.getUserByEmail(email)
        // Simple password check - in a real app use BCrypt or similar
        return if (user != null && user.passwordHash == password) {
            user
        } else {
            null
        }
    }

    fun getDashboardData(userId: String): Flow<UserEntity?> = flow {
        try {
            val response = api.getDashboard(userId)
            
            // PRESERVE CREDENTIALS
            val existingUser = dao.getUserById(userId)

            val userEntity = UserEntity(
                id = userId,
                username = response.username ?: userId,
                role = response.role ?: "Student",
                semesterStatus = response.semesterStatus ?: "Active",
                difficulty = response.difficulty ?: "Medium (Standard)",
                aiPersona = response.aiPersona ?: "Socratic Mentor",
                email = existingUser?.email ?: "",
                passwordHash = existingUser?.passwordHash ?: ""
            )
            dao.insertUser(userEntity)

            // Sync full hierarchy from backend if available
            if (response.units != null) {
                // We'll rely on REPLACE to update, but we might have orphans if units are deleted on backend.
                // dao.deleteUnitsForUser(userId) 
                
                response.units.forEach { apiUnit ->
                    val unitId = dao.insertUnits(listOf(UnitEntity(
                        localId = apiUnit.id.toLong(),
                        userId = userId,
                        unitName = apiUnit.name,
                        isActive = apiUnit.isActive
                    ))).first()
                    
                    apiUnit.modules.forEach { apiModule ->
                        val moduleId = dao.insertModules(listOf(ModuleEntity(
                            moduleId = apiModule.id.toLong(),
                            unitId = unitId,
                            name = apiModule.name
                        ))).first()
                        
                        apiModule.topics.forEach { apiTopic ->
                            val topicId = dao.insertTopics(listOf(TopicEntity(
                                topicId = apiTopic.id.toLong(),
                                moduleId = moduleId,
                                name = apiTopic.name
                            ))).first()
                            
                            val subtopics = apiTopic.subtopics.map { apiSubtopic ->
                                SubtopicEntity(
                                    subtopicId = apiSubtopic.id.toLong(),
                                    topicId = topicId,
                                    name = apiSubtopic.name,
                                    isCompleted = apiSubtopic.isCompleted
                                )
                            }
                            dao.insertSubtopics(subtopics)
                        }
                    }
                }
            } else {
                val unitEntities = response.activeUnits?.map { unitName ->
                    UnitEntity(userId = userId, unitName = unitName, isActive = true)
                } ?: emptyList()
                if (unitEntities.isNotEmpty()) {
                    dao.insertUnits(unitEntities)
                }
            }

            // Sync quiz history (REPLACE will handle duplicates if we had IDs)
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
                // Fallback for dev/offline first time
                val devUser = UserEntity(
                    id = userId,
                    username = userId,
                    role = "Student",
                    semesterStatus = "Active",
                    difficulty = "Medium (Standard)",
                    aiPersona = "Socratic Mentor"
                )
                dao.insertUser(devUser)
                emit(devUser)
            }
        }
    }

    suspend fun syncUserToLocal(userId: String, email: String, passwordHash: String) {
        val response = api.getDashboard(userId)
        val existingUser = dao.getUserById(userId)
        val userEntity = UserEntity(
            id = userId,
            username = response.username ?: userId,
            role = response.role ?: "Student",
            semesterStatus = response.semesterStatus ?: "Active",
            difficulty = response.difficulty ?: "Medium (Standard)",
            aiPersona = response.aiPersona ?: "Socratic Mentor",
            email = email.ifBlank { existingUser?.email ?: "" },
            passwordHash = passwordHash.ifBlank { existingUser?.passwordHash ?: "" }
        )
        dao.insertUser(userEntity)
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

    suspend fun getDashboardResponse(userId: String): com.example.edu_ai.data.remote.DashboardResponse {
        return api.getDashboard(userId)
    }

    // --- Ingestion / Library Methods ---
    suspend fun getLibraryUnits() = api.getLibraryUnits()
    suspend fun addUnitToUser(unitId: Int, userId: String) = api.addUnitToUser(unitId, userId)

    // --- Learning Trace Methods ---
    suspend fun getLearningSession(subtopicId: Int, userId: String) = api.getLearningSession(subtopicId, userId)
    suspend fun nextObjective(subtopicId: Int, userId: String, userMessage: String? = null) = api.nextObjective(subtopicId, userId)

    suspend fun saveLearningContent(content: LearningContentEntity) = dao.insertLearningContent(content)
    fun getSavedLearningContent(subtopicId: Long) = dao.getLearningContentForSubtopic(subtopicId)
    suspend fun updateSubtopicProgress(subtopicId: Long, isCompleted: Boolean) {
        dao.updateSubtopicStatus(subtopicId, isCompleted)
        try {
            api.updateSubtopicProgress(subtopicId.toInt(), isCompleted)
        } catch (e: Exception) {
            // Background sync could be added here
        }
    }
}
