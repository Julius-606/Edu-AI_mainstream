// IDENTITY: data/local/EduAIDao.kt
package com.example.edu_ai.data.local

import androidx.room.*
import com.example.edu_ai.data.local.cas.CasBlobEntity
import com.example.edu_ai.data.local.cas.CommitLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EduAIDao {

    // User
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM users LIMIT 1")
    fun getUser(): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?
    
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("DELETE FROM users")
    suspend fun clearUsers()

    // Units
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnits(units: List<UnitEntity>): List<Long>

    @Query("SELECT * FROM units WHERE userId = :userId AND isActive = 1")
    fun getAllUnits(userId: String): Flow<List<UnitEntity>>

    @Query("SELECT * FROM units WHERE userId = :userId AND isActive = 0")
    fun getArchivedUnits(userId: String): Flow<List<UnitEntity>>

    @Transaction
    @Query("SELECT * FROM units WHERE userId = :userId AND isActive = 1")
    fun getAllUnitsWithModules(userId: String): Flow<List<UnitWithModules>>

    @Transaction
    @Query("SELECT * FROM units WHERE userId = :userId")
    fun getAllUnitsWithModulesIncludeArchived(userId: String): Flow<List<UnitWithModules>>

    @Query("DELETE FROM units WHERE localId = :unitId")
    suspend fun deleteUnit(unitId: Long)

    @Query("UPDATE units SET isActive = :isActive WHERE localId = :unitId")
    suspend fun setUnitActiveStatus(unitId: Long, isActive: Boolean)

    @Query("DELETE FROM units WHERE userId = :userId")
    suspend fun deleteUnitsForUser(userId: String)

    @Query("DELETE FROM units")
    suspend fun deleteAllUnits()

    // Modules
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModules(modules: List<ModuleEntity>): List<Long>

    @Query("SELECT * FROM modules WHERE unitId = :unitId")
    fun getModulesForUnit(unitId: Long): Flow<List<ModuleEntity>>

    // Topics
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopics(topics: List<TopicEntity>): List<Long>

    @Query("SELECT * FROM topics WHERE moduleId = :moduleId")
    fun getTopicsForModule(moduleId: Long): Flow<List<TopicEntity>>

    // Subtopics
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtopics(subtopics: List<SubtopicEntity>): List<Long>

    @Query("SELECT * FROM subtopics WHERE topicId = :topicId")
    fun getSubtopicsForTopic(topicId: Long): Flow<List<SubtopicEntity>>

    @Query("UPDATE subtopics SET isCompleted = :isCompleted WHERE subtopicId = :subtopicId")
    suspend fun updateSubtopicStatus(subtopicId: Long, isCompleted: Boolean)

    // Quiz History
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizHistory(quiz: QuizHistoryEntity)

    @Query("SELECT * FROM quiz_history WHERE userId = :userId ORDER BY timestamp DESC")
    fun getQuizHistory(userId: String): Flow<List<QuizHistoryEntity>>

    @Query("DELETE FROM quiz_history")
    suspend fun clearAllQuizHistory()

    // Chat Sessions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatSession(session: ChatSessionEntity): Long

    @Update
    suspend fun updateChatSession(session: ChatSessionEntity)

    @Query("SELECT * FROM chat_sessions WHERE userId = :userId AND isArchived = 0 ORDER BY timestamp DESC LIMIT 1")
    suspend fun getActiveSession(userId: String): ChatSessionEntity?

    @Query("SELECT * FROM chat_sessions WHERE userId = :userId AND isArchived = 1 ORDER BY timestamp DESC")
    fun getArchivedSessions(userId: String): Flow<List<ChatSessionEntity>>

    @Query("UPDATE chat_sessions SET isArchived = 1 WHERE userId = :userId AND isArchived = 0")
    suspend fun archiveActiveSessions(userId: String)

    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Int)

    @Query("DELETE FROM chat_sessions")
    suspend fun clearAllChatSessions()

    // Chat Messages
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity)

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getChatMessagesBySession(sessionId: Int): Flow<List<ChatMessageEntity>>

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun clearSessionMessages(sessionId: Int)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllChatHistory()

    // Timetable
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimetable(timetable: TimetableEntity)

    @Query("SELECT * FROM timetables WHERE userId = :userId")
    suspend fun getTimetableByUserId(userId: String): TimetableEntity?

    @Query("DELETE FROM timetables WHERE userId = :userId")
    suspend fun deleteTimetable(userId: String)

    @Query("DELETE FROM timetables")
    suspend fun clearAllTimetables()

    // Notes
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("SELECT * FROM notes WHERE userId = :userId ORDER BY lastUpdated DESC")
    fun getAllNotes(userId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getNoteBySession(sessionId: Int): NoteEntity?

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteNote(noteId: Int)

    @Query("DELETE FROM notes")
    suspend fun clearAllNotes()

    // Learning Content
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLearningContent(content: LearningContentEntity)

    @Query("SELECT * FROM learning_content WHERE subtopicId = :subtopicId ORDER BY timestamp ASC")
    fun getLearningContentForSubtopic(subtopicId: Long): Flow<List<LearningContentEntity>>

    @Query("DELETE FROM learning_content")
    suspend fun clearAllLearningContent()

    // Sync Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueueSyncOperation(operation: SyncOperationEntity)

    @Query("SELECT * FROM sync_operations WHERE userId = :userId ORDER BY createdAt ASC")
    suspend fun getPendingSyncOperations(userId: String): List<SyncOperationEntity>

    @Query("DELETE FROM sync_operations WHERE operationId = :operationId")
    suspend fun deleteSyncOperation(operationId: String)

    // CAS Blobs & Commit Logs
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCasBlob(blob: CasBlobEntity)

    @Query("SELECT * FROM cas_blobs WHERE hash = :hash LIMIT 1")
    suspend fun getCasBlob(hash: String): CasBlobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommitLog(log: CommitLogEntity)

    @Query("SELECT * FROM commit_logs WHERE commitHash = :commitHash LIMIT 1")
    suspend fun getCommitLog(commitHash: String): CommitLogEntity?

    @Query("SELECT * FROM commit_logs WHERE entityId = :entityId ORDER BY timestamp ASC")
    fun getCommitLogsForEntity(entityId: String): Flow<List<CommitLogEntity>>
}
