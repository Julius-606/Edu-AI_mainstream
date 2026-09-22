package com.example.edu_ai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "content_completion")
data class ContentCompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val objectiveId: String, // ID of the course objective
    val contentId: String,   // ID of the specific content piece (e.g., subtopic ID, or a more granular content ID)
    val isCompleted: Boolean,
    val lastUpdated: Long = System.currentTimeMillis()
)
