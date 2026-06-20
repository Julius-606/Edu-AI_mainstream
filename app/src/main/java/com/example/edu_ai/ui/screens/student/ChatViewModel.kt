package com.example.edu_ai.ui.screens.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.edu_ai.EduAIApplication
import com.example.edu_ai.data.local.ChatMessageEntity
import com.example.edu_ai.data.local.ChatSessionEntity
import com.example.edu_ai.data.local.EduAIDao
import com.example.edu_ai.data.local.NoteEntity
import com.example.edu_ai.data.local.UserEntity
import com.example.edu_ai.data.remote.ai.AiService
import com.example.edu_ai.data.remote.ai.AiServiceFactory
import com.example.edu_ai.data.remote.ai.ChatMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isTyping: Boolean = false,
    val error: String? = null,
    val currentSessionTitle: String = "New Session",
    val currentSessionDescription: String? = null,
    val activeSessionId: Int? = null,
    val archivedSessions: List<ChatSessionEntity> = emptyList()
)

class ChatViewModel(
    private val aiService: AiService,
    private val dao: EduAIDao,
    private val user: UserEntity
) : ViewModel() {

    private val _isTyping = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _activeSession = MutableStateFlow<ChatSessionEntity?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ChatUiState> = combine(
        _activeSession.flatMapLatest { session ->
            if (session != null) dao.getChatMessagesBySession(session.id)
            else flowOf(emptyList())
        },
        _isTyping,
        _error,
        _activeSession,
        dao.getArchivedSessions(user.id)
    ) { localMessages, typing, err, session, archived ->
        ChatUiState(
            messages = localMessages.map { ChatMessage(it.role, it.content) },
            isTyping = typing,
            error = err,
            currentSessionTitle = session?.title ?: "New Session",
            currentSessionDescription = session?.description,
            activeSessionId = session?.id,
            archivedSessions = archived
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChatUiState()
    )

    init {
        loadActiveSession()
    }

    private fun loadActiveSession() {
        viewModelScope.launch {
            val session = dao.getActiveSession(user.id)
            _activeSession.value = session
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            var currentSession = _activeSession.value
            if (currentSession == null) {
                val sessionId = dao.insertChatSession(
                    ChatSessionEntity(userId = user.id, title = "New Session")
                ).toInt()
                currentSession = ChatSessionEntity(id = sessionId, userId = user.id, title = "New Session")
                _activeSession.value = currentSession
            }

            // 1. Save user message to Local DB
            dao.insertChatMessage(
                ChatMessageEntity(
                    sessionId = currentSession.id,
                    userId = user.id,
                    role = "user",
                    content = text
                )
            )
            
            _isTyping.value = true
            _error.value = null

            try {
                // 2. Get history for API context
                val history = uiState.value.messages

                // 3. Ask AI
                val response = aiService.getChatResponse(
                    prompt = text,
                    userContext = user,
                    history = history
                )
                
                // 4. Save AI response to Local DB
                dao.insertChatMessage(
                    ChatMessageEntity(
                        sessionId = currentSession.id,
                        userId = user.id,
                        role = "model",
                        content = response
                    )
                )

                // 5. Dynamic Titling and Description
                val userMessages = uiState.value.messages.filter { it.role == "user" }
                val userMessageCount = userMessages.size
                
                if (userMessageCount == 1) {
                    generateInitialMetadata(text, currentSession)
                    updateBackgroundNotes(currentSession.id, "user: $text\nmodel: $response")
                } else if (userMessageCount > 1 && userMessageCount % 3 == 0) {
                    updateMetadata(currentSession)
                    val chatContext = uiState.value.messages.takeLast(6).joinToString("\n") { "${it.role}: ${it.content}" }
                    updateBackgroundNotes(currentSession.id, chatContext)
                }

            } catch (e: Exception) {
                _error.value = "Connection lost. The AI is offline."
            } finally {
                _isTyping.value = false
            }
        }
    }

    private suspend fun updateBackgroundNotes(sessionId: Int, chatContext: String) {
        try {
            val notePrompt = "Based on the clinical/academic discussion below, generate concise summary notes.\n" +
                    "INSTRUCTIONS:\n" +
                    "- Tone: Academic and professional.\n" +
                    "- Content: Focus ONLY on key points, core concepts, and brief clinical/academic explanations.\n" +
                    "- Style: Use a structured format with clear headings and bullet points. Avoid conversational filler.\n" +
                    "Discussion Context:\n$chatContext\n\n" +
                    "Return ONLY the structured notes."
            
            val updatedContent = aiService.getChatResponse(notePrompt, user, emptyList())
            
            val existingNote = dao.getNoteBySession(sessionId)
            val sessionTitle = uiState.value.currentSessionTitle
            
            if (existingNote != null) {
                dao.updateNote(existingNote.copy(
                    title = "Notes: $sessionTitle",
                    content = updatedContent,
                    lastUpdated = System.currentTimeMillis()
                ))
            } else {
                dao.insertNote(NoteEntity(
                    userId = user.id,
                    sessionId = sessionId,
                    title = "Notes: $sessionTitle",
                    content = updatedContent
                ))
            }
        } catch (e: Exception) {
            // Silently fail note updates
        }
    }

    private suspend fun generateInitialMetadata(firstMessage: String, session: ChatSessionEntity) {
        try {
            val metadataPrompt = "Analyze this medical query: '$firstMessage'.\n" +
                    "Return exactly two lines:\n" +
                    "Line 1: A concise 2-5 word topic title.\n" +
                    "Line 2: A brief 1-sentence description of the query's goal.\n" +
                    "Return ONLY these two lines, no labels, no quotes."
            
            val metadataResponse = aiService.getChatResponse(metadataPrompt, user, emptyList())
            val lines = metadataResponse.lines().filter { it.isNotBlank() }
            
            if (lines.size >= 2) {
                val title = lines[0].replace("\"", "").replace("'", "").trim()
                val description = lines[1].trim()
                
                val updatedSession = session.copy(title = title, description = description)
                dao.updateChatSession(updatedSession)
                _activeSession.value = updatedSession
            }
        } catch (e: Exception) {
            val fallbackTitle = if (firstMessage.length > 30) firstMessage.take(27) + "..." else firstMessage
            val updatedSession = session.copy(title = fallbackTitle)
            dao.updateChatSession(updatedSession)
            _activeSession.value = updatedSession
        }
    }

    private suspend fun updateMetadata(session: ChatSessionEntity) {
        try {
            val recentMessages = uiState.value.messages.takeLast(6).joinToString("\n") { "${it.role}: ${it.content}" }
            val updatePrompt = "Current Title: '${session.title}'\nCurrent Description: '${session.description}'\n\n" +
                    "Recent chat context:\n$recentMessages\n\n" +
                    "If the focus has shifted or expanded, provide a new Title and Description. " +
                    "Return exactly two lines:\n" +
                    "Line 1: The Title (updated if needed)\n" +
                    "Line 2: The Description (updated if needed)\n" +
                    "Return ONLY these two lines, no labels, no quotes."
            
            val updatedMetadata = aiService.getChatResponse(updatePrompt, user, emptyList())
            val lines = updatedMetadata.lines().filter { it.isNotBlank() }
            
            if (lines.size >= 2) {
                val title = lines[0].replace("\"", "").replace("'", "").trim()
                val description = lines[1].trim()
                
                if (title != session.title || description != session.description) {
                    val updatedSession = session.copy(title = title, description = description)
                    dao.updateChatSession(updatedSession)
                    _activeSession.value = updatedSession
                }
            }
        } catch (e: Exception) {
            // Ignore update failures
        }
    }

    fun startNewChat() {
        viewModelScope.launch {
            dao.archiveActiveSessions(user.id)
            _activeSession.value = null
        }
    }

    fun resumeSession(session: ChatSessionEntity) {
        viewModelScope.launch {
            dao.archiveActiveSessions(user.id)
            val resumedSession = session.copy(isArchived = false)
            dao.updateChatSession(resumedSession)
            _activeSession.value = resumedSession
        }
    }

    fun deleteSession(sessionId: Int) {
        viewModelScope.launch {
            dao.deleteSession(sessionId)
            if (_activeSession.value?.id == sessionId) {
                _activeSession.value = null
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            _activeSession.value?.let {
                dao.deleteSession(it.id)
                _activeSession.value = null
            }
        }
    }

    companion object {
        fun provideFactory(user: UserEntity): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as EduAIApplication)
                val aiService = AiServiceFactory().createService(isProMode = false)
                ChatViewModel(aiService, application.database.dao(), user)
            }
        }
    }
}
