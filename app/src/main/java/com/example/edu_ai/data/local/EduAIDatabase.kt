// IDENTITY: data/local/EduAIDatabase.kt
package com.example.edu_ai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class, 
        UnitEntity::class, 
        QuizHistoryEntity::class,
        ChatMessageEntity::class,
        ChatSessionEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class EduAIDatabase : RoomDatabase() {
    abstract fun dao(): EduAIDao
}
