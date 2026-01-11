package com.focus.digitalwellbeing.data.repository

import com.focus.digitalwellbeing.data.local.StreakTrackerDao
import com.focus.digitalwellbeing.data.model.GoalType
import com.focus.digitalwellbeing.data.model.StreakTracker
import com.focus.digitalwellbeing.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class StreakRepository(
    private val streakDao: StreakTrackerDao
) {
    
    /**
     * Get streak for a specific goal type
     */
    fun getStreak(goalType: GoalType): Flow<StreakTracker?> {
        return streakDao.getStreak(goalType)
    }
    
    /**
     * Get all streaks
     */
    fun getAllStreaks(): Flow<List<StreakTracker>> {
        return streakDao.getAllStreaks()
    }
    
    /**
     * Update streak when user completes a goal
     * Returns the new streak count
     */
    suspend fun recordSuccess(goalType: GoalType): Int {
        val today = DateUtils.normalizeToStartOfDay(System.currentTimeMillis())
        val existing = streakDao.getStreak(goalType).first()
        
        if (existing == null) {
            // First time - create new streak
            val newStreak = StreakTracker(
                goalType = goalType,
                currentStreak = 1,
                longestStreak = 1,
                lastSuccessDate = today
            )
            streakDao.insertStreak(newStreak)
            return 1
        }
        
        val yesterday = today - 24 * 60 * 60 * 1000
        
        when {
            existing.lastSuccessDate == today -> {
                // Already succeeded today - no change
                return existing.currentStreak
            }
            existing.lastSuccessDate == yesterday -> {
                // Continuing streak
                val newCount = existing.currentStreak + 1
                streakDao.updateStreakCount(
                    goalType = goalType,
                    newStreak = newCount,
                    date = today
                )
                return newCount
            }
            else -> {
                // Streak broken - restart at 1
                streakDao.updateStreakCount(
                    goalType = goalType,
                    newStreak = 1,
                    date = today
                )
                return 1
            }
        }
    }
    
    /**
     * Reset a streak
     */
    suspend fun resetStreak(goalType: GoalType) {
        streakDao.resetStreak(goalType)
    }
    
    /**
     * Check if streak needs updating (for missed days)
     */
    suspend fun checkAndUpdateStreak(goalType: GoalType): Boolean {
        val today = DateUtils.normalizeToStartOfDay(System.currentTimeMillis())
        val yesterday = today - 24 * 60 * 60 * 1000
        val existing = streakDao.getStreak(goalType).first() ?: return false
        
        // If last success was before yesterday, streak is broken
        if (existing.lastSuccessDate < yesterday && existing.currentStreak > 0) {
            streakDao.resetStreak(goalType)
            return true // Streak was broken
        }
        
        return false // Streak is still active
    }
}

