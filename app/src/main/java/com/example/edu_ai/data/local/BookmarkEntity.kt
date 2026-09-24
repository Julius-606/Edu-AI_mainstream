package com.example.edu_ai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: String, // "learn", "browser", "chat"
    val title: String,
    val target: String, // subtopicId, URL, or session ID
    val context: String, // study excerpt context
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String? = null
)
