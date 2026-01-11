package com.focus.digitalwellbeing

import android.app.Application
import androidx.work.*
import com.focus.digitalwellbeing.data.local.AppDatabase
import com.focus.digitalwellbeing.worker.UsageSyncWorker
import java.util.concurrent.TimeUnit

class DigitalWellbeingApp : Application(), Configuration.Provider {

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)

        // Schedule periodic usage data sync
        scheduleUsageDataSync()
    }

    /**
     * Custom WorkManager configuration that doesn't use foreground services.
     * This prevents the need for FOREGROUND_SERVICE_DATA_SYNC permission.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    /**
     * Schedule periodic work to sync usage data from UsageStatsManager to Room database
     * This ensures historical data is preserved beyond system retention limits
     */
    private fun scheduleUsageDataSync() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)  // Only run when battery is not low
            .build()

        // Schedule daily sync at midnight
        val syncWork = PeriodicWorkRequestBuilder<UsageSyncWorker>(
            1, TimeUnit.DAYS  // Run once per day
        )
            .setConstraints(constraints)
            .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            UsageSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,  // Keep existing work if already scheduled
            syncWork
        )

        android.util.Log.d("DigitalWellbeingApp", "Scheduled periodic usage sync work")
    }

    /**
     * Calculate delay until next midnight for initial sync
     */
    private fun calculateInitialDelay(): Long {
        val calendar = java.util.Calendar.getInstance()
        val now = calendar.timeInMillis

        // Set to next midnight
        calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)

        return calendar.timeInMillis - now
    }
}
