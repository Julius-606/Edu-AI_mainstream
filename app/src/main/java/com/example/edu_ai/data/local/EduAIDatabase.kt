// IDENTITY: data/local/EduAIDatabase.kt
package com.example.edu_ai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.edu_ai.data.local.cas.CasBlobEntity
import com.example.edu_ai.data.local.cas.CommitLogEntity

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS cas_blobs (hash TEXT NOT NULL PRIMARY KEY, compressedContent BLOB NOT NULL, contentSize INTEGER NOT NULL, createdAt INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS commit_logs (commitHash TEXT NOT NULL PRIMARY KEY, parentHash TEXT, entityType TEXT NOT NULL, entityId TEXT NOT NULL, deltaPatch TEXT NOT NULL, blobHash TEXT NOT NULL, userId TEXT NOT NULL, timestamp INTEGER NOT NULL)")
        db.execSQL("ALTER TABLE chat_messages ADD COLUMN contentHash TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE notes ADD COLUMN contentHash TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE notes ADD COLUMN headCommitHash TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE notes ADD COLUMN parentHash TEXT")
        db.execSQL("ALTER TABLE quiz_history ADD COLUMN quizJsonHash TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE learning_content ADD COLUMN contentHash TEXT NOT NULL DEFAULT ''")
    }

}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS bookmarks (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "userId TEXT NOT NULL, " +
                "subtopicId INTEGER NOT NULL, " +
                "objectiveDescription TEXT NOT NULL, " +
                "excerpt TEXT NOT NULL, " +
                "createdAt INTEGER NOT NULL)"
        )
    }
}

@Database(
    entities = [
        UserEntity::class, 
        UnitEntity::class, 
        ModuleEntity::class,
        TopicEntity::class,
        SubtopicEntity::class,
        QuizHistoryEntity::class,
        ChatMessageEntity::class,
        ChatSessionEntity::class,
        TimetableEntity::class,
        NoteEntity::class,
        BookmarkEntity::class,
        LearningContentEntity::class,
        SyncOperationEntity::class,
        CasBlobEntity::class,
        CommitLogEntity::class,
        ContentCompletionEntity::class
    ],
    version = 14,
    exportSchema = false
)
abstract class EduAIDatabase : RoomDatabase() {
    abstract fun dao(): EduAIDao
}
