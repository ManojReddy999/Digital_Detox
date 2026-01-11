package com.focus.digitalwellbeing.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks user streaks for different goal types
 */
@Entity(tableName = "streak_tracker")
data class StreakTracker(
    @PrimaryKey val goalType: GoalType,
    val currentStreak: Int,
    val longestStreak: Int,
    val lastSuccessDate: Long // Normalized to start of day
)

enum class GoalType {
    TIMER_COMPLIANCE,
    FOCUS_SESSIONS,
    SCREEN_TIME_TARGET
}

