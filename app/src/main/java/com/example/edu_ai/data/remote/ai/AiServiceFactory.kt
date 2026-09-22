
package com.example.edu_ai.data.remote.ai

/**
 * Factory to create instances of [AiService].
 */
class AiServiceFactory {
    /**
     * Creates an [AiService] instance.
     * @param isProMode Boolean flag to determine which AI service to provide.
     * Currently, it always returns [GeminiAiService] as it's the primary network-first service.
     */
    fun createService(isProMode: Boolean): AiService {
        return GeminiAiService()
    }
}


