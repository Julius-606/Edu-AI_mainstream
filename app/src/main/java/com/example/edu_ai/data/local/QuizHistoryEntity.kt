// IDENTITY: data/local/QuizHistoryEntity.kt
// VERSION: 1.3.0

package com.example.edu_ai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quiz_history")
data class QuizHistoryEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val userId: String, 
    val unitName: String,
    val topic: String? = null,
    val pnlScore: Double,
    val timestamp: Long,
    val quizJson: String? = null,
    val quizJsonHash: String = ""
)
