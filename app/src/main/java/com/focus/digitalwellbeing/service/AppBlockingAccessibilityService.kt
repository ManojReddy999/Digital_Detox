package com.focus.digitalwellbeing.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.focus.digitalwellbeing.DigitalWellbeingApp
import com.focus.digitalwellbeing.data.repository.AppLimitRepository
import com.focus.digitalwellbeing.data.repository.FocusRepository
import com.focus.digitalwellbeing.data.model.FocusType
import com.focus.digitalwellbeing.util.OverlayPermissionUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import android.app.usage.UsageStatsManager
import android.content.Context
import com.focus.digitalwellbeing.service.OverlayManager
import android.accessibilityservice.AccessibilityServiceInfo

/**
 * Accessibility service that blocks apps when their timer limit is exceeded
 * Checks when apps are switched and periodically while using an app with limits
 */
class AppBlockingAccessibilityService : AccessibilityService() {

    companion object {
        // Grace period timestamp set when BlockingDialogActivity dismisses
        // This prevents re-triggering the overlay during the "Close App" transition
        @Volatile
        var dismissalGraceTimestamp: Long = 0L
            private set
        
        private const val DISMISSAL_GRACE_PERIOD_MS = 800L // Reduced to 0.8s to allow quick re-entry from Recents
        
        /**
         * Call this before navigating away from the blocking dialog
         * to prevent the overlay from re-triggering during the transition
         */
        fun setDismissalGracePeriod() {
            dismissalGraceTimestamp = System.currentTimeMillis()
            android.util.Log.d("AppBlocking", "Dismissal grace period set at $dismissalGraceTimestamp")
        }
        
        fun isInDismissalGracePeriod(): Boolean {
            val inGrace = System.currentTimeMillis() - dismissalGraceTimestamp < DISMISSAL_GRACE_PERIOD_MS
            if (inGrace) {
                android.util.Log.d("AppBlocking", "In dismissal grace period")
            }
            return inGrace
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var appLimitRepository: AppLimitRepository
    private lateinit var focusRepository: FocusRepository
    private var usageStatsManager: UsageStatsManager? = null
    private var lastCheckedPackage: String? = null
    
    // Cached active session
    private var currentActiveSession: com.focus.digitalwellbeing.data.model.FocusSession? = null

    // Periodic monitoring for active app usage
    private var periodicCheckJob: Job? = null
    private var currentLimitedApp: String? = null
    private val checkIntervalMillis = 2_000L // Check every 2 seconds for real-time blocking
    
    // Track when we're showing a blocking dialog to prevent duplicates
    private var isShowingBlockDialog = false
    
    // Track last blocked time to prevent double-blocking loop
    private val lastBlockedTimeMap = mutableMapOf<String, Long>()
    
    // Track when we leave our own app (dialog) to give a grace period
    private var lastInternalAppTimestamp: Long = 0

    // Job for monitoring timed sessions
    private var sessionTimerJob: Job? = null

    private lateinit var overlayManager: OverlayManager

    override fun onCreate() {
        super.onCreate()

        val application = applicationContext as DigitalWellbeingApp
        appLimitRepository = AppLimitRepository(application.database.appLimitDao())
        val scheduler = com.focus.digitalwellbeing.service.FocusScheduler(application)
        focusRepository = FocusRepository(application.database.focusDao(), scheduler, application)
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        overlayManager = OverlayManager(this)

        android.util.Log.d("AppBlocking", "Service created")
        
        // Observe active session
        serviceScope.launch {
            focusRepository.activeSession.collect { session ->
                currentActiveSession = session
                android.util.Log.d("AppBlocking", "Active session updated: ${session?.isActive}")
                
                // Monitor timed sessions
                sessionTimerJob?.cancel()
                if (session != null && session.isActive && session.endTime != null) {
                    sessionTimerJob = serviceScope.launch {
                        val remaining = session.endTime - System.currentTimeMillis()
                        if (remaining > 0) {
                            delay(remaining)
                            // Session duration expired, stop it
                            android.util.Log.d("AppBlocking", "Session timer expired, stopping session")
                            focusRepository.stopSession()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        overlayManager.dismiss()
        stopPeriodicChecks()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        android.util.Log.d("AppBlocking", "Accessibility service connected!")
        
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 100
        }
        this.serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                event.packageName?.toString()?.let { packageName ->
                    android.util.Log.d("AppBlocking", "Window state changed: $packageName")

                    // Skip system UI and our own app
                    val isSystemOrInternal = packageName == this.packageName ||
                        packageName == "com.android.systemui" ||
                        packageName == "android" ||
                        packageName.endsWith(".BlockingDialogActivity")

                    if (isSystemOrInternal) {
                        
                        // Record that we are in our app/dialog ONLY if it tests true for our app
                        // We DO NOT want System UI (Recents/Home) to trigger the grace period
                        if (packageName == this.packageName || packageName.endsWith(".BlockingDialogActivity")) {
                            lastInternalAppTimestamp = System.currentTimeMillis()
                        }
                        
                        // Stop periodic checks when switching to system UI or blocking dialog
                        stopPeriodicChecks()
                        return
                    }

                    // Grace Period: If we JUST left our app (within 0.8 seconds), skip check
                    // This handles the "Close App" -> "Blocked App Flash" -> "Home" transition
                    if (System.currentTimeMillis() - lastInternalAppTimestamp < 800) {
                        android.util.Log.d("AppBlocking", "In transition grace period ($packageName). Skipping.")
                        return
                    }
                    
                    // Additional check: If BlockingDialogActivity just dismissed
                    // This ensures we don't re-trigger during the "Close App" navigation
                    if (isInDismissalGracePeriod()) {
                        android.util.Log.d("AppBlocking", "In dismissal grace period from BlockingDialogActivity. Skipping.")
                        return
                    }

                    // Always check on window state change for real-time blocking
                    // This ensures immediate check when switching TO the app or when the app comes to foreground
                    checkAndBlockApp(packageName)

                    // Update periodic checks if it's a different app OR if periodic check is not running
                    if (packageName != lastCheckedPackage || currentLimitedApp != packageName) {
                        lastCheckedPackage = packageName
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

                android.util.Log.d("AppBlocking", "===== CHECK START =====")
                android.util.Log.d("AppBlocking", "Checking package: $packageName")
                android.util.Log.d("AppBlocking", "Periodic check running for: $currentLimitedApp")

                // Check Focus Session FIRST
                val activeSession = currentActiveSession
                if (activeSession != null && activeSession.isActive) {
                    // Only apply focus session blocking to user-facing apps
                    // Check if this package has a launcher intent (is a user-facing app)
                    val intent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        setPackage(packageName)
                    }
                    val resolveInfo = packageManager.queryIntentActivities(intent, 0)
                    val isUserFacingApp = resolveInfo.isNotEmpty()
                    
                    if (!isUserFacingApp) {
                        android.util.Log.d("AppBlocking", "Skipping non-user-facing app: $packageName")
                        // Don't apply focus session blocking to system/non-user-facing apps
                    } else {
                        // Exclude essential system apps even if they have launcher intents
                        val essentialApps = listOf(
                            "com.android.phone",
                            "com.android.dialer",
                            "com.google.android.dialer",
                            "com.android.contacts",
                            "com.google.android.contacts",
                            "com.android.mms",
                            "com.google.android.apps.messaging",
                            "com.android.settings"
                        )
                        
                        if (essentialApps.contains(packageName)) {
                            android.util.Log.d("AppBlocking", "Skipping essential app: $packageName")
                            return@launch
                        }
                        
                        val focusGroupId = activeSession.focusGroupId
                        if (focusGroupId != null) {
                            val focusGroup = focusRepository.getFocusGroupById(focusGroupId)
                            if (focusGroup != null) {
                                val isPackageInGroup = focusGroup.appPackages.contains(packageName)
                                var shouldBlock = false
                                
                                if (focusGroup.type == FocusType.ALLOWLIST) {
                                    // Block if NOT in allowlist
                                    if (!isPackageInGroup) {
                                        shouldBlock = true
                                    }
                                } else {
                                    // Block if IN blocklist
                                    if (isPackageInGroup) {
                                        shouldBlock = true
                                    }
                                }
                                
                                if (shouldBlock) {
                                    // Don't show another dialog if one is already showing
                                    if (isShowingBlockDialog) {
                                        android.util.Log.d("AppBlocking", "Already showing block dialog, skipping")
                                        return@launch
                                    }
                                    
                                    android.util.Log.d("AppBlocking", "🚫 BLOCKING APP (Focus Mode): $packageName")
                                    isShowingBlockDialog = true
                                    blockApp(packageName, "Focus Mode", 0, isFocusMode = true)
                                    // Reset flag after a delay
                                    serviceScope.launch {
                                        delay(2000) // 2 seconds
                                        isShowingBlockDialog = false
                                    }
                                    return@launch
                                }
                            }
                        }
                    }
                }

                // Get all enabled limits
                val limits = appLimitRepository.getEnabledLimits().first()
                val limit = limits.find { it.packageName == packageName }

                android.util.Log.d("AppBlocking", "Has limit: ${limit != null}")

                if (limit != null) {
                    // Get today's usage for this app
                    val usageMillis = getTodayUsageForApp(packageName)

                    val limitMinutes = limit.limitMillis / 1000 / 60
                    val usageMinutes = usageMillis / 1000 / 60

                    android.util.Log.d("AppBlocking", "===== LIMIT CHECK =====")
                    android.util.Log.d("AppBlocking", "App: ${limit.appName} ($packageName)")
                    android.util.Log.d("AppBlocking", "Usage: ${usageMinutes}m (${usageMillis}ms)")
                    android.util.Log.d("AppBlocking", "Limit: ${limitMinutes}m (${limit.limitMillis}ms)")
                    android.util.Log.d("AppBlocking", "Exceeded: ${usageMillis >= limit.limitMillis}")

                    // If limit reached or exceeded, block the app
                    if (usageMillis >= limit.limitMillis) {
                        if (isShowingBlockDialog) return@launch
                        
                        android.util.Log.d("AppBlocking", "🚫 BLOCKING APP: ${limit.appName}")
                        isShowingBlockDialog = true
                        blockApp(limit.packageName, limit.appName, limit.limitMillis, isFocusMode = false)

                        // Reset flag after a delay
                        serviceScope.launch {
                            delay(2000)
                            isShowingBlockDialog = false
                        }
                    } else {
                        android.util.Log.d("AppBlocking", "✓ Within limit, ${limitMinutes - usageMinutes}m remaining")
                    }
                } else {
                    android.util.Log.d("AppBlocking", "No limit set for $packageName")
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

                val resumeEventType = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED
                } else {
                    @Suppress("DEPRECATION")
                    android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND
                }

                val pauseEventType = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED
                } else {
                    @Suppress("DEPRECATION")
                    android.app.usage.UsageEvents.Event.MOVE_TO_BACKGROUND
                }

                while (events.hasNextEvent()) {
                    events.getNextEvent(event)

                    if (event.packageName == packageName) {
                        when (event.eventType) {
                            resumeEventType -> {
                                lastResumeTime = event.timeStamp
                                android.util.Log.d("AppBlocking", "Resume event for $packageName at ${event.timeStamp}")
                            }
                            pauseEventType -> {
                                if (lastResumeTime > 0) {
                                    val sessionDuration = event.timeStamp - lastResumeTime
                                    totalUsage += sessionDuration
                                    android.util.Log.d("AppBlocking", "Pause event for $packageName, session: ${sessionDuration / 1000}s")
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

    private fun blockApp(packageName: String, appName: String, limitMillis: Long, isFocusMode: Boolean) {
        try {
            val hours = limitMillis / (1000 * 60 * 60)
            val minutes = (limitMillis % (1000 * 60 * 60)) / (1000 * 60)
            val limitFormatted = when {
                hours > 0 -> "${hours}h ${minutes}m"
                minutes > 0 -> "${minutes}m"
                else -> "1m"
            }

            android.util.Log.d("AppBlocking", "Blocking app: $appName (Focus: $isFocusMode)")

            // Check if we have overlay permission for true overlay blocking
            if (OverlayPermissionUtils.canDrawOverlays(this)) {
                android.util.Log.d("AppBlocking", "Using overlay manager (permission granted)")
                // Show blocking overlay using OverlayManager
                // Note: OverlayManager needs update to support isFocusMode, or we just use the dialog activity for now which is safer
                
                // For now, let's use the Activity approach for consistency as it handles the UI better
                // We want to show the dialog ON TOP of the app, so we don't go home first
                
                val blockingIntent = Intent(this, BlockingDialogActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra(BlockingDialogActivity.EXTRA_PACKAGE_NAME, packageName)
                    putExtra(BlockingDialogActivity.EXTRA_APP_NAME, appName)
                    putExtra(BlockingDialogActivity.EXTRA_LIMIT_FORMATTED, limitFormatted)
                    putExtra(BlockingDialogActivity.EXTRA_IS_FOCUS_MODE, isFocusMode)
                }
                startActivity(blockingIntent)
                
            } else {
                // Fallback to dialog if no overlay permission
                // Go to home first to background the app
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)

                val blockingIntent = Intent(this, BlockingDialogActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra(BlockingDialogActivity.EXTRA_PACKAGE_NAME, packageName)
                    putExtra(BlockingDialogActivity.EXTRA_APP_NAME, appName)
                    putExtra(BlockingDialogActivity.EXTRA_LIMIT_FORMATTED, limitFormatted)
                    putExtra(BlockingDialogActivity.EXTRA_IS_FOCUS_MODE, isFocusMode)
                }
                startActivity(blockingIntent)
            }
        } catch (e: Exception) {
            android.util.Log.e("AppBlocking", "Error blocking app", e)
        }
    }

    /**
     * Start periodic checks if the app has limits set
     */
    private fun startPeriodicChecksIfNeeded(packageName: String) {
        serviceScope.launch {
            try {
                // Check if this app has a limit (time-based)
                // Note: Focus session blocking doesn't need periodic checks
                // because it's not tracking usage over time - just a simple list check
                val limits = appLimitRepository.getEnabledLimits().first()
                val hasLimit = limits.any { it.packageName == packageName }

                if (hasLimit) {
                    // Stop any existing periodic check
                    stopPeriodicChecks()

                    // Start new periodic check for this app
                    currentLimitedApp = packageName
                    android.util.Log.d("AppBlocking", "Starting periodic checks for $packageName (App Limit)")

                    periodicCheckJob = serviceScope.launch {
                        while (isActive && currentLimitedApp == packageName) {
                            android.util.Log.d("AppBlocking", "Periodic check for $packageName")
                            checkAndBlockApp(packageName)
                            delay(checkIntervalMillis)
                        }
                    }
                } else {
                    // No time-based limit, stop any periodic checks
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

}

