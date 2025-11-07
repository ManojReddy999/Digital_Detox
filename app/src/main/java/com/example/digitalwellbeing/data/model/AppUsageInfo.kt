package com.example.digitalwellbeing.data.model

import androidx.room.Entity

/**
 * Represents usage information for a single app on a specific date
 * Uses composite primary key of packageName + date to allow multiple records per app
 */
@Entity(tableName = "app_usage", primaryKeys = ["packageName", "date"])
data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val usageTimeMillis: Long,
    val lastTimeUsed: Long,
    val iconResId: Int = 0,
    val date: Long = System.currentTimeMillis()
) {
    val usageTimeFormatted: String
        get() {
            val hours = usageTimeMillis / (1000 * 60 * 60)
            val minutes = (usageTimeMillis % (1000 * 60 * 60)) / (1000 * 60)
            return when {
                hours > 0 -> "${hours}h ${minutes}m"
                minutes > 0 -> "${minutes}m"
                usageTimeMillis > 0 -> "<1m"  // Only show <1m if there's actual usage
                else -> "0m"  // Show 0m for apps with no usage
            }
        }

    val usagePercentage: Float
        get() = (usageTimeMillis / (1000f * 60 * 60 * 24)).coerceAtMost(1f)
}
