package com.example.digitalwellbeing.service

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.digitalwellbeing.DigitalWellbeingApp
import com.example.digitalwellbeing.MainActivity
import com.example.digitalwellbeing.R
import com.example.digitalwellbeing.data.repository.AppLimitRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/**
 * Foreground service that monitors app usage and blocks apps when limits are exceeded
 */
class AppMonitoringService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var appLimitRepository: AppLimitRepository
    private lateinit var usageStatsManager: UsageStatsManager
    private var monitoringJob: Job? = null
    private var lastCheckedPackage: String? = null

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "app_monitoring_channel"
        private const val CHECK_INTERVAL_MS = 1000L // Check every second

        fun start(context: Context) {
            val intent = Intent(context, AppMonitoringService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AppMonitoringService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("AppMonitoring", "Service created")

        val application = applicationContext as DigitalWellbeingApp
        appLimitRepository = AppLimitRepository(application.database.appLimitDao())
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        startMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d("AppMonitoring", "Service started")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors app usage for blocking"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("App Blocking Active")
            .setContentText("Monitoring app usage")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startMonitoring() {
        monitoringJob = serviceScope.launch {
            while (isActive) {
                try {
                    val foregroundApp = getForegroundApp()
                    if (foregroundApp != null && foregroundApp != packageName && foregroundApp != lastCheckedPackage) {
                        android.util.Log.d("AppMonitoring", "Foreground app: $foregroundApp")
                        lastCheckedPackage = foregroundApp
                        checkAndBlockApp(foregroundApp)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AppMonitoring", "Error in monitoring loop", e)
                }
                delay(CHECK_INTERVAL_MS)
            }
        }
    }

    private fun getForegroundApp(): String? {
        val time = System.currentTimeMillis()
        val usageEvents = usageStatsManager.queryEvents(time - 2000, time)
        val event = UsageEvents.Event()
        var foregroundApp: String? = null

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    foregroundApp = event.packageName
                }
            } else {
                @Suppress("DEPRECATION")
                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    foregroundApp = event.packageName
                }
            }
        }

        return foregroundApp
    }

    private fun checkAndBlockApp(packageName: String) {
        serviceScope.launch {
            try {
                val limits = appLimitRepository.getEnabledLimits().first()
                val limit = limits.find { it.packageName == packageName }

                android.util.Log.d("AppMonitoring", "Checking app: $packageName, has limit: ${limit != null}")

                if (limit != null) {
                    val usageMillis = getTodayUsageForApp(packageName)
                    val limitMinutes = limit.limitMillis / 1000 / 60
                    val usageMinutes = usageMillis / 1000 / 60

                    android.util.Log.d("AppMonitoring", "Usage: ${usageMinutes}m, Limit: ${limitMinutes}m, Should block: ${usageMillis >= limit.limitMillis}")

                    if (usageMillis >= limit.limitMillis) {
                        android.util.Log.d("AppMonitoring", "BLOCKING APP: ${limit.appName}")
                        blockApp(limit.appName, limit.limitMillis)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AppMonitoring", "Error checking app: $packageName", e)
            }
        }
    }

    private suspend fun getTodayUsageForApp(packageName: String): Long {
        return withContext(Dispatchers.IO) {
            try {
                val calendar = java.util.Calendar.getInstance()
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                calendar.set(java.util.Calendar.MINUTE, 0)
                calendar.set(java.util.Calendar.SECOND, 0)
                calendar.set(java.util.Calendar.MILLISECOND, 0)
                val startOfDay = calendar.timeInMillis
                val now = System.currentTimeMillis()

                val events = usageStatsManager.queryEvents(startOfDay, now)
                var totalUsage = 0L
                var lastResumeTime = 0L
                val event = UsageEvents.Event()

                while (events.hasNextEvent()) {
                    events.getNextEvent(event)

                    if (event.packageName == packageName) {
                        when (event.eventType) {
                            UsageEvents.Event.ACTIVITY_RESUMED -> {
                                lastResumeTime = event.timeStamp
                            }
                            UsageEvents.Event.ACTIVITY_PAUSED -> {
                                if (lastResumeTime > 0) {
                                    totalUsage += event.timeStamp - lastResumeTime
                                    lastResumeTime = 0
                                }
                            }
                        }
                    }
                }

                if (lastResumeTime > 0) {
                    totalUsage += now - lastResumeTime
                }

                android.util.Log.d("AppMonitoring", "Usage for $packageName: ${totalUsage / 1000 / 60}m")
                totalUsage
            } catch (e: Exception) {
                android.util.Log.e("AppMonitoring", "Error getting usage for $packageName", e)
                0L
            }
        }
    }

    private fun blockApp(appName: String, limitMillis: Long) {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)

        val hours = limitMillis / (1000 * 60 * 60)
        val minutes = (limitMillis % (1000 * 60 * 60)) / (1000 * 60)
        val limitFormatted = when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "1m"
        }

        val dialogIntent = Intent(this, BlockingDialogActivity::class.java).apply {
            putExtra(BlockingDialogActivity.EXTRA_APP_NAME, appName)
            putExtra(BlockingDialogActivity.EXTRA_LIMIT_FORMATTED, limitFormatted)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(dialogIntent)
    }

    override fun onDestroy() {
        android.util.Log.d("AppMonitoring", "Service destroyed")
        monitoringJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }
}
