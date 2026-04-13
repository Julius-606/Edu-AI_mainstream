// IDENTITY: repository/EduAIRepository.kt
package com.example.edu_ai.repository

import com.example.edu_ai.data.local.EduAIDao
import com.example.edu_ai.data.local.UserEntity
import com.example.edu_ai.data.local.UnitEntity
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
            
            val userEntity = UserEntity(
                id = userId,
                username = response.username,
                role = response.role,
                sensoryMode = response.sensoryMode,
                semesterStatus = response.semesterStatus,
                aiPersona = response.aiPersona
            )
            dao.insertUser(userEntity)
            
            dao.deleteAllUnits()
            val unitEntities = response.activeUnits.map { unitName ->
                UnitEntity(unitName = unitName, isActive = true)
            }
            dao.insertUnits(unitEntities)

            emit(userEntity)
        } catch (e: Exception) {
            // 2. Fallback: Check Local DB
            val cachedUser = dao.getUser().firstOrNull()
            if (cachedUser != null) {
                emit(cachedUser)
            } else {
                // 3. Dev Fallback: If everything fails and DB is empty, provide a mock user
                val devUser = UserEntity(
                    id = userId,
                    username = "Dev Trader",
                    role = "Student",
                    sensoryMode = "Visual",
                    semesterStatus = "Year 4 - Redemption Arc 🔥",
                    aiPersona = "Socratic Mentor"
                )
                dao.insertUser(devUser)

                // Add some default units so the Quiz tab isn't empty
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
