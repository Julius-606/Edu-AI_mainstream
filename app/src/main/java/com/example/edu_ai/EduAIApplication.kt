package com.example.edu_ai

import android.app.Application
import androidx.room.Room
import com.example.edu_ai.data.local.EduAIDatabase
import com.example.edu_ai.data.local.MIGRATION_12_13
import com.example.edu_ai.data.local.MIGRATION_13_14
import com.example.edu_ai.data.remote.RetrofitClient
import com.example.edu_ai.repository.EduAIRepository
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class EduAIApplication : Application() {

    lateinit var database: EduAIDatabase
    lateinit var repository: EduAIRepository

    override fun onCreate() {
        super.onCreate()
        RetrofitClient.init(this)

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS sync_operations (operationId TEXT NOT NULL PRIMARY KEY, userId TEXT NOT NULL, entityType TEXT NOT NULL, entityId INTEGER NOT NULL, payload TEXT NOT NULL, createdAt INTEGER NOT NULL)")
            }
        }

        database = Room.databaseBuilder(
            applicationContext,
            EduAIDatabase::class.java,
            "edu_ai_db"
        )
        .addMigrations(MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()

        repository = EduAIRepository(
            api = RetrofitClient.instance,
            dao = database.dao()
        )
    }
}
