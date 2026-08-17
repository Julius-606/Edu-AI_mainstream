package com.example.edu_ai.ui.screens.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.edu_ai.data.remote.EduAIApi
import com.example.edu_ai.repository.EduAIRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LearnUiState(
    val subtopicName: String = "",
    val objectiveDescription: String = "",
    val content: String = "",
    val isLoading: Boolean = false,
    val isLast: Boolean = false,
    val isFirst: Boolean = true,
    val subtopicCompleted: Boolean = false,
    val userMessage: String = "",
    val error: String? = null
)

class LearnViewModel(private val repository: EduAIRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LearnUiState())
    val uiState: StateFlow<LearnUiState> = _uiState.asStateFlow()

    fun onUserMessageChange(message: String) {
        _uiState.update { it.copy(userMessage = message) }
    }

    fun loadSession(subtopicId: Int, userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = repository.getLearningSession(subtopicId, userId)
                updateStateWithResponse(response, subtopicId.toLong())
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun nextObjective(subtopicId: Int, userId: String, onTriggerQuiz: () -> Unit) {
        viewModelScope.launch {
            val message = _uiState.value.userMessage
            _uiState.update { it.copy(isLoading = true, userMessage = "") }
            try {
                val response = repository.nextObjective(subtopicId, userId, message)
                if (response["status"] == "subtopic_completed") {
                    repository.updateSubtopicProgress(subtopicId.toLong(), true)
                    _uiState.update { it.copy(subtopicCompleted = true, isLoading = false) }
                    onTriggerQuiz()
                } else {
                    updateStateWithResponse(response, subtopicId.toLong())
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private suspend fun updateStateWithResponse(response: Map<String, Any>, subtopicId: Long) {
        val subtopicName = response["subtopic_name"] as String
        val objectiveDescription = response["objective_description"] as String
        val content = response["content"] as String
        val isLast = response["is_last"] as Boolean
        val isFirst = response["is_first"] as Boolean

        _uiState.update { it.copy(
            subtopicName = subtopicName,
            objectiveDescription = objectiveDescription,
            content = content,
            isLast = isLast,
            isFirst = isFirst,
            isLoading = false
        ) }

        // Save to repository
        repository.saveLearningContent(
            com.example.edu_ai.data.local.LearningContentEntity(
                subtopicId = subtopicId,
                objectiveDescription = objectiveDescription,
                content = content
            )
        )
    }

    class Factory(private val repository: EduAIRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return LearnViewModel(repository) as T
        }
    }
}
