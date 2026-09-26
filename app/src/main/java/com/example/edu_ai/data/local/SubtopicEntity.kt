
package com.example.edu_ai.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "subtopics",
    foreignKeys = [
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["topicId"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["topicId"])]
)
data class SubtopicEntity(
    @PrimaryKey(autoGenerate = true) val subtopicId: Long = 0,
    val topicId: Long,
    val name: String,
    val isCompleted: Boolean = false,
    val learningObjectivesJson: String? = null,
    val cachedContentJson: String? = null,
    val cachedQuizJson: String? = null
)


 