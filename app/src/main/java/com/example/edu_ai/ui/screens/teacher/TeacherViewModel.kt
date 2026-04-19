package com.example.edu_ai.ui.screens.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.edu_ai.data.remote.TeacherDashboardResponse
import com.example.edu_ai.repository.EduAIRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TeacherViewModel(private val repository: EduAIRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<TeacherUiState>(TeacherUiState.Loading)
    val uiState: StateFlow<TeacherUiState> = _uiState

    private val _classReport = MutableStateFlow<String?>(null)
    val classReport: StateFlow<String?> = _classReport

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = TeacherUiState.Loading
            try {
                val data = repository.getTeacherDashboard()
                _uiState.value = TeacherUiState.Success(data)
            } catch (e: Exception) {
                _uiState.value = TeacherUiState.Error(e.message ?: "Unknown Error")
            }
        }
    }

    fun generateClassReport() {
        viewModelScope.launch {
            try {
                val response = repository.generateClassReport()
                _classReport.value = response.report
            } catch (e: Exception) {
                _classReport.value = "Failed to generate report: ${e.message}"
            }
        }
    }

    fun updateStudentPath(userId: String, newUnits: List<String>, semesterStatus: String) {
        viewModelScope.launch {
            try {
                val updates = mapOf(
                    "active_units" to newUnits,
                    "semester_status" to semesterStatus
                )
                repository.updateStudentProfile(userId, updates)
                loadDashboard() // Refresh data
            } catch (e: Exception) {
                // Log error or show snackbar
            }
        }
    }
    
    fun clearReport() {
        _classReport.value = null
    }
}

sealed class TeacherUiState {
    object Loading : TeacherUiState()
    data class Success(val data: TeacherDashboardResponse) : TeacherUiState()
    data class Error(val message: String) : TeacherUiState()
}
