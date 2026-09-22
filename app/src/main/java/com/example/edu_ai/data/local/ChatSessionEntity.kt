
package com.example.edu_ai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val title: String = "New Session",
    val description: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false
)


 