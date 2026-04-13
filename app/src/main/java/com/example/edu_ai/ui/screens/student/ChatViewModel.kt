package com.example.edu_ai.ui.screens.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.edu_ai.EduAIApplication
import com.example.edu_ai.data.local.ChatMessageEntity
import com.example.edu_ai.data.local.EduAIDao
import com.example.edu_ai.data.local.UserEntity
import com.example.edu_ai.data.remote.ai.AiService
import com.example.edu_ai.data.remote.ai.AiServiceFactory
import com.example.edu_ai.data.remote.ai.ChatMessage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isTyping: Boolean = false,
    val error: String? = null
)

class ChatViewModel(
    private val aiService: AiService,
    private val dao: EduAIDao,
    private val user: UserEntity
) : ViewModel() {

    private val _isTyping = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ChatUiState> = combine(
        dao.getChatMessages(user.id),
        _isTyping,
        _error
    ) { localMessages, typing, err ->
        ChatUiState(
            messages = localMessages.map { ChatMessage(it.role, it.content) },
            isTyping = typing,
            error = err
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChatUiState()
    )

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            // 1. Save user message to Local DB
            dao.insertChatMessage(
                ChatMessageEntity(
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
                        userId = user.id,
                        role = "model",
                        content = response
                    )
                )
            } catch (e: Exception) {
                _error.value = "Connection lost. The AI is offline."
            } finally {
                _isTyping.value = false
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            dao.clearChatHistory(user.id)
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
