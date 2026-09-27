package com.example.edu_ai.utils

import android.content.Context
import com.example.edu_ai.data.model.TextHighlight
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object HighlightManager {
    private const val PREF_NAME = "edu_ai_highlights"
    private const val KEY_PREFIX = "highlights_target_"
    private val gson = Gson()

    fun getHighlights(context: Context, subtopicId: Long): List<TextHighlight> {
        return getHighlightsForTarget(context, "subtopic_$subtopicId")
    }

    fun saveHighlight(context: Context, highlight: TextHighlight): List<TextHighlight> {
        val key = if (highlight.targetKey.isNotEmpty()) highlight.targetKey else "subtopic_${highlight.subtopicId}"
        return saveHighlightForTarget(context, key, highlight.copy(targetKey = key))
    }

    fun removeHighlight(context: Context, subtopicId: Long, highlightId: String): List<TextHighlight> {
        return removeHighlightForTarget(context, "subtopic_$subtopicId", highlightId)
    }

    fun clearAllHighlights(context: Context, subtopicId: Long) {
        clearAllHighlightsForTarget(context, "subtopic_$subtopicId")
    }

    fun getHighlightsForTarget(context: Context, targetKey: String): List<TextHighlight> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString("$KEY_PREFIX$targetKey", null)
            ?: prefs.getString("highlights_subtopic_${targetKey.removePrefix("subtopic_")}", null)
            ?: return emptyList()
        return try {
            val type = object : TypeToken<List<TextHighlight>>() {}.type
            gson.fromJson<List<TextHighlight>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveHighlightForTarget(context: Context, targetKey: String, highlight: TextHighlight): List<TextHighlight> {
        val current = getHighlightsForTarget(context, targetKey).toMutableList()
        val existingIndex = current.indexOfFirst { it.text.equals(highlight.text.trim(), ignoreCase = true) }
        val updatedHighlight = highlight.copy(targetKey = targetKey, text = highlight.text.trim())
        if (existingIndex >= 0) {
            current[existingIndex] = updatedHighlight
        } else {
            current.add(0, updatedHighlight)
        }
        persist(context, targetKey, current)
        return current
    }

    fun removeHighlightForTarget(context: Context, targetKey: String, highlightId: String): List<TextHighlight> {
        val current = getHighlightsForTarget(context, targetKey).toMutableList()
        current.removeAll { it.id == highlightId }
        persist(context, targetKey, current)
        return current
    }

    fun clearAllHighlightsForTarget(context: Context, targetKey: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove("$KEY_PREFIX$targetKey").apply()
    }

    private fun persist(context: Context, targetKey: String, list: List<TextHighlight>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("$KEY_PREFIX$targetKey", gson.toJson(list)).apply()
    }
}
