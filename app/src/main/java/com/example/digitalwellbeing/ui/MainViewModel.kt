package com.example.digitalwellbeing.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digitalwellbeing.data.model.AppUsageInfo
import com.example.digitalwellbeing.data.model.ChallengeType
import com.example.digitalwellbeing.data.model.DailyStats
import com.example.digitalwellbeing.data.model.Task
import com.example.digitalwellbeing.data.repository.ChallengeRepository
import com.example.digitalwellbeing.data.repository.TaskRepository
import com.example.digitalwellbeing.data.repository.UsageRepository
import com.example.digitalwellbeing.util.AccessibilityUtils
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
    val setupCompleted: Boolean = false
)

data class StatsUiState(
    val allApps: List<AppUsageInfo> = emptyList(),
    val totalUsage: String = "0h 0m",
    val dailyGoal: Long = 6 * 60 * 60 * 1000, // 6 hours
    val progress: Float = 0f,
    val weeklyStats: List<Pair<String, Long>> = emptyList(), // (day label, usage millis)
    val currentWeekDate: String = "",
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
    val tasksCompleted: Int
)

class MainViewModel(
    private val context: Context,
    private val usageRepository: UsageRepository,
    private val challengeRepository: ChallengeRepository,
    private val taskRepository: TaskRepository,
    private val appLimitRepository: com.example.digitalwellbeing.data.repository.AppLimitRepository,
    private val appPreferencesRepository: com.example.digitalwellbeing.data.repository.AppPreferencesRepository
) : ViewModel() {

    private val _usagePermissionGranted = MutableStateFlow(usageRepository.hasUsagePermission())
    val usagePermissionGranted: StateFlow<Boolean> = _usagePermissionGranted.asStateFlow()

    private val _accessibilityRefresh = MutableStateFlow(0) // Trigger to refresh accessibility state

    private val _selectedWeekOffset = MutableStateFlow(0) // 0 = current week, -1 = last week, etc.

    private val dashboardData: Flow<DashboardData> = combine(
        usageRepository.getTodayStats(),
        usageRepository.getYesterdayStats(),
        usageRepository.getTopUsedApps(3),
        challengeRepository.getTodayCompletedCount(),
        taskRepository.getTodayCompletedCount()
    ) { today, yesterday, topApps, challengesCompleted, tasksCompleted ->
        DashboardData(
            today = today,
            yesterday = yesterday,
            topApps = topApps,
            challengesCompleted = challengesCompleted,
            tasksCompleted = tasksCompleted
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
        val percentageChange = if (hasPermission) {
            data.today?.percentageChange(data.yesterday) ?: 0
        } else {
            0
        }
        val limitsMap = limits.associate { it.packageName to it.limitMillis }
        val hasTimersSet = limits.isNotEmpty()
        val accessibilityEnabled = AccessibilityUtils.isAccessibilityServiceEnabled(context)

        // Debug logging
        android.util.Log.d("Dashboard", "Limits count: ${limits.size}, hasTimersSet: $hasTimersSet, accessibilityEnabled: $accessibilityEnabled")
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
            setupCompleted = setupCompleted
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(usagePermissionGranted = _usagePermissionGranted.value)
    )

    // Stats state with weekly data
    val statsState: StateFlow<StatsUiState> = _selectedWeekOffset.flatMapLatest { weekOffset ->
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

        combine(
            usageRepository.getTodayAppUsage(),
            usageRepository.getWeeklyStats(weekStartDate),
            _usagePermissionGranted,
            appLimitRepository.getEnabledLimits()
        ) { apps, weeklyDailyStats, hasPermission, limits ->
            val visibleApps = if (hasPermission) apps else emptyList()
            val totalUsage = visibleApps.sumOf { it.usageTimeMillis }
            val dailyGoal = 6L * 60 * 60 * 1000 // 6 hours
            val progress = if (hasPermission) {
                (totalUsage.toFloat() / dailyGoal).coerceAtMost(1f)
            } else {
                0f
            }

            val hours = totalUsage / (1000 * 60 * 60)
            val minutes = (totalUsage % (1000 * 60 * 60)) / (1000 * 60)
            val totalUsageFormatted = "${hours}h ${minutes}m"

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
                allApps = visibleApps,
                totalUsage = totalUsageFormatted,
                dailyGoal = dailyGoal,
                progress = progress,
                weeklyStats = weeklyStats,
                currentWeekDate = weekDateText,
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
        syncUsageData()
        // Sync past week data on init for historical view
        syncHistoricalData()
    }

    fun syncUsageData() {
        viewModelScope.launch {
            val hasPermission = usageRepository.hasUsagePermission()
            _usagePermissionGranted.value = hasPermission
            if (hasPermission) {
                usageRepository.syncUsageData()
            }
        }
    }

    private fun syncHistoricalData() {
        viewModelScope.launch {
            val hasPermission = usageRepository.hasUsagePermission()
            if (hasPermission) {
                android.util.Log.d("MainViewModel", "Syncing past week data...")
                usageRepository.syncPastWeekData()
                android.util.Log.d("MainViewModel", "Past week sync complete")
            }
        }
    }

    fun navigateToPreviousWeek() {
        _selectedWeekOffset.value -= 1
    }

    fun navigateToNextWeek() {
        if (_selectedWeekOffset.value < 0) {
            _selectedWeekOffset.value += 1
        }
    }

    fun onAppResumed() {
        syncUsageData()
        // Refresh accessibility state
        _accessibilityRefresh.value += 1
    }

    fun requestUsagePermission() {
        usageRepository.openUsageAccessSettings()
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

    fun setAppTimer(packageName: String, appName: String, limitMinutes: Int) {
        viewModelScope.launch {
            val limitMillis = limitMinutes * 60 * 1000L
            android.util.Log.d("Dashboard", "Setting timer: $appName ($packageName) - $limitMinutes minutes ($limitMillis ms)")
            appLimitRepository.setAppLimit(packageName, appName, limitMillis)
            android.util.Log.d("Dashboard", "Timer set successfully")
        }
    }

    fun removeAppTimer(packageName: String) {
        viewModelScope.launch {
            val limit = appLimitRepository.getLimitForApp(packageName).first()
            limit?.let {
                appLimitRepository.deleteLimit(it)
            }
        }
    }

    fun requestAccessibilityPermission() {
        AccessibilityUtils.openAccessibilitySettings(context)
    }

    fun markSetupCompleted() {
        viewModelScope.launch {
            appPreferencesRepository.markSetupCompleted()
        }
    }

    fun isSetupCompleted(): Flow<Boolean> {
        return appPreferencesRepository.isSetupCompleted()
    }

    /**
     * Get app usage for a specific date (for bar chart clicks)
     */
    fun getAppUsageForDay(dayIndex: Int, weekOffset: Int = 0): Flow<List<com.example.digitalwellbeing.data.model.AppUsageInfo>> {
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

        return usageRepository.getAppUsageForDate(dateMillis)
    }
}
