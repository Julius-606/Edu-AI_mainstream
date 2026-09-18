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
import kotlinx.coroutines.flow.first
import java.util.UUID

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
                email = response.email ?: existingUser?.email ?: "",
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
            email = response.email ?: email.ifBlank { existingUser?.email ?: "" },
            passwordHash = passwordHash.ifBlank { existingUser?.passwordHash ?: "" }
        )
        dao.insertUser(userEntity)
    }

    suspend fun updateUser(user: UserEntity) {
        dao.insertUser(user)
        try {
            api.updateStudentProfile(
                userId = user.id,
                updates = mapOf(
                    "username" to user.username,
                    "email" to user.email,
                    "difficulty" to user.difficulty,
                    "ai_persona" to user.aiPersona,
                    "semester_status" to user.semesterStatus
                )
            )
        } catch (e: Exception) {
            // Silently ignore network update errors if offline
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
    suspend fun getConnectionMessages(userId: String, withUserId: Int) =
        api.getConnectionMessages(userId, withUserId)
    suspend fun sendConnectionMessage(userId: String, recipientId: Int, content: String) =
        api.sendConnectionMessage(userId, com.example.edu_ai.data.remote.ConnectionMessageRequest(recipientId, content))

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
    suspend fun nextObjective(subtopicId: Int, userId: String, userMessage: String? = null) = api.nextObjective(subtopicId, userId, userMessage)
    suspend fun previousObjective(subtopicId: Int, userId: String) = api.previousObjective(subtopicId, userId)

    suspend fun saveLearningContent(content: LearningContentEntity, userId: String, objectiveId: Int) {
        dao.insertLearningContent(content)
        api.saveLearningContent(
            com.example.edu_ai.data.remote.LearningContentRequest(
                objectiveId = objectiveId,
                userId = userId,
                content = content.content
            )
        )
    }
    fun getSavedLearningContent(subtopicId: Long) = dao.getLearningContentForSubtopic(subtopicId)
    suspend fun updateSubtopicProgress(subtopicId: Long, isCompleted: Boolean) {
        dao.updateSubtopicStatus(subtopicId, isCompleted)
        val userId = dao.getUser().first()?.id ?: return
        dao.enqueueSyncOperation(
            SyncOperationEntity(
                operationId = UUID.randomUUID().toString(),
                userId = userId,
                entityType = "subtopic_progress",
                entityId = subtopicId,
                payload = gson.toJson(mapOf("is_completed" to isCompleted))
            )
        )
        syncPendingChanges(userId)
    }

    // --- Unit Management Methods ---
    fun getArchivedUnits(userId: String): Flow<List<UnitEntity>> = dao.getArchivedUnits(userId)
    fun getAllUnitsWithModulesIncludeArchived(userId: String): Flow<List<UnitWithModules>> = dao.getAllUnitsWithModulesIncludeArchived(userId)

    suspend fun deleteUnit(unitId: Long, userId: String) {
        api.deleteUserUnit(userId, unitId)
        dao.deleteUnit(unitId)
    }

    suspend fun archiveUnit(unitId: Long, isActive: Boolean, userId: String) {
        dao.setUnitActiveStatus(unitId, isActive)
        dao.enqueueSyncOperation(
            SyncOperationEntity(
                operationId = UUID.randomUUID().toString(),
                userId = userId,
                entityType = "unit_archive",
                entityId = unitId,
                payload = gson.toJson(mapOf("is_active" to isActive))
            )
        )
        syncPendingChanges(userId)
    }

    // --- CAS Methods ---
    suspend fun saveTextToCas(text: String): String {
        if (text.isBlank()) return ""
        val hash = com.example.edu_ai.data.local.cas.CasEngine.sha256(text)
        val compressed = com.example.edu_ai.data.local.cas.CasEngine.compress(text)
        val blob = com.example.edu_ai.data.local.cas.CasBlobEntity(
            hash = hash,
            compressedContent = compressed,
            contentSize = text.length
        )
        dao.insertCasBlob(blob)
        return hash
    }

    suspend fun resolveCasText(hash: String, fallback: String): String {
        if (hash.isBlank()) return fallback
        val blob = dao.getCasBlob(hash) ?: return fallback
        return try {
            com.example.edu_ai.data.local.cas.CasEngine.decompress(blob.compressedContent)
        } catch (e: Exception) {
            fallback
        }
    }

    suspend fun syncPendingChanges(userId: String) {
        val pending = dao.getPendingSyncOperations(userId)
        if (pending.isEmpty()) return
        try {
            val response = api.sync(
                com.example.edu_ai.data.remote.SyncRequest(
                    userId = userId,
                    operations = pending.map {
                        com.example.edu_ai.data.remote.SyncOperation(
                            operationId = it.operationId,
                            entityType = it.entityType,
                            entityId = it.entityId,
                            payload = gson.fromJson(it.payload, Map::class.java) as Map<String, Any?>
                        )
                    }
                )
            )
            response.appliedOperationIds.forEach { dao.deleteSyncOperation(it) }
        } catch (e: Exception) {
            // Keep the operation queued for the next synchronization attempt.
        }

    }

    suspend fun restoreAccountSnapshot(userId: String): Map<String, Any?> {
        return api.restoreUserData(userId)
    }
}
