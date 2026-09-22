
package com.example.edu_ai.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "topics",
    foreignKeys = [
        ForeignKey(
            entity = ModuleEntity::class,
            parentColumns = ["moduleId"],
            childColumns = ["moduleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["moduleId"])]
)
data class TopicEntity(
    @PrimaryKey(autoGenerate = true) val topicId: Long = 0,
    val moduleId: Long,
    val name: String
)


