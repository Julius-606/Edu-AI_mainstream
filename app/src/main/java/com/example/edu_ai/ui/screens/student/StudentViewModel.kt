
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
import com.example.edu_ai.repository.EduAIRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class StudentUiState(
    val user: UserEntity? = null,
    val units: List<UnitEntity> = emptyList(),
    val unitsWithModules: List<UnitWithModules> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class StudentViewModel(val repository: EduAIRepository, private val dao: com.example.edu_ai.data.local.EduAIDao) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<StudentUiState> = combine(
        dao.getUser(),
        dao.getAllUnits(),
        dao.getAllUnitsWithModules(),
        _isLoading,
        _error
    ) { user, units, unitsWithModules, isLoading, error ->
        StudentUiState(user, units, unitsWithModules, isLoading, error)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StudentUiState(isLoading = true)
    )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val bookmarks: Flow<List<com.example.edu_ai.data.local.BookmarkEntity>> = uiState.flatMapLatest { state ->
        val userId = state.user?.id ?: ""
        repository.getLocalBookmarks(userId)
    }

    suspend fun addBookmark(
        userId: String,
        type: String,
        title: String,
        target: String,
        context: String,
        notes: String?
    ) {
        repository.saveBookmarkAndSync(userId, type, title, target, context, notes)
    }

    suspend fun deleteBookmark(bookmarkId: String) {
        repository.deleteLocalBookmark(bookmarkId)
    }

    suspend fun toggleSubtopicCompleted(userId: String, subtopicId: Long, isCompleted: Boolean) {
        repository.updateSubtopicStatusAndSync(userId, subtopicId, isCompleted)
    }

    suspend fun updateSubtopicCachedContent(subtopicId: Long, cachedContentJson: String?) {
        dao.updateSubtopicCachedContent(subtopicId, cachedContentJson)
    }

    suspend fun getSubtopicById(subtopicId: Long): com.example.edu_ai.data.local.SubtopicEntity? {
        return dao.getSubtopicById(subtopicId)
    }

    fun triggerCloudSync(userId: String) {
        viewModelScope.launch {
            repository.triggerSync(userId)
        }
    }

    suspend fun repositoryChat(userId: String, promptOrTopic: String): String {
        val finalPrompt = if (promptOrTopic.length < 100 && !promptOrTopic.contains("You are simulating") && !promptOrTopic.contains("Teach me") && !promptOrTopic.contains("Explain")) {
            "Provide a very concise, structured medical/biochemical high-yield study review for: $promptOrTopic. Limit to 3 sentences emphasizing diagnostic tips or common exam traps."
        } else {
            promptOrTopic
        }
        val userContext = dao.getUserById(userId) ?: UserEntity(id = userId, username = "Student", role = "student", sensoryMode = "Visual", semesterStatus = "Active", aiPersona = "Helper")
        val aiService = com.example.edu_ai.data.remote.ai.AiServiceFactory().createService(isProMode = false)
        return aiService.getChatResponse(finalPrompt, userContext, emptyList())
    }

    init {
        viewModelScope.launch {
            dao.sanitizeCorruptedCachedContent()
        }
    }

    fun refreshDashboard(userId: String) {
        viewModelScope.launch {
            if (uiState.value.unitsWithModules.isEmpty()) {
                _isLoading.value = true
            }
            _error.value = null
            try {
                // repository.getDashboardData(userId) updates the DAO seamlessly in the background
                repository.getDashboardData(userId).collect()
                // Auto trigger sync on refresh to make sure we are synchronized!
                repository.triggerSync(userId)
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


 