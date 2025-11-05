package com.example.digitalwellbeing.data.local

import androidx.room.*
import com.example.digitalwellbeing.data.model.DailyStats
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyStatsDao {
    @Query("SELECT * FROM daily_stats WHERE date = :date LIMIT 1")
    fun getStatsForDate(date: Long): Flow<DailyStats?>

    @Query("SELECT * FROM daily_stats ORDER BY date DESC LIMIT :limit")
    fun getRecentStats(limit: Int): Flow<List<DailyStats>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStats(stats: DailyStats)

    @Update
    suspend fun updateStats(stats: DailyStats)

    @Query("DELETE FROM daily_stats WHERE date < :beforeDate")
    suspend fun deleteOldStats(beforeDate: Long)
}
