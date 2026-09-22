
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
                    user_id = userContext.id,
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
        userContext: UserEntity,
        topic: String?
    ): QuizResponse? {
        return try {
            val response = RetrofitClient.instance.generateAiQuiz(
                request = QuizRequest(
                    unit_name = unitName,
                    user_id = userContext.id
                ),
                topic = topic
            )
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

    override suspend fun recordQuizResult(
        unitName: String,
        score: Int,
        total: Int,
        userContext: UserEntity
    ) {
        try {
            RetrofitClient.instance.recordQuiz(
                QuizRecordRequest(
                    unit_name = unitName,
                    score = score,
                    total = total,
                    user_id = userContext.id,
                    timestamp = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            // Log error
        }
    }

    override suspend fun getRecommendations(
        userContext: UserEntity
    ): String {
        return try {
            val response = RetrofitClient.instance.getRecommendations(userContext.id)
            response.recommendation
        } catch (e: Exception) {
            "Keep focusing on your active units! Your personalized strategy is being updated."
        }
    }
}

interface AiService {
    suspend fun getChatResponse(prompt: String, userContext: UserEntity, history: List<ChatMessage> = emptyList()): String
    suspend fun generateQuiz(unitName: String, userContext: UserEntity, topic: String? = null): QuizResponse?
    suspend fun recordQuizResult(unitName: String, score: Int, total: Int, userContext: UserEntity)
    suspend fun getRecommendations(userContext: UserEntity): String
}

data class ChatMessage(val role: String, val content: String)


 