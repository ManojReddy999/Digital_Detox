package com.example.digitalwellbeing.data.local

import androidx.room.*
import com.example.digitalwellbeing.data.model.HistoricalUsageData
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoricalUsageDao {

    /**
     * Insert or update historical usage data for a specific app on a specific day
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsageData(data: HistoricalUsageData)

    /**
     * Insert multiple usage records at once
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsageDataBatch(data: List<HistoricalUsageData>)

    /**
     * Get all historical usage data for a specific date range
     * @param startDate Start of day timestamp
     * @param endDate End of day timestamp
     */
    @Query("SELECT * FROM historical_usage WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC, totalUsageMillis DESC")
    fun getUsageForDateRange(startDate: Long, endDate: Long): Flow<List<HistoricalUsageData>>

    /**
     * Get historical usage for a specific app across all time
     */
    @Query("SELECT * FROM historical_usage WHERE packageName = :packageName ORDER BY date DESC")
    fun getUsageForApp(packageName: String): Flow<List<HistoricalUsageData>>

    /**
     * Get usage data for a specific app on a specific day
     */
    @Query("SELECT * FROM historical_usage WHERE packageName = :packageName AND date = :date LIMIT 1")
    suspend fun getUsageForAppOnDate(packageName: String, date: Long): HistoricalUsageData?

    /**
     * Get total usage across all apps for a specific date range
     */
    @Query("SELECT SUM(totalUsageMillis) FROM historical_usage WHERE date >= :startDate AND date <= :endDate")
    suspend fun getTotalUsageForDateRange(startDate: Long, endDate: Long): Long?

    /**
     * Get the most recent sync date
     */
    @Query("SELECT MAX(date) FROM historical_usage")
    suspend fun getLastSyncDate(): Long?

    /**
     * Delete old data (older than specified date)
     * Can be used for cleanup if needed
     */
    @Query("DELETE FROM historical_usage WHERE date < :beforeDate")
    suspend fun deleteOldData(beforeDate: Long)

    /**
     * Delete all historical data
     */
    @Query("DELETE FROM historical_usage")
    suspend fun deleteAll()
}
