package com.example.edu_ai.data.remote.ai

import com.example.edu_ai.data.local.QuizHistoryEntity
import com.example.edu_ai.data.local.UserEntity
import com.example.edu_ai.data.remote.RetrofitClient
import com.example.edu_ai.data.remote.ChatRequest
import com.example.edu_ai.data.remote.QuizRequest
import com.example.edu_ai.data.remote.QuizRecordRequest
import com.example.edu_ai.data.remote.ChatMessage as ApiChatMessage

/**
 * 🚀 The New Network-First AI Service
 * This service now talks to your Python Backend instead of Gemini directly.
 */
class GeminiAiService : AiService {

    override suspend fun getChatResponse(
        prompt: String,
        userContext: UserEntity,
        history: List<ChatMessage>
    ): String {
        return try {
            val apiHistory = history.map { ApiChatMessage(role = it.role, content = it.content) }
            val response = RetrofitClient.instance.aiChat(
                ChatRequest(
                    prompt = prompt,
                    user_id = userContext.id.filter { it.isDigit() }.toIntOrNull() ?: 1,
                    history = apiHistory
                )
            )
            response.response
        } catch (e: Exception) {
            "Consultation failed: ${e.localizedMessage}. Ensure the Python Backend is running."
        }
    }

    override suspend fun generateQuiz(
        unitName: String,
        userContext: UserEntity
    ): QuizResponse? {
        return try {
            val response = RetrofitClient.instance.generateAiQuiz(
                QuizRequest(
                    unit_name = unitName,
                    user_id = userContext.id.filter { it.isDigit() }.toIntOrNull() ?: 1
                )
            )
            // Map API response to UI model
            QuizResponse(
                title = response.quiz_title,
                questions = response.questions.map { q ->
                    QuizQuestion(q.question_text, q.options, q.correct_option_index, q.explanation)
                }
            )
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getRecommendations(
        userContext: UserEntity,
        quizHistory: List<QuizHistoryEntity>
    ): String {
        // We'll let the chat endpoint handle recommendations for now to keep it simple
        return "Keep focusing on your active units! Your personalized strategy is being updated."
    }
}

// Interface stays the same to avoid breaking UI code
interface AiService {
    suspend fun getChatResponse(prompt: String, userContext: UserEntity, history: List<ChatMessage> = emptyList()): String
    suspend fun generateQuiz(unitName: String, userContext: UserEntity): QuizResponse?
    suspend fun getRecommendations(userContext: UserEntity, quizHistory: List<QuizHistoryEntity>): String
}

data class ChatMessage(val role: String, val content: String)
