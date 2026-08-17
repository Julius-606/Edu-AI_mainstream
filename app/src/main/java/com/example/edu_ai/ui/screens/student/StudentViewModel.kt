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

    val uiState: StateFlow<StudentUiState> = combine(
        dao.getUser(),
        dao.getAllUnits(),
        dao.getAllUnitsWithModules(),
        _isLoading,
        _error,
        _dashboardResponse
    ) { params: Array<Any?> ->
        StudentUiState(
            user = params[0] as? UserEntity,
            units = params[1] as? List<UnitEntity> ?: emptyList(),
            unitsWithModules = params[2] as? List<UnitWithModules> ?: emptyList(),
            isLoading = params[3] as? Boolean ?: false,
            error = params[4] as? String,
            dashboardResponse = params[5] as? DashboardResponse
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StudentUiState(isLoading = true)
    )

    fun refreshDashboard(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Fetch full response to get lastPoint
                val response = repository.getDashboardResponse(userId)
                _dashboardResponse.value = response
                // Still use the sync logic in repository to update local DB
                repository.getDashboardData(userId).collect()
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
