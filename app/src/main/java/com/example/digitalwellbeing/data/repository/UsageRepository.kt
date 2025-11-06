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

        val usageStats = usageStatsHelper.getUsageStatsForDate(dateMillis)

        appUsageDao.deleteUsageForDate(dateMillis)
        appUsageDao.insertAllAppUsage(usageStats)

        // Update daily stats
        val totalUsage = usageStats.sumOf { it.usageTimeMillis }

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
     * Sync usage data for the current calendar week (Monday-Sunday)
     * This matches what getWeeklyStats() expects to display
     */
    suspend fun syncPastWeekData() {
        if (!hasUsagePermission()) return

        val calendar = java.util.Calendar.getInstance()
        // Set to Monday of this week (matches MainViewModel calculation)
        calendar.firstDayOfWeek = java.util.Calendar.MONDAY
        calendar.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)

        android.util.Log.d("UsageRepository", "Syncing current week starting from Monday: ${java.text.SimpleDateFormat("yyyy-MM-dd").format(calendar.timeInMillis)}")

        // Sync all 7 days of this week (Mon through Sun)
        for (i in 0..6) {
            val date = calendar.timeInMillis
            android.util.Log.d("UsageRepository", "Syncing day $i: ${java.text.SimpleDateFormat("yyyy-MM-dd EEEE").format(date)}")
            syncUsageDataForDate(date)
            calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
        }

        android.util.Log.d("UsageRepository", "Finished syncing current week")
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
     * This is a reactive Flow that updates whenever any day's data changes
     */
    fun getWeeklyStats(weekStartDate: Long): Flow<List<DailyStats>> {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = weekStartDate

        // Get all 7 days of the week
        val dates = (0..6).map {
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            val date = calendar.timeInMillis
            calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
            date
        }

        // Get flows for all 7 days
        val flowsList = dates.map { date ->
            dailyStatsDao.getStatsForDate(date)
        }

        // Combine all 7 flows so updates to any day trigger re-emission
        // This ensures stats update when sync completes
        return kotlinx.coroutines.flow.combine(
            flowsList[0],
            flowsList[1],
            flowsList[2],
            flowsList[3],
            flowsList[4],
            flowsList[5],
            flowsList[6]
        ) { day0, day1, day2, day3, day4, day5, day6 ->
            listOf(
                day0 ?: DailyStats(date = dates[0], totalUsageTimeMillis = 0),
                day1 ?: DailyStats(date = dates[1], totalUsageTimeMillis = 0),
                day2 ?: DailyStats(date = dates[2], totalUsageTimeMillis = 0),
                day3 ?: DailyStats(date = dates[3], totalUsageTimeMillis = 0),
                day4 ?: DailyStats(date = dates[4], totalUsageTimeMillis = 0),
                day5 ?: DailyStats(date = dates[5], totalUsageTimeMillis = 0),
                day6 ?: DailyStats(date = dates[6], totalUsageTimeMillis = 0)
            )
        }
    }

    /**
     * Get app usage for a specific date
     */
    fun getAppUsageForDate(dateMillis: Long): Flow<List<AppUsageInfo>> {
        return appUsageDao.getAppUsageForDate(dateMillis)
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
