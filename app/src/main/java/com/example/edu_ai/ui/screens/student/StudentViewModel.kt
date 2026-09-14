package com.example.edu_ai.ui.screens.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.edu_ai.EduAIApplication
import com.example.edu_ai.data.local.UserEntity
import com.example.edu_ai.data.local.UnitEntity
import com.example.edu_ai.data.local.UnitWithModules
import com.example.edu_ai.data.remote.DashboardResponse
import com.example.edu_ai.repository.EduAIRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class StudentUiState(
    val user: UserEntity? = null,
    val units: List<UnitEntity> = emptyList(),
    val archivedUnits: List<UnitEntity> = emptyList(),
    val unitsWithModules: List<UnitWithModules> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val dashboardResponse: DashboardResponse? = null
)

class StudentViewModel(private val repository: EduAIRepository, private val dao: com.example.edu_ai.data.local.EduAIDao) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _dashboardResponse = MutableStateFlow<DashboardResponse?>(null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<StudentUiState> = dao.getUser().flatMapLatest { user ->
        if (user == null) {
            flowOf(StudentUiState(isLoading = true))
        } else {
            combine(
                combine(dao.getAllUnits(user.id), dao.getArchivedUnits(user.id), dao.getAllUnitsWithModules(user.id)) { units, archived, unitsWithModules ->
                    Triple(units, archived, unitsWithModules)
                },
                _isLoading,
                _error,
                _dashboardResponse
            ) { tuple, isLoading, error, dashboardResponse ->
                StudentUiState(
                    user = user,
                    units = tuple.first,
                    archivedUnits = tuple.second,
                    unitsWithModules = tuple.third,
                    isLoading = isLoading,
                    error = error,
                    dashboardResponse = dashboardResponse
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StudentUiState(isLoading = true)
    )

    private var lastRefreshTime = 0L
    private val DAILY_REFRESH_INTERVAL = 24 * 60 * 60 * 1000L // 24 hours

    fun refreshDashboard(userId: String, force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && (now - lastRefreshTime) < DAILY_REFRESH_INTERVAL) { // 24 hour cache
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = repository.getDashboardResponse(userId)
                _dashboardResponse.value = response
                repository.getDashboardData(userId).collect()
                lastRefreshTime = System.currentTimeMillis()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteUnit(unitId: Long) {
        val user = uiState.value.user ?: return
        viewModelScope.launch {
            repository.deleteUnit(unitId, user.id)
        }
    }

    fun archiveUnit(unitId: Long, isActive: Boolean) {
        val user = uiState.value.user ?: return
        viewModelScope.launch {
            repository.archiveUnit(unitId, isActive, user.id)
        }
    }

    fun updateUserProfile(username: String, email: String, difficulty: String, aiPersona: String, semesterStatus: String) {
        val currentUser = uiState.value.user ?: return
        val updatedUser = currentUser.copy(
            username = username,
            email = email,
            difficulty = difficulty,
            aiPersona = aiPersona,
            semesterStatus = semesterStatus
        )
        viewModelScope.launch {
            repository.updateUser(updatedUser)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as EduAIApplication)
                StudentViewModel(application.repository, application.database.dao())
            }
        }
    }
}
