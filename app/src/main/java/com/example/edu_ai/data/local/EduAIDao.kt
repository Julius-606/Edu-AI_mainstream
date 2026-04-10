// IDENTITY: data/local/EduAIDao.kt
// VERSION: 1.1.0
// ⚙️ GEAR 1.2: The Local Database (SQLite)
// This is our base currency. It handles the local ledger of all our data.

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

    @Query("SELECT * FROM quiz_history ORDER BY timestamp DESC")
    fun getQuizHistory(): Flow<List<QuizHistoryEntity>>
}
