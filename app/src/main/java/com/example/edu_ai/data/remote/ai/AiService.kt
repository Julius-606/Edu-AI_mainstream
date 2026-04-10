package com.example.edu_ai.data.remote.ai

import com.example.edu_ai.data.local.UserEntity

/**
 * The Brain of the operation. 
 * This interface allows us to hot-swap AI models (e.g., from Gemini Flash to Gemini Pro)
 * without breaking the UI.
 */
interface AiService {
    suspend fun getChatResponse(
        prompt: String,
        userContext: UserEntity,
        history: List<ChatMessage> = emptyList()
    ): String
}

data class ChatMessage(
    val role: String, // "user" or "model"
    val content: String
)

/**
 * AI Switch System: This factory decides which "Engine" to ignite.
 * For now, we'll use a simple flag or BuildConfig to switch.
 */
class AiServiceFactory {
    fun createService(isProMode: Boolean): AiService {
        return if (isProMode) {
            // This would be the "Elder Brother" model (e.g., a dedicated backend or Vertex AI)
            ProAiService() 
        } else {
            // This is the "Dev/Pitch" model (e.g., Gemini API via SDK)
            GeminiAiService()
        }
    }
}

// Placeholder implementations for now
class GeminiAiService : AiService {
    override suspend fun getChatResponse(prompt: String, userContext: UserEntity, history: List<ChatMessage>): String {
        return "Gemini (Dev Mode): I see you're in ${userContext.semesterStatus}. Let's talk about $prompt."
    }
}

class ProAiService : AiService {
    override suspend fun getChatResponse(prompt: String, userContext: UserEntity, history: List<ChatMessage>): String {
        return "Pro Engine (Scale Mode): Deep analysis for ${userContext.username} initiated..."
    }
}
