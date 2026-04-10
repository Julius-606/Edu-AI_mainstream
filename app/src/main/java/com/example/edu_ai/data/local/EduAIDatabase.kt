// IDENTITY: data/local/EduAIDatabase.kt
// VERSION: 1.1.0
// ⚙️ GEAR 1.2: The Local Database (SQLite)
// This is our base currency. It handles the local ledger of all our data.

package com.example.edu_ai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [UserEntity::class, UnitEntity::class, QuizHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class EduAIDatabase : RoomDatabase() {
    abstract fun dao(): EduAIDao
}
