
package com.example.edu_ai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "timetables")
data class TimetableEntity(
    @PrimaryKey val userId: String,
    val weeklyPlanJson: String, // Store the serialized List<TimetableSlot>
    val aiBrief: String,
    val timestamp: Long = System.currentTimeMillis()
)


