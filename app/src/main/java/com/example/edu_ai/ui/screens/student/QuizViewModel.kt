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
import com.example.edu_ai.data.remote.ai.QuizQuestion
import com.example.edu_ai.data.remote.ai.QuizResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuizUiState(
    val quiz: QuizResponse? = null,
    val currentQuestionIndex: Int = 0,
    val selectedOptions: Map<Int, Int> = emptyMap(), // Map of question index to selected option
    val submittedQuestions: Set<Int> = emptySet(), // Questions that have been submitted/locked
    val score: Int = 0,
    val isQuizFinished: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

class QuizViewModel(
    private val aiService: AiService,
    private val dao: EduAIDao,
    private val user: UserEntity
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    fun startQuiz(unitName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(
                quiz = null,
                currentQuestionIndex = 0,
                selectedOptions = emptyMap(),
                submittedQuestions = emptySet(),
                score = 0,
                isQuizFinished = false,
                isLoading = true,
                error = null
            ) }
            val quiz = aiService.generateQuiz(unitName, user)
            if (quiz != null) {
                _uiState.update { it.copy(quiz = quiz, isLoading = false) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Failed to ignite the Quiz Engine.") }
            }
        }
    }

    fun selectOption(index: Int) {
        val state = _uiState.value
        if (state.submittedQuestions.contains(state.currentQuestionIndex)) return
        
        val newOptions = state.selectedOptions.toMutableMap()
        newOptions[state.currentQuestionIndex] = index
        _uiState.update { it.copy(selectedOptions = newOptions) }
    }

    fun submitAnswer() {
        val state = _uiState.value
        val quiz = state.quiz ?: return
        val currentIdx = state.currentQuestionIndex
        
        if (state.submittedQuestions.contains(currentIdx)) return
        val selectedIdx = state.selectedOptions[currentIdx] ?: return
        
        val currentQuestion = quiz.questions[currentIdx]
        val isCorrect = selectedIdx == currentQuestion.correctIndex
        val newScore = if (isCorrect) state.score + 1 else state.score

        _uiState.update { it.copy(
            score = newScore,
            submittedQuestions = state.submittedQuestions + currentIdx
        ) }
    }

    fun nextQuestion() {
        val state = _uiState.value
        val quiz = state.quiz ?: return
        
        if (state.currentQuestionIndex + 1 < quiz.questions.size) {
            _uiState.update { it.copy(currentQuestionIndex = state.currentQuestionIndex + 1) }
        } else {
            finishQuiz()
        }
    }

    fun previousQuestion() {
        val state = _uiState.value
        if (state.currentQuestionIndex > 0) {
            _uiState.update { it.copy(currentQuestionIndex = state.currentQuestionIndex - 1) }
        }
    }

    private fun finishQuiz() {
        val state = _uiState.value
        val quiz = state.quiz ?: return
        
        val finalScorePercentage = (state.score.toDouble() / quiz.questions.size) * 100
        
        viewModelScope.launch {
            dao.insertQuizHistory(
                QuizHistoryEntity(
                    unitName = quiz.title,
                    pnlScore = finalScorePercentage,
                    timestamp = System.currentTimeMillis()
                )
            )
            _uiState.update { it.copy(isQuizFinished = true) }
        }
    }

    companion object {
        fun provideFactory(user: UserEntity): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as EduAIApplication)
                val aiService = AiServiceFactory().createService(isProMode = false)
                QuizViewModel(aiService, application.database.dao(), user)
            }
        }
    }
}
