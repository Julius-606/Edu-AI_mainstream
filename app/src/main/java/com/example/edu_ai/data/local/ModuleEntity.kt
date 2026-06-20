package com.example.edu_ai.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "modules",
    foreignKeys = [
        ForeignKey(
            entity = UnitEntity::class,
            parentColumns = ["localId"],
            childColumns = ["unitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["unitId"])]
)
data class ModuleEntity(
    @PrimaryKey(autoGenerate = true) val moduleId: Long = 0,
    val unitId: Long,
    val name: String
)
