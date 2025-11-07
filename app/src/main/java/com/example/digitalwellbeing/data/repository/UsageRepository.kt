package com.example.digitalwellbeing.data.repository

import com.example.digitalwellbeing.data.local.AppUsageDao
import com.example.digitalwellbeing.data.local.DailyStatsDao
import com.example.digitalwellbeing.data.model.AppUsageInfo
import com.example.digitalwellbeing.data.model.DailyStats
import com.example.digitalwellbeing.util.DateUtils
import com.example.digitalwellbeing.util.UsageStatsHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class UsageRepository(
    private val appUsageDao: AppUsageDao,
    private val dailyStatsDao: DailyStatsDao,
    private val usageStatsHelper: UsageStatsHelper
) {

    fun hasUsagePermission(): Boolean = usageStatsHelper.hasUsageStatsPermission()

    fun openUsageAccessSettings() {
        usageStatsHelper.openUsageAccessSettings()
    }

    /**
     * Sync usage data from system UsageStatsManager to database
     */
    suspend fun syncUsageData() {
        if (!hasUsagePermission()) {
            android.util.Log.d("UsageRepository", "No usage permission, skipping sync")
            return
        }

        android.util.Log.d("UsageRepository", "Starting sync for today")
        val today = DateUtils.getStartOfToday()
        val existingStats = dailyStatsDao.getStatsForDate(today).firstOrNull()
        val existingUsageCount = appUsageDao.getUsageCountForDate(today)

        val usageStats = usageStatsHelper.getTodayUsageStats()
        android.util.Log.d("UsageRepository", "Got ${usageStats.size} apps with usage data")

        if (usageStats.isEmpty()) {
            val hasExistingUsage = existingUsageCount > 0 ||
                ((existingStats?.totalUsageTimeMillis ?: 0L) > 0L)

            if (hasExistingUsage) {
                android.util.Log.w(
                    "UsageRepository",
                    "Usage stats query returned empty but we have existing data. " +
                        "Skipping update to avoid wiping today's usage."
                )
                return
            }
        }

        appUsageDao.deleteUsageForDate(today)
        if (usageStats.isNotEmpty()) {
            appUsageDao.insertAllAppUsage(usageStats)
        }

        // Update daily stats
        val totalUsage = usageStats.sumOf { it.usageTimeMillis }
        android.util.Log.d("UsageRepository", "Total usage for today: ${totalUsage / 1000 / 60} minutes")

        if (existingStats != null) {
            dailyStatsDao.updateStats(
                existingStats.copy(totalUsageTimeMillis = totalUsage)
            )
        } else {
            dailyStatsDao.insertStats(
                DailyStats(
                    date = today,
                    totalUsageTimeMillis = totalUsage
                )
            )
        }
        android.util.Log.d("UsageRepository", "Sync complete")
    }

    /**
     * Sync usage data for a specific date
     */
    suspend fun syncUsageDataForDate(dateMillis: Long) {
        if (!hasUsagePermission()) return

        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        android.util.Log.d("UsageRepository", "Syncing data for date: ${sdf.format(dateMillis)}")

        val usageStats = usageStatsHelper.getUsageStatsForDate(dateMillis)

        android.util.Log.d("UsageRepository", "Found ${usageStats.size} apps with usage for ${sdf.format(dateMillis)}")

        appUsageDao.deleteUsageForDate(dateMillis)
        appUsageDao.insertAllAppUsage(usageStats)

        // Update daily stats
        val totalUsage = usageStats.sumOf { it.usageTimeMillis }

        android.util.Log.d("UsageRepository", "Total usage for ${sdf.format(dateMillis)}: ${totalUsage / 1000 / 60} minutes")

        val existingStats = dailyStatsDao.getStatsForDate(dateMillis).firstOrNull()
        if (existingStats != null) {
            dailyStatsDao.updateStats(
                existingStats.copy(totalUsageTimeMillis = totalUsage)
            )
        } else {
            dailyStatsDao.insertStats(
                DailyStats(
                    date = dateMillis,
                    totalUsageTimeMillis = totalUsage
                )
            )
        }
    }

    /**
     * Sync usage data for the past week
     */
    suspend fun syncPastWeekData() {
        if (!hasUsagePermission()) return

        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)

        // Sync last 7 days
        for (i in 0..6) {
            val date = calendar.timeInMillis
            syncUsageDataForDate(date)
            calendar.add(java.util.Calendar.DAY_OF_MONTH, -1)
        }
    }

    /**
     * Sync all available historical usage data
     * UsageStatsManager typically provides data for the last 7-30 days depending on device
     */
    suspend fun syncAllAvailableData() {
        if (!hasUsagePermission()) return

        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)

        // Sync last 30 days (maximum available from UsageStatsManager)
        for (i in 0..29) {
            val date = calendar.timeInMillis
            syncUsageDataForDate(date)
            calendar.add(java.util.Calendar.DAY_OF_MONTH, -1)
        }
    }

    /**
     * Get today's app usage
     */
    fun getTodayAppUsage(): Flow<List<AppUsageInfo>> {
        return appUsageDao.getAppUsageForDate(DateUtils.getStartOfToday())
    }

    /**
     * Get top used apps for today
     */
    fun getTopUsedApps(limit: Int = 3): Flow<List<AppUsageInfo>> {
        return appUsageDao.getTopUsedApps(DateUtils.getStartOfToday(), limit)
    }

    /**
     * Get today's stats
     */
    fun getTodayStats(): Flow<DailyStats?> {
        return dailyStatsDao.getStatsForDate(DateUtils.getStartOfToday())
    }

    /**
     * Get yesterday's stats
     */
    fun getYesterdayStats(): Flow<DailyStats?> {
        return dailyStatsDao.getStatsForDate(DateUtils.getStartOfYesterday())
    }

    /**
     * Get recent daily stats
     */
    fun getRecentStats(days: Int = 7): Flow<List<DailyStats>> {
        return dailyStatsDao.getRecentStats(days)
    }

    /**
     * Get weekly stats starting from a specific date
     * Returns 7 days of data starting from the given date
     * Reactively updates when any day's data changes in the database
     */
    fun getWeeklyStats(weekStartDate: Long): Flow<List<DailyStats>> {
        // Calculate all 7 dates for the week
        val dates = (0..6).map { dayOffset ->
            val calendar = java.util.Calendar.getInstance()
            calendar.timeInMillis = weekStartDate
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            calendar.add(java.util.Calendar.DAY_OF_MONTH, dayOffset)
            calendar.timeInMillis
        }

        // Create a list of flows for each day
        val dailyFlows = dates.map { date ->
            dailyStatsDao.getStatsForDate(date).map { stats ->
                stats ?: DailyStats(date = date, totalUsageTimeMillis = 0)
            }
        }

        // Combine all 7 Flows so they all reactively update when database changes
        return kotlinx.coroutines.flow.combine(dailyFlows) { statsArray ->
            statsArray.toList()
        }
    }

    /**
     * Get app usage for a specific date (exact match)
     */
    fun getAppUsageForDate(dateMillis: Long): Flow<List<AppUsageInfo>> {
        return appUsageDao.getAppUsageForSpecificDate(dateMillis)
    }

    /**
     * Clean old usage data (older than 30 days)
     */
    suspend fun cleanOldData() {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        appUsageDao.deleteOldUsageData(thirtyDaysAgo)
        dailyStatsDao.deleteOldStats(thirtyDaysAgo)
    }
}
