package com.example.edu_ai.ui.screens.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.edu_ai.EduAIApplication
import com.example.edu_ai.data.local.EduAIDao
import com.example.edu_ai.data.local.QuizHistoryEntity
import com.example.edu_ai.data.local.UserEntity
import com.example.edu_ai.data.remote.ai.AiService
import com.example.edu_ai.data.remote.ai.AiServiceFactory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProgressUiState(
    val quizHistory: List<QuizHistoryEntity> = emptyList(),
    val aiRecommendation: String = "Generating your personalized strategy...",
    val isLoading: Boolean = false
)

class ProgressViewModel(
    private val aiService: AiService,
    private val dao: EduAIDao,
    private val user: UserEntity
) : ViewModel() {

    private val _aiRecommendation = MutableStateFlow("Analyze your recent quizzes to see where you can grow.")
    private val _isLoading = MutableStateFlow(false)

    val uiState: StateFlow<ProgressUiState> = combine(
        dao.getQuizHistory(),
        _aiRecommendation,
        _isLoading
    ) { history, recommendation, loading ->
        ProgressUiState(history, recommendation, loading)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProgressUiState(isLoading = true)
    )

    fun refreshRecommendations() {
        viewModelScope.launch {
            val history = uiState.value.quizHistory
            if (history.isEmpty()) {
                _aiRecommendation.value = "Take your first quiz to unlock personalized AI strategy!"
                return@launch
            }

            _isLoading.value = true
            try {
                val recommendation = aiService.getRecommendations(user, history)
                _aiRecommendation.value = recommendation
            } catch (e: Exception) {
                _aiRecommendation.value = "Stay consistent! Your next breakthrough is just one study session away."
            } finally {
                _isLoading.value = false
            }
        }
    }

    companion object {
        fun provideFactory(user: UserEntity): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as EduAIApplication)
                val aiService = AiServiceFactory().createService(isProMode = false)
                ProgressViewModel(aiService, application.database.dao(), user)
            }
        }
    }
}
