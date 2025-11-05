package com.example.digitalwellbeing.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.digitalwellbeing.DigitalWellbeingApp
import com.example.digitalwellbeing.data.repository.AppLimitRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/**
 * Accessibility service that blocks apps when their timer limit is exceeded
 * Checks when apps are switched and periodically while using an app with limits
 */
class AppBlockingAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var appLimitRepository: AppLimitRepository
    private var lastCheckedPackage: String? = null

    // Periodic monitoring for active app usage
    private var periodicCheckJob: Job? = null
    private var currentLimitedApp: String? = null
    private val checkIntervalMillis = 90_000L // Check every 90 seconds (1.5 minutes)

    override fun onServiceConnected() {
        super.onServiceConnected()
        android.util.Log.d("AppBlocking", "Accessibility service connected!")
        try {
            val application = applicationContext as DigitalWellbeingApp
            appLimitRepository = AppLimitRepository(application.database.appLimitDao())
            android.util.Log.d("AppBlocking", "Repository initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e("AppBlocking", "Error initializing service", e)
            e.printStackTrace()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                event.packageName?.toString()?.let { packageName ->
                    android.util.Log.d("AppBlocking", "Window state changed: $packageName")

                    // Skip system UI and our own app
                    if (packageName == this.packageName ||
                        packageName == "com.android.systemui" ||
                        packageName == "android") {
                        // Stop periodic checks when switching to system UI
                        stopPeriodicChecks()
                        return
                    }

                    // Only check if it's a different app than last time
                    if (packageName != lastCheckedPackage) {
                        lastCheckedPackage = packageName
                        checkAndBlockApp(packageName)

                        // Start periodic checks for this app if it has limits
                        startPeriodicChecksIfNeeded(packageName)
                    }
                }
            }
        }
    }

    private fun checkAndBlockApp(packageName: String) {
        serviceScope.launch {
            try {
                // Get all enabled limits
                val limits = appLimitRepository.getEnabledLimits().first()
                val limit = limits.find { it.packageName == packageName }

                android.util.Log.d("AppBlocking", "Checking app: $packageName, has limit: ${limit != null}")

                if (limit != null) {
                    // Get today's usage for this app
                    val usageMillis = getTodayUsageForApp(packageName)

                    val limitMinutes = limit.limitMillis / 1000 / 60
                    val usageMinutes = usageMillis / 1000 / 60

                    android.util.Log.d("AppBlocking", "Usage: ${usageMinutes}m, Limit: ${limitMinutes}m")

                    // If limit exceeded, block the app
                    if (usageMillis >= limit.limitMillis) {
                        android.util.Log.d("AppBlocking", "BLOCKING APP: ${limit.appName}")
                        blockApp(limit.appName, limit.limitMillis)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AppBlocking", "Error checking app: $packageName", e)
                e.printStackTrace()
            }
        }
    }

    private suspend fun getTodayUsageForApp(packageName: String): Long {
        return withContext(Dispatchers.IO) {
            try {
                val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager

                // Get start of today
                val calendar = java.util.Calendar.getInstance()
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                calendar.set(java.util.Calendar.MINUTE, 0)
                calendar.set(java.util.Calendar.SECOND, 0)
                calendar.set(java.util.Calendar.MILLISECOND, 0)
                val startOfDay = calendar.timeInMillis
                val now = System.currentTimeMillis()

                // Query usage events to calculate actual foreground time
                val events = usageStatsManager.queryEvents(startOfDay, now)
                var totalUsage = 0L
                var lastResumeTime = 0L
                val event = android.app.usage.UsageEvents.Event()

                while (events.hasNextEvent()) {
                    events.getNextEvent(event)

                    if (event.packageName == packageName) {
                        when (event.eventType) {
                            android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED -> {
                                lastResumeTime = event.timeStamp
                            }
                            android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED -> {
                                if (lastResumeTime > 0) {
                                    totalUsage += event.timeStamp - lastResumeTime
                                    lastResumeTime = 0
                                }
                            }
                        }
                    }
                }

                // If still in foreground, add current session
                if (lastResumeTime > 0) {
                    totalUsage += now - lastResumeTime
                }

                android.util.Log.d("AppBlocking", "Usage for $packageName: ${totalUsage / 1000 / 60}m")
                totalUsage
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("AppBlocking", "Error getting usage for $packageName", e)
                0L
            }
        }
    }

    private fun blockApp(appName: String, limitMillis: Long) {
        val hours = limitMillis / (1000 * 60 * 60)
        val minutes = (limitMillis % (1000 * 60 * 60)) / (1000 * 60)
        val limitFormatted = when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "1m"
        }

        android.util.Log.d("AppBlocking", "Blocking app: $appName")

        // Show blocking dialog activity (it will handle going to home screen)
        val dialogIntent = Intent(this, BlockingDialogActivity::class.java).apply {
            putExtra(BlockingDialogActivity.EXTRA_APP_NAME, appName)
            putExtra(BlockingDialogActivity.EXTRA_LIMIT_FORMATTED, limitFormatted)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(dialogIntent)
    }

    /**
     * Start periodic checks if the app has limits set
     */
    private fun startPeriodicChecksIfNeeded(packageName: String) {
        serviceScope.launch {
            try {
                // Check if this app has a limit
                val limits = appLimitRepository.getEnabledLimits().first()
                val hasLimit = limits.any { it.packageName == packageName }

                if (hasLimit) {
                    // Stop any existing periodic check
                    stopPeriodicChecks()

                    // Start new periodic check for this app
                    currentLimitedApp = packageName
                    android.util.Log.d("AppBlocking", "Starting periodic checks for $packageName")

                    periodicCheckJob = serviceScope.launch {
                        while (isActive && currentLimitedApp == packageName) {
                            delay(checkIntervalMillis)
                            android.util.Log.d("AppBlocking", "Periodic check for $packageName")
                            checkAndBlockApp(packageName)
                        }
                    }
                } else {
                    // No limit, stop any periodic checks
                    stopPeriodicChecks()
                }
            } catch (e: Exception) {
                android.util.Log.e("AppBlocking", "Error starting periodic checks", e)
            }
        }
    }

    /**
     * Stop periodic checks for the current app
     */
    private fun stopPeriodicChecks() {
        periodicCheckJob?.cancel()
        periodicCheckJob = null
        if (currentLimitedApp != null) {
            android.util.Log.d("AppBlocking", "Stopping periodic checks for $currentLimitedApp")
            currentLimitedApp = null
        }
    }

    override fun onInterrupt() {
        // Required override
    }

    override fun onDestroy() {
        stopPeriodicChecks()
        serviceScope.cancel()
        super.onDestroy()
    }
}
