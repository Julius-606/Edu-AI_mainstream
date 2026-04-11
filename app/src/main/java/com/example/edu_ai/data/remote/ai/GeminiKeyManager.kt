package com.example.edu_ai.data.remote.ai

import android.util.Log

/**
 * 🔑 The Key Rotary System
 * Inspired by your Python implementation!
 */
object GeminiKeyManager {
    
    private val apiKeys = listOf(
        "AIzaSyDmvjVkFmt0RoTMNER8fYoIKfy7Pkw1sfo",
        "AIzaSyDgvt1qfR_IG-UN__WcOPj1hv5s1IVUWHY",
        "AIzaSyAexK9L9QdaJriD0NN7bfeSewiqfyMaR7g",
        "AIzaSyAhUSxiUpljDD91wLNC1__18PywTvd0kCM"
    )

    private var currentKeyIndex = 0

    /**
     * Returns the current API key in the rotation.
     */
    fun getCurrentKey(): String {
        if (apiKeys.isEmpty()) return ""
        return apiKeys[currentKeyIndex % apiKeys.size]
    }

    /**
     * Swaps to the next key. Returns the new key.
     */
    fun rotateKey(): String {
        if (apiKeys.size <= 1) return getCurrentKey()
        currentKeyIndex = (currentKeyIndex + 1) % apiKeys.size
        Log.d("GeminiKeyManager", "🔄 Swapped to Key Index: $currentKeyIndex")
        return getCurrentKey()
    }

    fun getKeyCount(): Int = apiKeys.size

    /**
     * For debugging, returns the last 4 digits of the current key.
     */
    fun getKeySnippet(): String {
        val key = getCurrentKey()
        return if (key.length > 4) "...${key.takeLast(4)}" else "INVALID"
    }
    
    // Legacy support for older code
    fun getNextKey(): String = rotateKey()
}
