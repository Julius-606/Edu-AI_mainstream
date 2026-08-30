package com.example.edu_ai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "learning_content")
data class LearningContentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subtopicId: Long,
    val objectiveDescription: String,
    val content: String,
    val contentHash: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
