package com.example.edu_ai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_operations")
data class SyncOperationEntity(
    @PrimaryKey val operationId: String,
    val userId: String,
    val entityType: String,
    val entityId: Long,
    val payload: String,
    val createdAt: Long = System.currentTimeMillis()
)