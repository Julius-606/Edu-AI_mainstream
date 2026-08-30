package com.example.edu_ai.data.local.cas

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "commit_logs")
data class CommitLogEntity(
    @PrimaryKey val commitHash: String,
    val parentHash: String? = null,
    val entityType: String, // "note", "chat", "quiz", "subtopic"
    val entityId: String,
    val deltaPatch: String = "",
    val blobHash: String = "",
    val userId: String,
    val timestamp: Long = System.currentTimeMillis()
)
