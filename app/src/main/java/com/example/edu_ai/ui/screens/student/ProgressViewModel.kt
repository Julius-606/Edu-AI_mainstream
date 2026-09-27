package com.example.edu_ai.ui.screens.student

import android.content.Context
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
import com.example.edu_ai.utils.PreferenceManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProgressUiState(
    val quizHistory: List<QuizHistoryEntity> = emptyList(),
    val aiRecommendation: String = "Generating your personalized strategy...",
    val isLoading: Boolean = false,
    val lastUpdatedTimestamp: Long = 0L
)

class ProgressViewModel(
    private val aiService: AiService,
    private val dao: EduAIDao,
    private val user: UserEntity,
    private val repository: com.example.edu_ai.repository.EduAIRepository? = null,
    private val context: Context? = null
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _recommendation = MutableStateFlow("Analyze your recent quizzes to see where you can grow.")
    private val _lastUpdated = MutableStateFlow(0L)

    val uiState: StateFlow<ProgressUiState> = combine(
        dao.getQuizHistory(user.id),
        _isLoading,
        _lastUpdated
    ) { history, loading, updatedTime ->
        ProgressUiState(history, _recommendation.value, loading, updatedTime)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProgressUiState(isLoading = true)
    )

    val recommendation: StateFlow<String> = _recommendation.asStateFlow()

    init {
        // Load cached recommendation if available
        context?.let { ctx ->
            PreferenceManager.getZenithRecommendation(ctx, user.id)?.let { (cachedRec, time) ->
                _recommendation.value = cachedRec
                _lastUpdated.value = time
            }
        }
    }

    /**
     * Refreshes the Zenith recommendation.
     * Enforces ONLY ONCE A DAY request frequency unless explicitly forced by the user.
     */
    fun refreshRecommendations(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val oneDayInMillis = 24 * 60 * 60 * 1000L
            val cached = context?.let { PreferenceManager.getZenithRecommendation(it, user.id) }

            // 1. If cached within 24 hours and not forcing a refresh, respect the daily request limit
            if (!forceRefresh && cached != null && (System.currentTimeMillis() - cached.second) < oneDayInMillis) {
                _recommendation.value = cached.first
                _lastUpdated.value = cached.second
                _isLoading.value = false
                return@launch
            }

            _isLoading.value = true
            try {
                // Compile comprehensive student learning context: progress, quizzes, weak and uncovered areas
                val studyContext = repository?.buildStudyContext(user.id)
                val rec = aiService.getRecommendations(user, studyContext)

                _recommendation.value = rec
                _lastUpdated.value = System.currentTimeMillis()

                // Cache to ensure it is only requested once a day
                context?.let {
                    PreferenceManager.saveZenithRecommendation(it, user.id, rec, _lastUpdated.value)
                }
            } catch (e: Exception) {
                // If offline or error occurs, build motivating contextual diagnostic advice
                val studyContext = try { repository?.buildStudyContext(user.id) } catch (ex: Exception) { null }
                val weak = studyContext?.weakTopics?.firstOrNull()
                val mastered = studyContext?.masteredTopics?.firstOrNull()
                val pending = studyContext?.pendingSubtopicNames?.firstOrNull() ?: "core syllabus milestones"
                val avgScore = studyContext?.averageQuizScore?.toInt()

                val fallback = buildString {
                    append("🌟 **Zenith Daily Tactical Brief**\n\n")
                    if (mastered != null) {
                        append("Outstanding retention demonstrated in **$mastered**")
                        if (avgScore != null) append(" (average quiz diagnostic: $avgScore%)")
                        append("! ")
                    }
                    if (weak != null) {
                        append("🎯 **Targeted Growth Area**: Recent quiz diagnostics highlight high-yield opportunities in **$weak**. Dedicated active recall here will turn this directly into an exam advantage.\n\n")
                    } else {
                        append("🎯 **Diagnostic Focus**: Your assessment metrics are consolidating nicely across all active units.\n\n")
                    }
                    append("🚀 **Uncovered Milestones**: Ready to unlock new territory? Advance into **$pending** next. Every session deepens clinical intuition!")
                }

                _recommendation.value = fallback
                _lastUpdated.value = System.currentTimeMillis()
                context?.let {
                    PreferenceManager.saveZenithRecommendation(it, user.id, fallback, _lastUpdated.value)
                }
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
                ProgressViewModel(aiService, application.database.dao(), user, application.repository, application.applicationContext)
            }
        }
    }
}
