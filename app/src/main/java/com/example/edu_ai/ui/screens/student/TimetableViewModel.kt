
package com.example.edu_ai.ui.screens.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.edu_ai.EduAIApplication
import com.example.edu_ai.data.local.UserEntity
import com.example.edu_ai.data.remote.ApiTimetableResponse
import com.example.edu_ai.data.remote.ApiTimetableSlot
import com.example.edu_ai.repository.EduAIRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class TimetableUiState(
    val isLoading: Boolean = false,
    val weeklyPlan: List<ApiTimetableSlot> = emptyList(),
    val aiBrief: String = "",
    val error: String? = null
)

class TimetableViewModel(
    private val repository: EduAIRepository,
    private val user: UserEntity
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimetableUiState())
    val uiState: StateFlow<TimetableUiState> = _uiState

    init {
        loadTimetable()
    }

    fun loadTimetable() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = repository.getWeeklyTimetable(user.id)
                _uiState.value = TimetableUiState(
                    weeklyPlan = response.weeklyPlan,
                    aiBrief = response.aiBrief,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to synchronize your study plan."
                )
            }
        }
    }

    companion object {
        fun provideFactory(user: UserEntity): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as EduAIApplication)
                TimetableViewModel(application.repository, user)
            }
        }
    }
}


 