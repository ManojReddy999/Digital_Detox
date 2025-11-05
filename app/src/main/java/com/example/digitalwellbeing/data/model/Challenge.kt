package com.example.digitalwellbeing.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents different types of challenges
 */
enum class ChallengeType {
    CHESS,
    SUDOKU,
    MATH
}

/**
 * Represents a completed challenge
 */
@Entity(tableName = "challenges")
data class Challenge(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: ChallengeType,
    val completedAt: Long = System.currentTimeMillis(),
    val timeTakenSeconds: Int = 0
)
