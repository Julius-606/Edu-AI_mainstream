
package com.example.edu_ai.ui.screens.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.edu_ai.EduAIApplication
import com.example.edu_ai.data.local.EduAIDao
import com.example.edu_ai.data.local.QuizHistoryEntity
import com.example.edu_ai.data.local.UnitWithModules
import com.example.edu_ai.data.local.UserEntity
import com.example.edu_ai.data.remote.ai.AiService
import com.example.edu_ai.data.remote.ai.AiServiceFactory
import com.example.edu_ai.data.remote.ai.QuizQuestion
import com.example.edu_ai.data.remote.ai.QuizResponse
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class QuizUiState(
    val quiz: QuizResponse? = null,
    val currentQuestionIndex: Int = 0,
    val selectedOptions: Map<Int, Int> = emptyMap(),
    val submittedQuestions: Set<Int> = emptySet(),
    val score: Int = 0,
    val isQuizFinished: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedUnit: String? = null,
    val selectedTopic: String? = null,
    val unitsWithModules: List<UnitWithModules> = emptyList(),
    val isReviewMode: Boolean = false,
    val quizHistory: List<QuizHistoryEntity> = emptyList()
)

class QuizViewModel(
    private val aiService: AiService,
    private val dao: EduAIDao,
    private val user: UserEntity
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    init {
        loadQuizHistory()
        loadUnitsWithModules()
    }

    private fun loadQuizHistory() {
        viewModelScope.launch {
            // Filter history by the current user's ID
            dao.getQuizHistory(user.id).collect { history ->
                _uiState.update { it.copy(quizHistory = history) }
            }
        }
    }

    private fun loadUnitsWithModules() {
        viewModelScope.launch {
            dao.getAllUnitsWithModules().collect { units ->
                _uiState.update { it.copy(unitsWithModules = units) }
            }
        }
    }

    fun selectUnit(unitName: String) {
        _uiState.update { it.copy(selectedUnit = unitName) }
    }

    fun startQuiz(unitName: String, topic: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(
                quiz = null,
                currentQuestionIndex = 0,
                selectedOptions = emptyMap(),
                submittedQuestions = emptySet(),
                score = 0,
                isQuizFinished = false,
                isLoading = true,
                error = null,
                selectedUnit = unitName,
                selectedTopic = topic,
                isReviewMode = false
            ) }
            
            val gson = com.google.gson.Gson()
            var quiz = aiService.generateQuiz(unitName, user, topic)
            
            if (quiz != null) {
                // Cache the newly generated quiz online
                if (topic != null) {
                    val subtopicEntity = _uiState.value.unitsWithModules
                        .flatMap { it.modules }
                        .flatMap { it.topics }
                        .flatMap { it.subtopics }
                        .find { it.name.equals(topic, ignoreCase = true) }
                    if (subtopicEntity != null) {
                        try {
                            val quizJson = gson.toJson(quiz)
                            dao.updateSubtopicCachedQuiz(subtopicEntity.subtopicId, quizJson)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } else {
                    val unitEntity = _uiState.value.unitsWithModules
                        .find { it.unit.unitName.equals(unitName, ignoreCase = true) }?.unit
                    if (unitEntity != null) {
                        try {
                            val quizJson = gson.toJson(quiz)
                            dao.updateUnitCachedQuiz(unitEntity.localId, quizJson)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } else {
                // If network/AI fails, attempt to load from SQLite Offline Cache!
                if (topic != null) {
                    val subtopicEntity = _uiState.value.unitsWithModules
                        .flatMap { it.modules }
                        .flatMap { it.topics }
                        .flatMap { it.subtopics }
                        .find { it.name.equals(topic, ignoreCase = true) }
                    if (subtopicEntity != null) {
                        val dbSubtopic = dao.getSubtopicById(subtopicEntity.subtopicId)
                        val cachedJson = dbSubtopic?.cachedQuizJson
                        if (!cachedJson.isNullOrBlank()) {
                            try {
                                quiz = gson.fromJson(cachedJson, QuizResponse::class.java)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                } else {
                    val unitEntity = _uiState.value.unitsWithModules
                        .find { it.unit.unitName.equals(unitName, ignoreCase = true) }?.unit
                    if (unitEntity != null) {
                        val dbUnit = dao.getAllUnits().firstOrNull()?.find { it.localId == unitEntity.localId }
                        val cachedJson = dbUnit?.cachedQuizJson
                        if (!cachedJson.isNullOrBlank()) {
                            try {
                                quiz = gson.fromJson(cachedJson, QuizResponse::class.java)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }

            if (quiz != null) {
                // Shuffle options for each question to break predictable patterns
                val shuffledQuestions = quiz.questions.map { question ->
                    val optionsWithIndices = question.options.withIndex().toList()
                    val shuffled = optionsWithIndices.shuffled()
                    val newCorrectIndex = shuffled.indexOfFirst { it.index == question.correctIndex }
                    val newOptions = shuffled.map { it.value }
                    question.copy(options = newOptions, correctIndex = newCorrectIndex)
                }
                val shuffledQuiz = quiz.copy(questions = shuffledQuestions)
                _uiState.update { it.copy(quiz = shuffledQuiz, isLoading = false) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Failed to ignite the Quiz Engine. No offline cache found for this assessment.") }
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
            if (state.isReviewMode) {
                 _uiState.update { it.copy(isQuizFinished = true) }
            } else {
                finishQuiz()
            }
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
        val recordTitle = state.selectedTopic ?: quiz.title
        
        viewModelScope.launch {
            // Save locally with user isolation
            dao.insertQuizHistory(
                QuizHistoryEntity(
                    userId = user.id,
                    unitName = recordTitle,
                    pnlScore = finalScorePercentage,
                    timestamp = System.currentTimeMillis()
                )
            )
            // Save to backend
            aiService.recordQuizResult(
                unitName = recordTitle,
                score = state.score,
                total = quiz.questions.size,
                userContext = user
            )
            _uiState.update { it.copy(isQuizFinished = true) }
        }
    }

    fun enterReviewMode() {
        _uiState.update { it.copy(isQuizFinished = false, isReviewMode = true, currentQuestionIndex = 0) }
    }

    fun resetQuizSelection() {
        _uiState.update { it.copy(quiz = null, selectedUnit = null, isQuizFinished = false, isReviewMode = false) }
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


 