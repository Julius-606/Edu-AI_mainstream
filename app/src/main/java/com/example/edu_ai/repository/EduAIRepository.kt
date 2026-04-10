// IDENTITY: repository/EduAIRepository.kt
// VERSION: 1.1.0
// ⚙️ GEAR 1.3: The Repository (Data Orchestrator)
// This is the single source of truth. It decides whether to use the Network Broker or the Local Ledger.

package com.example.edu_ai.repository

import com.example.edu_ai.data.local.EduAIDao
import com.example.edu_ai.data.local.UserEntity
import com.example.edu_ai.data.local.UnitEntity
import com.example.edu_ai.data.remote.EduAIApi
import com.example.edu_ai.data.remote.DashboardResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.firstOrNull

class EduAIRepository(
    private val api: EduAIApi,
    private val dao: EduAIDao
) {

    /**
     * Fetches dashboard data from the API, updates the local database,
     * and returns a Flow that emits the user data.
     * Falls back to local database if network fails.
     */
    fun getDashboardData(userId: String): Flow<UserEntity?> = flow {
        try {
            // 1. Try to fetch from remote
            val response = api.getDashboard(userId)
            
            // 2. Map and Save to Local (Offline-First)
            val userEntity = UserEntity(
                id = userId,
                username = response.username,
                role = response.role,
                sensoryMode = response.sensoryMode,
                semesterStatus = response.semesterStatus,
                aiPersona = response.aiPersona
            )
            dao.insertUser(userEntity)
            
            // Handle units
            dao.deleteAllUnits()
            val unitEntities = response.activeUnits.map { unitName ->
                UnitEntity(unitName = unitName, isActive = true)
            }
            dao.insertUnits(unitEntities)
            
            emit(userEntity)
        } catch (e: Exception) {
            // 3. Fallback: Fetch from local ledger if network fails
            val cachedUser = dao.getUser().firstOrNull()
            emit(cachedUser)
        }
    }
}
