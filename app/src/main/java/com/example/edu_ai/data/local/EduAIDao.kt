// IDENTITY: data/local/EduAIDao.kt
package com.example.edu_ai.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EduAIDao {

    // User
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM users LIMIT 1")
    fun getUser(): Flow<UserEntity?>
    
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("DELETE FROM users")
    suspend fun clearUsers()

    // Units
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnits(units: List<UnitEntity>)

    @Query("SELECT * FROM units")
    fun getAllUnits(): Flow<List<UnitEntity>>

    @Query("DELETE FROM units")
    suspend fun deleteAllUnits()

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
}
