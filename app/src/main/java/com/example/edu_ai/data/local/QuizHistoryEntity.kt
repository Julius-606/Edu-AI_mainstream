// IDENTITY: data/local/QuizHistoryEntity.kt
// VERSION: 1.1.0
// ⚙️ GEAR 1.2: The Local Database (SQLite)
// This is our base currency. It handles the local ledger of all our data.

package com.example.edu_ai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quiz_history")
data class QuizHistoryEntity(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val unitName: String,
    val pnlScore: Double,
    val timestamp: Long
)
