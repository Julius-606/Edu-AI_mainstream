package com.example.edu_ai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_releases")
data class AppReleaseEntity(
    @PrimaryKey val id: Long,
    val version: String,
    val versionCode: Int,
    val artifactType: String = "Trace Android APK",
    val downloadUrl: String,
    val releaseNotes: String,
    val isCurrent: Boolean = false,
    val isMandatory: Boolean = false,
    val minSupportedVersionCode: Int = 1,
    val fileSize: String = "14.8 MB",
    val timestamp: Long = System.currentTimeMillis()
)
