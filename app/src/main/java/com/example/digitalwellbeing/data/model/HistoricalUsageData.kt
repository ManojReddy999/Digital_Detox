package com.example.digitalwellbeing.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents historical usage data for an app on a specific day
 * This data is synced periodically from UsageStatsManager to persist beyond system retention limits
 */
@Entity(
    tableName = "historical_usage",
    primaryKeys = ["packageName", "date"]
)
data class HistoricalUsageData(
    val packageName: String,
    val date: Long,  // Start of day timestamp (midnight)
    val appName: String,
    val totalUsageMillis: Long,
    val unlockCount: Int = 0,  // Number of times app was opened
    val lastUpdated: Long = System.currentTimeMillis()  // When this record was synced
)
