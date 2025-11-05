package com.example.digitalwellbeing.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a user task
 */
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
