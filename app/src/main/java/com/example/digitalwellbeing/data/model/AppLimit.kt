package com.example.digitalwellbeing.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a time limit set for an app
 */
@Entity(tableName = "app_limits")
data class AppLimit(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val limitMillis: Long,
    val isEnabled: Boolean = true
)
