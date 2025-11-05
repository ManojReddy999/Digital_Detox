package com.example.digitalwellbeing.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents daily usage statistics
 */
@Entity(tableName = "daily_stats")
data class DailyStats(
    @PrimaryKey
    val date: Long,
    val totalUsageTimeMillis: Long,
    val challengesCompleted: Int = 0,
    val tasksCompleted: Int = 0,
    val timeSavedMillis: Long = 0
) {
    val totalUsageFormatted: String
        get() {
            val hours = totalUsageTimeMillis / (1000 * 60 * 60)
            val minutes = (totalUsageTimeMillis % (1000 * 60 * 60)) / (1000 * 60)
            return "${hours}h ${minutes}m"
        }

    val timeSavedFormatted: String
        get() {
            val minutes = timeSavedMillis / (1000 * 60)
            return "${minutes} min"
        }

    fun percentageChange(previousStats: DailyStats?): Int {
        if (previousStats == null || previousStats.totalUsageTimeMillis == 0L) return 0
        val change = ((previousStats.totalUsageTimeMillis - totalUsageTimeMillis).toFloat() /
                     previousStats.totalUsageTimeMillis) * 100
        return change.toInt()
    }
}
