// IDENTITY: data/local/QuizHistoryEntity.kt
// VERSION: 1.2.0
// ⚙️ GEAR 1.2: The Local Database (SQLite)

package com.example.edu_ai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quiz_history")
data class QuizHistoryEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val userId: String, // Added to support multi-user isolation
    val unitName: String,
    val pnlScore: Double,
    val timestamp: Long
)
