package com.example.digitalwellbeing.util

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.provider.Settings
import com.example.digitalwellbeing.data.model.AppUsageInfo
import java.util.Calendar

class UsageStatsHelper(private val context: Context) {

    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val packageManager = context.packageManager


    /**
     * Check if usage access permission is granted
     */
    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Open usage access settings
     */
    fun openUsageAccessSettings() {
        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    /**
     * Get app usage for today
     */
    fun getTodayUsageStats(): List<AppUsageInfo> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        android.util.Log.d("UsageStatsHelper", "Query range: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(startTime)} to ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(endTime)}")

        return getUsageStats(startTime, endTime)
    }

    /**
     * Get app usage for a specific date
     */
    fun getUsageStatsForDate(dateMillis: Long): List<AppUsageInfo> {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = dateMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endTime = calendar.timeInMillis

        return getUsageStats(startTime, endTime)
    }

    /**
     * Get app usage for a specific time range by processing events
     * Uses the accurate queryEvents() approach instead of unreliable queryUsageStats()
     */
    fun getUsageStats(startTime: Long, endTime: Long): List<AppUsageInfo> {
        if (!hasUsageStatsPermission()) {
            return emptyList()
        }

        try {
            val usageEvents = usageStatsManager.queryEvents(startTime, endTime)

            val appUsageMap = mutableMapOf<String, Long>()
            val appResumeTimes = mutableMapOf<String, Long>()
            val appLastUsedMap = mutableMapOf<String, Long>()

            // Track Custom Tab sessions separately
            val customTabResumeTimes = mutableMapOf<String, Pair<Long, String>>() // className -> (resumeTime, parentApp)
            var lastNonChromeApp: String? = null

            val event = android.app.usage.UsageEvents.Event()

            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)

                val packageName = event.packageName ?: continue
                val className = event.className ?: ""

                // Check API level to use the correct events
                val isResumeEvent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED
                } else {
                    event.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND
                }

                val isPauseEvent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED
                } else {
                    event.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_BACKGROUND
                }

                // Check if this is Chrome
                val isChromePackage = packageName == "com.android.chrome" ||
                                    packageName == "com.chrome.beta" ||
                                    packageName == "com.chrome.dev" ||
                                    packageName == "com.chrome.canary"

                // Detect Chrome Custom Tabs by class name
                val isChromeCustomTab = isChromePackage &&
                                        (className.contains("CustomTabActivity") ||
                                         className.contains("customtabs"))

                when {
                    isResumeEvent -> {
                        if (isChromeCustomTab) {
                            // This is a Custom Tab - attribute to the last non-Chrome app
                            if (lastNonChromeApp != null) {
                                customTabResumeTimes[className] = Pair(event.timeStamp, lastNonChromeApp!!)
                                android.util.Log.d("UsageStatsHelper", "Custom Tab RESUME: $className launched by $lastNonChromeApp at ${event.timeStamp}")
                            } else {
                                // No parent app - treat as regular Chrome
                                appResumeTimes[packageName] = event.timeStamp
                                appLastUsedMap[packageName] = event.timeStamp
                            }
                        } else {
                            // Regular app
                            appResumeTimes[packageName] = event.timeStamp
                            appLastUsedMap[packageName] = event.timeStamp

                            // Track as potential parent app if not Chrome
                            if (!isChromePackage) {
                                lastNonChromeApp = packageName
                            }
                        }
                    }
                    isPauseEvent -> {
                        if (isChromeCustomTab) {
                            // Custom Tab paused - find its parent app and add duration
                            val sessionInfo = customTabResumeTimes.remove(className)
                            if (sessionInfo != null) {
                                val (resumeTime, parentApp) = sessionInfo
                                val duration = event.timeStamp - resumeTime
                                if (duration > 0) {
                                    val previousTime = appUsageMap.getOrDefault(parentApp, 0L)
                                    appUsageMap[parentApp] = previousTime + duration
                                    appLastUsedMap[parentApp] = event.timeStamp
                                    android.util.Log.d("UsageStatsHelper", "Custom Tab PAUSE: $className - added $duration ms to $parentApp")
                                }
                            } else {
                                // No matching resume - treat as regular Chrome
                                val resumeTime = appResumeTimes.remove(packageName)
                                if (resumeTime != null) {
                                    val duration = event.timeStamp - resumeTime
                                    if (duration > 0) {
                                        val previousTime = appUsageMap.getOrDefault(packageName, 0L)
                                        appUsageMap[packageName] = previousTime + duration
                                        appLastUsedMap[packageName] = event.timeStamp
                                    }
                                }
                            }
                        } else {
                            // Regular app pause
                            val resumeTime = appResumeTimes.remove(packageName)
                            if (resumeTime != null) {
                                val duration = event.timeStamp - resumeTime
                                if (duration > 0) {
                                    val previousTime = appUsageMap.getOrDefault(packageName, 0L)
                                    appUsageMap[packageName] = previousTime + duration
                                    appLastUsedMap[packageName] = event.timeStamp
                                }
                            }
                        }
                    }
                }
            }

            // CRITICAL STEP: Account for apps still in the foreground
            for ((packageName, resumeTime) in appResumeTimes) {
                val duration = endTime - resumeTime
                if (duration > 0) {
                    val previousTime = appUsageMap.getOrDefault(packageName, 0L)
                    appUsageMap[packageName] = previousTime + duration
                    appLastUsedMap[packageName] = endTime
                }
            }

            // First, merge apps using shared UID (works for apps that share UIDs)
            mergeRelatedAppsBySharedUid(appUsageMap, appLastUsedMap)

            // Then, apply manufacturer-agnostic semantic grouping rules
            // This handles cases where manufacturers use different UIDs for related system apps
            mergeSystemAppFamilies(appUsageMap, appLastUsedMap)

            // Filter and return results
            val launchableApps = getLaunchableApps()
            val maxReasonableTime = endTime - startTime

            return appUsageMap
                .filter { (packageName, usageTime) ->
                    usageTime > 0 &&
                    packageName != context.packageName &&
                    packageName != "com.google.android.apps.wellbeing" &&  // Exclude Digital Wellbeing
                    usageTime <= maxReasonableTime &&
                    (launchableApps.contains(packageName) || !isSystemApp(packageName))
                }
                .map { (packageName, usageTime) ->
                    AppUsageInfo(
                        packageName = packageName,
                        appName = getAppName(packageName),
                        usageTimeMillis = usageTime,
                        lastTimeUsed = appLastUsedMap[packageName] ?: endTime,
                        date = startTime
                    )
                }
                .sortedByDescending { it.usageTimeMillis }

        } catch (e: Exception) {
            android.util.Log.e("UsageStatsHelper", "Error getting usage stats", e)
            return emptyList()
        }
    }

    /**
     * Merge system app families based on semantic grouping
     * This handles cases where manufacturers assign different UIDs to related system apps
     * Works across all Android manufacturers (Samsung, OnePlus, Pixel, Xiaomi, etc.)
     */
    private fun mergeSystemAppFamilies(
        appUsageMap: MutableMap<String, Long>,
        appLastUsedMap: MutableMap<String, Long>
    ) {
        android.util.Log.d("UsageStatsHelper", "=== Starting semantic merging ===")

        // Define app families: [primary package, list of related packages to merge into it]
        val appFamilies = listOf(
            // Phone/Contacts/Dialer family - merge all into Contacts (matches Digital Wellbeing)
            "com.android.contacts" to listOf(
                "com.android.phone",
                "com.android.dialer",
                "com.google.android.dialer",
                "com.android.incallui",
                "com.android.server.telecom",
                "com.samsung.android.contacts",
                "com.samsung.android.dialer",
                "com.oneplus.contacts",
                "com.oplus.contacts",
                "com.oplus.aicall",
                "com.miui.contactsphone"
            )
        )

        for ((primaryPackage, relatedPackages) in appFamilies) {
            // Check if primary package exists in usage map
            val primaryExists = appUsageMap.containsKey(primaryPackage)

            var totalUsage = appUsageMap[primaryPackage] ?: 0L
            var latestTime = appLastUsedMap[primaryPackage] ?: 0L
            var mergedAny = false

            for (relatedPkg in relatedPackages) {
                if (appUsageMap.containsKey(relatedPkg)) {
                    val usage = appUsageMap[relatedPkg] ?: 0L
                    val lastUsed = appLastUsedMap[relatedPkg] ?: 0L

                    if (usage > 0) {
                        android.util.Log.d("UsageStatsHelper", "Semantic merge: $relatedPkg ($usage ms) -> $primaryPackage")
                        totalUsage += usage
                        latestTime = maxOf(latestTime, lastUsed)
                        mergedAny = true

                        appUsageMap.remove(relatedPkg)
                        appLastUsedMap.remove(relatedPkg)
                    }
                }
            }

            // Update primary package if we merged anything
            if (mergedAny || primaryExists) {
                appUsageMap[primaryPackage] = totalUsage
                appLastUsedMap[primaryPackage] = latestTime
                android.util.Log.d("UsageStatsHelper", "Total usage for $primaryPackage after semantic merge: $totalUsage ms")
            }
        }

        android.util.Log.d("UsageStatsHelper", "=== Semantic merging complete ===")
    }

    /**
     * Merge related apps by shared UID
     * Apps that share the same UID are part of the same app family and should be merged
     * This handles Phone/Contacts/Dialer/InCallUI across all manufacturers
     */
    private fun mergeRelatedAppsBySharedUid(
        appUsageMap: MutableMap<String, Long>,
        appLastUsedMap: MutableMap<String, Long>
    ) {
        android.util.Log.d("UsageStatsHelper", "=== Starting UID-based merging ===")
        android.util.Log.d("UsageStatsHelper", "Apps before merging: ${appUsageMap.keys.joinToString(", ")}")

        // Group packages by their shared UID
        val uidGroups = mutableMapOf<Int, MutableList<String>>()

        for (packageName in appUsageMap.keys.toList()) {
            try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                val uid = appInfo.uid
                val usage = appUsageMap[packageName] ?: 0L

                android.util.Log.d("UsageStatsHelper", "Package: $packageName, UID: $uid, Usage: $usage ms")
                uidGroups.getOrPut(uid) { mutableListOf() }.add(packageName)
            } catch (e: PackageManager.NameNotFoundException) {
                android.util.Log.w("UsageStatsHelper", "Package not found: $packageName")
            }
        }

        android.util.Log.d("UsageStatsHelper", "Found ${uidGroups.size} unique UIDs")

        // For each UID group with multiple packages, merge them into the "primary" package
        for ((uid, packages) in uidGroups) {
            if (packages.size <= 1) continue  // No merging needed

            android.util.Log.d("UsageStatsHelper", "UID $uid has ${packages.size} packages: ${packages.joinToString(", ")}")

            // Find the "primary" package - prefer the one with launcher activity
            val primaryPackage = packages.firstOrNull { pkg ->
                try {
                    val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
                    val hasLauncher = launchIntent != null
                    if (hasLauncher) {
                        android.util.Log.d("UsageStatsHelper", "  $pkg HAS launcher activity")
                    }
                    hasLauncher
                } catch (e: Exception) {
                    false
                }
            } ?: packages.first()  // Fallback to first package

            android.util.Log.d("UsageStatsHelper", "  Primary package selected: $primaryPackage")

            // Merge all other packages into the primary one
            var totalUsage = appUsageMap[primaryPackage] ?: 0L
            var latestTime = appLastUsedMap[primaryPackage] ?: 0L

            for (pkg in packages) {
                if (pkg == primaryPackage) continue

                val usage = appUsageMap[pkg] ?: 0L
                val lastUsed = appLastUsedMap[pkg] ?: 0L

                if (usage > 0) {
                    android.util.Log.d("UsageStatsHelper", "  Merging $pkg ($usage ms) into $primaryPackage")
                    totalUsage += usage
                    latestTime = maxOf(latestTime, lastUsed)

                    appUsageMap.remove(pkg)
                    appLastUsedMap.remove(pkg)
                }
            }

            // Update primary package with merged data
            if (totalUsage > 0) {
                android.util.Log.d("UsageStatsHelper", "  Final merged usage for $primaryPackage: $totalUsage ms")
                appUsageMap[primaryPackage] = totalUsage
                appLastUsedMap[primaryPackage] = latestTime
            }
        }

        android.util.Log.d("UsageStatsHelper", "=== UID merging complete ===")
        android.util.Log.d("UsageStatsHelper", "Apps after merging: ${appUsageMap.keys.joinToString(", ")}")
    }

    /**
     * Get all launchable apps (apps that appear in launcher)
     */
    private fun getLaunchableApps(): Set<String> {
        return try {
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            packageManager.queryIntentActivities(intent, 0)
                .map { it.activityInfo.packageName }
                .toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    /**
     * Get app name from package name
     */
    private fun getAppName(packageName: String): String {
        return try {
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    /**
     * Check if app is a pure system app (not user-facing)
     * We want to exclude only background system services, not apps like Chrome, Gmail, etc.
     */
    private fun isSystemApp(packageName: String): Boolean {
        return try {
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            val isSystem = (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isUpdatedSystem = (applicationInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

            // Include updated system apps (like Chrome, Gmail) - these are user-facing
            if (isUpdatedSystem) {
                return false
            }

            // Exclude pure system apps that are not launchable
            isSystem
        } catch (e: PackageManager.NameNotFoundException) {
            true
        }
    }

    /**
     * Get total screen time for today
     */
    fun getTodayTotalUsage(): Long {
        return getTodayUsageStats().sumOf { it.usageTimeMillis }
    }
}
