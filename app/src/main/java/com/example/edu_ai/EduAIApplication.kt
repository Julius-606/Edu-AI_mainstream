
package com.example.edu_ai

import android.app.Application
import androidx.room.Room
import com.example.edu_ai.data.local.EduAIDatabase
import com.example.edu_ai.data.remote.RetrofitClient
import com.example.edu_ai.repository.EduAIRepository

class EduAIApplication : Application() {

    lateinit var database: EduAIDatabase
    lateinit var repository: EduAIRepository

    override fun onCreate() {
        super.onCreate()
        RetrofitClient.init(this)
        database = Room.databaseBuilder(
            applicationContext,
            EduAIDatabase::class.java,
            "edu_ai_db"
        )
        .fallbackToDestructiveMigration() // Updated to non-deprecated version
        .build()

        repository = EduAIRepository(
            api = RetrofitClient.instance,
            dao = database.dao()
        )
    }
}


 