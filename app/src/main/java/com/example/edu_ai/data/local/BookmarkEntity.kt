package com.example.edu_ai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val subtopicId: Long,
    val objectiveDescription: String,
    val excerpt: String,
    val createdAt: Long = System.currentTimeMillis()
)
