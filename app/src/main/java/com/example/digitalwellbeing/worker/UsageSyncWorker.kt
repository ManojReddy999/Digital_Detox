package com.example.digitalwellbeing.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.digitalwellbeing.DigitalWellbeingApp
import com.example.digitalwellbeing.data.model.HistoricalUsageData
import com.example.digitalwellbeing.util.UsageStatsHelper
import java.util.Calendar

/**
 * Worker that periodically syncs usage data from UsageStatsManager to Room database
 * This ensures historical data is preserved beyond system retention limits
 */
class UsageSyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "UsageSyncWorker"
        const val WORK_NAME = "usage_sync_periodic_work"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting usage sync...")

        return try {
            val app = context.applicationContext as DigitalWellbeingApp
            val database = app.database
            val historicalUsageDao = database.historicalUsageDao()
            val usageStatsHelper = UsageStatsHelper(context)

            // Check if we have usage permission
            if (!usageStatsHelper.hasUsageStatsPermission()) {
                Log.w(TAG, "Usage stats permission not granted, skipping sync")
                return Result.retry()
            }

            // Get the last synced date
            val lastSyncDate = historicalUsageDao.getLastSyncDate()

            // Calculate which days to sync
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            // Start from yesterday (we don't sync today as it's incomplete)
            calendar.add(Calendar.DAY_OF_MONTH, -1)
            val endDate = calendar.timeInMillis

            // Determine start date - either last sync or 7 days ago (whichever is more recent)
            val startDate = if (lastSyncDate != null && lastSyncDate > 0) {
                // Continue from day after last sync
                val calendar2 = Calendar.getInstance()
                calendar2.timeInMillis = lastSyncDate
                calendar2.add(Calendar.DAY_OF_MONTH, 1)
                calendar2.set(Calendar.HOUR_OF_DAY, 0)
                calendar2.set(Calendar.MINUTE, 0)
                calendar2.set(Calendar.SECOND, 0)
                calendar2.set(Calendar.MILLISECOND, 0)
                calendar2.timeInMillis
            } else {
                // First sync - go back 7 days
                calendar.add(Calendar.DAY_OF_MONTH, -6) // Already went back 1, so -6 more
                calendar.timeInMillis
            }

            Log.d(TAG, "Syncing data from ${formatDate(startDate)} to ${formatDate(endDate)}")

            // If start date is after end date, nothing to sync
            if (startDate > endDate) {
                Log.d(TAG, "Already up to date, nothing to sync")
                return Result.success()
            }

            // Sync each day
            var currentDate = startDate
            val historicalDataList = mutableListOf<HistoricalUsageData>()
            var daysSynced = 0

            while (currentDate <= endDate) {
                Log.d(TAG, "Fetching usage for ${formatDate(currentDate)}")

                val usageForDay = usageStatsHelper.getUsageStatsForDate(currentDate)

                Log.d(TAG, "Found ${usageForDay.size} apps with usage on ${formatDate(currentDate)}")

                // Convert to HistoricalUsageData
                usageForDay.forEach { appUsage ->
                    val historicalData = HistoricalUsageData(
                        packageName = appUsage.packageName,
                        date = currentDate,
                        appName = appUsage.appName,
                        totalUsageMillis = appUsage.usageTimeMillis,
                        unlockCount = 0, // We could track this separately if needed
                        lastUpdated = System.currentTimeMillis()
                    )
                    historicalDataList.add(historicalData)
                }

                daysSynced++

                // Move to next day
                val cal = Calendar.getInstance()
                cal.timeInMillis = currentDate
                cal.add(Calendar.DAY_OF_MONTH, 1)
                currentDate = cal.timeInMillis
            }

            // Batch insert all data
            if (historicalDataList.isNotEmpty()) {
                historicalUsageDao.insertUsageDataBatch(historicalDataList)
                Log.d(TAG, "Successfully synced ${historicalDataList.size} usage records across $daysSynced days")
            } else {
                Log.d(TAG, "No usage data found for the date range")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing usage data", e)
            e.printStackTrace()
            // Retry on failure
            Result.retry()
        }
    }

    private fun formatDate(millis: Long): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(millis)
    }
}
