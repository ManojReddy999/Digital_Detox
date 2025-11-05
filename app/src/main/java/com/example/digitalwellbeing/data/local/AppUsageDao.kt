package com.example.digitalwellbeing.data.local

import androidx.room.*
import com.example.digitalwellbeing.data.model.AppUsageInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface AppUsageDao {
    @Query("SELECT * FROM app_usage WHERE date >= :startDate ORDER BY usageTimeMillis DESC")
    fun getAppUsageForDate(startDate: Long): Flow<List<AppUsageInfo>>

    @Query("SELECT * FROM app_usage WHERE date >= :startDate ORDER BY usageTimeMillis DESC LIMIT :limit")
    fun getTopUsedApps(startDate: Long, limit: Int): Flow<List<AppUsageInfo>>

    @Query("SELECT SUM(usageTimeMillis) FROM app_usage WHERE date >= :startDate")
    fun getTotalUsageTime(startDate: Long): Flow<Long?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppUsage(appUsage: AppUsageInfo)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllAppUsage(appUsageList: List<AppUsageInfo>)

    @Query("DELETE FROM app_usage WHERE date = :date")
    suspend fun deleteUsageForDate(date: Long)

    @Query("SELECT COUNT(*) FROM app_usage WHERE date = :date")
    suspend fun getUsageCountForDate(date: Long): Int

    @Query("DELETE FROM app_usage WHERE date < :beforeDate")
    suspend fun deleteOldUsageData(beforeDate: Long)

    @Query("DELETE FROM app_usage")
    suspend fun deleteAll()
}
