
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
        val apiHistory = history.map { ApiChatMessage(role = it.role, content = it.content) }
        val response = RetrofitClient.instance.aiChat(
            ChatRequest(
                prompt = prompt,
                user_id = userContext.id,
                history = apiHistory
            )
        )
        return response.response
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
        userContext: UserEntity,
        studyContext: com.example.edu_ai.data.remote.ApiStudyContextPayload?
    ): String {
        return try {
            if (studyContext != null) {
                RetrofitClient.instance.getRecommendationsWithContext(userContext.id, studyContext).recommendation
            } else {
                RetrofitClient.instance.getRecommendations(userContext.id).recommendation
            }
        } catch (e: Exception) {
            // Intelligent fallback referencing student's real metrics even if offline!
            val weak = studyContext?.weakTopics?.firstOrNull()
            val mastered = studyContext?.masteredTopics?.firstOrNull()
            val pending = studyContext?.pendingSubtopicNames?.firstOrNull() ?: "core syllabus nodes"
            
            if (weak != null && mastered != null) {
                "Superb retention in $mastered! Direct your immediate focus to $weak to resolve clinical distractor traps, then advance to $pending."
            } else if (weak != null) {
                "High-priority diagnostic review recommended in $weak where recent assessment accuracy flagged key pathophysiology gaps."
            } else if (mastered != null) {
                "Consistent mastery demonstrated across $mastered! Maintain this high-yield trajectory by conquering $pending today."
            } else {
                "Keep focusing on your active units! Your personalized strategy is continuously adapting to your progress."
            }
        }
    }
}

interface AiService {
    suspend fun getChatResponse(prompt: String, userContext: UserEntity, history: List<ChatMessage> = emptyList()): String
    suspend fun generateQuiz(unitName: String, userContext: UserEntity, topic: String? = null): QuizResponse?
    suspend fun recordQuizResult(unitName: String, score: Int, total: Int, userContext: UserEntity)
    suspend fun getRecommendations(userContext: UserEntity, studyContext: com.example.edu_ai.data.remote.ApiStudyContextPayload? = null): String
}

data class ChatMessage(val role: String, val content: String)


 