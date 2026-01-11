package com.focus.digitalwellbeing.data.local

import androidx.room.*
import com.focus.digitalwellbeing.data.model.GoalType
import com.focus.digitalwellbeing.data.model.StreakTracker
import kotlinx.coroutines.flow.Flow

@Dao
interface StreakTrackerDao {
    
    @Query("SELECT * FROM streak_tracker WHERE goalType = :goalType")
    fun getStreak(goalType: GoalType): Flow<StreakTracker?>
    
    @Query("SELECT * FROM streak_tracker")
    fun getAllStreaks(): Flow<List<StreakTracker>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreak(streak: StreakTracker)
    
    @Update
    suspend fun updateStreak(streak: StreakTracker)
    
    @Query("UPDATE streak_tracker SET currentStreak = :newStreak, longestStreak = MAX(longestStreak, :newStreak), lastSuccessDate = :date WHERE goalType = :goalType")
    suspend fun updateStreakCount(goalType: GoalType, newStreak: Int, date: Long)
    
    @Query("UPDATE streak_tracker SET currentStreak = 0 WHERE goalType = :goalType")
    suspend fun resetStreak(goalType: GoalType)
}

