package com.example.edu_ai.ui.screens.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.edu_ai.data.local.UserEntity
import com.example.edu_ai.data.local.UnitEntity
import com.example.edu_ai.repository.EduAIRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class StudentUiState(
    val user: UserEntity? = null,
    val units: List<UnitEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class StudentViewModel(private val repository: EduAIRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentUiState())
    val uiState: StateFlow<StudentUiState> = _uiState.asStateFlow()

    fun loadDashboard(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getDashboardData(userId)
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { user ->
                    // After repository updates local DB, we could also observe the DAO directly,
                    // but for simplicity we'll just update from what repository returns and then units.
                    // Actually, a better way is to observe the flows from the DAO.
                }
        }
    }

    // Better way: Observe the DAO flows
    val userFlow: Flow<UserEntity?> = repository.getDashboardData("STUDENT_001") // Mock ID for now
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Note: EduAIRepository.getDashboardData currently returns Flow<UserEntity?> and updates DB.
    // We might want to separate the "refresh" action from the "observe" action.
}

class StudentViewModelFactory(private val repository: EduAIRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StudentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StudentViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
