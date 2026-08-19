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
                dao.getAllUnits(user.id),
                dao.getAllUnitsWithModules(user.id),
                _isLoading,
                _error,
                _dashboardResponse
            ) { units, unitsWithModules, isLoading, error, dashboardResponse ->
                StudentUiState(
                    user = user,
                    units = units,
                    unitsWithModules = unitsWithModules,
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

    fun refreshDashboard(userId: String, force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && (now - lastRefreshTime) < 300_000) { // 5 minute cache
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Fetch full response to get lastPoint
                val response = repository.getDashboardResponse(userId)
                _dashboardResponse.value = response
                // Still use the sync logic in repository to update local DB
                repository.getDashboardData(userId).collect()
                lastRefreshTime = System.currentTimeMillis()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
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
