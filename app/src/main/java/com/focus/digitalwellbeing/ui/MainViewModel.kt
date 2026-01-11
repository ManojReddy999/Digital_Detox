package com.focus.digitalwellbeing.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focus.digitalwellbeing.data.model.AppUsageInfo
import com.focus.digitalwellbeing.data.model.ChallengeType
import com.focus.digitalwellbeing.data.model.DailyStats
import com.focus.digitalwellbeing.data.model.Task
import com.focus.digitalwellbeing.data.repository.AppTheme
import com.focus.digitalwellbeing.data.repository.ChallengeRepository
import com.focus.digitalwellbeing.data.repository.TaskRepository
import com.focus.digitalwellbeing.data.repository.UsageRepository
import com.focus.digitalwellbeing.data.repository.FocusRepository
import com.focus.digitalwellbeing.data.model.FocusGroup
import com.focus.digitalwellbeing.data.model.FocusSession
import com.focus.digitalwellbeing.util.AccessibilityUtils
import com.focus.digitalwellbeing.util.DateUtils
import com.focus.digitalwellbeing.util.OverlayPermissionUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DashboardUiState(
    val todayStats: DailyStats? = null,
    val yesterdayStats: DailyStats? = null,
    val topApps: List<AppUsageInfo> = emptyList(),
    val challengesCompleted: Int = 0,
    val tasksCompletedCount: Int = 0,
    val totalTasks: Int = 0,
    val percentageChange: Int = 0,
    val usagePermissionGranted: Boolean = false,
    val isLoading: Boolean = true,
    val appLimits: Map<String, Long> = emptyMap(), // packageName -> limitMillis
    val hasTimersSet: Boolean = false,
    val accessibilityEnabled: Boolean = false,
    val overlayPermissionGranted: Boolean = false,
    val setupCompleted: Boolean = false,
    val averageDailyUsage: Long = 0L
)

data class StatsUiState(
    val allApps: List<AppUsageInfo> = emptyList(),
    val totalUsage: String = "0h 0m",
    val dailyGoal: Long = 6 * 60 * 60 * 1000, // 6 hours
    val progress: Float = 0f,
    val weeklyStats: List<Pair<String, Long>> = emptyList(), // (day label, usage millis)
    val currentWeekDate: String = "",
    val weekStartDate: Long = 0L,
    val selectedDayIndex: Int = 0,
    val selectedDayApps: List<AppUsageInfo> = emptyList(),
    val canGoNextWeek: Boolean = false,
    val usagePermissionGranted: Boolean = false,
    val isLoading: Boolean = true,
    val appLimits: Map<String, Long> = emptyMap() // packageName -> limitMillis
)

data class TasksUiState(
    val tasks: List<Task> = emptyList(),
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val progress: Float = 0f,
    val isLoading: Boolean = true
)

private data class DashboardData(
    val today: DailyStats?,
    val yesterday: DailyStats?,
    val topApps: List<AppUsageInfo>,
    val challengesCompleted: Int,
    val tasksCompleted: Int,
    val averageDailyUsage: Long
)

class MainViewModel(
    private val context: Context,
    private val usageRepository: UsageRepository,
    private val challengeRepository: ChallengeRepository,
    private val taskRepository: TaskRepository,
    private val appLimitRepository: com.focus.digitalwellbeing.data.repository.AppLimitRepository,
    private val appPreferencesRepository: com.focus.digitalwellbeing.data.repository.AppPreferencesRepository,
    private val coinWalletRepository: com.focus.digitalwellbeing.data.repository.CoinWalletRepository,
    private val streakRepository: com.focus.digitalwellbeing.data.repository.StreakRepository,
    private val focusRepository: FocusRepository,
    private val billingManager: com.focus.digitalwellbeing.data.billing.BillingManager
) : ViewModel() {

    private val _usagePermissionGranted = MutableStateFlow(usageRepository.hasUsagePermission())
    val usagePermissionGranted: StateFlow<Boolean> = _usagePermissionGranted.asStateFlow()

    private val _accessibilityRefresh = MutableStateFlow(0) // Trigger to refresh accessibility state

    private val _selectedWeekOffset = MutableStateFlow(0) // 0 = current week, -1 = last week, etc.
    private val _selectedDayIndex = MutableStateFlow(0) // 0-6 (Mon-Sun)

    // Theme state
    val currentTheme: StateFlow<AppTheme> = appPreferencesRepository.getTheme()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppTheme.SYSTEM
        )

    // Coin state
    val coinBalance: StateFlow<Int> = coinWalletRepository.getBalance()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // Focus state
    val focusGroups: StateFlow<List<FocusGroup>> = focusRepository.getAllFocusGroups()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeFocusSession: StateFlow<FocusSession?> = focusRepository.activeSession
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val mostRecentFocusSession: StateFlow<FocusSession?> = focusRepository.mostRecentSession
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // User Profile state
    val userName: StateFlow<String?> = appPreferencesRepository.getUserName()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val userProfileUri: StateFlow<String?> = appPreferencesRepository.getUserProfileUri()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Streak state
    val streakCount: StateFlow<Int> = streakRepository.getStreak(com.focus.digitalwellbeing.data.model.GoalType.SCREEN_TIME_TARGET)
        .map { it?.currentStreak ?: 0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
        
    fun toggleTheme() {
        val newTheme = if (currentTheme.value == AppTheme.DARK) AppTheme.LIGHT else AppTheme.DARK
        setTheme(newTheme)
    }

    // Initialize wallet on first run
    init {
        viewModelScope.launch {
            try {
                coinWalletRepository.initializeWallet()
            } catch (e: Exception) {
                // Wallet already exists
            }
        }
        
        // Connect billing
        billingManager.startConnection()
    }

    // Billing state
    val billingFlowInProcess = billingManager.billingFlowInProcess
    
    fun launchPurchaseFlow(activity: android.app.Activity, skuId: String) {
        billingManager.launchPurchaseFlow(activity, skuId)
    }

    private val dashboardData: Flow<DashboardData> = combine(
        usageRepository.getTodayStats(),
        usageRepository.getYesterdayStats(),
        usageRepository.getWeeklyAverageTopApps(3),
        challengeRepository.getTodayCompletedCount(),
        taskRepository.getTodayCompletedCount(),
        usageRepository.getRecentStats(7)
    ) { args: Array<Any?> ->
        val today = args[0] as? DailyStats
        val yesterday = args[1] as? DailyStats
        @Suppress("UNCHECKED_CAST")
        val topApps = args[2] as? List<AppUsageInfo> ?: emptyList()
        val challengesCompleted = args[3] as? Int ?: 0
        val tasksCompleted = args[4] as? Int ?: 0
        @Suppress("UNCHECKED_CAST")
        val recentStats = args[5] as? List<DailyStats> ?: emptyList()

        val totalRecentUsage = recentStats.sumOf { it.totalUsageTimeMillis }
        val daysCount = if (recentStats.isNotEmpty()) recentStats.size else 1
        val averageUsage = totalRecentUsage / daysCount

        DashboardData(
            today = today,
            yesterday = yesterday,
            topApps = topApps,
            challengesCompleted = challengesCompleted,
            tasksCompleted = tasksCompleted,
            averageDailyUsage = averageUsage
        )
    }

    // Dashboard state
    val dashboardState: StateFlow<DashboardUiState> = combine(
        dashboardData,
        _usagePermissionGranted,
        appLimitRepository.getEnabledLimits(),
        _accessibilityRefresh,
        appPreferencesRepository.isSetupCompleted()
    ) { data, hasPermission, limits, _, setupCompleted ->
        android.util.Log.d("MainViewModel", "dashboardState combine: hasPermission=$hasPermission, limits=${limits.size}, setupCompleted=$setupCompleted")
        val percentageChange = if (hasPermission) {
            data.today?.percentageChange(data.yesterday) ?: 0
        } else {
            0
        }
        val limitsMap = limits.associate { it.packageName to it.limitMillis }
        val hasTimersSet = limits.isNotEmpty()
        val accessibilityEnabled = AccessibilityUtils.isAccessibilityServiceEnabled(context)
        val overlayPermissionGranted = OverlayPermissionUtils.canDrawOverlays(context)

        // Debug logging
        android.util.Log.d("Dashboard", "Limits count: ${limits.size}, hasTimersSet: $hasTimersSet, accessibilityEnabled: $accessibilityEnabled, overlayPermission: $overlayPermissionGranted")
        limits.forEach { limit ->
            android.util.Log.d("Dashboard", "Limit: ${limit.appName} - ${limit.limitMillis}ms (enabled: ${limit.isEnabled})")
        }

        DashboardUiState(
            todayStats = if (hasPermission) data.today else null,
            yesterdayStats = if (hasPermission) data.yesterday else null,
            topApps = if (hasPermission) data.topApps else emptyList(),
            challengesCompleted = data.challengesCompleted,
            tasksCompletedCount = data.tasksCompleted,
            totalTasks = 12, // Placeholder - will be updated
            percentageChange = percentageChange,
            usagePermissionGranted = hasPermission,
            isLoading = false,
            appLimits = limitsMap,
            hasTimersSet = hasTimersSet,
            accessibilityEnabled = accessibilityEnabled,
            overlayPermissionGranted = overlayPermissionGranted,
            setupCompleted = setupCompleted,
            averageDailyUsage = if (hasPermission) data.averageDailyUsage else 0L
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(usagePermissionGranted = _usagePermissionGranted.value)
    )

    // Stats state with weekly data
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val statsState: StateFlow<StatsUiState> = combine(
        _selectedWeekOffset,
        _selectedDayIndex,
        _usagePermissionGranted,
        appLimitRepository.getEnabledLimits()
    ) { weekOffset, dayIndex, hasPermission, limits ->
        Triple(weekOffset, dayIndex, hasPermission to limits)
    }.flatMapLatest { (weekOffset, dayIndex, pair) ->
        val (hasPermission, limits) = pair
        
        // Calculate week start date based on offset
        val calendar = java.util.Calendar.getInstance()
        calendar.firstDayOfWeek = java.util.Calendar.MONDAY
        calendar.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        calendar.add(java.util.Calendar.WEEK_OF_YEAR, weekOffset)
        val weekStartDate = calendar.timeInMillis

        // Calculate selected day date
        val selectedDayCal = java.util.Calendar.getInstance()
        selectedDayCal.timeInMillis = weekStartDate
        selectedDayCal.add(java.util.Calendar.DAY_OF_MONTH, dayIndex)
        val selectedDayDate = selectedDayCal.timeInMillis

        combine(
            usageRepository.getAppUsageForDate(selectedDayDate),
            usageRepository.getWeeklyStats(weekStartDate)
        ) { selectedDayApps, weeklyDailyStats ->
            
            // Calculate total usage for the selected day
            val totalUsage = if (hasPermission) {
                selectedDayApps.sumOf { it.usageTimeMillis }
            } else {
                0L
            }
            
            val dailyGoal = 6L * 60 * 60 * 1000 // 6 hours
            val progress = if (hasPermission) {
                (totalUsage.toFloat() / dailyGoal).coerceAtMost(1f)
            } else {
                0f
            }

            val totalUsageFormatted = DateUtils.formatDuration(totalUsage)

            // Get week end date for display
            val endCal = java.util.Calendar.getInstance()
            endCal.timeInMillis = weekStartDate
            endCal.add(java.util.Calendar.DAY_OF_MONTH, 6)

            val dateFormat = java.text.SimpleDateFormat("d MMM", java.util.Locale.getDefault())
            val weekDateText = if (weekOffset == 0) {
                "Today"
            } else {
                val startStr = dateFormat.format(weekStartDate)
                val endStr = dateFormat.format(endCal.timeInMillis)
                "$startStr - $endStr"
            }

            // Convert DailyStats to weekly bar chart data
            val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            val weeklyStats = weeklyDailyStats.mapIndexed { index, dailyStat ->
                dayLabels[index] to (dailyStat.totalUsageTimeMillis)
            }

            val limitsMap = limits.associate { it.packageName to it.limitMillis }

            StatsUiState(
                allApps = if (hasPermission) selectedDayApps else emptyList(),
                totalUsage = totalUsageFormatted,
                dailyGoal = dailyGoal,
                progress = progress,
                weeklyStats = weeklyStats,
                currentWeekDate = weekDateText,
                weekStartDate = weekStartDate,
                selectedDayIndex = dayIndex,
                selectedDayApps = if (hasPermission) selectedDayApps else emptyList(),
                canGoNextWeek = weekOffset < 0,
                usagePermissionGranted = hasPermission,
                isLoading = false,
                appLimits = limitsMap
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatsUiState(usagePermissionGranted = _usagePermissionGranted.value)
    )

    // Tasks state
    val tasksState: StateFlow<TasksUiState> = taskRepository.getAllTasks()
        .map { tasks ->
            val completed = tasks.count { it.isCompleted }
            val total = tasks.size
            val progress = if (total > 0) completed.toFloat() / total else 0f

            TasksUiState(
                tasks = tasks,
                completedCount = completed,
                totalCount = total,
                progress = progress,
                isLoading = false
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TasksUiState()
        )

    init {
        // Initialize selected day to today
        val calendar = java.util.Calendar.getInstance()
        calendar.firstDayOfWeek = java.util.Calendar.MONDAY
        val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
        // Convert Sunday (1) to 6, Monday (2) to 0, etc.
        val todayIndex = if (dayOfWeek == java.util.Calendar.SUNDAY) 6 else dayOfWeek - 2
        _selectedDayIndex.value = todayIndex

        syncUsageData()
        // Sync past week data on init for historical view
        syncHistoricalData()
    }

    fun syncUsageData() {
        viewModelScope.launch {
            val hasPermission = usageRepository.hasUsagePermission()
            android.util.Log.d("MainViewModel", "syncUsageData: hasPermission=$hasPermission")
            _usagePermissionGranted.value = hasPermission
            if (hasPermission) {
                android.util.Log.d("MainViewModel", "Syncing usage data...")
                try {
                    usageRepository.syncUsageData()
                    android.util.Log.d("MainViewModel", "Usage data sync completed")

                    // Add a small delay then force refresh to ensure UI updates
                    kotlinx.coroutines.delay(500)
                    _usagePermissionGranted.value = hasPermission // Force UI refresh
                } catch (e: Exception) {
                    android.util.Log.e("MainViewModel", "Error syncing usage data", e)
                }
            } else {
                android.util.Log.w("MainViewModel", "No usage permission, skipping sync")
            }
        }
    }

    private fun syncHistoricalData() {
        viewModelScope.launch {
            val hasPermission = usageRepository.hasUsagePermission()
            if (hasPermission) {
                android.util.Log.d("MainViewModel", "Syncing all available historical data...")
                usageRepository.syncAllAvailableData()
                android.util.Log.d("MainViewModel", "Historical data sync complete")
            }
        }
    }

    fun refreshHistoricalData() {
        syncHistoricalData()
    }

    fun navigateToPreviousWeek() {
        _selectedWeekOffset.value -= 1
        // When swiping to previous week, keep the same day index or default to Sunday?
        // User didn't specify for swipe, but for arrow "left arrow ... go to previous week" (from Monday).
        // If we swipe, we probably want to keep the same day or go to last day?
        // Let's keep the same day index for swipe continuity, unless it's future.
        // Actually, for swipe, usually we just change the week.
    }

    fun navigateToNextWeek() {
        if (_selectedWeekOffset.value < 0) {
            _selectedWeekOffset.value += 1
        }
    }

    fun navigateToPreviousDay() {
        val currentDay = _selectedDayIndex.value
        if (currentDay > 0) {
            _selectedDayIndex.value = currentDay - 1
        } else {
            // Go to previous week, Sunday
            _selectedWeekOffset.value -= 1
            _selectedDayIndex.value = 6
        }
    }

    fun navigateToNextDay() {
        val currentDay = _selectedDayIndex.value
        
        // Check if we are at the limit (Today)
        if (_selectedWeekOffset.value == 0) {
            val calendar = java.util.Calendar.getInstance()
            calendar.firstDayOfWeek = java.util.Calendar.MONDAY
            val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
            val todayIndex = if (dayOfWeek == java.util.Calendar.SUNDAY) 6 else dayOfWeek - 2
            
            if (currentDay >= todayIndex) {
                return // Cannot go to future
            }
        }

        if (currentDay < 6) {
            _selectedDayIndex.value = currentDay + 1
        } else {
            // Go to next week, Monday
            if (_selectedWeekOffset.value < 0) {
                _selectedWeekOffset.value += 1
                _selectedDayIndex.value = 0
            }
        }
    }

    fun selectDay(dayIndex: Int) {
        // Prevent selecting future days
        if (_selectedWeekOffset.value == 0) {
            val calendar = java.util.Calendar.getInstance()
            calendar.firstDayOfWeek = java.util.Calendar.MONDAY
            val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
            val todayIndex = if (dayOfWeek == java.util.Calendar.SUNDAY) 6 else dayOfWeek - 2
            
            if (dayIndex > todayIndex) {
                return
            }
        }
        _selectedDayIndex.value = dayIndex
    }

    fun onAppResumed() {
        // Force refresh permission state and sync data (including history)
        forceRefreshUsageData()
        
        // Refresh accessibility state
        _accessibilityRefresh.value += 1
    }
    
    /**
     * Force refresh usage data - can be called manually
     */
    fun forceRefreshUsageData() {
        viewModelScope.launch {
            android.util.Log.d("MainViewModel", "forceRefreshUsageData called")
            val hasPermission = usageRepository.hasUsagePermission()
            _usagePermissionGranted.value = hasPermission
            if (hasPermission) {
                usageRepository.syncUsageData()
                // Also sync historical data to ensure we have complete data
                syncHistoricalData()
            }
        }
    }

    fun requestUsagePermission() {
        usageRepository.openUsageAccessSettings()
    }

    fun requestAccessibilityPermission() {
        AccessibilityUtils.openAccessibilitySettings(context)
    }

    fun requestOverlayPermission() {
        OverlayPermissionUtils.requestOverlayPermission(context)
    }



    fun addTask(title: String) {
        viewModelScope.launch {
            taskRepository.addTask(title)
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            taskRepository.toggleTaskCompletion(task)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskRepository.deleteTask(task)
        }
    }

    fun completeChallenge(type: ChallengeType, timeTakenSeconds: Int = 0) {
        viewModelScope.launch {
            challengeRepository.completeChallenge(type, timeTakenSeconds)
        }
    }

    fun setAppTimer(packageName: String, appName: String, limitMillis: Long) {
        viewModelScope.launch {
            android.util.Log.d("Dashboard", "Setting timer: $appName ($packageName) - $limitMillis ms")

            // Check if there's an existing timer
            val existingLimit = appLimitRepository.getLimitForApp(packageName).first()

            if (existingLimit != null && limitMillis > existingLimit.limitMillis) {
                // User is increasing the timer, charge coins
                val differenceMillis = limitMillis - existingLimit.limitMillis
                val differenceMinutes = (differenceMillis / (60 * 1000)).toInt()

                if (differenceMinutes > 0) {
                    val success = coinWalletRepository.chargeCoins(
                        amount = differenceMinutes,
                        reason = "Increased timer for $appName by $differenceMinutes minutes",
                        category = com.focus.digitalwellbeing.data.model.TransactionCategory.TIMER_ADJUST
                    )

                    if (!success) {
                        android.util.Log.d("Dashboard", "Insufficient coins to increase timer")
                        return@launch
                    }
                }
            }

            // Set the app limit
            appLimitRepository.setAppLimit(packageName, appName, limitMillis)
            android.util.Log.d("Dashboard", "Timer set successfully")
        }
    }

    fun removeAppTimer(packageName: String) {
        viewModelScope.launch {
            val currentLimit = appLimitRepository.getLimitForApp(packageName).first()
            
            if (currentLimit != null) {
                // Charge 100 coins to delete
                val success = coinWalletRepository.chargeCoins(
                    amount = 100,
                    reason = "Deleted timer for ${currentLimit.appName}",
                    category = com.focus.digitalwellbeing.data.model.TransactionCategory.TIMER_DELETED
                )
                
                if (!success) {
                    // Insufficient balance
                    return@launch
                }
                
                // Delete the timer
                appLimitRepository.deleteLimit(currentLimit)
            }
        }
    }



    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            appPreferencesRepository.setTheme(theme)
        }
    }

    fun markSetupCompleted() {
        viewModelScope.launch {
            appPreferencesRepository.markSetupCompleted()
        }
    }

    fun saveUserProfile(name: String, profileUri: String?) {
        viewModelScope.launch {
            appPreferencesRepository.saveUserProfile(name, profileUri)
            appPreferencesRepository.markSetupCompleted()
        }
    }

    fun isSetupCompleted(): Flow<Boolean> {
        return appPreferencesRepository.isSetupCompleted()
    }

    /**
     * Get app usage for a specific date (for bar chart clicks)
     */
    fun getAppUsageForDay(dayIndex: Int): Flow<List<com.focus.digitalwellbeing.data.model.AppUsageInfo>> {
        // Use the current selected week offset
        val weekOffset = _selectedWeekOffset.value

        // Calculate the date for the specific day
        val calendar = java.util.Calendar.getInstance()
        calendar.firstDayOfWeek = java.util.Calendar.MONDAY
        calendar.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        calendar.add(java.util.Calendar.WEEK_OF_YEAR, weekOffset)
        calendar.add(java.util.Calendar.DAY_OF_MONTH, dayIndex)
        val dateMillis = calendar.timeInMillis

        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        android.util.Log.d("MainViewModel", "getAppUsageForDay: dayIndex=$dayIndex, weekOffset=$weekOffset, date=${sdf.format(dateMillis)}, dateMillis=$dateMillis")

        return usageRepository.getAppUsageForDate(dateMillis)
    }
    fun checkUsageStatsPermission() {
        val intent = android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    /**
     * Get today's coin statistics (rewards vs charges)
     * Returns Pair of (rewards, charges)
     */
    suspend fun getTodayCoinStats(): Pair<Int, Int> {
        return coinWalletRepository.getTodayStats()
    }

    /**
     * Get total coin statistics (all-time earned vs spent)
     * Returns Pair of (earned, spent)
     */
    suspend fun getTotalCoinStats(): Pair<Int, Int> {
        return coinWalletRepository.getTotalStats()
    }

    /**
     * Get app limit for a specific package
     */
    suspend fun getAppLimit(packageName: String): com.focus.digitalwellbeing.data.model.AppLimit? {
        return appLimitRepository.getLimitForApp(packageName).first()
    }

    /**
     * Check if all required permissions are granted
     */
    fun hasRequiredPermissions(): Boolean {
        val currentState = dashboardState.value
        return currentState.usagePermissionGranted == true &&
               currentState.overlayPermissionGranted == true
    }

    private fun calculateTimerCost(currentLimitMillis: Long?, newLimitMillis: Long): Int {
        if (currentLimitMillis == null) {
            // Setting NEW timer is FREE
            return 0
        }

        val diff = newLimitMillis - currentLimitMillis
        if (diff <= 0) {
            // Reducing timer is free
            return 0
        }

        // Adding time costs 1 coin per minute
        return (diff / (60 * 1000)).toInt()
    }

    private fun formatDuration(millis: Long): String {
        return DateUtils.formatDuration(millis)
    }

    // Focus Session Methods
    fun createFocusGroup(focusGroup: FocusGroup) {
        viewModelScope.launch {
            focusRepository.createFocusGroup(focusGroup)
        }
    }
    
    fun updateFocusGroup(focusGroup: FocusGroup) {
        viewModelScope.launch {
            focusRepository.updateFocusGroup(focusGroup)
        }
    }

    fun deleteFocusGroup(focusGroup: FocusGroup) {
        viewModelScope.launch {
            focusRepository.deleteFocusGroup(focusGroup)
        }
    }

    suspend fun getFocusGroupById(id: Long): FocusGroup? {
        return focusRepository.getFocusGroupById(id)
    }

    fun startFocusSession(focusGroup: FocusGroup, durationMinutes: Int? = null) {
        viewModelScope.launch {
            val durationMillis = durationMinutes?.let { it * 60 * 1000L }
            focusRepository.startSession(focusGroup, durationMillis)
        }
    }

    fun stopFocusSession() {
        viewModelScope.launch {
            focusRepository.stopSession()
        }
    }

    fun stopFocusSessionWithCharge() {
        viewModelScope.launch {
            // Get the current active session
            val currentSession = activeFocusSession.value

            if (currentSession != null && currentSession.isActive && currentSession.endTime != null) {
                // Calculate remaining time
                val currentTime = System.currentTimeMillis()
                val remainingMillis = currentSession.endTime - currentTime
                val remainingMinutes = (remainingMillis / (60 * 1000)).toInt().coerceAtLeast(0)

                if (remainingMinutes > 0) {
                    // Charge coins for early termination
                    val success = coinWalletRepository.chargeCoins(
                        amount = remainingMinutes,
                        reason = "Early termination of focus session '${currentSession.focusGroupName}' with $remainingMinutes minutes remaining",
                        category = com.focus.digitalwellbeing.data.model.TransactionCategory.FOCUS_SKIP
                    )

                    if (!success) {
                        android.util.Log.d("MainViewModel", "Insufficient coins to stop session early")
                        return@launch
                    }
                }
            }

            // Stop the session
            focusRepository.stopSession()
        }
    }

    fun getAppsOrderedByUsage(): List<Pair<String, String>> {
        // Get all apps with usage stats from the last 7 days
        val endTime = System.currentTimeMillis()
        val startTime = endTime - (7 * 24 * 60 * 60 * 1000L) // 7 days ago
        
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? android.app.usage.UsageStatsManager
        val usageStats = usageStatsManager?.queryUsageStats(
            android.app.usage.UsageStatsManager.INTERVAL_WEEKLY,
            startTime,
            endTime
        ) ?: return emptyList()
        
        // Calculate average daily usage for each app
        val appUsageMap = usageStats
            .filter { it.totalTimeInForeground > 0 }
            .groupBy { it.packageName }
            .mapValues { (_, stats) ->
                stats.sumOf { it.totalTimeInForeground } / 7 // Average per day over 7 days
            }
        
        // Get installed apps with launcher intents
        val mainIntent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        val packageManager = context.packageManager
        val installedApps = packageManager.queryIntentActivities(mainIntent, 0)
        
        // Create list of (packageName, appName) pairs
        val appList = installedApps.mapNotNull { resolveInfo ->
            val packageName = resolveInfo.activityInfo.packageName
            val appName = resolveInfo.loadLabel(packageManager).toString()
            packageName to appName
        }
        
        // Sort by usage (most used first), then alphabetically for apps with no usage
        return appList.sortedWith(
            compareByDescending<Pair<String, String>> { (packageName, _) ->
                appUsageMap[packageName] ?: 0L
            }.thenBy { (_, appName) ->
                appName
            }
        )
    }
}

